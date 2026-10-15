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

package org.apache.linkis.manager.rm.utils

import org.apache.linkis.common.conf.{CommonVars, TimeType}
import org.apache.linkis.common.utils.{ByteTimeUtils, JsonUtils, Logging, Utils}
import org.apache.linkis.manager.am.vo.EMNodeVo
import org.apache.linkis.manager.common.constant.RMConstant
import org.apache.linkis.manager.common.entity.persistence.{
  PersistenceLabelRel,
  PersistenceResource
}
import org.apache.linkis.manager.common.entity.resource.{Resource, _}
import org.apache.linkis.manager.common.utils.ResourceUtils
import org.apache.linkis.manager.label.LabelManagerUtils.labelFactory
import org.apache.linkis.manager.label.builder.CombinedLabelBuilder
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext
import org.apache.linkis.manager.label.entity.engine.{EngineType, EngineTypeLabel, UserCreatorLabel}
import org.apache.linkis.manager.rm.conf.ResourceStatus
import org.apache.linkis.manager.rm.restful.vo.{UserCreatorEngineType, UserResourceVo}

import org.apache.commons.lang3.StringUtils

import java.util
import java.util.{List, UUID}

import scala.collection.JavaConverters.asScalaBufferConverter

import com.google.common.collect.Lists

object RMUtils extends Logging {

  val jacksonUtil = JsonUtils.jackson

  val MANAGER_KILL_ENGINE_EAIT =
    CommonVars("wds.linkis.manager.rm.kill.engine.wait", new TimeType("30s"))

  val RM_REQUEST_ENABLE = CommonVars("wds.linkis.manager.rm.request.enable", true)

  val RM_RESOURCE_LOCK_WAIT_TIME = CommonVars("wds.linkis.manager.rm.lock.wait", 5 * 60 * 1000)

  val RM_DEBUG_ENABLE = CommonVars("wds.linkis.manager.rm.debug.enable", false)

  val RM_DEBUG_LOG_PATH =
    CommonVars("wds.linkis.manager.rm.debug.log.path", "file:///tmp/linkis/rmLog")

  val EXTERNAL_RESOURCE_REFRESH_TIME =
    CommonVars("wds.linkis.manager.rm.external.resource.regresh.time", new TimeType("30m"))

  var COMBINED_USERCREATOR_ENGINETYPE: String = _

  val ENGINE_TYPE = CommonVars.apply(
    "wds.linkis.configuration.engine.type",
    EngineType.getAllEngineTypes().asScala.mkString(",")
  )

  val RM_RESOURCE_ACTION_RECORD = CommonVars("wds.linkis.manager.rm.resource.action.record", true)

  // REQ-01 O3: 引擎退出消息负值丢弃开关，默认开启防御，关闭时回退原有行为
  val RM_NEGATIVE_RESOURCE_DISCARD_ENABLE =
    CommonVars("linkis.manager.engine.exit.negative.resource.discard.enable", true)

  // REQ-01 OC1: 资源管理界面查询结果负数归零开关，默认开启，关闭时回退原有行为(直接返回原值不归一化)
  val RM_USER_RESOURCE_NEGATIVE_NORMALIZE_ENABLE =
    CommonVars("linkis.manager.rm.user.resource.negative.normalize.enable", true)

  // REQ-01 增强: allUserResource 剩余资源重算开关(left = max - used)，默认开启，关闭时回退原有行为(保留DB快照left，不重算)
  val RM_USER_RESOURCE_LEFT_RECOMPUTE_ENABLE =
    CommonVars("linkis.manager.rm.user.resource.left.recompute.enable", true)

  def deserializeResource(plainResource: String): Resource = {
    jacksonUtil.readValue(plainResource, classOf[Resource])
  }

  def serializeResource(resource: Resource): String = {
    jacksonUtil.writeValueAsString(resource)
  }

