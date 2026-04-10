/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.manager.rm.service

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.manager.am.conf.AMConfiguration
import org.apache.linkis.manager.am.conf.AMConfiguration.{
  SUPPORT_CLUSTER_RULE_EC_TYPES,
  YARN_QUEUE_NAME_CONFIG_KEY
}
import org.apache.linkis.manager.am.vo.CanCreateECRes
import org.apache.linkis.manager.common.conf.RMConfiguration
import org.apache.linkis.manager.common.constant.RMConstant
import org.apache.linkis.manager.common.entity.resource._
import org.apache.linkis.manager.common.errorcode.ManagerCommonErrorCodeSummary._
import org.apache.linkis.manager.common.exception.{RMErrorException, RMWarnException}
import org.apache.linkis.manager.common.protocol.engine.EngineCreateRequest
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.em.EMInstanceLabel
import org.apache.linkis.manager.label.utils.LabelUtil
import org.apache.linkis.manager.rm.domain.RMLabelContainer
import org.apache.linkis.manager.rm.exception.RMErrorCode
import org.apache.linkis.manager.rm.external.service.ExternalResourceService
import org.apache.linkis.manager.rm.external.yarn.YarnResourceIdentifier
import org.apache.linkis.manager.rm.utils.{RMUtils, UserConfiguration}
import org.apache.linkis.manager.rm.utils.AcrossClusterRulesJudgeUtils.{
  originClusterResourceCheck,
  targetClusterResourceCheck
}

import org.apache.commons.lang3.StringUtils

import java.math.RoundingMode
import java.text.MessageFormat
import java.util

abstract class RequestResourceService(labelResourceService: LabelResourceService) extends Logging {

  val resourceType: ResourceType = ResourceType.Default

  val enableRequest = RMUtils.RM_REQUEST_ENABLE.getValue

  var externalResourceService: ExternalResourceService = null

  def setExternalResourceService(externalResourceService: ExternalResourceService): Unit = {
    this.externalResourceService = externalResourceService
  }

  def canRequestResource(
      labelContainer: RMLabelContainer,
      resource: NodeResource,
      engineCreateRequest: EngineCreateRequest
  ): CanCreateECRes = {
    val canCreateECRes = new CanCreateECRes
    val emInstanceLabel = labelContainer.getEMInstanceLabel
    val ecmResource = labelResourceService.getLabelResource(emInstanceLabel)
    val requestResource = resource.getMinResource
    if (ecmResource != null) {
      val labelAvailableResource = ecmResource.getLeftResource
      canCreateECRes.setEcmResource(RMUtils.serializeResource(labelAvailableResource))
      if (!labelAvailableResource.notLess(requestResource)) {
        logger.info(
          s"user want to use resource[${requestResource}] > em ${emInstanceLabel.getInstance()} available resource[${labelAvailableResource}]"
        )
        val notEnoughMessage = generateECMNotEnoughMessage(
          requestResource,
          labelAvailableResource,
          ecmResource.getMaxResource
        )
        canCreateECRes.setCanCreateEC(false)
        canCreateECRes.setReason(notEnoughMessage._2)
      }
    }
    // get CombinedLabel Resource Usage
    labelContainer.setCurrentLabel(labelContainer.getCombinedResourceLabel)
    val labelResource = getCombinedLabelResourceUsage(labelContainer, resource)
    labelResourceService.setLabelResource(
      labelContainer.getCurrentLabel,
      labelResource,
      labelContainer.getCombinedResourceLabel.getStringValue
    )

    if (labelResource != null) {
      val labelAvailableResource = labelResource.getLeftResource
      canCreateECRes.setLabelResource(RMUtils.serializeResource(labelAvailableResource))
      val labelMaxResource = labelResource.getMaxResource
      if (!labelAvailableResource.notLess(requestResource)) {
        logger.info(
          s"Failed check: ${labelContainer.getUserCreatorLabel.getUser} want to use label [${labelContainer.getCurrentLabel}] resource[${requestResource}] > " +
            s"label available resource[${labelAvailableResource}]"
        )
        val notEnoughMessage =
          generateNotEnoughMessage(requestResource, labelAvailableResource, labelMaxResource)
        canCreateECRes.setCanCreateEC(false);
        canCreateECRes.setReason(notEnoughMessage._2)
      }
    }
    canCreateECRes
  }

