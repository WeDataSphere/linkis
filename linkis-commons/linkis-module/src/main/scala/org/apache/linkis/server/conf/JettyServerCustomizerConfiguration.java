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

package org.apache.linkis.server.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.DispatcherType;

import java.io.File;
import java.util.EnumSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty-specific {@link WebServerFactoryCustomizer} bean.
 *
 * <p>Conditionally loaded only when Jetty is on the classpath. When the enterprise version
 * excludes Jetty and uses BES, this class will not be loaded by Spring, and BES's own
 * auto-configuration will provide the embedded servlet container.
 *
 * <p><b>Important:</b> All Jetty-specific types are referenced only inside method bodies
 * (anonymous inner classes), never in method signatures. This prevents
 * {@code NoClassDefFoundError} when Spring calls {@code getDeclaredMethods()} on this class
 * during bean definition processing, before the {@code @ConditionalOnClass} condition is
 * evaluated.
 */
@org.springframework.context.annotation.Configuration
@ConditionalOnClass(JettyServletWebServerFactory.class)
public class JettyServerCustomizerConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(JettyServerCustomizerConfiguration.class);

  @Bean
  public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyFactoryCustomizer() {
    return new WebServerFactoryCustomizer<JettyServletWebServerFactory>() {
      @Override
      public void customize(JettyServletWebServerFactory jettyServletWebServerFactory) {
        jettyServletWebServerFactory.addServerCustomizers(
            new JettyServerCustomizer() {
              @Override
              public void customize(Server server) {
                Handler[] childHandlersByClass =
                    server.getChildHandlersByClass(WebAppContext.class);
                final WebAppContext webApp = (WebAppContext) childHandlersByClass[0];
                FilterHolder filterHolder = new FilterHolder(CharacterEncodingFilter.class);
                filterHolder.setInitParameter(
                    "encoding",
                    org.apache.linkis.common.conf.Configuration.BDP_ENCODING().getValue());
                filterHolder.setInitParameter("forceEncoding", "true");
                webApp.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));

                // session handler
                webApp.setSessionHandler(new SessionHandler());

                if (!ServerConfiguration.BDP_SERVER_DISTINCT_MODE().getValue()) {
                  // setupWebAppContext inlined to avoid Jetty types in method signatures
                  webApp.setContextPath(
                      ServerConfiguration.BDP_SERVER_SERVER_CONTEXT_PATH().getValue());
                  File warPath = new File(ServerConfiguration.BDP_SERVER_WAR().getValue());
                  File[] warFiles = warPath.listFiles();
                  if (warFiles != null) {
                    for (File f : warFiles) {
                      if (f.getName().endsWith(".war")) {
                        warPath = f;
                        break;
                      }
                    }
                  }
                  if (warPath.isDirectory()) {
                    webApp.setResourceBase(warPath.getPath());
                    webApp.setParentLoaderPriority(true);
                  } else {
                    webApp.setWar(warPath.getAbsolutePath());
                    webApp.setExtractWAR(true);
                    File warTempDirectory =
                        new File(ServerConfiguration.BDP_SERVER_WAR_TEMPDIR().getValue());
                    if (warTempDirectory.exists()) {
                      logger.warn("delete {}, since it is exists.", warTempDirectory.getPath());
                      try {
                        FileUtils.deleteDirectory(warTempDirectory);
                      } catch (java.io.IOException e) {
                        logger.warn(
                            "Failed to delete war temp directory: {}",
                            warTempDirectory.getPath(),
                            e);
                      }
                    }
                    warTempDirectory.mkdir();
                    logger.info("BDPJettyServer Webapps path: {}.", warTempDirectory.getPath());
                    webApp.setTempDirectory(warTempDirectory);
                  }
                  // Explicit bind to root
                  webApp.addServlet(new ServletHolder(new DefaultServlet()), "/*");
                  webApp.setWelcomeFiles(new String[] {"index.html", "index.htm"});
                  webApp
                      .getSessionHandler()
                      .setMaxInactiveInterval(
                          (int)
                              (ServerConfiguration.BDP_SERVER_WEB_SESSION_TIMEOUT()
                                      .getValue()
                                      .toLong()
                                  / 1000));
                  webApp.setInitParameter(
                      "org.eclipse.jetty.servlet.Default.dirAllowed",
                      ServerConfiguration.BDP_SERVER_SERVER_DEFAULT_DIR_ALLOWED().getValue());
                }
              }
            });
      }
    };
  }
}