  def toUserResourceVo(userResource: UserResource): UserResourceVo = {
    val userResourceVo = new UserResourceVo
    if (userResource.getCreator != null) userResourceVo.setCreator(userResource.getCreator)
    if (userResource.getEngineType != null) {
      userResourceVo.setEngineTypeWithVersion(
        userResource.getEngineType + "-" + userResource.getVersion
      )
    }
    if (userResource.getUsername != null) userResourceVo.setUsername(userResource.getUsername)
    if (userResource.getCreateTime != null) {
      userResourceVo.setCreateTime(userResource.getCreateTime)
    }
    if (userResource.getUpdateTime != null) {
      userResourceVo.setUpdateTime(userResource.getUpdateTime)
    }
    if (userResource.getId != null) userResourceVo.setId(userResource.getId)
    if (userResource.getUsedResource != null) {
      userResourceVo.setUsedResource(
        jacksonUtil.readValue(
          jacksonUtil.writeValueAsString(userResource.getUsedResource),
          classOf[util.Map[String, Any]]
        )
      )
    }
    if (userResource.getLeftResource != null) {
      userResourceVo.setLeftResource(
        jacksonUtil.readValue(
          jacksonUtil.writeValueAsString(userResource.getLeftResource),
          classOf[util.Map[String, Any]]
        )
      )
    }
    if (userResource.getLockedResource != null) {
      userResourceVo.setLockedResource(
        jacksonUtil.readValue(
          jacksonUtil.writeValueAsString(userResource.getLockedResource),
          classOf[util.Map[String, Any]]
        )
      )
    }
    if (userResource.getMaxResource != null) {
      userResourceVo.setMaxResource(
        jacksonUtil.readValue(
          jacksonUtil.writeValueAsString(userResource.getMaxResource),
          classOf[util.Map[String, Any]]
        )
      )
    }
    if (userResource.getMinResource != null) {
      userResourceVo.setMinResource(
        jacksonUtil.readValue(
          jacksonUtil.writeValueAsString(userResource.getMinResource),
          classOf[util.Map[String, Any]]
        )
      )
    }
    if (userResource.getResourceType != null) {
      userResourceVo.setResourceType(userResource.getResourceType)
    }
    if (userResource.getLeftResource != null && userResource.getMaxResource != null) {
      if (userResource.getResourceType.equals(ResourceType.DriverAndYarn)) {
        val leftDriverResource =
          userResource.getLeftResource.asInstanceOf[DriverAndYarnResource].getLoadInstanceResource
        val leftYarnResource =
          userResource.getLeftResource.asInstanceOf[DriverAndYarnResource].getYarnResource
        val maxDriverResource =
          userResource.getMaxResource.asInstanceOf[DriverAndYarnResource].getLoadInstanceResource
        val maxYarnResource =
          userResource.getMaxResource.asInstanceOf[DriverAndYarnResource].getYarnResource
        userResourceVo.setLoadResourceStatus(
          ResourceStatus.measure(leftDriverResource, maxDriverResource)
        )
        userResourceVo.setQueueResourceStatus(
          ResourceStatus.measure(leftYarnResource, maxYarnResource)
        )
      } else {
        userResourceVo.setLoadResourceStatus(
          ResourceStatus.measure(userResource.getLeftResource, userResource.getMaxResource)
        )
      }
    }
    userResourceVo
  }

  /**
   * REQ-01 需求变更:资源管理界面查询时,若"已使用资源/剩余资源"存在负数,强制归零。 资源值以数值字段(如 memory/cores/instances 等)存储,遍历 Map
   * 中所有数值型字段, 将 < 0 的值原地改写为与字段类型一致的 0,保证前端展示不再出现负资源。 对 DriverAndYarn 等复合资源,值本身是嵌套 Map
   * (loadInstanceResource/yarnResource), 需递归进入内层对其数值字段归零,避免嵌套负值漏归一。
   * @return
   *   归一化后的同一 Map(原地修改,便于链式调用)
   */
  def normalizeNegativeResource(resourceMap: util.Map[String, Any]): util.Map[String, Any] = {
    if (resourceMap == null) {
      return resourceMap
    }
    val it = resourceMap.entrySet().iterator()
    while (it.hasNext) {
      val entry = it.next()
      entry.getValue match {
        case m: util.Map[String, Any] @unchecked =>
          // 复合资源(如 DriverAndYarn)的嵌套结构,递归归零内层数值字段
          normalizeNegativeResource(m)
        case n: java.lang.Integer if n.intValue() < 0 => entry.setValue(0: java.lang.Integer)
        case n: java.lang.Long if n.longValue() < 0 => entry.setValue(0: java.lang.Long)
        case n: java.lang.Double if n.doubleValue() < 0 => entry.setValue(0: java.lang.Double)
        case n: java.lang.Float if n.floatValue() < 0 => entry.setValue(0: java.lang.Float)
        case n: java.lang.Short if n.shortValue() < 0 =>
          entry.setValue(java.lang.Short.valueOf(0.toShort))
        case n: java.lang.Byte if n.byteValue() < 0 =>
          entry.setValue(java.lang.Byte.valueOf(0.toByte))
        case n: java.math.BigDecimal if n.doubleValue() < 0 =>
          entry.setValue(java.math.BigDecimal.ZERO)
        case n: java.math.BigInteger if n.signum() < 0 =>
          entry.setValue(java.math.BigInteger.ZERO)
        case _ => // 忽略非数值字段(如字符串 queueName/applicationId 等)
      }
    }
    resourceMap
  }

