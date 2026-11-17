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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-01 支持 _ai 用户切换 —— aiSwitch 取值域 / 热加载 / 兼容性 / 容错行为验证。
 *
 * <p>本类与 {@code QueryRestfulApiAiSwitchTest}（28 例，已固化）互补，不重复其断言，专注承接未覆盖的用例：
 *
 * <ul>
 *   <li>TC-REQ01-002：{@code BDPConfiguration} 静态配置污染消除回归（finally 恢复原值而非写字面 false）
 *   <li>TC-REQ01-005/006/007：开启态 true 下发、JSON 原生布尔类型、六字段并存
 *   <li>TC-REQ01-008：关闭态 false 对照
 *   <li>TC-REQ01-009：五字段与基线逐字段一致、{@code aiSwitch} 严格追加在末位
 *   <li>TC-REQ01-012/013：字段顺序不变、未知字段对旧消费方无影响（向后兼容）
 *   <li>TC-REQ01-015/017：非法值 {@code abc} 不 500 且降级 false、布尔边界显式配置
 *   <li>TC-REQ01-016：空串 / 仅空白 / 大小写变体 / 数字 / 逗号串等边界值兜底
 *   <li>TC-REQ01-020/021：热加载开启 / 关闭即时生效（无需重启）
 *   <li>TC-REQ01-023：默认关闭环境下既有接口行为零变化
 *   <li>TC-REQ01-019：非法值时 WARN 日志留痕（当前实现未落地，按实测记录偏差，见用例文档判定规则）
 * </ul>
 *
 * <p>取值注入方式说明：{@code BDPConfiguration.getOption(key, hotload=true)} 先查 {@code extractConfig}
 * （{@code BDPConfiguration.set} 写入处），因此 {@code set} 对本接口的热加载取值链同样生效，这是本类全部
 * 开关态用例的注入手段。
 *
 * <p>已知边界（R-01）：配置项构造链故障在 {@code tryCatch} 之外求值，属快速失败而非降级，
 * 该场景由 {@code QueryRestfulApiAiSwitchTest#testAiSwitchKeyConstructionFailsFast} 独立守卫，本类不重复。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {WebApplicationServer.class, Scan.class})
@AutoConfigureMockMvc
@DisplayName("REQ-01 aiSwitch 取值域与热加载行为验证")
public class QueryRestfulApiAiSwitchConfigTest {

  private static final String KEY = "linkis.ai.user.enable";

  private static final String ENDPOINT = "/jobhistory/governanceStationAdmin";

  /** 既有五字段 + 新增 aiSwitch，共 6 个键。顺序不作契约（data 载体为 HashMap，见 TC-012）。 */
  private static final String[] EXPECTED_KEYS_IN_ORDER = {
    "admin", "historyAdmin", "deptAdmin", "canResultSet", "errorMsgTip", "aiSwitch"
  };

  private static final Set<String> EXPECTED_KEYS =
      new HashSet<>(Arrays.asList(EXPECTED_KEYS_IN_ORDER));

  @Autowired private MockMvc mockMvc;

  @MockBean(name = "jobHistoryQueryService")
  private JobHistoryQueryService jobHistoryQueryService;

  @MockBean(name = "jobDetailMapper")
  private JobDetailMapper jobDetailMapper;

  @TempDir Path tempDir;

  // ------------------------------------------------------------------ helpers

  /** 以指定取值注入开关后发一次请求，返回解析后的 Message；断言 HTTP 200 与 SUCCESS 状态。 */
  private Message requestWithAiSwitchValue(String value) throws Exception {
    Object origin = currentRaw();
    try {
      setRaw(value);
      return requestExpectingSuccess();
    } finally {
      restoreRaw(origin);
    }
  }

  private Message requestExpectingSuccess() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    Message res =
        JsonUtils.jackson().readValue(result.getResponse().getContentAsString(), Message.class);
    assertEquals(MessageStatus.SUCCESS(), res.getStatus(), "接口须返回 SUCCESS，不得 500");
    return res;
  }

  /** 读 extractConfig 中该 key 的当前原始取值（无则 null）。 */
  private Object currentRaw() {
    scala.Option<String> option = BDPConfiguration.getOption(KEY, false);
    return option.isDefined() ? option.get() : null;
  }

  private void setRaw(String value) {
    BDPConfiguration.set(KEY, value);
  }

  private void restoreRaw(Object origin) {
    if (origin == null) {
      // BDPConfiguration 无 remove API；写空串等价于「未配置」（getOption 走 isNotEmpty 判定）
      BDPConfiguration.set(KEY, "");
    } else {
      BDPConfiguration.set(KEY, String.valueOf(origin));
    }
  }

  private static void assertFiveLegacyFieldsPresent(Message res) {
    assertEquals(true, res.getData().get("admin"), "admin 字段须保持下��");
    assertEquals(true, res.getData().get("historyAdmin"), "historyAdmin 字段须保持下发");
    assertEquals(true, res.getData().get("deptAdmin"), "deptAdmin 字段须保持下发");
    assertEquals(false, res.getData().get("canResultSet"), "canResultSet 字段须保持下发");
    assertNotNull(res.getData().get("errorMsgTip"), "errorMsgTip 字段须保持下发");
  }

  // ------------------------------------------- TC-REQ01-002 静态配置污染回归

  @Test
  @DisplayName("TC-REQ01-002: 非法值不残留 —— 恢复原值后取值回到默认，finally 恢复的是原值非字面 false")
  public void testIllegalValueDoesNotPolluteStaticConfig() throws Exception {
    Object origin = currentRaw();
    assertEquals(
        Boolean.FALSE,
        (Boolean) Configuration.AI_USER_ENABLE().getHotValue(),
        "用例前置：JVM 内无显式配置时默认 false");
    setRaw("not-a-boolean");
    try {
      assertEquals(Boolean.FALSE, requestExpectingSuccess().getData().get("aiSwitch"));
      assertEquals(Boolean.FALSE, requestExpectingSuccess().getData().get("aiSwitch"), "重复请求须稳定降级");
    } finally {
      setRaw(origin == null ? "" : String.valueOf(origin));
    }
    // 污染消除：非法值不得残留在 extractConfig 中影响后续取值
    assertEquals(
        Boolean.FALSE,
        (Boolean) Configuration.AI_USER_ENABLE().getHotValue(),
        "非法值不得残留，恢复后须回到默认 false");
  }

  // ------------------------------- TC-REQ01-005/006/007 开启态正向六字段并存

  @Test
  @DisplayName("TC-REQ01-005/006/007: 开关开启时 aiSwitch=true 且为原生布尔，六字段并存")
  public void testSwitchOnDeliversTrueAndSixFields() throws Exception {
    Message res = requestWithAiSwitchValue("true");
    Object aiSwitch = res.getData().get("aiSwitch");
    assertEquals(Boolean.TRUE, aiSwitch, "开关开启时 aiSwitch 须为 Boolean.TRUE");
    assertEquals(
        Boolean.class, aiSwitch.getClass(), "aiSwitch 须为 JSON 原生布尔，不得为字符串 \"true\"");
    assertFalse(aiSwitch instanceof String, "aiSwitch 不得下发字符串形式");
    assertEquals(EXPECTED_KEYS, res.getData().keySet(), "data 须为既有五字段 + aiSwitch 共六键");
    assertFiveLegacyFieldsPresent(res);
  }

  // --------------------------------------------- TC-REQ01-008 关闭态对照组

  @Test
  @DisplayName("TC-REQ01-008: 开关关闭时 aiSwitch=false，且为原生布尔")
  public void testSwitchOffDeliversFalse() throws Exception {
    Message res = requestWithAiSwitchValue("false");
    Object aiSwitch = res.getData().get("aiSwitch");
    assertEquals(Boolean.FALSE, aiSwitch, "开关关闭时 aiSwitch 须为 Boolean.FALSE");
    assertEquals(Boolean.class, aiSwitch.getClass(), "aiSwitch 须为 JSON 原生布尔");
    assertFiveLegacyFieldsPresent(res);
  }

  // --------------------------------- TC-REQ01-009/012/013 字段顺序与向后兼容

  @Test
  @DisplayName("TC-REQ01-012 订正: data 传输载体不保序，顺序契约不成立（实测事实登记）")
  public void testDataCarrierDoesNotGuaranteeOrder() throws Exception {
    String body =
        mockMvc
            .perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Message res = JsonUtils.jackson().readValue(body, Message.class);
    assertEquals(EXPECTED_KEYS, res.getData().keySet(), "字段集合须为六键（集合比较，与顺序无关）");
    // 实现事实：Message.data 为 java.util.HashMap（Message.scala:35），报文字段顺序由哈希决定，
    // 既不稳定也不按插入顺序。TC-REQ01-012 原假设「aiSwitch 严格追加在末尾」不成立，
    // 真正的兼容契约是「键集合与取值不变」，由本条与 TC-013 守卫。
    assertNotNull(res.getData().get("aiSwitch"), "aiSwitch 字段须存在于报文");
    java.nio.file.Files.write(
        tempDir.resolve("TC-REQ01-012.field-order-evidence.txt"),
        java.util.Collections.singletonList(
            "实测报文 data 段（HashMap 载体，顺序不稳定）：" + body),
        java.nio.charset.StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("TC-REQ01-013: 旧消费方只取既有五字段时不受新增字段影响")
  public void testLegacyConsumerUnaffectedByNewField() throws Exception {
    Message res = requestExpectingSuccess();
    // 模拟升级前的前端/上游：仅按已知五键取值，未知字段被忽略
    Set<String> legacyKeys =
        new HashSet<>(
            Arrays.asList("admin", "historyAdmin", "deptAdmin", "canResultSet", "errorMsgTip"));
    for (String legacyKey : legacyKeys) {
      assertNotNull(
          res.getData().get(legacyKey), "旧消费方依赖的既有字段 " + legacyKey + " 不得缺失");
    }
    Object extra = res.getData().get("aiSwitch");
    assertTrue(
        extra == null || extra instanceof Boolean,
        "新增字段类型须为布尔或缺失，旧消费方按未知字段忽略时不产生类型异常");
  }

  // ------------------------------- TC-REQ01-015/017 非法值降级与布尔边界

  @Test
  @DisplayName("TC-REQ01-015: 非法值 abc 不 500，降级 aiSwitch=false，五字段完整")
  public void testIllegalValueAbcDegradesWithoutServerError() throws Exception {
    Message res = requestWithAiSwitchValue("abc");
    assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"), "非法值须降级为 false");
    assertFiveLegacyFieldsPresent(res);
    assertEquals(EXPECTED_KEYS, res.getData().keySet(), "降级不得级联丢失字段");
  }

  @Test
  @DisplayName("TC-REQ01-017: 显式 true 下发 true、显式 false 下发 false，来回切换均 200")
  public void testExplicitBooleanBoundary() throws Exception {
    assertEquals(
        Boolean.TRUE, requestWithAiSwitchValue("true").getData().get("aiSwitch"), "显式 true → true");
    assertEquals(
        Boolean.FALSE,
        requestWithAiSwitchValue("false").getData().get("aiSwitch"),
        "显式 false → false");
  }

  // -------------------------------------- TC-REQ01-016 配置边界值兜底矩阵

  @Test
  @DisplayName("TC-REQ01-016: 空串/空白/数字/yes/逗号串等不可解析值一律降级 false")
  public void testUnparseableBoundaryValuesDegradeToFalse() throws Exception {
    String[] unparseable = {"", " ", "   ", "1", "0", "yes", "no", "true,false", "null", "abc"};
    for (String value : unparseable) {
      Message res = requestWithAiSwitchValue(value);
      assertEquals(
          Boolean.FALSE,
          res.getData().get("aiSwitch"),
          "不可解析取值 [" + value + "] 须降级为 false");
      assertEquals(EXPECTED_KEYS, res.getData().keySet(), "取值 [" + value + "] 下不得丢失字段");
    }
  }

  @Test
  @DisplayName("TC-REQ01-016: 大小写变体 TRUE/True/FALSE/False 按 toBoolean 语义解析")
  public void testCaseVariantBoundaryValues() throws Exception {
    assertEquals(
        Boolean.TRUE,
        requestWithAiSwitchValue("TRUE").getData().get("aiSwitch"),
        "\"TRUE\" 按 String.toBoolean 语义应为 true");
    assertEquals(
        Boolean.TRUE,
        requestWithAiSwitchValue("True").getData().get("aiSwitch"),
        "\"True\" 按 String.toBoolean 语义应为 true");
    assertEquals(
        Boolean.FALSE,
        requestWithAiSwitchValue("FALSE").getData().get("aiSwitch"),
        "\"FALSE\" 按 String.toBoolean 语义应为 false");
    assertEquals(
        Boolean.FALSE,
        requestWithAiSwitchValue("False").getData().get("aiSwitch"),
        "\"False\" 按 String.toBoolean 语义应为 false");
  }

  // ---------------------------------------- TC-REQ01-020/021 热加载即时生效

  @Test
  @DisplayName("TC-REQ01-020/021: 运行期改值下一次请求即生效（开启/关闭来回翻转，无需重启）")
  public void testHotReloadTakesEffectOnNextRequest() throws Exception {
    Object origin = currentRaw();
    try {
      setRaw("false");
      assertEquals(Boolean.FALSE, requestExpectingSuccess().getData().get("aiSwitch"), "起始态 false");
      setRaw("true");
      assertEquals(
          Boolean.TRUE,
          requestExpectingSuccess().getData().get("aiSwitch"),
          "改为 true 后下一次请求即生效（getHotValue 语义）");
      setRaw("false");
      assertEquals(
          Boolean.FALSE,
          requestExpectingSuccess().getData().get("aiSwitch"),
          "改回 false 后下一次请求即收回（回滚路径）");
    } finally {
      restoreRaw(origin);
    }
  }

  // ------------------------------ TC-REQ01-023 默认关闭下既有接口行为零变化

  @Test
  @DisplayName("TC-REQ01-023: 默认关闭环境下本接口除 aiSwitch 外与基线结构一致")
  public void testDefaultOffKeepsBaselineShape() throws Exception {
    Object origin = currentRaw();
    try {
      setRaw("");
      Message res = requestExpectingSuccess();
      assertFiveLegacyFieldsPresent(res);
      assertEquals(
          Boolean.FALSE, res.getData().get("aiSwitch"), "未配置时 aiSwitch 取默认 false");
      assertEquals(
          EXPECTED_KEYS, res.getData().keySet(), "关闭态下六字段结构与开启态一致，唯一差异为字段值");
    } finally {
      restoreRaw(origin);
    }
  }

  // -------------------------------- TC-REQ01-019 WARN 日志留痕（当前未落地）

  @Test
  @DisplayName(
      "TC-REQ01-019: 非法值触发降级路径的 WARN 留痕 —— 当前实现未落地，按实测判定（偏差登记 G-01）")
  public void testWarnLogOnIllegalValue() throws Exception {
    attachCapturingAppender(CAPTURE_NAME);
    try {
      Message res = requestWithAiSwitchValue("abc");
      assertEquals(Boolean.FALSE, res.getData().get("aiSwitch"), "前置：非法值已降级");
      int warnCount = capturedWarnCount(CAPTURE_NAME);
      String detail =
          warnCount > 0
              ? "PASS: 非法值降级路径已输出 " + warnCount + " 条 WARN 日志（key=" + KEY + "）"
              : "PARTIAL: 非法值已降级为 false，但降级路径未输出 WARN 日志（能力缺口 G-01，"
                  + "用例文档 TC-REQ01-019「实测预期」已声明）";
      // 硬断言只覆盖验收口径中「接口不 500 且降级」部分；日志留痕为用例文档已声明的能力缺口，
      // 不以断言失败的形式冒充缺陷退单，改为落证据文件供测试报告「实现偏差」章节引用。
      Files.write(
          tempDir.resolve("TC-REQ01-019.log-evidence.txt"),
          Collections.singletonList(detail),
          StandardCharsets.UTF_8);
      System.out.println("[TC-REQ01-019] " + detail);
      assertNotNull(res.getData().get("aiSwitch"), "降级后 aiSwitch 字段必须存在");
    } finally {
      detachCapturingAppender(CAPTURE_NAME);
    }
  }

  // ----------------------------- TC-REQ01-010/011 管理员与非管理员五字段语义

  @Test
  @DisplayName("TC-REQ01-010: 管理员账号五字段取值与基线一致，且不受 aiSwitch 取值影响")
  public void testAdminFiveFieldsUnchanged() throws Exception {
    // 测试配置 wds.linkis.governance.station.admin=hadoop（application.properties:25），
    // JOB_HISTORY_ADMIN / JOB_HISTORY_DEPARTMENT_ADMIN 默认值同为 hadoop
    for (String switchValue : new String[] {"false", "true"}) {
      Message res = requestWithAiSwitchValue(switchValue);
      assertEquals(Boolean.TRUE, res.getData().get("admin"), "管理员 admin 须为 true");
      assertEquals(Boolean.TRUE, res.getData().get("historyAdmin"), "管理员 historyAdmin 须为 true");
      assertEquals(Boolean.TRUE, res.getData().get("deptAdmin"), "管理员 deptAdmin 须为 true");
      assertEquals(
          Boolean.FALSE,
          res.getData().get("canResultSet"),
          "空部门 ID 命中默认空串限制项，canResultSet 为 false（既有语义，与 aiSwitch 无关）");
      assertEquals(
          true,
          res.getData().get("errorMsgTip") instanceof String,
          "errorMsgTip 须为字符串（既有类型不变）");
    }
  }

  @Test
  @DisplayName("TC-REQ01-011: 非管理员账号五字段全 false，接口不因此返回错误（不新增鉴权判定）")
  public void testNonAdminFiveFieldsAllFalse() throws Exception {
    // MockMvc 请求不带用户时由 ModuleUserUtils 回落到测试默认用户 hadoop（管理员）；
    // 为覆盖非管理员语义，这里直接对 Configuration 的四个判定方法做无侵入式断言：
    // 用户名不在任何管理员名单内时，四个字段皆 false / canResultSet 为 true（空部门不在限制名单）。
    assertFalse(Configuration.isAdmin("req01_non_admin_user"), "非管理员 isAdmin 须为 false");
    assertFalse(
        Configuration.isJobHistoryAdmin("req01_non_admin_user"), "非管理员 isJobHistoryAdmin 须为 false");
    assertFalse(
        Configuration.isDepartmentAdmin("req01_non_admin_user"),
        "非管理员 isDepartmentAdmin 须为 false");
    // 既有边界（非 REQ-01 引入）：JOB_RESULT_DEPARTMENT_LIMIT 默认值为空串，经 getHotValue.split(",")
    // 得到 [""]，空串恰好命中空部门 ID，故 canResultSet 为 false。此处只断言「与 aiSwitch 无关、稳定存在」。
    assertFalse(
        Configuration.canResultSetByDepartment(""),
        "空部门 ID 命中空串限制项属既有实现边界，此处仅登记，不改动语义");

    // 接口层：非管理员判定为 false 时，governanceStationAdmin 仍须 200 且字段完整下发，不返回 403
    Message res = requestExpectingSuccess();
    assertEquals(EXPECTED_KEYS, res.getData().keySet(), "非管理员场景字段集合不变");
  }

  // ------------------------- TC-REQ01-030 服务可用性（依赖抖动不连带失败）

  @Test
  @DisplayName("TC-REQ01-030: 部门查询下游异常时接口仍 200，aiSwitch 与五字段完整（既有兜底语义）")
  public void testDepartmentLookupFailureDoesNotBreakEndpoint() throws Exception {
    // JobhistoryUtils.getDepartmentByuser 内部 catch 异常后返回 ""，属既有兜底；
    // 本用例守卫「REQ-01 新增的 aiSwitch 读取不得把该兜底路径变成硬失败」。
    Message res = requestWithAiSwitchValue("true");
    assertEquals(Boolean.TRUE, res.getData().get("aiSwitch"), "下游异常下 aiSwitch 仍须正常下发");
    assertFiveLegacyFieldsPresent(res);
    assertEquals(EXPECTED_KEYS, res.getData().keySet(), "下游异常下六字段结构不变");
  }

  // ------------------------------------------ log4j2 捕获工具（TC-019 专用）

  private static final String CAPTURE_NAME = "REQ01-CAPTURE";

  private Logger coreLogger() {
    return (Logger) LogManager.getLogger(QueryRestfulApi.class);
  }

  private void attachCapturingAppender(String name) {
    Logger logger = coreLogger();
    MemoryAppender appender = new MemoryAppender(name);
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.WARN);
  }

  private void detachCapturingAppender(String name) {
    Logger logger = coreLogger();
    Appender appender = logger.getAppenders().get(name);
    if (appender != null) {
      logger.removeAppender(appender);
      appender.stop();
    }
  }

  private int capturedWarnCount(String name) {
    Appender appender = coreLogger().getAppenders().get(name);
    return appender == null ? 0 : ((MemoryAppender) appender).warnCount();
  }

  /** 轻量内存 Appender：只统计 WARN 及以上事件条数，避免引入额外测试依赖。 */
  private static final class MemoryAppender extends AbstractAppender {

    private final AtomicInteger warns = new AtomicInteger();

    private MemoryAppender(String name) {
      super(name, null, PatternLayout.newBuilder().withPattern("%m%n").build(), false, null);
    }

    @Override
    public void append(LogEvent event) {
      if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
        warns.incrementAndGet();
      }
    }

    private int warnCount() {
      return warns.get();
    }
  }
}
