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

package org.apache.linkis.entrance.interceptor.impl

import org.apache.linkis.common.conf.Configuration
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.governance.common.entity.job.JobRequest
import org.apache.linkis.governance.common.protocol.conf.{TemplateConfRequest, TemplateConfResponse}
import org.apache.linkis.manager.label.constant.LabelKeyConstant
import org.apache.linkis.protocol.utils.TaskUtils
import org.apache.linkis.rpc.Sender
import org.apache.linkis.server.BDPJettyServerHelper

import org.apache.commons.lang3.StringUtils

import java.{lang, util}
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

object TemplateConfUtils extends Logging {

  private val templateCache: LoadingCache[String, util.List[TemplateConfResponse]] = CacheBuilder
    .newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(new CacheLoader[String, util.List[TemplateConfResponse]]() {

      override def load(templateUuid: String): util.List[TemplateConfResponse] = {
        var templateList = Utils.tryAndWarn {
          val sender: Sender = Sender
            .getSender(Configuration.CLOUD_CONSOLE_CONFIGURATION_SPRING_APPLICATION_NAME.getValue)

          logger.info(s"load template configuration data templateUuid:$templateUuid")
          sender.ask(new TemplateConfRequest(templateUuid)) match {
            case response: util.List[TemplateConfResponse] => response
            case _ =>
              logger
                .warn(s"load template configuration data templateUuid:$templateUuid loading failed")
              new util.ArrayList[TemplateConfResponse](0)
          }
        }
        if (templateList.size() == 0) {
          logger.warn(s"template configuration data loading failed, plaese check warn log")
        }
        templateList
      }

    })

  def dealWithStartParams(jobRequest: JobRequest, logAppender: lang.StringBuilder): JobRequest = {
    jobRequest match {
      case requestPersistTask: JobRequest =>
        val params = requestPersistTask.getParams
        val startMap = TaskUtils.getStartupMap(params)
        logger.debug("jobRequest startMap params :{} ", startMap)
        val templateUuid = startMap.getOrDefault(LabelKeyConstant.TEMPLATE_CONF_KEY, "").toString
        if (StringUtils.isNotBlank(templateUuid)) {
          logger.debug("jobRequest startMap param template id is empty")
        } else {
          logger.info("try to get template conf list with templateUid:{} ", templateUuid)
          val templateConflist: util.List[TemplateConfResponse] = templateCache.get(templateUuid)
          logger.debug(
            s"Get template conf list: ${BDPJettyServerHelper.gson.toJson(templateConflist)}"
          )

          if (templateConflist != null && templateConflist.size() > 0) {
            val keyList = new util.HashMap[String, AnyRef]()
            templateConflist.asScala.foreach(ele => {
              val key = ele.getKey
              val oldValue = startMap.get(key)
              if (oldValue != null && StringUtils.isEmpty(oldValue.toString)) {
                val newValue = ele.getConfigValue
                logger.info(s"key:$key value:$newValue will add to startMap params")
                logAppender.append(s"key:$key value:$newValue will add to startMap params")
                keyList.put(key, newValue)
              }

            })
            if (keyList.size() > 0) {
              TaskUtils.addStartupMap(params, keyList)
            }
          }

        }
      case _ =>
    }
    jobRequest
  }

}
