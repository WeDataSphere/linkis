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

package org.apache.linkis.entrance.utils

import org.apache.linkis.common.exception.ErrorException
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.entrance.conf.EntranceConfiguration
import org.apache.linkis.entrance.exception.JobHistoryFailedException
import org.apache.linkis.entrance.execute.EntranceJob
import org.apache.linkis.governance.common.constant.job.JobRequestConstants
import org.apache.linkis.governance.common.entity.job.JobRequest
import org.apache.linkis.governance.common.protocol.job._
import org.apache.linkis.governance.common.utils.ECPathUtils
import org.apache.linkis.manager.common.protocol.resource.ResourceWithStatus
import org.apache.linkis.manager.label.utils.LabelUtil
import org.apache.linkis.protocol.constants.TaskConstant
import org.apache.linkis.protocol.query.cache.{CacheTaskResult, RequestReadCache}
import org.apache.linkis.rpc.Sender
import org.apache.linkis.scheduler.queue.SchedulerEventState

import org.apache.commons.lang3.StringUtils

import javax.servlet.http.HttpServletRequest

import java.io.File
import java.util
import java.util.Date

import scala.collection.JavaConverters._

import com.google.common.net.InetAddresses

object JobHistoryHelper extends Logging {

  private val sender =
    Sender.getSender(EntranceConfiguration.JOBHISTORY_SPRING_APPLICATION_NAME.getValue)

  private val SUCCESS_FLAG = 0

  def getCache(
      executionCode: String,
      user: String,
      labelStrList: util.List[String],
      readCacheBefore: Long
  ): CacheTaskResult = {
    val requestReadCache =
      new RequestReadCache(executionCode, user, labelStrList, readCacheBefore)
    sender.ask(requestReadCache) match {
      case c: CacheTaskResult => c
      case _ => null
    }
  }

  def getStatusByTaskID(taskID: Long): String = {
    val task = getTaskByTaskID(taskID)
    if (task == null) SchedulerEventState.Cancelled.toString
    else task.getStatus
  }

  def getProgressByTaskID(taskID: Long): String = {
    val task = getTaskByTaskID(taskID)
    if (task == null) "0" else task.getProgress
  }

  def getRequestIpAddr(req: HttpServletRequest): String = {
    val addrList = List(
      Option(req.getHeader("x-forwarded-for")).getOrElse("").split(",")(0),
      Option(req.getHeader("Proxy-Client-IP")).getOrElse(""),
      Option(req.getHeader("WL-Proxy-Client-IP")).getOrElse(""),
      Option(req.getHeader("HTTP_CLIENT_IP")).getOrElse(""),
      Option(req.getHeader("HTTP_X_FORWARDED_FOR")).getOrElse("")
    )
    val afterProxyIp = addrList
      .find(ip => {
        StringUtils.isNotEmpty(ip) && InetAddresses.isInetAddress(ip)
      })
      .getOrElse("")
    if (StringUtils.isNotEmpty(afterProxyIp)) {
      afterProxyIp
    } else {
      req.getRemoteAddr
    }
  }

  /**
   * If the task cannot be found in memory, you can directly kill it
   *
   * @param taskID
   */
  def forceKill(taskID: Long): Unit = {
    val jobRequest = new JobRequest
    jobRequest.setId(taskID)
    jobRequest.setStatus(SchedulerEventState.Cancelled.toString)
    jobRequest.setProgress(EntranceJob.JOB_COMPLETED_PROGRESS.toString)
    jobRequest.setUpdatedTime(new Date(System.currentTimeMillis()))
    val jobReqUpdate = JobReqUpdate(jobRequest)
    sender.ask(jobReqUpdate)
  }

