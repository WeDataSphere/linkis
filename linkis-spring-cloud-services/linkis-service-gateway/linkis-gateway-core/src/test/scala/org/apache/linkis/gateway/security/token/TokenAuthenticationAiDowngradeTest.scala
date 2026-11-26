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

package org.apache.linkis.gateway.security.token

import org.apache.linkis.common.conf.{CommonVars, Configuration}
import org.apache.linkis.gateway.authentication.bo.{Token, User}
import org.apache.linkis.gateway.authentication.service.TokenService
import org.apache.linkis.gateway.config.GatewayConfiguration
import org.apache.linkis.gateway.http.{GatewayContext, GatewayHttpRequest}

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import org.junit.jupiter.api.{AfterEach, Assertions, BeforeEach, DisplayName, Test}
import org.mockito.ArgumentMatchers.{anyString, eq => eqTo}
import org.mockito.Mockito

/**
 * Unit tests for AI user token downgrade logic ([[TokenAuthentication.tryAiUserTokenDowngrade]]).
 *
 * The downgrade switch and suffix are read via `CommonVars.getValue`, a startup snapshot frozen at
 * construction time. Runtime changes (`System.setProperty` / `BDPConfiguration.set`) no longer
 * affect them, so tests build the desired snapshot by reflectively overriding the frozen `getValue`
 * field and restore it afterwards.
 */
class TokenAuthenticationAiDowngradeTest {

  private val SwitchKey = GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE.key
  private val SuffixKey = Configuration.AI_USER_SUFFIX.key

  private var gatewayContext: GatewayContext = _
  private var tokenService: TokenService = _

  @BeforeEach
  def setUp(): Unit = {
    System.clearProperty(SwitchKey)
    System.clearProperty(SuffixKey)
    gatewayContext = Mockito.mock(classOf[GatewayContext])
    val request = Mockito.mock(classOf[GatewayHttpRequest])
    Mockito.when(gatewayContext.getRequest).thenReturn(request)
    Mockito.when(request.getRequestURI).thenReturn("/api/rest_j/v1/test")
    tokenService = Mockito.mock(classOf[TokenService])
    TokenAuthentication.setTokenService(tokenService)
  }

  @AfterEach
  def tearDown(): Unit = {
    System.clearProperty(SwitchKey)
    System.clearProperty(SuffixKey)
  }

  /** Reflectively override a CommonVars' frozen getValue snapshot; returns the old value. */
  private def setSnapshot[T](cv: CommonVars[T], value: T): AnyRef = {
    val f = classOf[CommonVars[_]].getDeclaredField("getValue")
    f.setAccessible(true)
    val old = f.get(cv)
    f.set(cv, value.asInstanceOf[AnyRef])
    old
  }

  private def restoreSnapshot[T](cv: CommonVars[T], old: AnyRef): Unit = {
    val f = classOf[CommonVars[_]].getDeclaredField("getValue")
    f.setAccessible(true)
    f.set(cv, old)
  }

  /** Run body with the downgrade switch snapshot forced to `value`, then restore it. */
  private def withSwitchSnapshot[A](value: Boolean)(body: => A): A = {
    val cv = GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE
    val old = setSnapshot(cv, value)
    try body
    finally restoreSnapshot(cv, old)
  }

  /** Run body with the AI user suffix snapshot forced to `value`, then restore it. */
  private def withSuffixSnapshot[A](value: String)(body: => A): A = {
    val cv = Configuration.AI_USER_SUFFIX
    val old = setSnapshot(cv, value)
    try body
    finally restoreSnapshot(cv, old)
  }

