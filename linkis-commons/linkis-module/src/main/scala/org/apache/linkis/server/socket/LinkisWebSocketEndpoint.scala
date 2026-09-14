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

import org.apache.linkis.common.utils.Logging

import javax.servlet.http.{Cookie, HttpServletRequest}
import javax.websocket.{CloseReason, Endpoint, EndpointConfig, MessageHandler, Session}
import javax.websocket.server.HandshakeRequest

import scala.collection.JavaConverters._

/**
 * JSR-356 WebSocket endpoint for Linkis.
 *
 * <p>This endpoint is registered via
 * [[org.apache.linkis.server.conf.LinkisServletContextInitializer]] using the standard JSR-356
 * ServerContainer API. It works with any servlet container that supports JSR-356 (Jetty 9.4+,
 * Tomcat 8+, BES, etc.).
 *
 * <p>The [[ControllerServer]] instance is retrieved from the endpoint's user properties (set during
 * registration). The [[HandshakeRequest]] is retrieved from the user properties (stored by
 * [[HttpRequestCaptureConfigurator]] during the handshake) and adapted to an [[HttpServletRequest]]
 * for use by [[ServerSocket]] and `SecurityFilter.getLoginUser`.
 */
class LinkisWebSocketEndpoint extends Endpoint with Logging {

  private var controllerServer: ControllerServer = _
  private var socket: ServerSocket = _

  override def onOpen(session: Session, config: EndpointConfig): Unit = {
    val userProperties = config.getUserProperties

    // Retrieve ControllerServer from user properties (set during endpoint registration)
    controllerServer = userProperties
      .get("linkis.controllerServer")
      .asInstanceOf[ControllerServer]
    if (controllerServer == null) {
      logger.error("ControllerServer not found in EndpointConfig user properties")
      return
    }

    // Retrieve HandshakeRequest stored by HttpRequestCaptureConfigurator
    val handshakeRequest = userProperties
      .get("handshakeRequest")
      .asInstanceOf[HandshakeRequest]

    // Try to find the original HttpServletRequest from user properties
    // (containers like Jetty/Tomcat may store it under a container-specific key)
    val request = findHttpServletRequest(userProperties) match {
      case req: HttpServletRequest => req
      case _ if handshakeRequest != null =>
        // Fall back to an adapter that wraps the HandshakeRequest
        new HandshakeRequestAdapter(handshakeRequest)
      case _ =>
        logger.warn("No HttpServletRequest or HandshakeRequest found; WebSocket user auth may fail")
        null
    }

    socket = ServerSocket(request, controllerServer)
    socket.setSession(session)
    controllerServer.onOpen(socket)

    session.addMessageHandler(new MessageHandler.Whole[String] {
      override def onMessage(message: String): Unit =
        controllerServer.onMessage(socket, message)
    })
  }

  override def onClose(session: Session, closeReason: CloseReason): Unit = {
    if (controllerServer != null && socket != null) {
      controllerServer.onClose(
        socket,
        closeReason.getCloseCode.getCode,
        closeReason.getReasonPhrase
      )
    }
  }

  override def onError(session: Session, thr: Throwable): Unit = {
    logger.error(s"WebSocket error for session: ${session.getId}", thr)
  }

  /**
   * Try to find the `HttpServletRequest` from the user properties map. Different containers store
   * it under different keys:
   *   - Jetty: `"javax.servlet.http.HttpServletRequest"`
   *   - Tomcat: `"javax.servlet.http.HttpServletRequest"`
   *   - BES: similar key or container-specific
   */
  private def findHttpServletRequest(
      userProperties: java.util.Map[String, AnyRef]
  ): HttpServletRequest = {
    val keys = List("javax.servlet.http.HttpServletRequest", "httpRequest", "request")
    keys.foreach { key =>
      val value = userProperties.get(key)
      if (value != null && value.isInstanceOf[HttpServletRequest]) {
        return value.asInstanceOf[HttpServletRequest]
      }
    }
    val it = userProperties.values().iterator()
    while (it.hasNext) {
      val value = it.next()
      if (value != null && value.isInstanceOf[HttpServletRequest]) {
        return value.asInstanceOf[HttpServletRequest]
      }
    }
    null
  }

}

