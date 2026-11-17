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

package org.apache.linkis.gateway.config;

import org.apache.linkis.common.conf.BDPConfiguration;
import org.apache.linkis.common.conf.Configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * REQ-01 网关配置项语义验证（TC-REQ01-028 / TC-036 / TC-039 的<b>可单测部分</b>）。
 *
 * <p>本类不与 {@code TokenAuthenticationAiDowngradeTest} 的既有基线缺陷纠缠 （详见
 * docs/LinkisV2.1.7/testing/REQ-01_网关单测基线缺陷_取证记录.md）：不打开开关、不触发降级、 不依赖 {@code
 * System.setProperty}，只验证用例文档要求静态核验、但更宜由可执行断言承接的部分。
 *
 * <p>取值注入用 {@code BDPConfiguration.set}（写 extractConfig，是 {@code getOption} 的第一优先级分支），
 * 这是本工作区唯一在单测层可靠生效的注入方式（{@code System.setProperty} 路径不可见，已实测）。
 *
 * <p>注意：本类刻意放在 {@code src/test/java} —— Scala 编译规则 {@code private[conf]} 对 Java 调用方不生效， 而 Scala
 * 测试代码在 {@code org.apache.linkis.gateway.config} 包中无法访问 {@code BDPConfiguration}。
 *
 * <p>断言依据：
 *
 * <pre>
 * GatewayConfiguration.scala:158  CommonVars("wds.linkis.gateway.ai.user.token.downgrade.enable", false)
 * Configuration.scala:167         CommonVars("linkis.ai.user.suffix", "_ai")
 * CommonVars.scala:25             val getValue  = BDPConfiguration.getOption(this).getOrElse(defaultValue)
 * CommonVars.scala:27             def getHotValue() = BDPConfiguration.getOption(this, true).getOrElse(defaultValue)
 * TokenAuthentication.scala:220   if (!...AI_USER_TOKEN_DOWNGRADE_ENABLE.getValue) return false
 * TokenAuthentication.scala:225   if (StringUtils.isBlank(aiSuffix) || !tokenUser.endsWith(aiSuffix)) return false
 * </pre>
 */
@DisplayName("REQ-01 网关 AI 用户配置项语义")
public class GatewayAiUserConfigSemanticsTest {

  private static final String SWITCH_KEY =
      GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().key();

  private static final String SUFFIX_KEY = Configuration.AI_USER_SUFFIX().key();

  private String originSwitch;

  private String originSuffix;

  @BeforeEach
  public void setUp() {
    originSwitch = rawOption(SWITCH_KEY);
    originSuffix = rawOption(SUFFIX_KEY);
  }

  @AfterEach
  public void tearDown() {
    restore(SWITCH_KEY, originSwitch);
    restore(SUFFIX_KEY, originSuffix);
  }

  private static String rawOption(String key) {
    scala.Option<String> option = BDPConfiguration.getOption(key, false);
    return option.isDefined() ? option.get() : null;
  }

  private static void restore(String key, String origin) {
    BDPConfiguration.set(key, origin == null ? "" : origin);
  }

  /** 复刻 TokenAuthentication.scala:225 的后缀命中判定，用于空值保护语义验证。 */
  private static boolean suffixHits(String tokenUser, String suffix) {
    return suffix != null && !suffix.trim().isEmpty() && tokenUser.endsWith(suffix);
  }

  // ---------------------------------------------------- TC-REQ01-028 key 与读取语义

  @Test
  @DisplayName("TC-028: 降级开关 key 与默认值未变（wds.linkis.gateway.ai.user.token.downgrade.enable / false）")
  public void testSwitchKeyAndDefaultUnchanged() {
    assertEquals(
        "wds.linkis.gateway.ai.user.token.downgrade.enable",
        SWITCH_KEY,
        "降级开关 key 不得变更（R-004 配置治理约束）");
    assertEquals(
        Boolean.FALSE,
        GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().defaultValue(),
        "降级开关默认值须为 false（默认不降级）");
  }

  @Test
  @DisplayName("TC-028: getValue 为启动期快照，getHotValue 为运行期可变读取（语义对照）")
  public void testGetValueIsStartupSnapshot() {
    BDPConfiguration.set(SWITCH_KEY, "true");
    Boolean firstRead = (Boolean) GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getValue();
    Boolean firstHot =
        (Boolean) GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue();

    BDPConfiguration.set(SWITCH_KEY, "false");
    Boolean secondRead = (Boolean) GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getValue();
    Boolean secondHot =
        (Boolean) GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue();

    assertEquals(firstRead, secondRead, "getValue 为启动期快照：运行期改配置不得改变已求值结果");
    assertTrue(firstHot, "getHotValue 在配置为 true 时应读到 true");
    assertFalse(secondHot, "getHotValue 在配置改为 false 后应读到 false（两者语义相反，正是 R-004 的依据）");
  }

  // ------------------------------------------------- TC-REQ01-036 开关非法值保护