  // AC1: switch off -> downgrade skipped, doAuth not retried at all
  @Test
  @DisplayName("AC1: switch off -> no downgrade, doAuth not retried")
  def switchOffNoDowngrade(): Unit = {
    val ok = TokenAuthentication.tryAiUserTokenDowngrade(
      "TOKEN-A",
      "zhangsan_ai",
      "127.0.0.1",
      gatewayContext
    )
    Assertions.assertFalse(ok)
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // AC2: switch on + zhangsan_ai -> retry doAuth once with zhangsan, success
  @Test
  @DisplayName("AC2: switch on + zhangsan_ai -> retry doAuth with zhangsan, success")
  def downgradeSuccess(): Unit = withSwitchSnapshot(true) {
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    val ok = TokenAuthentication.tryAiUserTokenDowngrade(
      "TOKEN-A",
      "zhangsan_ai",
      "127.0.0.1",
      gatewayContext
    )
    Assertions.assertTrue(ok)
    Mockito
      .verify(tokenService, Mockito.times(1))
      .doAuth(eqTo("TOKEN-A"), eqTo("zhangsan"), eqTo("127.0.0.1"))
  }

  // AC3: retry fails -> false, single retry, no second attempt
  @Test
  @DisplayName("AC3: retry fails -> false, single retry, no second attempt")
  def downgradeRetryFails(): Unit = withSwitchSnapshot(true) {
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(false)
    val ok = TokenAuthentication.tryAiUserTokenDowngrade(
      "TOKEN-A",
      "zhangsan_ai",
      "127.0.0.1",
      gatewayContext
    )
    Assertions.assertFalse(ok)
    Mockito.verify(tokenService, Mockito.times(1)).doAuth(anyString, anyString, anyString)
  }

  // AC4: non-_ai suffix users not triggered
  @Test
  @DisplayName("AC4: non-_ai user (zhangsan, zhangsan_ai2) -> no downgrade")
  def nonAiUserNotTriggered(): Unit = withSwitchSnapshot(true) {
    // plain user without suffix
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan", "h", gatewayContext)
    )
    // different suffix is not the configured one
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_ai2", "h", gatewayContext)
    )
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // AC5: doAuth throws -> caught, returns false, no propagation
  @Test
  @DisplayName("AC5: doAuth throws exception -> fallback false, no propagation")
  def downgradeExceptionCaught(): Unit = withSwitchSnapshot(true) {
    Mockito
      .when(tokenService.doAuth(anyString, anyString, anyString))
      .thenThrow(new RuntimeException("simulated doAuth failure"))
    val ok = TokenAuthentication.tryAiUserTokenDowngrade(
      "TOKEN-A",
      "zhangsan_ai",
      "127.0.0.1",
      gatewayContext
    )
    Assertions.assertFalse(ok)
    Mockito.verify(tokenService, Mockito.times(1)).doAuth(anyString, anyString, anyString)
  }

