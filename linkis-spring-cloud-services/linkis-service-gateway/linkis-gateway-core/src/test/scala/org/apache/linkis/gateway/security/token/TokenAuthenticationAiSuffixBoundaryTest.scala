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

import org.apache.linkis.common.conf.Configuration
import org.apache.linkis.gateway.authentication.service.TokenService
import org.apache.linkis.gateway.config.GatewayConfiguration
import org.apache.linkis.gateway.http.{GatewayContext, GatewayHttpRequest}

import scala.util.Try

import org.junit.jupiter.api.{AfterEach, Assertions, BeforeEach, Disabled, DisplayName, Test}
import org.mockito.ArgumentMatchers.{anyString, eq => eqTo}
import org.mockito.Mockito

/**
 * REQ-01 补丁用例：AI 用户后缀的边界值保护（TC-REQ01-039 / RTR-003-CONFIG-BOUNDARY）。
 *
 * <p>「待落地补丁」：默认不进入 incubator-linkis 代码库，需经用户授权后拷入
 * `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/test/scala/org/apache/linkis/gateway/security/token/`
 * 并以 `./mvnw -pl linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core
 * -Dtest=TokenAuthenticationAiSuffixBoundaryTest test` 执行。
 *
 * <p>断言依据（TokenAuthentication.scala:220-267 @ 227555fce）：
 * {{{
 * if (!GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE.getValue) return false  // 启动期 getValue
 * val aiSuffix = Configuration.AI_USER_SUFFIX.getValue
 * if (StringUtils.isBlank(aiSuffix) || !tokenUser.endsWith(aiSuffix)) return false // isBlank 空值保护
 * // originalUser 合法性：非空白 / 长度 <= 64 / 不含 .. / 不含空白与 \/:*?"<>|
 * }}}
 *
 * <p><b>⚠️ 本类在 JUnit 单测层无法通过配置注入打开开关</b>（2026-11-11 实测确认）： `TokenAuthentication` 走
 * `CommonVars.getValue`，其实现为
 * `BDPConfiguration.getOption(this).getOrElse(defaultValue)`，而该路径在本工作区实测读不到 运行期
 * `System.setProperty` 写入的系统属性（`getOption(key)` 返回 `Some(...)`，但 `getValue` 仍回落默认值）。同包既有
 * `TokenAuthenticationAiDowngradeTest` 在同一基线上 8 条用例失败 5 条（`downgradeSuccess` /
 * `downgradeRetryFails` / `configurableSuffix` / `concurrentDowngrade` /
 * `downgradeExceptionCaught`），可证明这是**既有基线问题**， 非本补丁引入。根因取证与影响面见
 * `docs/LinkisV2.1.7/testing/REQ-01_网关单测基线缺陷_取证记录.md`。
 *
 * <p>因此本类的定位是 **@Disabled 存档**：保留完整断言作为 Stage 7/10 在真实网关环境下 复跑的素材，但不在当前基线纳入 CI
 * 红绿判定。开关与后缀的正向验证走「控制面配置 + 网关重启 + 接口实测」（TC-027 运行态部分），不依赖 `System.setProperty`。
 */
