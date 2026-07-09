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

package org.apache.linkis.configuration.cucumber.steps;

import org.apache.linkis.configuration.Scan;
import org.apache.linkis.configuration.WebApplicationServer;
import org.apache.linkis.configuration.entity.ConfigValue;
import org.apache.linkis.configuration.restful.api.ConfigurationRestfulApi;
import org.apache.linkis.configuration.service.ConfigKeyService;
import org.apache.linkis.server.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cucumber Step Definitions for 用户配置删除功能测试
 *
 * <p>测试范围: - Rule 1: 现有功能回归测试 - Rule 2: 权限控制测试 - Rule 3: 删除功能测试 - Rule 4: 安全测试 - Rule 5: 配置定义保留测试 -
 * Rule 6: 不同配置类型测试
 */
@CucumberContextConfiguration
@SpringBootTest(classes = {WebApplicationServer.class, Scan.class})
public class UserConfigDeleteSteps {

  @Autowired private ConfigurationRestfulApi configurationRestfulApi;

  @MockBean private ConfigKeyService configKeyService;

  private MockHttpServletRequest request;
  private Message result;
  private ConfigValue testConfigValue;
  private String currentUser;
  private boolean isAdmin;

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
    result = null;
    testConfigValue = null;
    currentUser = null;
    isAdmin = false;
  }

  // ==================== Background Steps ====================

  @Given("系统已启动")
  public void systemStarted() {
    // 系统启动验证
    assertNotNull(configurationRestfulApi, "Configuration API should be initialized");
  }

  @And("用户已登录")
  public void userLoggedIn() {
    // 默认用户登录
    currentUser = "testUser";
    request.setRemoteUser(currentUser);
  }

  @And("配置管理功能正常")
  public void configManagementFunctionNormal() {
    // 配置管理功能验证
    assertNotNull(configKeyService, "ConfigKeyService should be available");
  }

  // ==================== Rule 1: 现有功能回归测试 ====================

  @Given("配置数据库中存在用户配置")
  public void configDatabaseHasUserConfig() {
    testConfigValue = new ConfigValue();
    testConfigValue.setId(123L);
    testConfigValue.setConfigKeyId(1L);
    testConfigValue.setConfigValue("test-value");

    // Mock deleteConfigValueById 方法（API实际调用的方法）
    when(configKeyService.deleteConfigValueById(anyLong())).thenReturn(testConfigValue);
    when(configKeyService.deleteConfigValue(anyString(), anyList()))
        .thenReturn(Arrays.asList(testConfigValue));
  }

  @And("普通用户{string}已登录")
  public void normalUserLoggedIn(String username) {
    currentUser = username;
    isAdmin = false;
    request.setRemoteUser(username);
  }

  @When("用户访问配置管理页面")
  public void userAccessConfigManagementPage() {
    // 前端操作，此处为模拟
    // 实际测试在Playwright E2E测试中完成
  }

  @And("用户查询自己的配置列表")
  public void userQueryOwnConfigList() {
    // 查询配置列表
    // 实际测试在Playwright E2E测试中完成
  }

  @Then("应该显示用户的配置列表")
  public void shouldDisplayUserConfigList() {
    // 验证配置列表显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("行为应该与增强前完全一致")
  public void behaviorShouldBeConsistentWithBefore() {
    // 验证行为一致性
    // 实际测试在Playwright E2E测试中完成
  }

  @And("不应该显示删除按钮")
  public void shouldNotDisplayDeleteButton() {
    // 验证删除按钮不显示
    // 实际测试在Playwright E2E测试中完成
  }

  @When("用户点击编辑某个配置")
  public void userClickEditConfig() {
    // 前端操作
    // 实际测试在Playwright E2E测试中完成
  }

  @Then("应该打开编辑对话框")
  public void shouldOpenEditDialog() {
    // 验证编辑对话框
    // 实际测试在Playwright E2E测试中完成
  }

  @And("用户可以修改配置值")
  public void userCanModifyConfigValue() {
    // 验证配置值修改
    // 实际测试在Playwright E2E测试中完成
  }

  @And("保存后配置应该更新成功")
  public void saveConfigShouldSuccess() {
    // 验证保存成功
    // 实际测试在Playwright E2E测试中完成
  }

  // ==================== Rule 2: 权限控制测试 ====================

  @And("管理员{string}已登录")
  public void adminUserLoggedIn(String username) {
    currentUser = username;
    isAdmin = true;
    request.setRemoteUser(username);
  }

  @When("管理员访问配置管理页面")
  public void adminAccessConfigManagementPage() {
    // 前端操作
    // 实际测试在Playwright E2E测试中完成
  }

  @Then("每个配置项的操作列应该显示{string}按钮")
  public void eachConfigShouldDisplayDeleteButton(String buttonText) {
    // 验证删除按钮显示
    // 实际测试在Playwright E2E测试中完成
  }

  @Then("操作列不应该显示{string}按钮")
  public void operationColumnShouldNotDisplayButton(String buttonText) {
    // 验证按钮不显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("只应该显示{string}按钮")
  public void shouldOnlyDisplayButton(String buttonText) {
    // 验证只显示指定按钮
    // 实际测试在Playwright E2E测试中完成
  }

  // ==================== Rule 3: 删除功能测试 ====================

  @Given("配置数据库中存在配置:")
  public void configDatabaseHasConfig(DataTable dataTable) {
    List<Map<String, String>> rows = dataTable.asMaps();
    for (Map<String, String> row : rows) {
      testConfigValue = new ConfigValue();
      testConfigValue.setId(123L); // 设置有效ID，避免删除时ID为null
      testConfigValue.setConfigKeyId(1L);
      testConfigValue.setConfigValue(row.get("configValue"));
    }

    // Mock deleteConfigValueById 方法（API实际调用的方法）
    when(configKeyService.deleteConfigValueById(anyLong())).thenReturn(testConfigValue);
    when(configKeyService.deleteConfigValue(anyString(), anyList()))
        .thenReturn(Arrays.asList(testConfigValue));
  }

  // 无参数版本，用于 "删除失败时显示错误信息" 场景
  @Given("配置数据库中存在配置")
  public void configDatabaseHasConfigSimple() {
    testConfigValue = new ConfigValue();
    testConfigValue.setId(123L);
    testConfigValue.setConfigKeyId(1L);
    testConfigValue.setConfigValue("test-value");

    // Mock deleteConfigValueById 方法（API实际调用的方法）
    when(configKeyService.deleteConfigValueById(anyLong())).thenReturn(testConfigValue);
    when(configKeyService.deleteConfigValue(anyString(), anyList()))
        .thenReturn(Arrays.asList(testConfigValue));
  }

  @When("管理员点击配置{string}的{string}按钮")
  public void adminClickConfigDeleteButton(String configKey, String buttonName) {
    // 前端操作
    // 实际测试在Playwright E2E测试中完成
  }

  @And("管理员在确认对话框中点击{string}")
  public void adminConfirmDeleteInDialog(String action) {
    if ("确认".equals(action)) {
      // 执行删除（GET请求，id作为参数）
      result = configurationRestfulApi.deleteKeyValueByAdmin(request, testConfigValue.getId());
    }
  }

  @Then("应该显示{string}提示")
  public void shouldDisplayMessage(String message) {
    assertNotNull(result, "Result should not be null");
    if (message.contains("成功")) {
      assertEquals(0, result.getStatus(), "Status should be success");
    } else {
      assertNotEquals(0, result.getStatus(), "Status should be error");
    }
  }

  @And("配置列表应该自动刷新")
  public void configListShouldRefresh() {
    // 验证列表刷新
    // 实际测试在Playwright E2E测试中完成
  }

  @And("配置{string}不应该再显示在列表中")
  public void configShouldNotDisplayInList(String configKey) {
    // 验证配置不显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("数据库中该配置应该被删除")
  public void configShouldBeDeletedInDatabase() {
    // 验证调用了 deleteConfigValueById 方法
    verify(configKeyService, atLeastOnce()).deleteConfigValueById(anyLong());
  }

  @Then("确认对话框应该关闭")
  public void confirmDialogShouldClose() {
    // 验证对话框关闭
    // 实际测试在Playwright E2E测试中完成
  }

  @And("配置列表应该保持不变")
  public void configListShouldRemainUnchanged() {
    // 验证列表不变
    // 实际测试在Playwright E2E测试中完成
  }

  @And("配置{string}应该仍然显示在列表中")
  public void configShouldStillDisplayInList(String configKey) {
    // 验证配置仍显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("数据库中该配置应该仍然存在")
  public void configShouldStillExistInDatabase() {
    // 验证配置仍存在（未调用删除方法）
    verify(configKeyService, never()).deleteConfigValueById(anyLong());
    verify(configKeyService, never()).deleteConfigValue(anyString(), anyList());
  }

  @Then("确认对话框应该显示:")
  public void confirmDialogShouldDisplayInfo(DataTable dataTable) {
    // 验证对话框信息
    // 实际测试在Playwright E2E测试中完成
  }

  @And("后端服务不可用或返回错误")
  public void backendServiceUnavailable() {
    // Mock deleteConfigValueById 返回 null，模拟服务不可用
    when(configKeyService.deleteConfigValueById(anyLong())).thenReturn(null);
    when(configKeyService.deleteConfigValue(anyString(), anyList()))
        .thenThrow(new RuntimeException("Service unavailable"));
  }

  @When("管理员点击某个配置的{string}按钮")
  public void adminClickSomeConfigDeleteButton(String buttonName) {
    // 前端操作
    // 实际测试在Playwright E2E测试中完成
  }

  @And("确认对话框应该保持打开")
  public void confirmDialogShouldRemainOpen() {
    // 验证对话框保持打开
    // 实际测试在Playwright E2E测试中完成
  }

  // 删除失败的错误提示
  @Then("应该显示删除失败的错误提示")
  public void shouldDisplayDeleteFailedError() {
    assertNotNull(result, "Result should not be null");
    assertNotEquals(0, result.getStatus(), "Status should indicate error");
  }

  // 配置不存在的错误提示
  @Then("应该显示\"配置不存在\"的错误提示")
  public void shouldDisplayConfigNotExistError() {
    assertNotNull(result, "Result should not be null");
    assertNotEquals(0, result.getStatus(), "Status should indicate error");
  }

  // 管理员点击该配置的删除按钮（用于关键配置场景）
  @When("管理员点击该配置的{string}按钮")
  public void adminClickThisConfigDeleteButton(String buttonName) {
    // 前端操作，用于关键配置删除场景
    // 实际测试在Playwright E2E测试中完成
  }

  // DELETE接口调用（安全测试场景）
  @When("用户尝试直接调用删除接口 DELETE \\/configuration\\/keyvalue")
  public void userTryDirectCallDeleteApiWithDelete() {
    // 模拟直接调用DELETE API
    if (!isAdmin) {
      // 非管理员调用DELETE接口
      result = configurationRestfulApi.deleteKeyValueByAdmin(request, 999L);
    }
  }

  @Given("配置数据库中不存在某个配置")
  public void configDatabaseNotHasSomeConfig() {
    when(configKeyService.deleteConfigValue(anyString(), anyList())).thenReturn(null);
  }

  @When("管理员尝试删除该不存在的配置")
  public void adminTryDeleteNonExistentConfig() {
    // 执行删除（GET请求，id作为参数）
    result = configurationRestfulApi.deleteKeyValueByAdmin(request, 999999L);
  }

  @Given("配置数据库中存在关键配置:")
  public void configDatabaseHasCriticalConfig(DataTable dataTable) {
    configDatabaseHasConfig(dataTable);
  }

  @Then("确认对话框应该显示警告:{string}")
  public void confirmDialogShouldDisplayWarning(String warning) {
    // 验证警告显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("管理员仍然可以执行删除操作")
  public void adminCanStillExecuteDelete() {
    // 验证删除按钮可用
    // 实际测试在Playwright E2E测试中完成
  }

  // ==================== Rule 4: 安全测试 ====================

  @When("用户尝试直接调用删除接口 GET \\/configuration\\/admin\\/keyvalue")
  public void userTryDirectCallDeleteApi() {
    // 模拟直接调用API（GET请求，id作为参数）
    if (!isAdmin) {
      // 非管理员调用
      result = configurationRestfulApi.deleteKeyValueByAdmin(request, 999L);
    }
  }

  @Then("应该返回权限不足错误")
  public void shouldReturnPermissionDeniedError() {
    assertNotNull(result, "Result should not be null");
    assertNotEquals(0, result.getStatus(), "Status should indicate error");
  }

  @And("HTTP状态码应该是{int}或{int}")
  public void httpStatusShouldBe(int status1, int status2) {
    // HTTP状态码验证
    // 实际测试在API集成测试中完成
  }

  @And("配置不应该被删除")
  public void configShouldNotBeDeleted() {
    verify(configKeyService, never()).deleteConfigValue(anyString(), anyList());
  }

  @Given("后端删除接口已启用管理员权限检查")
  public void backendDeleteApiEnabledAdminPermissionCheck() {
    // 权限检查已启用
    // 验证在API层面实现
  }

  @When("用户尝试通过界面删除配置")
  public void userTryDeleteConfigViaUI() {
    // 前端操作
    // 实际测试在Playwright E2E测试中完成
  }

  @Then("删除按钮不应该显示")
  public void deleteButtonShouldNotDisplay() {
    // 验证删除按钮不显示
    // 实际测试在Playwright E2E测试中完成
  }

  @And("用户无法触发删除操作")
  public void userCannotTriggerDeleteOperation() {
    // 验证无法触发删除
    // 实际测试在Playwright E2E测试中完成
  }

  // ==================== Rule 5: 配置定义保留测试 ====================

  @And("配置{string}的配置定义已存在")
  public void configDefinitionExists(String configKey) {
    // 配置定义已存在
    // 验证在数据库层面
  }

  @When("管理员删除该配置值")
  public void adminDeleteConfigValue() {
    // GET请求，id作为参数
    result = configurationRestfulApi.deleteKeyValueByAdmin(request, testConfigValue.getId());
  }

  @Then("配置值应该被删除")
  public void configValueShouldBeDeleted() {
    assertNotNull(result, "Result should not be null");
    assertEquals(0, result.getStatus(), "Status should be success");
  }

  @And("配置定义应该仍然存在")
  public void configDefinitionShouldStillExist() {
    // 验证配置定义存在
    // 实际测试在数据库层面
  }

  @And("用户可以重新创建该配置")
  public void userCanRecreateConfig() {
    // 验证可重新创建
    // 实际测试在功能测试中完成
  }

  // ==================== Rule 6: 不同配置类型测试 ====================

  @Given("配置数据库中存在全局配置:")
  public void configDatabaseHasGlobalConfig(DataTable dataTable) {
    configDatabaseHasConfig(dataTable);
  }

  @When("管理员删除该全局配置")
  public void adminDeleteGlobalConfig() {
    adminDeleteConfigValue();
  }

  @Then("全局配置应该被成功删除")
  public void globalConfigShouldBeDeletedSuccessfully() {
    configValueShouldBeDeleted();
  }

  @Given("配置数据库中存在引擎特定配置:")
  public void configDatabaseHasEngineSpecificConfig(DataTable dataTable) {
    configDatabaseHasConfig(dataTable);
  }

  @When("管理员删除该引擎配置")
  public void adminDeleteEngineConfig() {
    adminDeleteConfigValue();
  }

  @Then("引擎配置应该被成功删除")
  public void engineConfigShouldBeDeletedSuccessfully() {
    configValueShouldBeDeleted();
  }

  @And("不应该影响其他引擎的相同配置")
  public void shouldNotAffectOtherEngineConfig() {
    // 验证不影响其他引擎配置
    // 实际测试在数据库层面
  }

  @Given("配置数据库中存在用户特定配置:")
  public void configDatabaseHasUserSpecificConfig(DataTable dataTable) {
    configDatabaseHasConfig(dataTable);
  }

  @When("管理员删除该用户配置")
  public void adminDeleteUserConfig() {
    adminDeleteConfigValue();
  }

  @Then("用户配置应该被成功删除")
  public void userConfigShouldBeDeletedSuccessfully() {
    configValueShouldBeDeleted();
  }

  @And("不应该影响其他用户的相同配置")
  public void shouldNotAffectOtherUserConfig() {
    // 验证不影响其他用户配置
    // 实际测试在数据库层面
  }
}
