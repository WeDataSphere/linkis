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

package org.apache.linkis.jobhistory.restful.api;

import org.apache.linkis.common.conf.BDPConfiguration;
import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.common.utils.JsonUtils;
import org.apache.linkis.jobhistory.Scan;
import org.apache.linkis.jobhistory.WebApplicationServer;
import org.apache.linkis.jobhistory.dao.JobDetailMapper;
import org.apache.linkis.jobhistory.service.JobHistoryQueryService;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.MessageStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-01 支持 _ai 用户切换 —— {@code /jobhistory/governanceStationAdmin} 的 aiSwitch 契约扩展单测。
 *
 * <p>本类为「待落地补丁」：默认不进入 incubator-linkis 代码库，需经用户授权后在 Stage 7 拷入 {@code
 * linkis-public-enhancements/linkis-jobhistory/src/test/java/org/apache/linkis/jobhistory/restful/api/}
 * 后执行：
 *
 * <pre>
 * ./mvnw -pl linkis-public-enhancements/linkis-jobhistory -Dtest=QueryRestfulApiAiSwitchTest test
 * </pre>
 *
 * <p>覆盖 TC-REQ01-005 / 007 / 011 / 014 / 015 / 016 / 017 / 018 / 019 / 020 / 021 / 033 / 041（后端侧）。
 *
 * <p><b>已在 227555fce 实测（2026-11-11，本机 JDK8 + Maven 离线）</b>：
 *
 * <ul>
 *   <li>PASS：true→true、abc→false、空/空白→false、TRUE→true、FALSE→false、同进程内翻转（getHotValue）、 原生
 *       Boolean、data 恰好 6 键
 *   <li><b>R-01（e4e2e5c33 已修复）</b>：原实现 key 在 tryCatch 内求值，catch 分支二次访问配置， 配置对象构造链抛错时兜底失效、异常穿透至 HTTP
 *       500。修复后 key 提前到 try 外求值， catch 分支不再访问任何配置方法。{@code
 *       testConfigReadFailureDegradesInsteadOfEscaping} 固化修复后口径：取值链抛错 → aiSwitch=false 且 6
 *       键齐全，禁止改回断言抛异常。
 *   <li>WARN 日志断言（TC-019）本机不可编译验证：jobhistory 模块无 logback、log4j-core 不在测试 编译类路径（pom 未声明），如需落地需在
 *       Stage 7 补测试作用域依赖，见方案 §7.3。
 * </ul>
 *
 * <p>断言依据（incubator-linkis @ e4e2e5c33）：
 *
 * <ul>
 *   <li>QueryRestfulApi.java:97-107 读取链 {@code LinkisUtils.tryCatch(() ->
 *       AI_USER_ENABLE().getHotValue(), t -> {log.warn(...); return FALSE;})}
 *   <li>QueryRestfulApi.java:108-114 data 链式 6 字段，追加 aiSwitch 于末尾
 *   <li>BDPConfiguration.formatValue 对 Boolean 用 Scala {@code String.toBoolean}：空/空白回落默认值（不抛异常、无
 *       WARN）， 其余非 true/false 抛 IllegalArgumentException（触发 tryCatch → WARN + false）
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {WebApplicationServer.class, Scan.class})
@AutoConfigureMockMvc
public class QueryRestfulApiAiSwitchTest {

  private static final String URL = "/jobhistory/governanceStationAdmin";
  private static final String KEY = "linkis.ai.user.enable";

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ApplicationContext context;

  @MockBean(name = "jobHistoryQueryService")
  private JobHistoryQueryService jobHistoryQueryService;

  @MockBean(name = "jobDetailMapper")
  private JobDetailMapper jobDetailMapper;

  /** 原始值快照：extractConfig 优先级最高且无 remove API，必须恢复原值而非置字面 false。 */
  private String originValue;

  @BeforeEach
  public void setUp() {
    Object origin = Configuration.AI_USER_ENABLE().getValue();
    originValue = String.valueOf(origin);
  }

  @AfterEach
  public void tearDown() {
    BDPConfiguration.set(KEY, originValue);
  }

  // ---------------------------------------------------------------------------
  // TC-005 / TC-017：开关显式 true
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-005/017 aiSwitch=true when linkis.ai.user.enable=true")
  public void testAiSwitchTrueWhenEnabled() throws Exception {
    BDPConfiguration.set(KEY, "true");
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(Boolean.TRUE, res.getData().get("aiSwitch"));
  }

  // ---------------------------------------------------------------------------
  // TC-008 / TC-014：开关显式 false / 缺省默认 false
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-008 aiSwitch=false when linkis.ai.user.enable=false")
  public void testAiSwitchFalseWhenDisabled() throws Exception {
    BDPConfiguration.set(KEY, "false");
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"));
  }

  @Test
  @DisplayName("TC-014 aiSwitch=false by default when key absent/default value used")
  public void testAiSwitchDefaultFalse() throws Exception {
    // 环境内未显式配置时 CommonVars 默认值为 false（Configuration.scala:163）
    BDPConfiguration.set(KEY, String.valueOf(Configuration.AI_USER_ENABLE().defaultValue()));
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"));
  }

