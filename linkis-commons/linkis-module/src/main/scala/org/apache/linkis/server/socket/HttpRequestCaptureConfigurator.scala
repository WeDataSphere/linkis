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

package org.apache.linkis.server.socket

import javax.websocket.HandshakeResponse
import javax.websocket.server.{HandshakeRequest, ServerEndpointConfig}

/**
 * JSR-356 `ServerEndpointConfig.Configurator` that captures HTTP request data during the WebSocket
 * handshake.
 *
 * <p>The `HandshakeRequest` is stored in the endpoint's user properties under the key
 * `"handshakeRequest"` so that [[LinkisWebSocketEndpoint]] can retrieve it during `onOpen` to
 * extract cookies and headers for user authentication.
 *
 * <p>This replaces the Jetty-specific `WebSocketCreator` that was previously used in
 * `ControllerServer.configure()`.
 */
class HttpRequestCaptureConfigurator extends ServerEndpointConfig.Configurator {

  override def modifyHandshake(
      sec: ServerEndpointConfig,
      request: HandshakeRequest,
      response: HandshakeResponse
  ): Unit = {
    // Store the handshake request so the endpoint can extract cookies/headers
    sec.getUserProperties.put("handshakeRequest", request)
  }

}
