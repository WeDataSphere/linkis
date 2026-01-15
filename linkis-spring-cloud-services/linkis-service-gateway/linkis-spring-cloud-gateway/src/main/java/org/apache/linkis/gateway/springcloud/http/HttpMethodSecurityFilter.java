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

package org.apache.linkis.gateway.springcloud.http;

import org.apache.linkis.server.Message;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * HttpMethodSecurityFilter is used to disable unsafe HTTP methods (TRACE and TRACK)
 * to comply with security requirements.
 */
public class HttpMethodSecurityFilter implements GlobalFilter, Ordered {
  private static final Logger logger = LoggerFactory.getLogger(HttpMethodSecurityFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    HttpMethod method = exchange.getRequest().getMethod();

    // Check if the request method is TRACE or TRACK
    if (HttpMethod.TRACE.equals(method) || isTrackMethod(method)) {
      logger.warn(
          "Blocked unsafe HTTP method: {} for request: {}",
          method,
          exchange.getRequest().getURI());

      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

      Message message =
          Message.error("HTTP method " + method.name() + " is not allowed for security reasons");
      String messageJson = message.getMessage();
      DataBuffer buffer = response.bufferFactory().wrap(messageJson.getBytes(StandardCharsets.UTF_8));

      return response.writeWith(Mono.just(buffer));
    }

    // Allow all other HTTP methods
    return chain.filter(exchange);
  }

  /**
   * Check if the method is TRACK (non-standard HTTP method)
   *
   * @param method the HTTP method
   * @return true if the method is TRACK
   */
  private boolean isTrackMethod(HttpMethod method) {
    if (method == null) {
      return false;
    }
    return "TRACK".equalsIgnoreCase(method.name());
  }

  @Override
  public int getOrder() {
    // Set high priority to ensure this filter is executed early
    return -100;
  }
}