  // AC6: malformed original user guard (empty, path sep, .., whitespace) -> skip downgrade
  @Test
  @DisplayName("AC6/security: malformed original user -> skip downgrade, doAuth not called")
  def malformedOriginalUser(): Unit = withSwitchSnapshot(true) {
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    // suffix only -> original empty
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "_ai", "h", gatewayContext)
    )
    // path separator
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "a/b_ai", "h", gatewayContext)
    )
    // parent-dir traversal
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "a/../b_ai", "h", gatewayContext)
    )
    // whitespace
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "a b_ai", "h", gatewayContext)
    )
    // wildcard / illegal char
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "a*b_ai", "h", gatewayContext)
    )
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // Q2: suffix configurable
  @Test
  @DisplayName("configurable suffix: _bot triggers downgrade for zhangsan_bot, _ai no longer")
  def configurableSuffix(): Unit = withSwitchSnapshot(true) {
    withSuffixSnapshot("_bot") {
      Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
      val ok =
        TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_bot", "h", gatewayContext)
      Assertions.assertTrue(ok)
      Mockito.verify(tokenService).doAuth(eqTo("T"), eqTo("zhangsan"), eqTo("h"))
      // default _ai no longer triggers under custom suffix
      Assertions.assertFalse(
        TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_ai", "h", gatewayContext)
      )
    }
  }

  // A3: runtime property change does not affect the frozen snapshot (no hot reload)
  @Test
  @DisplayName("A3: runtime property change does not affect the startup snapshot")
  def snapshotNotFollowRuntimeProperty(): Unit = {
    // 快照默认 false：即使运行期写入 true，降级闸门仍读到 false（不降级）
    val svc = new RecordingTokenService(Set("zhangsan"))
    TokenAuthentication.setTokenService(svc)
    System.setProperty(SwitchKey, "true")
    try {
      Assertions.assertFalse(
        TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_ai", "h", gatewayContext),
        "getValue 为启动快照，运行期 System.setProperty 不改变降级闸门（仍为 false）"
      )
      // 对比：显式构造 true 快照后，同一调用即可降级（证明闸门读的确实是快照值）
      withSwitchSnapshot(true) {
        Assertions.assertTrue(
          TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_ai", "h", gatewayContext),
          "快照为 true 时降级生效"
        )
      }
    } finally System.clearProperty(SwitchKey)
  }

  // AC7: concurrent _ai requests are independent, no cross-talk
  @Test
  @DisplayName("AC7: concurrent _ai requests are independent, no cross-talk")
  def concurrentDowngrade(): Unit = withSwitchSnapshot(true) {
    val successOriginals = (1 to 15).map(i => s"user$i").toSet
    val failOriginals = (1 to 15).map(i => s"failuser$i").toSet
    val svc = new RecordingTokenService(successOriginals)
    TokenAuthentication.setTokenService(svc)

    val allAi = (successOriginals ++ failOriginals).map(_ + "_ai").toVector
    val latch = new CountDownLatch(allAi.size)
    val start = new CountDownLatch(1)
    val errors =
      java.util.Collections.synchronizedList(new java.util.ArrayList[Throwable]())
    val results = new ConcurrentHashMap[String, java.lang.Boolean]()

    val threads = allAi.map { aiUser =>
      new Thread(new Runnable {
        override def run(): Unit = {
          try {
            start.await()
            val r = TokenAuthentication.tryAiUserTokenDowngrade("T", aiUser, "h", gatewayContext)
            results.put(aiUser, java.lang.Boolean.valueOf(r))
          } catch {
            case t: Throwable => errors.add(t)
          } finally latch.countDown()
        }
      })
    }
    threads.foreach(_.start())
    start.countDown()
    Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS), "concurrent downgrade timed out")
    Assertions.assertTrue(errors.isEmpty, s"concurrent errors occurred: $errors")
    successOriginals.foreach(u =>
      Assertions.assertTrue(results.get(u + "_ai"), s"${u}_ai should downgrade-success")
    )
    failOriginals.foreach(u =>
      Assertions.assertFalse(results.get(u + "_ai"), s"${u}_ai should downgrade-fail")
    )
    Assertions.assertTrue(
      svc.callCount.get() == allAi.size,
      s"expected ${allAi.size} retry doAuth calls, got ${svc.callCount.get()}"
    )
  }

}

/** Thread-safe recording [[TokenService]] for the concurrency test. */
class RecordingTokenService(successOriginals: Set[String]) extends TokenService {
  val callCount = new AtomicInteger(0)

  override def doAuth(tokenName: String, userName: String, host: String): Boolean = {
    callCount.incrementAndGet()
    successOriginals.contains(userName)
  }

  override def addNewToken(token: Token): Boolean = false
  override def removeToken(tokenName: String): Boolean = false
  override def updateToken(token: Token): Boolean = false
  override def addUserForToken(tokenName: String, user: User): Boolean = false
  override def addHostForToken(tokenName: String, ip: String): Boolean = false
  override def addHostAndUserForToken(tokenName: String, user: User, ip: String): Boolean = false
  override def removeUserForToken(tokenName: String, user: User): Boolean = false
  override def removeHostForToken(tokenName: String, ip: String): Boolean = false
  override def isTokenValid(tokenName: String): Boolean = false
  override def isTokenAcceptableWithUser(tokenName: String, userName: String): Boolean = false
  override def isTokenAcceptableWithHost(tokenName: String, host: String): Boolean = false
}
