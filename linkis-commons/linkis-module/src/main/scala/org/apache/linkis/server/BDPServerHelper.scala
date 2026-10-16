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

package org.apache.linkis.server

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.server.conf.ServerConfiguration._
import org.apache.linkis.server.socket.ControllerServer
import org.apache.linkis.server.socket.controller.{ServerEventService, ServerListenerEventBus}

import javax.servlet.{Filter, MultipartConfigElement}

import java.lang
import java.lang.reflect.Type
import java.text.SimpleDateFormat

import scala.collection.mutable

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson._

/**
 * Container-agnostic server helper. Holds gson, jacksonJson, controllerServer and event bus logic
 * that is independent of the underlying servlet container (Jetty, BES, etc.).
 *
 * [[BDPJettyServerHelper]] delegates to this object for backward compatibility.
 */
private[linkis] object BDPServerHelper extends Logging {

  private var serverListenerEventBus: ServerListenerEventBus = _
  private var controllerServer: ControllerServer = _
  private val services = mutable.Buffer[ServerEventService]()

  private[server] def getControllerServer: ControllerServer = controllerServer

  private[server] def getOrCreateControllerServer(): ControllerServer = {
    if (controllerServer != null) return controllerServer
    synchronized {
      if (controllerServer != null) return controllerServer
      createServerListenerEventBus()
      controllerServer = new ControllerServer(serverListenerEventBus)
      controllerServer
    }
  }

  private def createServerListenerEventBus(): Unit = {
    serverListenerEventBus = new ServerListenerEventBus(
      BDP_SERVER_EVENT_QUEUE_SIZE.getValue,
      "WebSocket-Server-Event-ListenerBus",
      BDP_SERVER_EVENT_CONSUMER_THREAD_SIZE.getValue,
      BDP_SERVER_EVENT_CONSUMER_THREAD_FREE_MAX.getValue.toLong
    )
    services.foreach(serverListenerEventBus.addListener)
    serverListenerEventBus.start()
  }

  def addServerEventService(serverEventService: ServerEventService): Unit = {
    if (serverListenerEventBus != null) serverListenerEventBus.addListener(serverEventService)
    else services += serverEventService
  }

  def getSecurityFilterClass(): Class[Filter] =
    Class.forName(BDP_SERVER_SECURITY_FILTER.getValue).asInstanceOf[Class[Filter]]

  def getMultipartConfigElement(): MultipartConfigElement =
    org.apache.linkis.DataWorkCloudApplication.getApplicationContext
      .getBean(classOf[MultipartConfigElement])

  implicit val gson: Gson = new GsonBuilder()
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    .serializeNulls
    .registerTypeAdapter(
      classOf[java.lang.Double],
      new JsonSerializer[java.lang.Double] {

        override def serialize(
            t: lang.Double,
            `type`: Type,
            jsonSerializationContext: JsonSerializationContext
        ): JsonElement =
          if (t == t.longValue()) new JsonPrimitive(t.longValue()) else new JsonPrimitive(t)

      }
    )
    .create

  implicit val jacksonJson: ObjectMapper =
    new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))

}
