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

package org.apache.linkis.server.utils

import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.bind.{Bindable, Binder}
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

import java.util

import org.junit.jupiter.api.{AfterEach, Assertions, DisplayName, Test}

/**
 * REQ-06 TC010 (unit-test form): thread-pool key effectiveness.
 *
 * <p>`spring.server.jetty.threads.max` (new key) is passed through by
 * LinkisMainHelper#getExtraSpringOptions (spring. prefix stripped -> `--server.jetty.threads.max`)
 * AND is bound by Spring Boot's ServerProperties.
 *
 * <p>`spring.server.jetty.max-threads` (legacy key, W-3) is passed through the very same way — its
 * ineffectiveness is NOT caused by the option-conversion layer, but by the Spring Boot
 * ServerProperties key space, which has no such field: the Binder silently ignores it.
 */
class LinkisMainHelperTest {

  private val NEW_KEY = "spring.server.jetty.threads.max"
  private val OLD_KEY = "spring.server.jetty.max-threads"

  @AfterEach
  def cleanup(): Unit = {
    System.clearProperty(NEW_KEY)
    System.clearProperty(OLD_KEY)
  }

  @Test
  @DisplayName("constTest")
  def constTest(): Unit = {

    val servernamekey = LinkisMainHelper.SERVER_NAME_KEY
    Assertions.assertEquals("serviceName", servernamekey)

  }

  @Test
  @DisplayName("getExtraSpringOptionsTest")
  def getExtraSpringOptionsTest(): Unit = {

    val array = LinkisMainHelper.getExtraSpringOptions("dev")
    Assertions.assertTrue(array.length > 0)

  }

  @Test
  @DisplayName("TC010-a: new key spring.server.jetty.threads.max is passed through")
  def newKeyPassedThrough(): Unit = {
    System.setProperty(NEW_KEY, "500")
    val options = LinkisMainHelper.getExtraSpringOptions("test")
    Assertions.assertTrue(
      options.contains("--server.jetty.threads.max=500"),
      s"expected --server.jetty.threads.max=500 in ${options.mkString(",")}"
    )
  }

  @Test
  @DisplayName("TC010-b: legacy key spring.server.jetty.max-threads is passed through too")
  def legacyKeyPassedThrough(): Unit = {
    System.setProperty(OLD_KEY, "200")
    val options = LinkisMainHelper.getExtraSpringOptions("test")
    Assertions.assertTrue(
      options.contains("--server.jetty.max-threads=200"),
      s"expected --server.jetty.max-threads=200 in ${options.mkString(",")}"
    )
  }

  @Test
  @DisplayName("TC010-c: Spring Boot binds server.jetty.threads.max onto ServerProperties")
  def binderAcceptsNewKey(): Unit = {
    val source = new util.HashMap[String, AnyRef]()
    source.put("server.jetty.threads.max", "500")
    val binder = new Binder(new MapConfigurationPropertySource(source))
    val serverProperties = binder
      .bind("server", Bindable.of(classOf[ServerProperties]))
      .orElse(new ServerProperties)
    Assertions.assertTrue(serverProperties.getJetty.getThreads.getMax == 500)
  }

  @Test
  @DisplayName("TC010-d: legacy key server.jetty.max-threads is NOT bound (default 200 kept)")
  def binderRejectsLegacyKey(): Unit = {
    val source = new util.HashMap[String, AnyRef]()
    // 999 != Spring Boot default 200: if the legacy key were bound, max would become 999
    source.put("server.jetty.max-threads", "999")
    val binder = new Binder(new MapConfigurationPropertySource(source))
    val serverProperties = binder
      .bind("server", Bindable.of(classOf[ServerProperties]))
      .orElse(new ServerProperties)
    Assertions.assertTrue(serverProperties.getJetty.getThreads.getMax == 200)
  }

}