  /**
   * REQ-01 需求变更:对单个 UserResourceVo 的"已使用资源"与"剩余资源"执行负数归零。
   */
  def normalizeUserResourceVoNegative(vo: UserResourceVo): UserResourceVo = {
    if (vo == null) {
      return vo
    }
    // 功能开关关闭时回退原有行为，直接返回原 vo 不做负数归零
    if (!RM_USER_RESOURCE_NEGATIVE_NORMALIZE_ENABLE.getValue) {
      return vo
    }
    normalizeNegativeResource(vo.getUsedResource.asInstanceOf[util.Map[String, Any]])
    normalizeNegativeResource(vo.getLeftResource.asInstanceOf[util.Map[String, Any]])
    vo
  }

  /**
   * REQ-01 增强: allUserResource 界面剩余资源重算为"最大资源 - 已用资源"。 仅在展示层(查询出口)生效，不改动底层DB数据。逻辑顺序:
   *   1. 对已用资源 usedResource 中的负数强制归零(收集已用资源负值) 2. 取最大资源 maxResource 与归一化后的已用资源，逐数值字段计算 left = max -
   *      used 3. 重算结果若为负(used > max)同样归零 主体接口逻辑不变，仅在接口最后追加本层计算。开关关闭时回退原有行为(保留DB快照left)。
   */
  def recomputeUserResourceLeftNegative(vo: UserResourceVo): UserResourceVo = {
    if (vo == null) {
      return vo
    }
    // 功能开关关闭时回退原有行为，不重算剩余资源(保留DB快照)
    if (!RM_USER_RESOURCE_LEFT_RECOMPUTE_ENABLE.getValue) {
      return vo
    }
    val usedMap = vo.getUsedResource.asInstanceOf[util.Map[String, Any]]
    val maxMap = vo.getMaxResource.asInstanceOf[util.Map[String, Any]]
    if (maxMap == null || usedMap == null) {
      return vo
    }
    // 1. 先对已用资源负数归零(递归,兼容 DriverAndYarn 嵌套结构)
    normalizeNegativeResource(usedMap)
    // 2. 递归计算 left = max - used（数值字段相减、嵌套结构递归、非数值字段留 max）
    val leftMap = computeLeftMap(maxMap, usedMap)
    // 3. 重算结果若为负归零
    normalizeNegativeResource(leftMap)
    vo.setLeftResource(leftMap)
    vo
  }

