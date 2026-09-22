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
 * The downgrade switch and suffix are declared with `getHotValue`, so they read Java system
 * properties at runtime; tests control them via `System.setProperty` and clean up afterwards.
 */
class TokenAuthenticationAiDowngradeTest {

  private val SwitchKey = GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE.key
  private val SuffixKey = GatewayConfiguration.AI_USER_SUFFIX.key

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

  private def enableSwitch(): Unit = System.setProperty(SwitchKey, "true")

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
  def downgradeSuccess(): Unit = {
    enableSwitch()
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
  def downgradeRetryFails(): Unit = {
    enableSwitch()
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
  def nonAiUserNotTriggered(): Unit = {
    enableSwitch()
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
  def downgradeExceptionCaught(): Unit = {
    enableSwitch()
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
  def malformedOriginalUser(): Unit = {
    enableSwitch()
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
  def configurableSuffix(): Unit = {
    enableSwitch()
    System.setProperty(SuffixKey, "_bot")
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    val ok = TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_bot", "h", gatewayContext)
    Assertions.assertTrue(ok)
    Mockito.verify(tokenService).doAuth(eqTo("T"), eqTo("zhangsan"), eqTo("h"))
    // default _ai no longer triggers under custom suffix
    Assertions.assertFalse(
      TokenAuthentication.tryAiUserTokenDowngrade("T", "zhangsan_ai", "h", gatewayContext)
    )
  }

  // AC7: concurrent _ai requests are independent, no cross-talk
  @Test
  @DisplayName("AC7: concurrent _ai requests are independent, no cross-talk")
  def concurrentDowngrade(): Unit = {
    enableSwitch()
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