  private def getCombinedLabelResourceUsage(
      labelContainer: RMLabelContainer,
      resource: NodeResource
  ): NodeResource = {
    // 1. get label resource from db
    var labelResource = labelResourceService.getLabelResource(labelContainer.getCurrentLabel)
    // 2. get label configuration resource only CombinedUserCreatorEngineTypeLabel
    if (labelResource == null) {
      labelResource = new CommonNodeResource
      labelResource.setResourceType(resource.getResourceType)
      labelResource.setUsedResource(Resource.initResource(resource.getResourceType))
      labelResource.setLockedResource(Resource.initResource(resource.getResourceType))
      logger.info(s"ResourceInit: ${labelContainer.getCurrentLabel.getStringValue} ")
    }
    val configuredResource = UserConfiguration.getUserConfiguredResource(
      resource.getResourceType,
      labelContainer.getUserCreatorLabel,
      labelContainer.getEngineTypeLabel
    )
    logger.debug(
      s"Get configured resource ${configuredResource} for [${labelContainer.getUserCreatorLabel}] and [${labelContainer.getEngineTypeLabel}] "
    )
    labelResource.setMaxResource(configuredResource)
    labelResource.setMinResource(Resource.initResource(labelResource.getResourceType))
    labelResource.setLeftResource(
      labelResource.getMaxResource
        .minus(labelResource.getUsedResource)
        .minus(labelResource.getLockedResource)
    )
    logger.debug(
      s"${labelContainer.getCurrentLabel} ecmResource: Max: ${labelResource.getMaxResource}  \t " +
        s"use:  ${labelResource.getUsedResource}  \t locked: ${labelResource.getLockedResource}"
    )
    labelResource
  }

