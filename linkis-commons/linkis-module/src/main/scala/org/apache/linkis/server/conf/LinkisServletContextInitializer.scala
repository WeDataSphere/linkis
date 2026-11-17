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

package org.apache.linkis.server.conf

import org.apache.linkis.common.conf.Configuration
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.server.BDPServerHelper
import org.apache.linkis.server.socket.{HttpRequestCaptureConfigurator, LinkisWebSocketEndpoint}

import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.{Bean, Configuration => SpringConfiguration}
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.servlet.DispatcherServlet

import javax.servlet.{DispatcherType, ServletContext}
import javax.websocket.server.ServerContainer

import java.util.EnumSet

/**
 * Container-agnostic servlet context initializer.
 *
 * <p>Registers REST API (DispatcherServlet + SecurityFilter) and WebSocket endpoint via standard
 * Servlet API and JSR-356. Works with any embedded servlet container (Jetty, Tomcat, BES, etc.).
 */
@SpringConfiguration
class LinkisServletContextInitializer extends Logging {

  @Bean(name = Array("linkisServletContextInitializerBean"))
  def servletContextInitializerBean(): ServletContextInitializer =
    new ServletContextInitializer {

      override def onStartup(servletContext: ServletContext): Unit = {
        // REQ-06 v1.2: this container-agnostic initializer is the SOLE REST/WS/Security registrar
        // for jetty|tomcat|bes (it is intentionally unconditional). Log the branch decision so
        // AC-1 can observe which Jetty-private customizer state is in effect.
        val container =
          Option(System.getProperty("linkis.web.container"))
            .map(_.trim)
            .filter(_.nonEmpty)
            .getOrElse("jetty")
        logger.info(
          s"[REQ-06] LinkisServletContextInitializer applied (universal REST/WS/encoding/Security for " +
            s"jetty|tomcat|bes); linkis.web.container=$container; Jetty customizer " +
            s"${if (container == "jetty") "ACTIVE (jetty branch)"
            else "NOT applied (non-jetty branch)"}"
        )
        // 0. Session timeout (REQ-06 FR-5): ServletContext.setSessionTimeout takes MINUTES.
        //    Under the Jetty branch the timeout is applied by JettyServerCustomizerConfiguration
        //    via SessionHandler#setMaxInactiveInterval (seconds), so we skip it here: calling
        //    ServletContext#setSessionTimeout on Jetty resolves to an abstract method on
        //    WebAppContext$Context and throws AbstractMethodError (an Error — Utils.tryCatch
        //    does not catch it), aborting web server startup.
        try {
          applySessionTimeout(servletContext)
        } catch {
          case t: Throwable =>
            logger.error(
              s"[REQ-06] session timeout setup failed, continue startup: ${t.getMessage}",
              t
            )
        }

        // 1. CharacterEncodingFilter
        val encodingFilter =
          servletContext.addFilter("characterEncoding", classOf[CharacterEncodingFilter])
        encodingFilter.setInitParameter("encoding", Configuration.BDP_ENCODING.getValue)
        encodingFilter.setInitParameter("forceEncoding", "true")
        encodingFilter.addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), false, "/*")

        // 2. Spring REST API DispatcherServlet
        val context = new AnnotationConfigWebApplicationContext
        context.setConfigLocation("")
        val dispatcher = new DispatcherServlet(context)
        val servletReg = servletContext.addServlet("springrestful", dispatcher)
        val multipartConfigElement =
          BDPServerHelper.getMultipartConfigElement()
        servletReg.setMultipartConfig(multipartConfigElement)

        val restfulUri = ServerConfiguration.BDP_SERVER_RESTFUL_URI.getValue
        val restfulPath =
          if (restfulUri.endsWith("/*")) restfulUri
          else if (restfulUri.endsWith("/")) restfulUri + "*"
          else restfulUri + "/*"
        servletReg.addMapping(restfulPath)

        // 3. SecurityFilter
        val securityFilterClass = BDPServerHelper.getSecurityFilterClass()
        val securityFilter = servletContext.addFilter("securityFilter", securityFilterClass)
        securityFilter.addMappingForUrlPatterns(
          EnumSet.allOf(classOf[DispatcherType]),
          false,
          restfulPath
        )

        // 4. WebSocket endpoint (JSR-356)
        if (ServerConfiguration.BDP_SERVER_SOCKET_MODE.getValue) {
          setupWebSocketEndpoint(servletContext)
        }
      }

    }

  private def applySessionTimeout(servletContext: ServletContext): Unit = {
    // REQ-06 v1.3: under the jetty branch the timeout is applied by
    // JettyServerCustomizerConfiguration via SessionHandler#setMaxInactiveInterval (seconds).
    // Calling ServletContext#setSessionTimeout here on Jetty is both redundant and unsafe:
    // WebAppContext$Context does not declare setSessionTimeout, so the call resolves to an
    // abstract method and throws AbstractMethodError, which aborts web server startup.
    val container =
      Option(System.getProperty("linkis.web.container"))
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse("jetty")
    if (container == "jetty") {
      logger.info(
        "[REQ-06] session timeout under jetty is applied by JettyServerCustomizerConfiguration " +
          "(SessionHandler); skip ServletContext#setSessionTimeout"
      )
      return
    }
    Utils.tryCatch[Unit] {
      val timeoutMs =
        ServerConfiguration.BDP_SERVER_WEB_SESSION_TIMEOUT.getValue.toLong
      // Servlet spec unit is MINUTES; clamp to >= 1 minute (design §7.3)
      val minutes = math.max(1, (timeoutMs / 60000L).toInt)
      servletContext.setSessionTimeout(minutes)
      logger.info(
        s"[REQ-06] session timeout normalized: $timeoutMs ms -> $minutes min (container-agnostic)"
      )
    } { e =>
      logger.warn(s"Failed to apply session timeout, container default kept: $e", e)
      ()
    }
  }

  private def setupWebSocketEndpoint(servletContext: ServletContext): Unit = {
    val controllerServer = BDPServerHelper.getOrCreateControllerServer()
    val serverContainer = servletContext
      .getAttribute("javax.websocket.server.ServerContainer")
      .asInstanceOf[ServerContainer]
    if (serverContainer == null) {
      logger.warn(
        "javax.websocket.server.ServerContainer not found in ServletContext. " +
          "WebSocket endpoint will not be registered. Make sure the servlet container supports JSR-356."
      )
      return
    }
    val configurator = new HttpRequestCaptureConfigurator()
    val socketUri = ServerConfiguration.BDP_SERVER_SOCKET_URI.getValue
    val socketPath =
      if (socketUri.endsWith("/*")) socketUri
      else if (socketUri.endsWith("/")) socketUri + "*"
      else socketUri + "/*"
    val endpointConfig = javax.websocket.server.ServerEndpointConfig.Builder
      .create(classOf[LinkisWebSocketEndpoint], socketPath)
      .configurator(configurator)
      .build()
    // Store controllerServer reference in endpoint user properties so it's
    // available in EndpointConfig.getUserProperties() during onOpen
    endpointConfig.getUserProperties.put("linkis.controllerServer", controllerServer)
    serverContainer.addEndpoint(endpointConfig)
    logger.info(s"WebSocket endpoint registered at path: $socketPath")
  }

}