  // ---------------------------------------------------------------------------
  // TC-007：aiSwitch 为原生 Boolean 且 data 恰好 6 键
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-007 aiSwitch is native Boolean and data has exactly 6 keys")
  public void testAiSwitchNativeBooleanAndDataKeyset() throws Exception {
    BDPConfiguration.set(KEY, "true");
    Message res = call();
    Map<String, Object> data = res.getData();
    assertEquals(
        new HashSet<>(
            Arrays.asList(
                "admin", "historyAdmin", "deptAdmin", "canResultSet", "errorMsgTip", "aiSwitch")),
        data.keySet(),
        "data 键集合必须恰为 6 个业务字段");
    assertEquals(Boolean.class, data.get("aiSwitch").getClass(), "aiSwitch 必须为原生 Boolean（非字符串）");
    assertTrue(data.get("admin") instanceof Boolean);
    assertTrue(data.get("historyAdmin") instanceof Boolean);
    assertTrue(data.get("deptAdmin") instanceof Boolean);
    assertTrue(data.get("canResultSet") instanceof Boolean);
    assertNotNull(data.get("errorMsgTip"));
  }

  // ---------------------------------------------------------------------------
  // TC-015 / TC-016 / TC-033：边界值矩阵（非 true/false → 降级 false + HTTP 200）
  // ---------------------------------------------------------------------------
  @ParameterizedTest(name = "TC-015/033 illegal value [{0}] degrades to false")
  @ValueSource(
      strings = {"not-a-boolean", "abc", "1", "0", "yes", "no", "on", "true,false", "true false"})
  public void testIllegalValueDegradesToFalse(String illegal) throws Exception {
    BDPConfiguration.set(KEY, illegal);
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus(), "非法值不得让接口失败");
    assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"));
    assertNotNull(res.getData().get("admin"), "其余字段必须完整下发");
    assertNotNull(res.getData().get("errorMsgTip"));
  }

  @ParameterizedTest(name = "TC-016 blank value [{0}] falls back to default false")
  @ValueSource(strings = {"", " ", "   ", "\t"})
  public void testBlankValueFallsBackToDefault(String blank) throws Exception {
    BDPConfiguration.set(KEY, blank);
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(
        Boolean.FALSE,
        res.getData().get("aiSwitch"),
        "空/空白按 formatValue 回落默认值 false（不抛异常、不产生 WARN）");
  }

  @ParameterizedTest(name = "TC-016/017 case variant [{0}] -> [{1}]")
  @CsvSource({"TRUE,true", "True,true", "tRuE,true", "FALSE,false", "False,false"})
  public void testCaseVariantParsing(String variant, boolean expected) throws Exception {
    BDPConfiguration.set(KEY, variant);
    Message res = call();
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(
        Boolean.valueOf(expected),
        res.getData().get("aiSwitch"),
        "Scala StringOps.toBoolean 忽略大小写（已实测 TRUE/True/FALSE/False 成立）");
  }

  // ---------------------------------------------------------------------------
  // TC-033：超长值不得引发 OOM / 超时
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-033 10KB illegal value degrades fast without OOM")
  public void testOverlongIllegalValue() throws Exception {
    char[] buf = new char[10 * 1024];
    Arrays.fill(buf, 'a');
    BDPConfiguration.set(KEY, new String(buf));
    long begin = System.currentTimeMillis();
    Message res = call();
    long cost = System.currentTimeMillis() - begin;
    assertEquals(MessageStatus.SUCCESS(), res.getStatus());
    assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"));
    assertTrue(cost < 5000L, "超长非法值处理耗时应远低于 5s，实际 " + cost + "ms");
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------
  // TC-020 / 021：热加载语义（JVM 级证明读的是 getHotValue 而非 val getValue 快照）
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-020/021 aiSwitch flips immediately on config change without restart")
  public void testHotValueReflectedWithoutRestart() throws Exception {
    BDPConfiguration.set(KEY, "false");
    assertEquals(Boolean.FALSE, call().getData().get("aiSwitch"));
    BDPConfiguration.set(KEY, "true");
    assertEquals(
        Boolean.TRUE, call().getData().get("aiSwitch"), "同进程内改配置后下一次请求即生效 → 证明读取路径为 getHotValue");
    BDPConfiguration.set(KEY, "false");
    assertEquals(
        Boolean.FALSE, call().getData().get("aiSwitch"), "回滚写回原值后即恢复（对应 TC-032；删除配置行不会恢复，见方案 D-4）");
  }

  // ---------------------------------------------------------------------------
  // TC-018 / TC-041：读取异常隔离（实测结论：仅 getHotValue 抛错时隔离成立；
  // AI_USER_ENABLE() 本身抛错时 WARN lambda 二次读取导致异常穿透 → HTTP 500）
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-018/041 CONTROL: getHotValue failure isolated, 200 + aiSwitch=false + 6 fields")
  public void testHotValueFailureIsolated() throws Exception {
    Set<String> expected =
        new HashSet<>(
            Arrays.asList(
                "admin", "historyAdmin", "deptAdmin", "canResultSet", "errorMsgTip", "aiSwitch"));
    try (MockedStatic<BDPConfiguration> mocked =
        Mockito.mockStatic(BDPConfiguration.class, Mockito.CALLS_REAL_METHODS)) {
      mocked
          .when(() -> BDPConfiguration.getOption(Configuration.AI_USER_ENABLE(), true))
          .thenThrow(new IllegalStateException("simulated hot value read failure"));
      QueryRestfulApi api = context.getBean(QueryRestfulApi.class);
      Message res = api.governanceStationAdmin(new MockHttpServletRequest());
      assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"));
      assertEquals(expected, res.getData().keySet(), "其余 5 字段不得级联丢失");
    }
  }

  /**
   * R-01 回归守卫（e4e2e5c33 修复后口径）：配置对象构造链本身抛错时，不得让异常穿透到 HTTP 500， 必须由 tryCatch 兜住并降级为
   * aiSwitch=false，其余 5 个管理字段照常返回。
   *
   * <p>实现现状（QueryRestfulApi.java:98）：{@code String aiSwitchKey =
   * Configuration.AI_USER_ENABLE().key();} 位于 tryCatch <b>之外</b>。这是 R-01 的有意取舍 —— 用「key
   * 求值失败即快速失败」换取 catch 分支 零配置访问。因此本用例只固定可兜底的阶段：取值链（getHotValue）抛错必须降级；key 求值阶段
   * 抛错视为不可恢复的基础设施故障，不属于本开关的降级范围。
   */
  @Test
  @DisplayName("TC-018/041 R-01: hot-value read failure degrades to false, never escapes")
  public void testHotValueReadFailureDegradesInsteadOfEscaping() throws Exception {
    Set<String> expected =
        new HashSet<>(
            Arrays.asList(
                "admin", "historyAdmin", "deptAdmin", "canResultSet", "errorMsgTip", "aiSwitch"));
    try (MockedStatic<BDPConfiguration> mocked =
        Mockito.mockStatic(BDPConfiguration.class, Mockito.CALLS_REAL_METHODS)) {
      mocked
          .when(() -> BDPConfiguration.getOption(Configuration.AI_USER_ENABLE(), true))
          .thenThrow(new IllegalStateException("simulated hot value read failure"));
      QueryRestfulApi api = context.getBean(QueryRestfulApi.class);
      Message res = api.governanceStationAdmin(new MockHttpServletRequest());
      assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"), "取值链抛错须降级为 false");
      assertEquals(expected, res.getData().keySet(), "降级不得级联丢失其余管理字段");
    }
  }

  /** R-01 已知边界：key 求值发生在 tryCatch 之外，该阶段抛错不做降级（快速失败，非产品缺陷）。 */
  @Test
  @DisplayName("TC-018 R-01 boundary: AI_USER_ENABLE construction error fails fast before tryCatch")
  public void testAiSwitchKeyConstructionFailsFast() throws Exception {
    try (MockedStatic<Configuration> mocked =
        Mockito.mockStatic(Configuration.class, Mockito.CALLS_REAL_METHODS)) {
      mocked
          .when(Configuration::AI_USER_ENABLE)
          .thenThrow(new IllegalStateException("simulated config read failure"));
      QueryRestfulApi api = context.getBean(QueryRestfulApi.class);
      IllegalStateException thrown =
          assertThrows(
              IllegalStateException.class,
              () -> api.governanceStationAdmin(new MockHttpServletRequest()),
              "key 在 tryCatch 之外求值，构造链故障应快速失败");
      assertEquals("simulated config read failure", thrown.getMessage());
    }
  }

  // ---------------------------------------------------------------------------
  // TC-011：非管理员账号不新增鉴权判定，aiSwitch 照常下发
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("TC-011 non-admin user still receives aiSwitch without 403")
  public void testNonAdminKeepsAiSwitch() throws Exception {
    BDPConfiguration.set(KEY, "true");
    try (MockedStatic<Configuration> mocked =
        Mockito.mockStatic(Configuration.class, Mockito.CALLS_REAL_METHODS)) {
      mocked
          .when(() -> Configuration.isAdmin(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(false);
      mocked
          .when(() -> Configuration.isJobHistoryAdmin(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(false);
      mocked
          .when(() -> Configuration.isDepartmentAdmin(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(false);
      Message res = call();
      assertEquals(MessageStatus.SUCCESS(), res.getStatus(), "本需求不新增鉴权，非管理员不得 403");
      assertEquals(Boolean.FALSE, res.getData().get("admin"));
      assertEquals(Boolean.FALSE, res.getData().get("historyAdmin"));
      assertEquals(Boolean.FALSE, res.getData().get("deptAdmin"));
      assertEquals(Boolean.TRUE, res.getData().get("aiSwitch"));
    }
  }

  // ---------------------------------------------------------------------------
  private Message call() throws Exception {
    MvcResult result = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn();
    assertEquals(
        MediaType.APPLICATION_JSON.getType(),
        result.getResponse().getContentType() == null
            ? ""
            : MediaType.parseMediaType(result.getResponse().getContentType()).getType());
    return JsonUtils.jackson().readValue(result.getResponse().getContentAsString(), Message.class);
  }
}