  /**
   * Batch forced kill
   *
   * @param taskIdList
   */
  def forceBatchKill(taskIdList: util.ArrayList[java.lang.Long]): Unit = {
    val jobReqList = new util.ArrayList[JobRequest]()
    taskIdList.asScala.foreach(taskID => {
      val jobRequest = new JobRequest
      jobRequest.setId(taskID)
      jobRequest.setStatus(SchedulerEventState.Cancelled.toString)
      jobRequest.setProgress(EntranceJob.JOB_COMPLETED_PROGRESS.toString)
      jobRequest.setUpdatedTime(new Date(System.currentTimeMillis()))
      jobReqList.add(jobRequest)
    })
    val jobReqBatchUpdate = JobReqBatchUpdate(jobReqList)
    sender.ask(jobReqBatchUpdate)
  }

  /**
   * Get all consume queue task and batch update instances(获取所有消费队列中的任务进行批量更新)
   *
   * @param taskIdList
   * @param retryWhenUpdateFail
   */
  def updateAllConsumeQueueTask(
      taskIdList: util.List[Long],
      retryWhenUpdateFail: Boolean = false
  ): Unit = {

    if (taskIdList.isEmpty) return

    val updateTaskIds = new util.ArrayList[Long]()

    if (
        EntranceConfiguration.ENTRANCE_UPDATE_BATCH_SIZE.getValue > 0 &&
        taskIdList.size() > EntranceConfiguration.ENTRANCE_UPDATE_BATCH_SIZE.getValue
    ) {
      for (i <- 0 until EntranceConfiguration.ENTRANCE_UPDATE_BATCH_SIZE.getValue) {
        updateTaskIds.add(taskIdList.get(i))
      }
    } else {
      updateTaskIds.addAll(taskIdList)
    }
    val list = new util.ArrayList[Long]()
    list.addAll(taskIdList)
    try {
      val successTaskIds = updateBatchInstancesEmpty(updateTaskIds)
      if (retryWhenUpdateFail) {
        list.removeAll(successTaskIds)
      } else {
        list.removeAll(updateTaskIds)
      }
    } catch {
      case e: Exception =>
        logger.warn("update batch instances failed, wait for retry", e)
        Thread.sleep(1000)
    }
    updateAllConsumeQueueTask(list, retryWhenUpdateFail)

  }

  /**
   * Batch update instances(批量更新instances字段)
   *
   * @param taskIdList
   * @return
   */
  def updateBatchInstancesEmpty(taskIdList: util.List[Long]): util.List[Long] = {
    val jobReqList = new util.ArrayList[JobRequest]()
    taskIdList.asScala.foreach(taskID => {
      val jobRequest = new JobRequest
      jobRequest.setId(taskID)
      jobRequest.setInstances("")
      jobReqList.add(jobRequest)
    })
    val jobReqBatchUpdate = JobReqBatchUpdate(jobReqList)
    Utils.tryCatch {
      val response = sender.ask(jobReqBatchUpdate)
      response match {
        case resp: util.List[JobRespProtocol] =>
          // todo filter success data, rpc have bug
          //          resp.asScala
          //            .filter(r =>
          //              r.getStatus == SUCCESS_FLAG && r.getData.containsKey(JobRequestConstants.JOB_ID)
          //            )
          //            .map(_.getData.get(JobRequestConstants.JOB_ID).asInstanceOf[java.lang.Long])
          //            .toList

          taskIdList
        case _ =>
          throw JobHistoryFailedException(
            "update batch instances from jobhistory not a correct List type"
          )
      }
    } {
      case errorException: ErrorException => throw errorException
      case e: Exception =>
        val e1 =
          JobHistoryFailedException(
            s"update batch instances ${taskIdList.asScala.mkString(",")} error"
          )
        e1.initCause(e)
        throw e
    }
  }