  /**
   * 递归计算剩余资源 left = max - used。兼容两类资源结构: 1. 扁平结构(hive/shell 等): 顶层字段为数值(memory/cores/instances) 2.
   * 复合结构(DriverAndYarn 等): 顶层字段为嵌套 Map(loadInstanceResource/yarnResource) 对嵌套 Map 递归处理; 对非数值字段(如
   * queueName/applicationId)保留 max 值,不参与相减。
   */
  private def computeLeftMap(
      maxMap: util.Map[String, Any],
      usedMap: util.Map[String, Any]
  ): util.Map[String, Any] = {
    val leftMap = new util.HashMap[String, Any]()
    if (maxMap == null) {
      return leftMap
    }
    val it = maxMap.entrySet().iterator()
    while (it.hasNext) {
      val entry = it.next()
      entry.getValue match {
        case maxVal: java.lang.Number =>
          val usedVal = usedMap.get(entry.getKey) match {
            case u: java.lang.Number => u
            case _ => null
          }
          if (usedVal == null) {
            // 已用资源无该字段：剩余 = 最大(不重算，保留max值，但为负归0)
            leftMap.put(entry.getKey, normalizeNumberNegative(maxVal))
          } else {
            // left = max - used
            leftMap.put(entry.getKey, subtractNumbers(maxVal, usedVal))
          }
        case maxInner: util.Map[String, Any] @unchecked =>
          // 复合资源(如 DriverAndYarn)嵌套结构：递归计算内层 left
          val usedInner = usedMap.get(entry.getKey) match {
            case u: util.Map[String, Any] @unchecked => u
            case _ => new util.HashMap[String, Any]()
          }
          leftMap.put(entry.getKey, computeLeftMap(maxInner, usedInner))
        case other =>
          // 非数值、非嵌套结构字段(如字符串 queueName/applicationId)：保留 max 值
          leftMap.put(entry.getKey, other)
      }
    }
    leftMap
  }

  /** 数值相减，返回与 maxVal 类型一致的数值；结果为负时归零。 */
  private def subtractNumbers(
      maxVal: java.lang.Number,
      usedVal: java.lang.Number
  ): java.lang.Object = {
    val result = (maxVal: Any, usedVal: Any) match {
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        new java.lang.Integer(a.intValue() - b.intValue())
      case (a: java.lang.Long, b: java.lang.Long) =>
        new java.lang.Long(a.longValue() - b.longValue())
      case (a: java.lang.Double, b: java.lang.Double) =>
        new java.lang.Double(a.doubleValue() - b.doubleValue())
      case (a: java.lang.Float, b: java.lang.Float) =>
        new java.lang.Float(a.floatValue() - b.floatValue())
      case (a: java.lang.Short, b: java.lang.Short) =>
        java.lang.Short.valueOf((a.shortValue() - b.shortValue()).toShort)
      case (a: java.lang.Byte, b: java.lang.Byte) =>
        java.lang.Byte.valueOf((a.byteValue() - b.byteValue()).toByte)
      case (a: java.math.BigDecimal, b: java.math.BigDecimal) =>
        a.subtract(b)
      case (a: java.math.BigInteger, b: java.math.BigInteger) =>
        a.subtract(b)
      case _ =>
        // 类型不一致时按 double 兜底
        new java.lang.Double(maxVal.doubleValue() - usedVal.doubleValue())
    }
    // 结果为负时归零
    result match {
      case n: java.lang.Number if n.doubleValue() < 0 => zeroLike(result)
      case other => other
    }
  }

  /** 生成与目标数值同类型的 0。 */
  private def zeroLike(value: Any): java.lang.Object = value match {
    case _: java.lang.Integer => new java.lang.Integer(0)
    case _: java.lang.Long => new java.lang.Long(0L)
    case _: java.lang.Double => new java.lang.Double(0d)
    case _: java.lang.Float => new java.lang.Float(0f)
    case _: java.lang.Short => java.lang.Short.valueOf(0.toShort)
    case _: java.lang.Byte => java.lang.Byte.valueOf(0.toByte)
    case _: java.math.BigDecimal => java.math.BigDecimal.ZERO
    case _: java.math.BigInteger => java.math.BigInteger.ZERO
    case _ => new java.lang.Integer(0)
  }

  /** 数值若为负则归零（供 max 单独存在时使用）。 */
  private def normalizeNumberNegative(value: java.lang.Number): java.lang.Object = {
    if (value.doubleValue() < 0) zeroLike(value) else value
  }

