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

package org.apache.linkis.server.security

import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable.ArrayBuffer

import org.junit.jupiter.api.{Assertions, DisplayName, Test}

class SSOUtilsTest {

  @Test
  @DisplayName("constTest")
  def constTest(): Unit = {

    val sslEnable = SSOUtils.sslEnable
    Assertions.assertFalse(sslEnable)

  }

  @Test
  @DisplayName("getUserTicketIdMapTest")
  def getUserTicketIdMapTest(): Unit = {

    val ticketIdMap = SSOUtils.getUserTicketIdMap
    Assertions.assertNotNull(ticketIdMap)

  }

  @Test
  @DisplayName("decryptLoginTest")
  def decryptLoginTest(): Unit = {

    val decryptPwd = SSOUtils.decryptLogin("hadoop")
    Assertions.assertNotNull(decryptPwd)
  }

  @Test
  @DisplayName("expireTokenTicketsTest")
  def expireTokenTicketsTest(): Unit = {
    // 侧表必须能被独立清理：SsoRedis 下主容器本地 Map 恒为空，靠遍历主容器永远清不到 token 票证
    val issuedAt = new ConcurrentHashMap[String, Long]()
    val stale = System.currentTimeMillis - 10 * 60 * 1000L
    issuedAt.put("stale-token-ticket", stale)
    issuedAt.put("fresh-token-ticket", System.currentTimeMillis())
    val expired = ArrayBuffer.empty[String]

    SSOUtils.expireTokenTickets(System.currentTimeMillis(), issuedAt, id => expired += id)

    Assertions.assertEquals(Seq("stale-token-ticket"), expired.toSeq)
    Assertions.assertFalse(issuedAt.containsKey("stale-token-ticket"))
    Assertions.assertTrue(issuedAt.containsKey("fresh-token-ticket"))
  }

}
