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

package org.apache.linkis.server.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpMethodSecurityFilter is used to disable unsafe HTTP methods (TRACE and TRACK) to comply with
 * security requirements. This filter applies to all Spring MVC based services.
 */
public class HttpMethodSecurityFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(HttpMethodSecurityFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    logger.info("HttpMethodSecurityFilter initialized successfully");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String method = httpRequest.getMethod();
      String uri = httpRequest.getRequestURI();

      logger.debug("Incoming request - Method: {}, URI: {}", method, uri);

      // Check if the request method is TRACE or TRACK
      if ("TRACE".equalsIgnoreCase(method) || "TRACK".equalsIgnoreCase(method)) {
        logger.warn("Blocked unsafe HTTP method: {} for request: {}", method, uri);

        httpResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        httpResponse.setContentType("application/json;charset=UTF-8");

        String errorMessage = "HTTP method " + method + " is not allowed for security reasons";
        try (PrintWriter writer = httpResponse.getWriter()) {
          writer.write("{\"message\":\"" + errorMessage + "\"}");
          writer.flush();
        }
        return;
      }
    }

    // Allow all other HTTP methods
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Cleanup if needed
  }
}