  def toPersistenceResource(nodeResource: NodeResource): PersistenceResource = {
    val persistenceResource = new PersistenceResource
    if (nodeResource.getMaxResource != null) {
      persistenceResource.setMaxResource(serializeResource(nodeResource.getMaxResource))
    }
    if (nodeResource.getMinResource != null) {
      persistenceResource.setMinResource(serializeResource(nodeResource.getMinResource))
    }
    if (nodeResource.getLockedResource != null) {
      persistenceResource.setLockedResource(serializeResource(nodeResource.getLockedResource))
    }
    if (nodeResource.getExpectedResource != null) {
      persistenceResource.setExpectedResource(serializeResource(nodeResource.getExpectedResource))
    }
    if (nodeResource.getLeftResource != null) {
      persistenceResource.setLeftResource(serializeResource(nodeResource.getLeftResource))
    }
    persistenceResource.setResourceType(nodeResource.getResourceType.toString())
    persistenceResource
  }

  def aggregateNodeResource(
      firstNodeResource: NodeResource,
      secondNodeResource: NodeResource
  ): CommonNodeResource = {
    if (firstNodeResource != null && secondNodeResource != null) {
      val aggregatedNodeResource = new CommonNodeResource
      aggregatedNodeResource.setResourceType(firstNodeResource.getResourceType)
      aggregatedNodeResource.setMaxResource(
        aggregateResource(firstNodeResource.getMaxResource, secondNodeResource.getMaxResource)
      )
      aggregatedNodeResource.setMinResource(
        aggregateResource(firstNodeResource.getMinResource, secondNodeResource.getMinResource)
      )
      aggregatedNodeResource.setUsedResource(
        aggregateResource(firstNodeResource.getUsedResource, secondNodeResource.getUsedResource)
      )
      aggregatedNodeResource.setLockedResource(
        aggregateResource(firstNodeResource.getLockedResource, secondNodeResource.getLockedResource)
      )
      aggregatedNodeResource.setLeftResource(
        aggregateResource(firstNodeResource.getLeftResource, secondNodeResource.getLeftResource)
      )
      return aggregatedNodeResource
    }
    if (firstNodeResource == null && secondNodeResource == null) {
      return null
    }
    if (firstNodeResource == null) {
      secondNodeResource.asInstanceOf[CommonNodeResource]
    } else {
      firstNodeResource.asInstanceOf[CommonNodeResource]
    }
  }

  def aggregateResource(firstResource: Resource, secondResource: Resource): Resource = {
    (firstResource, secondResource) match {
      case (null, null) => null
      case (null, secondResource) => secondResource
      case (firstResource, null) => firstResource
      case (firstResource, secondResource)
          if firstResource.getClass.equals(secondResource.getClass) =>
        firstResource.add(secondResource)
      case _ => null
    }
  }

  def getResourceInfoMsg(
      resourceType: String,
      unitType: String,
      requestResource: Any,
      availableResource: Any,
      maxResource: Any,
      queueName: String = ""
  ): String = {

    def dealMemory(resourceType: String, unitType: String, resource: Any): String = {
      if (RMConstant.MEMORY.equals(resourceType) && RMConstant.MEMORY_UNIT_BYTE.equals(unitType)) {
        Utils.tryCatch {
          if (logger.isDebugEnabled()) {
            logger.debug(s"Will change ${resource.toString} from ${unitType} to GB")
          }
          ByteTimeUtils.negativeByteStringAsGb(resource.toString + "b").toString + "GB"
        } { case e: Exception =>
          logger.error(s"Cannot convert ${resource} to Gb, " + e.getMessage)
          resource.toString + unitType
        }
      } else {
        resource.toString + unitType
      }
    }

    val reqMsg =
      if (null == requestResource) "null" + unitType
      else dealMemory(resourceType, unitType, requestResource)
    val availMsg =
      if (null == availableResource) "null" + unitType
      else dealMemory(resourceType, unitType, availableResource.toString)
    val maxMsg =
      if (null == maxResource) "null" + unitType
      else dealMemory(resourceType, unitType, maxResource.toString)
    if (StringUtils.isEmpty(queueName)) {
      s" use ${resourceType}, requestResource : ${reqMsg} > availableResource : ${availMsg},  maxResource : ${maxMsg}."
    } else {
      s" use ${resourceType}, requestResource : ${reqMsg} > availableResource : ${availMsg},  maxResource : ${maxMsg}, queueName : ${queueName}."
    }
  }

