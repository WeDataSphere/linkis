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

package org.apache.linkis.gateway.authentication.service.impl

import org.apache.linkis.gateway.authentication.exception.TokenAuthException

import org.junit.jupiter.api.{Assertions, BeforeAll, BeforeEach, Test}
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicTokenServiceImplTest {

  private var dynamicTokenService: DynamicTokenServiceImpl = _

  @BeforeAll
  def init(): Unit = {
    // CommonVars.getValue is computed at class loading time, set system props before class loads
    System.setProperty("linkis.gateway.dynamic.token.enable", "true")
    System.setProperty(
      "wds.linkis.gateway.dynamic.token.hmac.key",
      "test-secret-key-for-hmac-sha256"
    )
    System.setProperty("wds.linkis.gateway.dynamic.token.window.minutes", "30")
    System.setProperty("wds.linkis.gateway.dynamic.token.expire.days", "1")
    System.setProperty("wds.linkis.gateway.dynamic.token.clock.skew.windows", "1")
  }

  @BeforeEach
  def setUp(): Unit = {
    dynamicTokenService = new DynamicTokenServiceImpl
  }

  @Test
  def testGenerateTokenSuccessfully(): Unit = {
    val (token, expireTime) = dynamicTokenService.generateToken("userA")

    Assertions.assertNotNull(token)
    Assertions.assertTrue(token.startsWith("dyn-"))
    Assertions.assertTrue(token.contains("."))
    Assertions.assertTrue(expireTime > System.currentTimeMillis() / 1000)
  }

  @Test
  def testGenerateTokenDeterministicSameWindow(): Unit = {
    val (token1, _) = dynamicTokenService.generateToken("userA")
    // Same username and same window should produce same token
    val (token2, _) = dynamicTokenService.generateToken("userA")

    Assertions.assertEquals(
      token1,
      token2,
      "Same user in same window should get same token (deterministic generation)"
    )
  }

  @Test
  def testGenerateTokenDifferentUsersDifferentToken(): Unit = {
    val (token1, _) = dynamicTokenService.generateToken("userA")
    val (token2, _) = dynamicTokenService.generateToken("userB")

    Assertions.assertNotEquals(token1, token2, "Different users should get different tokens")
  }

  @Test
  def testValidateDynamicTokenSuccessfully(): Unit = {
    val (token, _) = dynamicTokenService.generateToken("userA")
    val result = dynamicTokenService.validateDynamicToken(token, "userA")

    Assertions.assertTrue(result)
  }

  @Test
  def testValidateDynamicTokenUserMismatch(): Unit = {
    val (token, _) = dynamicTokenService.generateToken("userA")

    val exception = Assertions.assertThrows(
      classOf[TokenAuthException],
      new Executable {
        override def execute(): Unit = dynamicTokenService.validateDynamicToken(token, "userB")
      }
    )
    Assertions.assertEquals(15212, exception.getErrCode)
  }

  @Test
  def testValidateDynamicTokenTamperedSignature(): Unit = {
    val (token, _) = dynamicTokenService.generateToken("userA")
    // Tamper with the signature by changing the last character
    val tamperedToken =
      if (token.endsWith("A")) token.dropRight(1) + "B"
      else token.dropRight(1) + "A"

    val exception = Assertions.assertThrows(
      classOf[TokenAuthException],
      new Executable {
        override def execute(): Unit =
          dynamicTokenService.validateDynamicToken(tamperedToken, "userA")
      }
    )
    Assertions.assertEquals(15210, exception.getErrCode)
  }

  @Test
  def testValidateDynamicTokenInvalidFormat(): Unit = {
    val invalidToken = "dyn-!!!invalid-base64!!!"

    val exception = Assertions.assertThrows(
      classOf[TokenAuthException],
      new Executable {
        override def execute(): Unit =
          dynamicTokenService.validateDynamicToken(invalidToken, "userA")
      }
    )
    Assertions.assertEquals(15209, exception.getErrCode)
  }

  @Test
  def testValidateDynamicTokenNoSeparator(): Unit = {
    val invalidToken = "dyn-no-separator-token"

    val exception = Assertions.assertThrows(
      classOf[TokenAuthException],
      new Executable {
        override def execute(): Unit =
          dynamicTokenService.validateDynamicToken(invalidToken, "userA")
      }
    )
    Assertions.assertEquals(15209, exception.getErrCode)
  }

  @Test
  def testValidateDynamicTokenStaticTokenDoesNotMatch(): Unit = {
    // A static token should NOT be prefixed with dyn-, but if passed here
    // it would fail because validateDynamicToken expects dyn- prefix
    val staticTokenLike = "dyn-something.different"
    val exception = Assertions.assertThrows(
      classOf[TokenAuthException],
      new Executable {
        override def execute(): Unit =
          dynamicTokenService.validateDynamicToken(staticTokenLike, "userA")
      }
    )
    // Format error because it contains a dot but payload is invalid
    Assertions.assertEquals(15209, exception.getErrCode)
  }

  @Test
  def testGenerateTokenWithExpireTime(): Unit = {
    val (token, expireTime) = dynamicTokenService.generateToken("userA")
    val currentTime = System.currentTimeMillis() / 1000
    val maxExpireTime = currentTime + 86400 + 1800 // 1 day + 30 minutes window

    Assertions.assertTrue(
      expireTime > currentTime,
      s"Expire time ($expireTime) should be in the future (current: $currentTime)"
    )
    Assertions.assertTrue(
      expireTime <= maxExpireTime,
      s"Expire time ($expireTime) should be within 1 day + window from now"
    )
  }

}