  /**
   * query wait for failover task(获取待故障转移的任务)
   *
   * @param reqMap
   * @param statusList
   * @param startTimestamp
   * @param limit
   * @return
   */
  def queryWaitForFailoverTask(
      reqMap: util.Map[String, java.lang.Long],
      statusList: util.List[String],
      startTimestamp: Long,
      limit: Int
  ): util.List[JobRequest] = {
    val requestFailoverJob = RequestFailoverJob(reqMap, statusList, startTimestamp, limit)
    val tasks = Utils.tryCatch {
      val response = sender.ask(requestFailoverJob)
      response match {
        case responsePersist: JobRespProtocol =>
          val status = responsePersist.getStatus
          if (status != SUCCESS_FLAG) {
            logger.error(s"query from jobHistory status failed, status is $status")
            throw JobHistoryFailedException("query from jobHistory status failed")
          }
          val data = responsePersist.getData
          data.get(JobRequestConstants.JOB_HISTORY_LIST) match {
            case tasks: List[JobRequest] =>
              tasks.asJava
            case _ =>
              throw JobHistoryFailedException(
                s"query from jobhistory not a correct List type, instances ${reqMap.keySet()}"
              )
          }
        case _ =>
          logger.error("get query response incorrectly")
          throw JobHistoryFailedException("get query response incorrectly")
      }
    } {
      case errorException: ErrorException => throw errorException
      case e: Exception =>
        val e1 =
          JobHistoryFailedException(s"query failover task error, instances ${reqMap.keySet()} ")
        e1.initCause(e)
        throw e
    }
    tasks
  }

  private def getTaskByTaskID(taskID: Long): JobRequest = {
    val jobRequest = new JobRequest
    jobRequest.setId(taskID)
    jobRequest.setSource(null)
    val jobReqQuery = JobReqQuery(jobRequest)
    val task = Utils.tryCatch {
      val taskResponse = sender.ask(jobReqQuery)
      taskResponse match {
        case responsePersist: JobRespProtocol =>
          val status = responsePersist.getStatus
          if (status != SUCCESS_FLAG) {
            logger.error(s"query from jobHistory status failed, status is $status")
            throw JobHistoryFailedException("query from jobHistory status failed")
          } else {
            val data = responsePersist.getData
            data.get(JobRequestConstants.JOB_HISTORY_LIST) match {
              case tasks: util.List[JobRequest] =>
                if (tasks.size() > 0) tasks.get(0)
                else null
              case _ =>
                throw JobHistoryFailedException(
                  s"query from jobhistory not a correct List type taskId is $taskID"
                )
            }
          }
        case _ =>
          logger.error("get query response incorrectly")
          throw JobHistoryFailedException("get query response incorrectly")
      }
    } {
      case errorException: ErrorException => throw errorException
      case e: Exception =>
        val e1 = JobHistoryFailedException(s"query taskId $taskID error")
        e1.initCause(e)
        throw e
    }
    task
  }