  def canRequest(
      labelContainer: RMLabelContainer,
      resource: NodeResource,
      engineCreateRequest: EngineCreateRequest
  ): Boolean = {
    if (!enableRequest) {
      logger.info("Resource judgment switch is not turned on, the judgment will be skipped")
      return true
    }
    // ========== 智能队列选择逻辑 (Secondary Queue Selection) ==========
    // 重要：任何异常都不能影响任务执行，异常时直接使用主队列
    try {
      // 1. 获取用户配置（从任务参数）
      val properties = if (engineCreateRequest.getProperties != null) {
        engineCreateRequest.getProperties
      } else {
        new util.HashMap[String, String]()
      }

      // 2. 获取队列配置（用户配置）
      val primaryQueue = properties.get(YARN_QUEUE_NAME_CONFIG_KEY)
      val secondaryQueue = properties.getOrDefault("wds.linkis.rm.secondary.yarnqueue", "")

      // 3. 获取系统配置（Linkis 配置）
      val enabled = RMConfiguration.SECONDARY_QUEUE_ENABLED.getValue
      val threshold = RMConfiguration.SECONDARY_QUEUE_THRESHOLD.getValue
      val supportedEngines = RMConfiguration.SECONDARY_QUEUE_ENGINES.getValue
        .split(",")
        .map(_.trim)
        .map(_.toLowerCase())
        .toSet
      val supportedCreators = RMConfiguration.SECONDARY_QUEUE_CREATORS.getValue
        .split(",")
        .map(_.trim)
        .map(_.toUpperCase())
        .toSet

      // 4. 检查是否启用第二队列功能
      if (
          enabled && StringUtils.isNotBlank(secondaryQueue) && StringUtils.isNotBlank(primaryQueue)
      ) {

        // 5. 获取引擎类型和 Creator（从 Labels）
        var engineType: String = null
        var creator: String = null

        try {
          val labels: util.List[Label[_]] = labelContainer.getLabels
          if (labels != null && !labels.isEmpty) {
            engineType = LabelUtil.getEngineType(labels)
            val userCreatorLabel = labelContainer.getUserCreatorLabel
            if (userCreatorLabel != null) {
              creator = userCreatorLabel.getCreator
            }
          }
        } catch {
          case e: Exception =>
            logger.error("Failed to parse labels for queue selection, fallback to primary queue", e)
          // Label 解析失败，直接使用主队列，不影响任务
        }

        logger.info(
          s"Queue selection enabled: primary=$primaryQueue, secondary=$secondaryQueue, threshold=$threshold"
        )
        logger.info(s"Request info: engineType=$engineType, creator=$creator")

        // 6. 检查引擎类型和 Creator 是否在支持列表中
        val engineMatched =
          engineType == null || supportedEngines.contains(engineType.toLowerCase())
        val creatorMatched = creator == null || supportedCreators.contains(creator.toUpperCase())

        if (engineMatched && creatorMatched) {
          try {
            // 7. 查询第二队列资源使用率
            val queueInfo = externalResourceService.getResource(
              ResourceType.Yarn,
              labelContainer,
              new YarnResourceIdentifier(secondaryQueue)
            )

            if (queueInfo != null) {
              val usedResource = queueInfo.getUsedResource.asInstanceOf[YarnResource]
              val maxResource = queueInfo.getMaxResource.asInstanceOf[YarnResource]

              // 8. 分别计算三个维度的资源使用率
              // 只要有一个维度超过阈值，就使用主队列
              val useSecondaryQueue = if (maxResource != null && maxResource.getQueueMemory > 0) {
                // 计算内存使用率
                val memoryUsage =
                  usedResource.getQueueMemory.toDouble / maxResource.getQueueMemory.toDouble
                val memoryOverThreshold = memoryUsage > threshold

                // 计算 CPU 使用率
                val cpuUsage = if (maxResource.getQueueCores > 0) {
                  usedResource.getQueueCores.toDouble / maxResource.getQueueCores.toDouble
                } else {
                  0.0
                }
                val cpuOverThreshold = cpuUsage > threshold

                // 计算实例数使用率
                val instancesUsage = if (maxResource.getQueueInstances > 0) {
                  usedResource.getQueueInstances.toDouble / maxResource.getQueueInstances.toDouble
                } else {
                  0.0
                }
                val instancesOverThreshold = instancesUsage > threshold

                // 记录详细的资源使用情况
                logger.info(
                  s"Resource usage details for queue $secondaryQueue (threshold: ${(threshold * 100)
                    .formatted("%.2f%%")}):"
                )
                logger.info(s"  Memory: ${(memoryUsage * 100)
                  .formatted("%.2f%%")} ${if (memoryOverThreshold) "✗ OVER" else "✓ OK"}")
                logger.info(
                  s"  CPU: ${(cpuUsage * 100).formatted("%.2f%%")} ${if (cpuOverThreshold) "✗ OVER"
                  else "✓ OK"}"
                )
                logger.info(s"  Instances: ${(instancesUsage * 100)
                  .formatted("%.2f%%")} ${if (instancesOverThreshold) "✗ OVER" else "✓ OK"}")

                // 判断：所有维度都必须在阈值以下，才使用备用队列
                val allUnderThreshold =
                  !memoryOverThreshold && !cpuOverThreshold && !instancesOverThreshold

                if (allUnderThreshold) {
                  logger.info(
                    s"Secondary queue available: all dimensions under threshold, use secondary queue: $secondaryQueue"
                  )
                } else {
                  val overDimensions = Seq(
                    if (memoryOverThreshold) "Memory" else null,
                    if (cpuOverThreshold) "CPU" else null,
                    if (instancesOverThreshold) "Instances" else null
                  ).filter(_ != null).mkString(", ")
                  logger.info(
                    s"Secondary queue not available: $overDimensions over threshold, use primary queue: $primaryQueue"
                  )
                }

                allUnderThreshold
              } else {
                false
              }

              // 9. 判断使用哪个队列
              val selectedQueue = if (useSecondaryQueue) {
                secondaryQueue
              } else {
                primaryQueue
              }

              // 10. 更新 properties
              properties.put(YARN_QUEUE_NAME_CONFIG_KEY, selectedQueue)
              logger.info(s"Updated queue config: $selectedQueue")

            } else {
              logger.warn(
                s"Failed to get queue info for $secondaryQueue, use primary queue: $primaryQueue"
              )
            }

          } catch {
            case e: Exception =>
              // 异常处理：记录详细错误日志，使用主队列，确保不影响任务执行
              logger.error(
                s"Exception during queue resource check for secondary queue: $secondaryQueue, fallback to primary queue: $primaryQueue",
                e
              )
          }
        } else {
          // 引擎类型或 Creator 不在支持列表中
          if (!engineMatched) {
            logger.info(
              s"Engine type '$engineType' not in supported list: ${supportedEngines.mkString(",")}, use primary queue: $primaryQueue"
            )
          }
          if (!creatorMatched) {
            logger.info(
              s"Creator '$creator' not in supported list: ${supportedCreators.mkString(",")}, use primary queue: $primaryQueue"
            )
          }
        }
      } else {
        logger.debug(
          "Secondary queue not configured or disabled, use primary queue from properties"
        )
      }

    } catch {
      case e: Exception =>
        // 最外层异常捕获：确保任何异常都不影响任务执行
        logger.error(
          "Unexpected error in queue selection logic, task will continue with primary queue",
          e
        )
      // 不做任何处理，让任务继续使用原始配置的主队列
    }
    // ========== 队列选择逻辑结束 ==========

    // check ecm label resource
    labelContainer.getCurrentLabel match {
      case emInstanceLabel: EMInstanceLabel =>
        return checkEMResource(emInstanceLabel, resource)
      case _ =>
    }
    // check combined label resource
    if (!labelContainer.getCombinedResourceLabel.equals(labelContainer.getCurrentLabel)) {
      throw new RMErrorException(
        RESOURCE_LATER_ERROR.getErrorCode,
        RESOURCE_LATER_ERROR.getErrorDesc + labelContainer.getCurrentLabel
      )
    }
    val labels: util.List[Label[_]] = labelContainer.getLabels
    val engineType: String = LabelUtil.getEngineType(labels)
    val props: util.Map[String, String] = engineCreateRequest.getProperties

    // 是否是跨集群的任务
    var acrossClusterTask: Boolean = false
    if (props != null) {
      acrossClusterTask = props.getOrDefault(AMConfiguration.ACROSS_CLUSTER_TASK, "false").toBoolean
    }

    // hive cluster check
    if (
        externalResourceService != null && StringUtils.isNotBlank(
          engineType
        ) && SUPPORT_CLUSTER_RULE_EC_TYPES.contains(
          engineType
        ) && props != null && acrossClusterTask && !"spark".equals(engineType)
    ) {
      val queueName = props.getOrDefault(YARN_QUEUE_NAME_CONFIG_KEY, "default")
      logger.info(s"hive cluster check with queue: $queueName")
      val yarnIdentifier = new YarnResourceIdentifier(queueName)
      val providedYarnResource =
        externalResourceService.getResource(ResourceType.Yarn, labelContainer, yarnIdentifier)
      val (maxCapacity, usedCapacity) =
        (providedYarnResource.getMaxResource, providedYarnResource.getUsedResource)
      // judge if is cross cluster task and origin cluster priority first
      originClusterResourceCheck(engineCreateRequest, maxCapacity, usedCapacity)
      // judge if is cross cluster task and target cluster priority first
      targetClusterResourceCheck(
        labelContainer,
        engineCreateRequest,
        maxCapacity,
        usedCapacity,
        externalResourceService
      )
    }

    val requestResource = resource.getMinResource
    // get CombinedLabel Resource Usage
    val labelResource = getCombinedLabelResourceUsage(labelContainer, resource)
    labelResourceService.setLabelResource(
      labelContainer.getCurrentLabel,
      labelResource,
      labelContainer.getCombinedResourceLabel.getStringValue
    )
    logger.debug(s"Label [${labelContainer.getCurrentLabel}] has resource + [${labelResource}]")
    if (labelResource != null) {
      val labelAvailableResource = labelResource.getLeftResource
      val labelMaxResource = labelResource.getMaxResource
      if (!labelAvailableResource.notLess(requestResource)) {
        logger.info(
          s"Failed check: ${labelContainer.getUserCreatorLabel.getUser} want to use label [${labelContainer.getCurrentLabel}] resource[${requestResource}] > " +
            s"label available resource[${labelAvailableResource}]"
        )
        val notEnoughMessage =
          generateNotEnoughMessage(requestResource, labelAvailableResource, labelMaxResource)
        throw new RMWarnException(notEnoughMessage._1, notEnoughMessage._2)
      }
      logger.debug(
        s"Passed check: ${labelContainer.getUserCreatorLabel.getUser} want to use label [${labelContainer.getCurrentLabel}] resource[${requestResource}] <= " +
          s"label available resource[${labelAvailableResource}]"
      )
      true
    } else {
      logger.warn(s"No resource available found for label ${labelContainer.getCurrentLabel}")
      throw new RMWarnException(
        NO_RESOURCE.getErrorCode,
        MessageFormat.format(NO_RESOURCE.getErrorDesc(), labelContainer.getCurrentLabel)
      )
    }
  }

  private def checkEMResource(emInstanceLabel: EMInstanceLabel, resource: NodeResource): Boolean = {
    val labelResource = labelResourceService.getLabelResource(emInstanceLabel)
    val requestResource = resource.getMinResource
    logger.debug(s"emInstanceLabel resource info ${labelResource}")
    if (labelResource != null) {
      val labelAvailableResource = labelResource.getLeftResource
      if (!labelAvailableResource.notLess(requestResource)) {
        logger.info(
          s"user want to use resource[${requestResource}] > em ${emInstanceLabel.getInstance()} available resource[${labelAvailableResource}]"
        )
        val notEnoughMessage = generateECMNotEnoughMessage(
          requestResource,
          labelAvailableResource,
          labelResource.getMaxResource
        )
        throw new RMWarnException(
          notEnoughMessage._1,
          notEnoughMessage._2 + s"ECM Instance:${emInstanceLabel.getInstance()}"
        )
      }
      logger.debug(s"Passed check: resource[${requestResource}] want to use em ${emInstanceLabel
        .getInstance()}  available resource[${labelAvailableResource}]")
      true
    } else {
      logger.warn(s"No resource available found for em ${emInstanceLabel.getInstance()} ")
      throw new RMWarnException(
        NO_RESOURCE_AVAILABLE.getErrorCode,
        MessageFormat.format(NO_RESOURCE_AVAILABLE.getErrorDesc, emInstanceLabel.getInstance())
      )
    }
  }

  def generateECMNotEnoughMessage(
      requestResource: Resource,
      availableResource: Resource,
      maxResource: Resource
  ): (Int, String) = {
    val loadRequestResource = requestResource match {
      case li: LoadInstanceResource => li
      case driverAndYarnResource: DriverAndYarnResource =>
        driverAndYarnResource.getLoadInstanceResource
      case _ => null
    }
    loadRequestResource match {
      case li: LoadInstanceResource =>
        val loadInstanceAvailable = availableResource.asInstanceOf[LoadInstanceResource]
        val loadInstanceMax = maxResource.asInstanceOf[LoadInstanceResource]
        if (li.getCores > loadInstanceAvailable.getCores) {
          (
            RMErrorCode.ECM_CPU_INSUFFICIENT.getErrorCode,
            RMErrorCode.ECM_CPU_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.CPU,
                RMConstant.CPU_UNIT,
                li.getCores,
                loadInstanceAvailable.getCores,
                loadInstanceMax.getCores
              )
          )
        } else if (li.getMemory > loadInstanceAvailable.getMemory) {
          (
            RMErrorCode.ECM_MEMORY_INSUFFICIENT.getErrorCode,
            RMErrorCode.ECM_MEMORY_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.MEMORY,
                RMConstant.MEMORY_UNIT_BYTE,
                li.getMemory,
                loadInstanceAvailable.getMemory,
                loadInstanceMax.getMemory
              )
          )
        } else {
          (
            RMErrorCode.ECM_INSTANCES_INSUFFICIENT.getErrorCode,
            RMErrorCode.ECM_INSTANCES_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.APP_INSTANCE,
                RMConstant.INSTANCE_UNIT,
                li.getInstances,
                loadInstanceAvailable.getInstances,
                loadInstanceMax.getInstances
              )
          )
        }
      case _ =>
        (
          RMErrorCode.ECM_RESOURCE_INSUFFICIENT.getErrorCode,
          RMErrorCode.ECM_RESOURCE_INSUFFICIENT.getErrorDesc + " Unusual insufficient queue memory."
        )
    }
  }

  def generateNotEnoughMessage(
      requestResource: Resource,
      availableResource: Resource,
      maxResource: Resource
  ): (Int, String) = {
    requestResource match {
      case m: MemoryResource =>
        val avail = availableResource.asInstanceOf[MemoryResource]
        val max = maxResource.asInstanceOf[MemoryResource]
        (
          RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorCode,
          RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorDesc +
            RMUtils.getResourceInfoMsg(
              RMConstant.MEMORY,
              RMConstant.MEMORY_UNIT_BYTE,
              m.getMemory,
              avail.getMemory,
              max.getMemory
            )
        )
      case i: InstanceResource =>
        val avail = availableResource.asInstanceOf[InstanceResource]
        val max = maxResource.asInstanceOf[InstanceResource]
        (
          RMErrorCode.INSTANCES_INSUFFICIENT.getErrorCode,
          RMErrorCode.INSTANCES_INSUFFICIENT.getErrorDesc +
            RMUtils.getResourceInfoMsg(
              RMConstant.APP_INSTANCE,
              RMConstant.INSTANCE_UNIT,
              i.getInstances,
              avail.getInstances,
              max.getInstances
            )
        )
      case c: CPUResource =>
        val avail = availableResource.asInstanceOf[CPUResource]
        val max = maxResource.asInstanceOf[CPUResource]
        (
          RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorCode,
          RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorDesc +
            RMUtils.getResourceInfoMsg(
              RMConstant.CPU,
              RMConstant.CPU_UNIT,
              c.getCores,
              avail.getCores,
              max.getCores
            )
        )
      case l: LoadResource =>
        val loadAvailable = availableResource.asInstanceOf[LoadResource]
        val avail = availableResource.asInstanceOf[LoadResource]
        val max = maxResource.asInstanceOf[LoadResource]
        if (l.getCores > loadAvailable.getCores) {
          (
            RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorCode,
            RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.CPU,
                RMConstant.CPU_UNIT,
                l.getCores,
                avail.getCores,
                max.getCores
              )
          )
        } else {
          (
            RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorCode,
            RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.MEMORY,
                RMConstant.MEMORY_UNIT_BYTE,
                l.getMemory,
                avail.getMemory,
                max.getMemory
              )
          )
        }
      case li: LoadInstanceResource =>
        val loadInstanceAvailable = availableResource.asInstanceOf[LoadInstanceResource]
        val avail = availableResource.asInstanceOf[LoadInstanceResource]
        val max = maxResource.asInstanceOf[LoadInstanceResource]
        if (li.getCores > loadInstanceAvailable.getCores) {
          (
            RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorCode,
            RMErrorCode.DRIVER_CPU_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.CPU,
                RMConstant.CPU_UNIT,
                li.getCores,
                avail.getCores,
                max.getCores
              )
          )
        } else if (li.getMemory > loadInstanceAvailable.getMemory) {
          (
            RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorCode,
            RMErrorCode.DRIVER_MEMORY_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.MEMORY,
                RMConstant.MEMORY_UNIT_BYTE,
                li.getMemory,
                avail.getMemory,
                max.getMemory
              )
          )
        } else {
          (
            RMErrorCode.INSTANCES_INSUFFICIENT.getErrorCode,
            RMErrorCode.INSTANCES_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.APP_INSTANCE,
                RMConstant.INSTANCE_UNIT,
                li.getInstances,
                avail.getInstances,
                max.getInstances
              )
          )
        }
      case yarn: YarnResource =>
        val yarnAvailable = availableResource.asInstanceOf[YarnResource]
        val avail = availableResource.asInstanceOf[YarnResource]
        val max = maxResource.asInstanceOf[YarnResource]
        if (yarn.getQueueCores > yarnAvailable.getQueueCores) {
          (
            RMErrorCode.QUEUE_CPU_INSUFFICIENT.getErrorCode,
            RMErrorCode.QUEUE_CPU_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.CPU,
                RMConstant.CPU_UNIT,
                yarn.getQueueCores,
                avail.getQueueCores,
                max.getQueueCores
              )
          )
        } else if (yarn.getQueueMemory > yarnAvailable.getQueueMemory) {
          (
            RMErrorCode.QUEUE_MEMORY_INSUFFICIENT.getErrorCode,
            RMErrorCode.QUEUE_MEMORY_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.MEMORY,
                RMConstant.MEMORY_UNIT_BYTE,
                yarn.getQueueMemory,
                avail.getQueueMemory,
                max.getQueueMemory
              )
          )
        } else {
          (
            RMErrorCode.QUEUE_INSTANCES_INSUFFICIENT.getErrorCode,
            RMErrorCode.QUEUE_INSTANCES_INSUFFICIENT.getErrorDesc +
              RMUtils.getResourceInfoMsg(
                RMConstant.APP_INSTANCE,
                RMConstant.INSTANCE_UNIT,
                yarn.getQueueInstances,
                avail.getQueueInstances,
                max.getQueueInstances
              )
          )
        }
      case dy: DriverAndYarnResource =>
        val dyAvailable = availableResource.asInstanceOf[DriverAndYarnResource]
        val dyMax = maxResource.asInstanceOf[DriverAndYarnResource]
        if (
            dy.getLoadInstanceResource.getMemory > dyAvailable.getLoadInstanceResource.getMemory ||
            dy.getLoadInstanceResource.getCores > dyAvailable.getLoadInstanceResource.getCores ||
            dy.getLoadInstanceResource.getInstances > dyAvailable.getLoadInstanceResource.getInstances
        ) {
          val detail = generateNotEnoughMessage(
            dy.getLoadInstanceResource,
            dyAvailable.getLoadInstanceResource,
            dyMax.getLoadInstanceResource
          )
          (detail._1, { detail._2 })
        } else {
          val detail =
            generateNotEnoughMessage(
              dy.getYarnResource,
              dyAvailable.getYarnResource,
              dyMax.getYarnResource
            )
          (detail._1, { detail._2 })
        }
      case s: SpecialResource =>
        throw new RMWarnException(
          NOT_RESOURCE_TYPE.getErrorCode,
          MessageFormat.format(NOT_RESOURCE_TYPE.getErrorDesc, s.getClass)
        )
      case r: Resource =>
        throw new RMWarnException(
          NOT_RESOURCE_TYPE.getErrorCode,
          MessageFormat.format(NOT_RESOURCE_TYPE.getErrorDesc, r.getClass)
        )
    }
  }

}