  def getECTicketID: String = UUID.randomUUID().toString

  def getCombinedLabel: String = {
    if (COMBINED_USERCREATOR_ENGINETYPE == null) {
      val userCreatorLabel = labelFactory.createLabel(classOf[UserCreatorLabel])
      val engineTypeLabel = labelFactory.createLabel(classOf[EngineTypeLabel])
      val combinedLabelBuilder = new CombinedLabelBuilder
      val combinedLabel =
        combinedLabelBuilder.build("", Lists.newArrayList(userCreatorLabel, engineTypeLabel))
      COMBINED_USERCREATOR_ENGINETYPE = combinedLabel.getLabelKey
      COMBINED_USERCREATOR_ENGINETYPE
    } else {
      COMBINED_USERCREATOR_ENGINETYPE
    }
  }

  def getUserYarnResources(
      resources: util.List[PersistenceResource]
  ): util.HashMap[String, util.HashMap[String, Any]] = {
    val userResourceMap = new util.HashMap[String, util.HashMap[String, Any]]
    resources.asScala.foreach(resource => {
      val userResource = ResourceUtils.fromPersistenceResourceAndUser(resource)
      userResourceMap.put("maxResource", getYarnResourceMap(userResource.getMaxResource))
      userResourceMap.put("usedResource", getYarnResourceMap(userResource.getUsedResource))
      userResourceMap.put("lockedResource", getYarnResourceMap(userResource.getLockedResource))
      userResourceMap.put("leftResource", getYarnResourceMap(userResource.getLeftResource))
    })
    userResourceMap
  }

  def getLinkisResources(
      resources: util.List[PersistenceResource],
      engineType: String
  ): util.HashMap[String, util.HashMap[String, Any]] = {
    val userResourceMap = new util.HashMap[String, util.HashMap[String, Any]]
    resources.asScala.foreach(resource => {
      val userResource = ResourceUtils.fromPersistenceResourceAndUser(resource)
      userResourceMap.put("maxResource", getEcmResourceMap(userResource.getMaxResource, engineType))
      userResourceMap.put(
        "usedResource",
        getEcmResourceMap(userResource.getUsedResource, engineType)
      )
      userResourceMap.put(
        "lockedResource",
        getEcmResourceMap(userResource.getLockedResource, engineType)
      )
      userResourceMap.put(
        "leftResource",
        getEcmResourceMap(userResource.getLeftResource, engineType)
      )
    })
    userResourceMap
  }

  def getYarnResourceMap(resource: Resource): util.HashMap[String, Any] = {
    val resourceMap = new util.HashMap[String, Any]
    val yarnResource = resource.asInstanceOf[DriverAndYarnResource].getYarnResource
    resourceMap.put(
      "queueMemory",
      ByteTimeUtils.negativeByteStringAsGb(yarnResource.getQueueMemory + "b") + "G"
    )
    resourceMap.put("queueCpu", yarnResource.getQueueCores)
    resourceMap.put("instance", yarnResource.getQueueInstances)
    resourceMap
  }

  def getEcmResourceMap(resource: Resource, engineType: String): util.HashMap[String, Any] = {
    val resourceMap = new util.HashMap[String, Any]
    var loadInstanceResource = new LoadInstanceResource(0, 0, 0)
    if (engineType.contains("spark")) {
      loadInstanceResource = resource.asInstanceOf[DriverAndYarnResource].getLoadInstanceResource
    } else {
      loadInstanceResource = resource.asInstanceOf[LoadInstanceResource]
    }

    resourceMap.put(
      "memory",
      ByteTimeUtils.negativeByteStringAsGb(loadInstanceResource.getMemory + "b") + "G"
    )
    resourceMap.put("core", loadInstanceResource.getCores)
    resourceMap
  }

  def getUserCreator(userCreatorLabel: UserCreatorLabel): String = {
    "(" + userCreatorLabel.getUser + "," + userCreatorLabel.getCreator + ")"
  }

  def getEngineType(engineTypeLabel: EngineTypeLabel): String = {
    "(" + engineTypeLabel.getEngineType + "," + engineTypeLabel.getVersion + ")"
  }

}