  def updateJobRequestMetrics(
      jobRequest: JobRequest,
      resourceInfo: util.Map[String, ResourceWithStatus],
      infoMap: util.Map[String, AnyRef]
  ): Unit = {
    // update resource
    if (jobRequest.getMetrics == null) {
      jobRequest.setMetrics(new util.HashMap[String, AnyRef]())
    }
    val metricsMap = jobRequest.getMetrics
    val resourceMap = metricsMap.get(TaskConstant.JOB_YARNRESOURCE)
    val ecResourceMap =
      if (resourceInfo == null) new util.HashMap[String, ResourceWithStatus] else resourceInfo
    if (resourceMap != null) {
      resourceMap.asInstanceOf[util.Map[String, ResourceWithStatus]].putAll(ecResourceMap)
    } else {
      metricsMap.put(TaskConstant.JOB_YARNRESOURCE, ecResourceMap)
    }
    var engineInstanceMap: util.Map[String, AnyRef] = null
    if (metricsMap.containsKey(TaskConstant.JOB_ENGINECONN_MAP)) {
      engineInstanceMap = metricsMap
        .get(TaskConstant.JOB_ENGINECONN_MAP)
        .asInstanceOf[util.Map[String, AnyRef]]
    } else {
      engineInstanceMap = new util.HashMap[String, AnyRef]()
      metricsMap.put(TaskConstant.JOB_ENGINECONN_MAP, engineInstanceMap)
    }

    if (null != infoMap && infoMap.containsKey(TaskConstant.TICKET_ID)) {
      val ticketId = infoMap.get(TaskConstant.TICKET_ID).asInstanceOf[String]
      val engineExtraInfoMap = engineInstanceMap
        .getOrDefault(ticketId, new util.HashMap[String, AnyRef])
        .asInstanceOf[util.Map[String, AnyRef]]
      engineExtraInfoMap.putAll(infoMap)
      engineInstanceMap.put(ticketId, engineExtraInfoMap)
    } else {
      logger.warn("Ec info map must contains ticketID")
    }

    if (null != infoMap && infoMap.containsKey(TaskConstant.JOB_REQUEST_EC_TIME)) {
      metricsMap.put(
        TaskConstant.JOB_REQUEST_EC_TIME,
        infoMap.get(TaskConstant.JOB_REQUEST_EC_TIME)
      )
    }

    if (null != infoMap && infoMap.containsKey(TaskConstant.JOB_SUBMIT_TO_EC_TIME)) {
      metricsMap.put(
        TaskConstant.JOB_SUBMIT_TO_EC_TIME,
        infoMap.get(TaskConstant.JOB_SUBMIT_TO_EC_TIME)
      )
    }
    if (null != infoMap && infoMap.containsKey(TaskConstant.ENGINE_INSTANCE)) {
      metricsMap.put(TaskConstant.ENGINE_INSTANCE, infoMap.get(TaskConstant.ENGINE_INSTANCE))
    }
    if (null != infoMap && infoMap.containsKey(TaskConstant.JOB_IS_REUSE)) {
      metricsMap.put(TaskConstant.JOB_IS_REUSE, infoMap.get(TaskConstant.JOB_IS_REUSE))
    }

    // 添加引擎日志路径到metrics
    addEngineLogPathToMetrics(jobRequest, metricsMap, infoMap)
  }

  /**
   * 添加引擎日志路径到metrics中
   * Add engine log path to metrics
   *
   * @param jobRequest jobRequest对象
   * @param metricsMap metrics映射
   * @param infoMap 信息映射
   */
  private def addEngineLogPathToMetrics(
      jobRequest: JobRequest,
      metricsMap: util.Map[String, AnyRef],
      infoMap: util.Map[String, AnyRef]
  ): Unit = {
    // 检查功能开关
    if (!EntranceConfiguration.ENGINE_LOG_PATH_FEATURE_ENABLE.getValue) {
      return
    }

    Utils.tryCatch {
      // 获取必要参数
      val user = jobRequest.getSubmitUser
      val ticketId = if (null != infoMap && infoMap.containsKey(TaskConstant.TICKET_ID)) {
        infoMap.get(TaskConstant.TICKET_ID).asInstanceOf[String]
      } else {
        null
      }
      
      // 使用标准API获取引擎类型
      val engineType = LabelUtil.getEngineType(jobRequest.getLabels)
      
      if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(ticketId) && StringUtils.isNotBlank(engineType)) {
        // 构建引擎日志路径 - 参考DefaultLocalDirsHandleService的实现
        val pathSuffix = ECPathUtils.getECWOrkDirPathSuffix(user, ticketId, engineType)
        val engineLogPath = pathSuffix + File.separator + "logs"
        
        // 将引擎日志路径插入到metrics中
        metricsMap.put("engineLogPath", engineLogPath)
        logger.info(s"Added engineLogPath to metrics: $engineLogPath for user: $user, ticketId: $ticketId, engineType: $engineType")
        
        // 如果启用UDF日志路径功能，也添加UDF日志路径
        if (EntranceConfiguration.UDF_LOG_PATH_FEATURE_ENABLE.getValue) {
          val udfLogPath = pathSuffix + File.separator + "logs" + File.separator + "udf"
          metricsMap.put("udfLogPath", udfLogPath)
          logger.info(s"Added udfLogPath to metrics: $udfLogPath for user: $user, ticketId: $ticketId, engineType: $engineType")
        }
      } else {
        logger.warn(s"Cannot build engine log path due to missing parameters: user=$user, ticketId=$ticketId, engineType=$engineType")
      }
    } { t =>
      logger.warn("Failed to add engine log path to metrics", t)
    }
  }

}