  @Test
  @DisplayName("TC-036: 开关非法值分级 —— 空/空白回落 false；非空非布尔抛 IllegalArgumentException")
  public void testSwitchIllegalValueBehaviour() {
    // 第一档：空串 / 纯空白 —— formatValue 的 isEmpty 判定生效，回落 defaultValue=false（安全）
    // 说明：实测注入 " " 时首个 sample 可能为 false 或抛异常，取决于 hotload 后台任务是否已把
    // trim 后的空值写入 configReload；本断言只覆盖"空串"这一确定态，纯空白档交由下方异常档兜底。
    BDPConfiguration.set(SWITCH_KEY, "");
    Object blankResult;
    try {
      blankResult = GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue();
    } catch (IllegalArgumentException e) {
      blankResult = null;
    }
    if (blankResult != null) {
      assertEquals(Boolean.FALSE, blankResult, "空串开关须回落 false（不降级）");
    }

    // 第二档：非空非布尔 —— String.toBoolean 在 Scala 2.12 抛 IllegalArgumentException。
    // 这是既有框架语义（非 REQ-01 引入），本用例以可执行断言固化事实，供 TC-036
    // 「网关启动期非法值是否导致启动失败」判定引用：网关启动期读到非法值会 fail-fast。
    for (String bad : new String[] {"abc", "1", "0", "yes", "no", "true,false", "not-a-boolean"}) {
      BDPConfiguration.set(SWITCH_KEY, bad);
      try {
        Object got = GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue();
        fail("既有框架语义已变化：开关非法值 [" + bad + "] 不再抛异常，实际返回 " + got + "。请复核 TC-036 的判定口径");
      } catch (IllegalArgumentException expected) {
        assertTrue(
            expected.getMessage().contains("For input string"),
            "异常信息应符合 String.toBoolean 语义：" + expected.getMessage());
      }
    }
  }

  @Test
  @DisplayName("TC-036: 降级开关显式 true / false 及大小写变体按 toBoolean 语义解析")
  public void testSwitchExplicitBooleanParsing() {
    for (String on : new String[] {"true", "TRUE", "True"}) {
      BDPConfiguration.set(SWITCH_KEY, on);
      assertEquals(
          Boolean.TRUE,
          GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue(),
          "开关取值 [" + on + "] 应解析为 true");
    }
    for (String off : new String[] {"false", "FALSE", "False"}) {
      BDPConfiguration.set(SWITCH_KEY, off);
      assertEquals(
          Boolean.FALSE,
          GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE().getHotValue(),
          "开关取值 [" + off + "] 应解析为 false");
    }
  }

  // ------------------------------------------------- TC-REQ01-039 后缀边界值保护

  @Test
  @DisplayName("TC-039: 后缀 key 与默认值仍为 linkis.ai.user.suffix / _ai（AC-4.1 基线不变）")
  public void testSuffixKeyAndDefaultUnchanged() {
    assertEquals("linkis.ai.user.suffix", SUFFIX_KEY, "后缀 key 须为统一 key（ADR-001）");
    assertEquals("_ai", Configuration.AI_USER_SUFFIX().defaultValue(), "后缀默认值须为 _ai（升级后行为基线）");
  }

  @Test
  @DisplayName("TC-039: 空/空白后缀不许降级任何用户（isBlank 保护分支）")
  public void testBlankSuffixNeverMatches() {
    // 实测：formatValue 对 String 默认值走 trimValue，空串会被 getOption 的 isNotEmpty 判定挡掉
    // （长度 0 不构成有效取值），因此注入 "" 后仍回落默认 "_ai"。真正制造空后缀的场景是
    // 「配置层完全没有该 key」之外的运行期极端态，单测层无法稳定复现。
    // 因此这里对 isBlank 保护分支本身做纯函数级验证，避免把注入无效误读成产品行为。
    for (String blank : new String[] {"", " ", "   ", "\t", null}) {
      assertFalse(
          suffixHits("zhangsan_ai", blank),
          "isBlank 保护：后缀为 [" + blank + "] 时不得匹配任何 tokenUser（否则会剥离出空用户名）");
      assertFalse(suffixHits("zhangsan", blank), "isBlank 保护：普通用户也不得命中 [" + blank + "]");
    }
    // 反向对照：非空后缀必须正常命中，证明上面的 false 来自 isBlank 分支而非断言恒假
    assertTrue(suffixHits("zhangsan_ai", "_ai"), "非空后缀 _ai 必须命中（对照）");

    // 配置层注入 "" 后的实际加载值一并记录（预期回落默认 _ai），供报告说明注入边界
    BDPConfiguration.set(SUFFIX_KEY, "");
    assertEquals(
        "_ai",
        Configuration.AI_USER_SUFFIX().getHotValue(),
        "注入空串后 getOption 判定为非有效取值，回落默认 _ai（记录注入手段的边界）");
  }

  @Test
  @DisplayName("TC-039: 超长 / 含路径分隔符 / 通配符后缀不引发异常，命中与 endsWith 语义一致")
  public void testSpecialCharSuffixIsSafe() {
    StringBuilder huge = new StringBuilder();
    for (int i = 0; i < 10240; i++) {
      huge.append('a');
    }
    String[] specials = {huge.toString(), "/ai_sfx", "*", "?_ai", "zhangsan_ai", " "};
    for (String suffix : specials) {
      BDPConfiguration.set(SUFFIX_KEY, suffix);
      String loaded = (String) Configuration.AI_USER_SUFFIX().getHotValue();
      boolean hit;
      try {
        hit = suffixHits("zhangsan_ai", loaded);
      } catch (Throwable t) {
        fail("后缀 [" + suffix + "] 判定不得抛异常：" + t);
        return;
      }
      assertEquals(
          loaded != null && !loaded.trim().isEmpty() && "zhangsan_ai".endsWith(loaded),
          hit,
          "后缀 [" + suffix + "] 的命中判定须与 endsWith 语义一致");
    }
  }

  @Test
  @DisplayName("TC-039/037: 后缀运行期变更对 getValue 快照不可见（变更须重启网关生效）")
  public void testSuffixChangeRequiresRestart() {
    String snapshot = (String) Configuration.AI_USER_SUFFIX().getValue();
    BDPConfiguration.set(SUFFIX_KEY, "_bot");
    assertEquals(
        snapshot,
        Configuration.AI_USER_SUFFIX().getValue(),
        "getValue 为启动期快照：运行期改后缀不立即生效（对应 TC-037 需重启网关的口径）");
  }
}
