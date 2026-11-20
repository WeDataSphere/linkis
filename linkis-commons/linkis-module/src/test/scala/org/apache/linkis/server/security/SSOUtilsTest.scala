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

import org.apache.linkis.server.exception.LoginExpireException

import javax.servlet.http.Cookie

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
    val lastAccess = new ConcurrentHashMap[String, Long]()
    lastAccess.put("stale-token-ticket", stale)
    lastAccess.put("fresh-token-ticket", System.currentTimeMillis())
    val expired = ArrayBuffer.empty[String]

    SSOUtils.expireTokenTickets(
      System.currentTimeMillis(),
      issuedAt,
      id => lastAccess.getOrDefault(id, 0L),
      id => expired += id
    )

    Assertions.assertEquals(Seq("stale-token-ticket"), expired.toSeq)
    Assertions.assertFalse(issuedAt.containsKey("stale-token-ticket"))
    Assertions.assertTrue(issuedAt.containsKey("fresh-token-ticket"))
  }

  @Test
  @DisplayName("expireTokenTicketsKeepsSharedTicketRefreshedByPeerTest")
  def expireTokenTicketsKeepsSharedTicketRefreshedByPeerTest(): Unit = {
    // HA 缺陷回归：本实例侧表印记已超时，但共享主容器（SsoRedis 下由对端网关滑动续期）中的
    // 最后访问时间仍然新鲜 —— 不得删除活跃使用中的票据，只允许重新对齐本地印记
    val now = System.currentTimeMillis()
    val issuedAt = new ConcurrentHashMap[String, Long]()
    issuedAt.put("active-shared-ticket", now - 10 * 60 * 1000L)
    val lastAccess = new ConcurrentHashMap[String, Long]()
    lastAccess.put("active-shared-ticket", now - 1000L) // 对端网关 1 秒前刚续期
    val expired = ArrayBuffer.empty[String]

    SSOUtils.expireTokenTickets(
      now,
      issuedAt,
      id => lastAccess.getOrDefault(id, 0L),
      id => expired += id
    )

    Assertions.assertTrue(expired.isEmpty)
    Assertions.assertTrue(issuedAt.containsKey("active-shared-ticket"))
    // 本地印记被对齐到共享的最后访问时间，避免清扫线程每个周期重复复查
    Assertions.assertTrue(issuedAt.get("active-shared-ticket") >= now - 1000L)
  }

  @Test
  @DisplayName("expireTokenTicketsRemovesTicketMissingFromSharedContainerTest")
  def expireTokenTicketsRemovesTicketMissingFromSharedContainerTest(): Unit = {
    // 票据已从共享主容器消失（如对端网关注销）时，本地残留的侧表印记必须被清理
    val now = System.currentTimeMillis()
    val issuedAt = new ConcurrentHashMap[String, Long]()
    issuedAt.put("logged-out-elsewhere", now - 10 * 60 * 1000L)
    val lastAccess = new ConcurrentHashMap[String, Long]() // 共享容器中无此票据
    val mainRemoved = ArrayBuffer.empty[String]

    SSOUtils.expireTokenTickets(
      now,
      issuedAt,
      id => lastAccess.getOrDefault(id, 0L),
      id => mainRemoved += id
    )

    Assertions.assertEquals(Seq("logged-out-elsewhere"), mainRemoved.toSeq)
    Assertions.assertFalse(issuedAt.containsKey("logged-out-elsewhere"))
  }

  @Test
  @DisplayName("removeLoginUserDiscardsTicketTest")
  def removeLoginUserDiscardsTicketTest(): Unit = {
    // 登出必须当场从票证存储里删掉票证，而不是靠清扫线程等到 sessionTimeout 过期：
    // 删除后同一个票证再校验应直接判失效，这正是 TC031「登出后原票证仍可用」的回归点。
    val ticketId = SSOUtils.getUserTicketKV("logout-user")._2
    SSOUtils.putLoginUserTicketForTest(ticketId)
    // 必须断言「实际写进去的那个存储」：未开 Redis 时 getUserTicketIdMap 每次返回一个新的
    // ConcurrentHashMap，用 getter 断言永远拿不到写入的票证，测的就不是生产路径了。
    Assertions.assertTrue(SSOUtils.getUserTicketIdMapOfTicketForTest(ticketId).isDefined)

    val cookie = new Cookie(SSOUtils.USER_TICKET_ID_STRING, ticketId)
    SSOUtils.removeLoginUser(Array(cookie))

    Assertions.assertFalse(SSOUtils.getUserTicketIdMapOfTicketForTest(ticketId).isDefined)
    Assertions.assertNull(cookie.getValue)
    Assertions.assertEquals(0, cookie.getMaxAge)
    Assertions.assertThrows(
      classOf[LoginExpireException],
      new org.junit.jupiter.api.function.Executable {
        override def execute(): Unit = SSOUtils.updateLastAccessTime(_ => Some(ticketId))
      }
    )
  }

  @Test
  @DisplayName("gatewayLogoutReadsDiscardedTicketBeforeCookieIsBlankedTest")
  def gatewayLogoutReadsDiscardedTicketBeforeCookieIsBlankedTest(): Unit = {
    // 登出响应要自证「结束了哪个会话」，就必须在删票之前把票证 id 读出来：SSOUtils.removeLoginUser
    // 会把传进去的 cookie 的 value 置空。这里按网关的取值顺序复现一遍，锁住这个顺序。
    val ticketId = SSOUtils.getUserTicketKV("logout-gateway-user")._2
    SSOUtils.putLoginUserTicketForTest(ticketId)
    val cookies = Array(new Cookie(SSOUtils.USER_TICKET_ID_STRING, ticketId))
    val discardedTicketId =
      cookies.find(_.getName == SSOUtils.USER_TICKET_ID_STRING).map(_.getValue)

    SSOUtils.removeLoginUser(cookies)

    Assertions.assertEquals(Some(ticketId), discardedTicketId)
    Assertions.assertNull(cookies.head.getValue)
    Assertions.assertFalse(SSOUtils.getUserTicketIdMapOfTicketForTest(ticketId).isDefined)
  }

  @Test
  @DisplayName("logoutDiscardsTicketEvenWhenStoreProbeSaysAbsentTest")
  def logoutDiscardsTicketEvenWhenStoreProbeSaysAbsentTest(): Unit = {
    // 原缺陷的核心：删除被 containsKey 守卫，存储探测说「没有」就整段跳过，票证留在 Redis 里。
    // 这里把探测和删除打平成同一对闭包，断言即便探测结果为 false，删除也照样发生。
    val ticketId = SSOUtils.getUserTicketKV("logout-probe-user")._2
    var probed = false
    var removed: Option[String] = None
    val store = new ConcurrentHashMap[String, Long]()
    store.put(ticketId, System.currentTimeMillis())

    SSOUtils.removeLoginUser { _ =>
      probed = true
      removed = Some(ticketId)
      store.remove(ticketId)
      Some(ticketId)
    }

    Assertions.assertTrue(probed)
    Assertions.assertEquals(Some(ticketId), removed)
    Assertions.assertTrue(store.isEmpty)
  }

  @Test
  @DisplayName("logoutWithoutTicketCookieStillSucceedsTest")
  def logoutWithoutTicketCookieStillSucceedsTest(): Unit = {
    // 未带票证 cookie 的登出不能抛异常：登出接口在没有 cookie 时也会被调用。
    val blank = new Cookie(SSOUtils.USER_TICKET_ID_STRING, "")
    Assertions.assertDoesNotThrow(new org.junit.jupiter.api.function.Executable {
      override def execute(): Unit = SSOUtils.removeLoginUser(Array.empty[Cookie])
    })
    Assertions.assertDoesNotThrow(new org.junit.jupiter.api.function.Executable {
      override def execute(): Unit = SSOUtils.removeLoginUser(Array[Cookie](blank))
    })
  }

  @Test
  @DisplayName("removeLoginUserTicketForTestCleansUpTestTicketTest")
  def removeLoginUserTicketForTestCleansUpTestTicketTest(): Unit = {
    // 全局票证存储是进程级单例，测试写入的票证必须能自己清掉，否则会污染后续用例与清扫线程。
    val ticketId = SSOUtils.getUserTicketKV("logout-cleanup-user")._2
    SSOUtils.putLoginUserTicketForTest(ticketId)
    Assertions.assertTrue(SSOUtils.getUserTicketIdMapOfTicketForTest(ticketId).isDefined)

    SSOUtils.removeLoginUserTicketForTest(ticketId)

    Assertions.assertFalse(SSOUtils.getUserTicketIdMapOfTicketForTest(ticketId).isDefined)
  }

}
