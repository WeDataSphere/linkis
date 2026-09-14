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
import org.apache.linkis.server.socket.controller.ServerEventService

/**
 * Thin facade that delegates to [[BDPServerHelper]] for backward compatibility.
 *
 * The original Jetty-specific setup methods (setupSpringRestApiContextHandler,
 * setupControllerServer, setupWebAppContext) have been moved to
 * [[org.apache.linkis.server.conf.JettyServerCustomizerConfiguration]] which is conditionally
 * loaded only when Jetty is on the classpath.
 *
 * The gson, jacksonJson, getControllerServer and addServerEventService members are preserved here
 * as delegates so that the 79+ callers across the codebase continue to work without any changes.
 */
private[linkis] object BDPJettyServerHelper extends Logging {

  // ---- Delegates to BDPServerHelper for backward compatibility ----

  implicit val gson: com.google.gson.Gson = BDPServerHelper.gson

  implicit val jacksonJson: com.fasterxml.jackson.databind.ObjectMapper =
    BDPServerHelper.jacksonJson

  private[server] def getControllerServer =
    BDPServerHelper.getControllerServer

  def addServerEventService(serverEventService: ServerEventService): Unit =
    BDPServerHelper.addServerEventService(serverEventService)

}