@Disabled(
  "既有基线缺陷：CommonVars.getValue 在本工作区读不到 System.setProperty，开关无法在单测层打开。" +
    "同包 TokenAuthenticationAiDowngradeTest 同基线 8 跑 5 败。取证见 REQ-01_网关单测基线缺陷_取证记录.md；" +
    "正向验证走控制面配置 + 网关重启接口实测。"
)
class TokenAuthenticationAiSuffixBoundaryTest {

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
    Mockito
      .when(request.getRequestURI)
      .thenReturn("/api/rest_j/v1/jobhistory/governanceStationAdmin")
    tokenService = Mockito.mock(classOf[TokenService])
    TokenAuthentication.setTokenService(tokenService)
  }

  @AfterEach
  def tearDown(): Unit = {
    System.clearProperty(SwitchKey)
    System.clearProperty(SuffixKey)
  }

  private def downgrade(tokenUser: String): Boolean =
    TokenAuthentication.tryAiUserTokenDowngrade("TOKEN-A", tokenUser, "127.0.0.1", gatewayContext)

  private def enableSwitch(): Unit = System.setProperty(SwitchKey, "true")

  // TC-039：空白后缀必须一律不降级（isBlank 保护），且不得抛 NPE
  @Test
  @DisplayName("TC-039 blank suffix -> no downgrade for any user, no NPE")
  def blankSuffixNoDowngrade(): Unit = {
    enableSwitch()
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    Seq("", " ", "   ", "\t").foreach { blank =>
      System.setProperty(SuffixKey, blank)
      Assertions.assertFalse(downgrade("zhangsan_ai"), s"suffix=[$blank] 时不得降级")
      Assertions.assertFalse(downgrade("zhangsan"), s"suffix=[$blank] 时不得降级")
    }
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // TC-027：后缀为启动期 getValue 快照，运行期改系统属性不生效（须改控制面配置并重启网关）
  @Test
  @DisplayName("TC-027 PROBE: custom suffix needs gateway restart (startup getValue snapshot)")
  def customSuffixSnapshotAtStartup(): Unit = {
    enableSwitch()
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    // CommonVars.getValue 在启动期已固化后缀，运行期 setProperty 不影响本次判定：
    // 默认 _ai 仍命中降级，自定义 _bot 不命中（正向验证须重启网关，见 TC-027 运行态部分）。
    System.setProperty(SuffixKey, "_bot")
    Assertions.assertTrue(downgrade("zhangsan_ai"), "默认 _ai 后缀应命中降级")
    Mockito.verify(tokenService).doAuth(eqTo("TOKEN-A"), eqTo("zhangsan"), eqTo("127.0.0.1"))
    Mockito.clearInvocations(tokenService)
    Assertions.assertFalse(downgrade("zhangsan_bot"), "_bot 在启动期快照口径下不命中降级")
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // TC-039：超长 / 特殊字符后缀不得引发异常，也不得误判降级
  @Test
  @DisplayName("TC-039 overlong / special-char suffix -> no exception, no false downgrade")
  def overlongAndSpecialSuffix(): Unit = {
    enableSwitch()
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    Seq("/ai", "_ai/", "_ai x", "*").foreach { suffix =>
      System.setProperty(SuffixKey, suffix)
      Assertions.assertDoesNotThrow(new org.junit.jupiter.api.function.Executable {
        override def execute(): Unit = downgrade("zhangsan")
      })
      Assertions.assertFalse(downgrade("zhangsan"), s"suffix=[$suffix] 不应命中普通用户")
    }
    // 超长后缀整体剥离后原始用户长度 > 64 → isLegalDowngradeUser 必须拦截
    val overlong = "_" + ("a" * 10 * 1024)
    System.setProperty(SuffixKey, overlong)
    Assertions.assertDoesNotThrow(new org.junit.jupiter.api.function.Executable {
      override def execute(): Unit = downgrade("zhangsan" + overlong)
    })
    Assertions.assertFalse(
      downgrade("zhangsan" + overlong),
      "剥离后长度>64 的原始用户必须被 isLegalDowngradeUser 拦截"
    )
  }

  // TC-040 前置：非法原始用户（路径分隔/穿越/通配）不得触发降级重试
  @Test
  @DisplayName("TC-025 security: illegal stripped user -> no downgrade retry")
  def illegalStrippedUserGuard(): Unit = {
    enableSwitch()
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    Seq("_ai", "a/b_ai", "a/../b_ai", "a b_ai", "a*b_ai").foreach { tokenUser =>
      Assertions.assertFalse(downgrade(tokenUser), s"tokenUser=[$tokenUser] 不应降级")
    }
    Mockito.verify(tokenService, Mockito.never()).doAuth(anyString, anyString, anyString)
  }

  // TC-036：降级开关取值非法时不得让网关启动/鉴权链路抛未捕获异常
  @Test
  @DisplayName("TC-036 illegal downgrade-switch value -> no uncaught exception")
  def illegalSwitchValueGuard(): Unit = {
    Mockito.when(tokenService.doAuth(anyString, anyString, anyString)).thenReturn(true)
    System.setProperty(SwitchKey, "abc")
    val outcome = Try(downgrade("zhangsan_ai"))
    outcome match {
      case scala.util.Success(r) =>
        Assertions.assertFalse(r, "非法开关值应按默认 false 处理（不降级）")
      case scala.util.Failure(t) =>
        Assertions.fail[Unit](
          s"非法开关值导致未捕获异常，登记为 RTR-004-CONFIG-BOUNDARY 缺口：" +
            s"${t.getClass.getName}: ${t.getMessage}"
        )
    }
  }

}