/**
 * Minimal adapter that wraps a JSR-356 [[HandshakeRequest]] and exposes it as an
 * [[HttpServletRequest]] for use by `SecurityFilter.getLoginUser`.
 *
 * <p>Only `getCookies()`, `getHeader(String)`, `getHeaders(String)`, and `getRequestURI()` are
 * implemented. All other methods throw `UnsupportedOperationException`.
 *
 * <p>This is needed because the standard JSR-356 `HandshakeRequest` interface does not expose the
 * original `HttpServletRequest`, but `SecurityFilter.getLoginUser` requires cookies and headers to
 * authenticate the WebSocket connection.
 */
private[socket] class HandshakeRequestAdapter(request: HandshakeRequest)
    extends HttpServletRequest {

  override def getCookies: Array[Cookie] = {
    val cookieHeaders = request.getHeaders.get("Cookie")
    if (cookieHeaders == null || cookieHeaders.isEmpty) null
    else {
      cookieHeaders.asScala
        .flatMap(_.split(";"))
        .map(_.trim)
        .filter(_.nonEmpty)
        .map { pair =>
          val idx = pair.indexOf('=')
          if (idx > 0) {
            new Cookie(pair.substring(0, idx).trim, pair.substring(idx + 1).trim)
          } else new Cookie(pair.trim, "")
        }
        .toArray
    }
  }

  override def getHeader(name: String): String = {
    val headers = request.getHeaders.get(name)
    if (headers == null || headers.isEmpty) null
    else headers.get(0)
  }

  override def getHeaders(name: String): java.util.Enumeration[String] = {
    val headers = request.getHeaders.get(name)
    if (headers == null || headers.isEmpty) {
      java.util.Collections.emptyEnumeration[String]
    } else {
      java.util.Collections.enumeration(headers)
    }
  }

  override def getRequestURI: String = {
    val uri = request.getRequestURI
    if (uri == null) "" else uri.getPath
  }

  // The following methods are not needed for WebSocket authentication.
  // They throw UnsupportedOperationException to prevent accidental misuse.
  override def getMethod: String = "GET"
  override def getPathInfo: String = null
  override def getPathTranslated: String = null
  override def getQueryString: String = request.getQueryString

  override def getRemoteUser: String =
    if (request.getUserPrincipal != null) request.getUserPrincipal.getName else null

  override def getUserPrincipal: java.security.Principal = request.getUserPrincipal
  override def isUserInRole(role: String): Boolean = request.isUserInRole(role)
  override def getRequestedSessionId: String = null
  override def getRequestURL: StringBuffer = new StringBuffer(getRequestURI)
  override def getServletPath: String = ""

  override def getContextPath: String = ""

  override def getSession(create: Boolean): javax.servlet.http.HttpSession = {
    val httpSession = request.getHttpSession
    if (httpSession != null) httpSession.asInstanceOf[javax.servlet.http.HttpSession] else null
  }

  override def getSession: javax.servlet.http.HttpSession = getSession(false)
  override def isRequestedSessionIdValid: Boolean = false
  override def isRequestedSessionIdFromCookie: Boolean = false
  override def isRequestedSessionIdFromURL: Boolean = false
  override def isRequestedSessionIdFromUrl: Boolean = false

  // --- ServletRequest methods (minimal) ---
  override def getAttribute(name: String): AnyRef = null

  override def getAttributeNames: java.util.Enumeration[String] =
    java.util.Collections.emptyEnumeration[String]

  override def getCharacterEncoding: String = null
  override def setCharacterEncoding(env: String): Unit = {}
  override def getContentLength: Int = -1
  override def getContentLengthLong: Long = -1L
  override def getContentType: String = null

  override def getInputStream: javax.servlet.ServletInputStream =
    throw new UnsupportedOperationException

  override def getParameter(name: String): String = null

  override def getParameterNames: java.util.Enumeration[String] = {
    val params = request.getParameterMap
    if (params == null) java.util.Collections.emptyEnumeration[String]
    else java.util.Collections.enumeration(params.keySet)
  }

  override def getParameterValues(name: String): Array[String] = {
    val params = request.getParameterMap
    if (params == null) null
    else {
      val values = params.get(name)
      if (values == null) null else values.toArray.asInstanceOf[Array[String]]
    }
  }

  override def getParameterMap: java.util.Map[String, Array[String]] = {
    val src = request.getParameterMap
    if (src == null) null
    else {
      val result = new java.util.HashMap[String, Array[String]]()
      val it = src.entrySet().iterator()
      while (it.hasNext) {
        val entry = it.next()
        val values = entry.getValue
        if (values != null) {
          result.put(entry.getKey, values.toArray.asInstanceOf[Array[String]])
        }
      }
      result
    }
  }

  override def getProtocol: String = "HTTP/1.1"
  override def getScheme: String = "ws"
  override def getServerName: String = "localhost"
  override def getServerPort: Int = 0

  override def getReader: java.io.BufferedReader =
    throw new UnsupportedOperationException

  override def getRemoteAddr: String = null
  override def getRemoteHost: String = null
  override def setAttribute(name: String, o: AnyRef): Unit = {}
  override def removeAttribute(name: String): Unit = {}
  override def getLocale: java.util.Locale = java.util.Locale.getDefault

  override def getLocales: java.util.Enumeration[java.util.Locale] =
    java.util.Collections.enumeration(
      java.util.Collections.singletonList(java.util.Locale.getDefault)
    )

  override def isSecure: Boolean = false
  override def getRequestDispatcher(path: String): javax.servlet.RequestDispatcher = null
  override def getRealPath(path: String): String = null
  override def getRemotePort: Int = 0
  override def getLocalName: String = null
  override def getLocalAddr: String = null
  override def getLocalPort: Int = 0
  override def getServletContext: javax.servlet.ServletContext = null

  override def startAsync: javax.servlet.AsyncContext =
    throw new UnsupportedOperationException

  override def startAsync(
      request: javax.servlet.ServletRequest,
      response: javax.servlet.ServletResponse
  ): javax.servlet.AsyncContext = throw new UnsupportedOperationException

  override def isAsyncStarted: Boolean = false
  override def isAsyncSupported: Boolean = false
  override def getAsyncContext: javax.servlet.AsyncContext = null

  override def getDispatcherType: javax.servlet.DispatcherType =
    javax.servlet.DispatcherType.REQUEST

  override def getAuthType: String = null
  override def changeSessionId: String = null
  override def authenticate(response: javax.servlet.http.HttpServletResponse): Boolean = false
  override def login(username: String, password: String): Unit = {}
  override def logout(): Unit = {}

  override def getParts: java.util.Collection[javax.servlet.http.Part] =
    java.util.Collections.emptyList[javax.servlet.http.Part]

  override def getPart(name: String): javax.servlet.http.Part = null

  override def upgrade[T <: javax.servlet.http.HttpUpgradeHandler](handlerClass: Class[T]): T =
    throw new UnsupportedOperationException

  override def getDateHeader(name: String): Long = -1L
  override def getIntHeader(name: String): Int = -1

  override def getHeaderNames: java.util.Enumeration[String] = {
    val headers = request.getHeaders
    if (headers == null) java.util.Collections.emptyEnumeration[String]
    else java.util.Collections.enumeration(headers.keySet)
  }

  override def getHttpServletMapping: javax.servlet.http.HttpServletMapping =
    throw new UnsupportedOperationException

  override def getTrailerFields: java.util.Map[String, String] =
    java.util.Collections.emptyMap[String, String]

  override def newPushBuilder: javax.servlet.http.PushBuilder = null
}
