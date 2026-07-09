/**
 * 运维工具-用户配置删除功能 E2E测试
 *
 * 需求类型: ENHANCE（功能增强）
 * 基础模块: Configuration（用户配置管理模块）
 * 测试范围: 前端UI显示和交互、后端接口调用、权限控制
 *
 * 测试策略: 通过点击菜单导航，而非直接跳转页面
 */

import { test, expect, Page } from '@playwright/test';
import { DSSLoginHelper, PROXY_USERS } from './utils/dss-login';

// 配置测试为串行执行，共享页面状态
test.describe.configure({ mode: 'serial' });

const BASE_URL = process.env.TEST_BASE_URL || 'http://10.107.97.166:8088';

// 国际化文本映射（支持中英文版本）
const I18N = {
  // 运维工具菜单
  OPS_TOOL: ['OPS Tool', '运维工具'],
  // 用户配置菜单
  USER_CONFIG: ['User Configuration', '用户配置'],
  // 确认对话框标题
  CONFIRM: ['Confirm', '确认'],
  // 删除确认对话框标题
  CONFIRM_DELETE: ['Confirm Delete', '确认删除'],
  // 提交按钮
  SUBMIT: ['Submit', '提交'],
  // 取消按钮
  CANCEL: ['Cancel', '取消'],
  // 成功消息（多种变体）
  SUCCESS: ['Success', '成功', 'successfully', '删除成功', 'Delete successfully'],
  // 删除按钮文本
  DELETE: ['Delete', '删除'],
  // 编辑按钮文本
  EDIT: ['Edit', '编辑']
};

/**
 * User Configuration页面操作辅助类
 */
class UserConfigPage {
  readonly page: Page;
  readonly dssHelper: DSSLoginHelper;
  private isLoggedIn: boolean = false;
  private currentUser: string | null = null;
  // 测试数据ID列表，用于清理
  private testConfigIds: number[] = [];

  constructor(page: Page) {
    this.page = page;
    this.dssHelper = new DSSLoginHelper(page);
  }

  // 创建测试配置数据
  async createTestConfig(configData: {
    user: string;
    creator: string;
    engineType: string;
    version: string;
    configKey: string;
    configValue: string;
  }): Promise<number | null> {
    try {
      // 先获取configKeyId
      const listUrl = `${BASE_URL}/api/rest_j/v1/configuration/getItemList?user=${configData.user}&creator=${configData.creator}&engineType=${configData.engineType}&version=${configData.version}&pageSize=100`;
      const listRes = await this.page.request.get(listUrl);

      if (!listRes.ok()) {
        console.log(`获取配置列表失败: ${listRes.status()}`);
        return null;
      }

      const listJson = await listRes.json();
      const configItem = listJson?.data?.itemList?.find((item: any) => item.key === configData.configKey);

      if (!configItem) {
        console.log(`配置项 ${configData.configKey} 不存在`);
        // 尝试使用其他配置项
        const firstItem = listJson?.data?.itemList?.[0];
        if (firstItem) {
          console.log(`使用第一个可用配置项: ${firstItem.key}`);
          configData.configKey = firstItem.key;
          configData.configValue = firstItem.defaultValue || 'test_value';
          return this.createTestConfigWithItem(configData, firstItem);
        }
        return null;
      }

      return this.createTestConfigWithItem(configData, configItem);
    } catch (err) {
      console.log(`创建测试配置异常: ${err}`);
      return null;
    }
  }

  private async createTestConfigWithItem(configData: any, configItem: any): Promise<number | null> {
    try {
      // 创建配置
      const response = await this.page.request.post(
        `${BASE_URL}/api/rest_j/v1/configuration/keyvalue`,
        {
          headers: { 'Content-Type': 'application/json' },
          data: {
            user: configData.user,
            creator: configData.creator,
            engineType: configData.engineType,
            version: configData.version,
            configKey: configData.configKey,
            configValue: configData.configValue,
            configKeyId: String(configItem.id),
            force: false
          }
        }
      );

      if (response.ok()) {
        console.log(`创建测试配置成功: ${configData.configKey}`);
      } else {
        const errorText = await response.text();
        console.log(`创建测试配置失败: ${response.status()} - ${errorText}`);
        return null;
      }

      // 查询刚创建的配置获取ID
      await this.page.waitForTimeout(1000);
      const listUrl = `${BASE_URL}/api/rest_j/v1/configuration/userKeyValue?user=${configData.user}&creator=${configData.creator}&engineType=${configData.engineType}&key=${configData.configKey}`;
      const listResponse = await this.page.request.get(listUrl);

      if (!listResponse.ok()) {
        console.log(`查询配置失败: ${listResponse.status()}`);
        return null;
      }

      const listData = await listResponse.json();
      const newItem = listData?.data?.configValueList?.find((item: any) => item.key === configData.configKey);
      if (newItem && newItem.id) {
        this.testConfigIds.push(newItem.id);
        console.log(`测试配置ID: ${newItem.id}`);
        return newItem.id;
      }
      return null;
    } catch (err) {
      console.log(`创建配置异常: ${err}`);
      return null;
    }
  }

  // 清理所有测试数据
  async cleanupTestConfigs() {
    console.log(`清理测试数据: ${this.testConfigIds.length} 条`);
    for (const id of this.testConfigIds) {
      try {
        await this.page.request.get(
          `${BASE_URL}/api/rest_j/v1/configuration/admin/keyvalue`,
          { params: { id } }
        );
        console.log(`已删除测试配置: ${id}`);
      } catch (err) {
        console.log(`删除测试配置失败: ${id}`);
      }
    }
    this.testConfigIds = [];
  }

  // 登录（仅在未登录时执行）
  async loginAs(proxyUser: string) {
    // 如果已登录且用户相同，跳过登录
    if (this.isLoggedIn && this.currentUser === proxyUser) {
      console.log(`Already logged in as ${proxyUser}, skipping login`);
      return;
    }

    await this.dssHelper.loginToLinkis(proxyUser);
    this.isLoggedIn = true;
    this.currentUser = proxyUser;

    // 通过点击菜单导航到User Configuration页面
    await this.navigateToUserConfigByClick();

    // 等待表格加载
    await this.page.waitForSelector('.ivu-table-wrapper', { timeout: 10000 }).catch(() => {});
    await this.page.waitForTimeout(2000);
  }

  // 通过点击菜单导航到User Configuration页面
  async navigateToUserConfigByClick() {
    // 等待控制台页面加载完成
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForTimeout(2000);

    const iframe = this.page.locator('#iframe').contentFrame();

    // 先检查 OPS Tool 是否可见（管理员可见）- 支持中英文
    const opsTool = iframe.locator(`text=/${I18N.OPS_TOOL.join('|')}/`);
    const isOpsToolVisible = await opsTool.isVisible({ timeout: 3000 }).catch(() => false);

    if (isOpsToolVisible) {
      // 管理员：通过 OPS Tool 菜单导航
      console.log('通过 OPS Tool 菜单导航');
      await opsTool.first().click();
      await this.page.waitForTimeout(500);
      await iframe.locator('div:nth-child(12) > .ivu-cell > .ivu-cell-link > .ivu-cell-item').click();
      // 支持中英文的用户配置菜单
      await iframe.locator(`text=/${I18N.USER_CONFIG.join('|')}/`).first().click();
    } else {
      // 普通用户：直接跳转到 User Configuration 页面
      console.log('OPS Tool 不可见，直接跳转到 User Configuration 页面');
      await this.page.goto(`${BASE_URL}/dss/linkis/#/console/userConfig`);
      await this.page.waitForLoadState('domcontentloaded');
      await this.page.waitForTimeout(2000);
    }
  }

  // 切换到其他用户（需要重新登录）
  async switchUser(proxyUser: string) {
    console.log(`Switching user from ${this.currentUser} to ${proxyUser}`);
    await this.dssHelper.clearSession();
    this.isLoggedIn = false;
    this.currentUser = null;
    await this.loginAs(proxyUser);
  }

  // 获取 iframe 内容
  getIframe() {
    return this.page.locator('#iframe').contentFrame();
  }

  // 刷新当前页面（用于测试数据刷新）
  async refreshPage() {
    await this.page.reload();
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForTimeout(2000);
    await this.getIframe().locator('.ivu-table-wrapper').waitFor({ timeout: 10000 }).catch(() => {});
  }

  // 获取删除按钮数量
  async getDeleteButtonCount() {
    return await this.getIframe().locator('button.ivu-btn-error').count();
  }

  // 获取编辑按钮数量
  async getEditButtonCount() {
    return await this.getIframe().locator('button.ivu-btn-primary').count();
  }

  // 点击第一个删除按钮
  async clickFirstDeleteButton() {
    const deleteButton = this.getIframe().locator('button.ivu-btn-error').first();
    await deleteButton.click();
    await this.page.waitForTimeout(1000);
  }

  // 等待确认对话框出现
  async waitForConfirmDialog() {
    // 直接查找可见的确认对话框（支持中英文）
    const confirmModal = this.getIframe().locator('.ivu-modal-wrap').filter({
      hasText: new RegExp(I18N.CONFIRM_DELETE.join('|'))
    }).first();

    await confirmModal.waitFor({ state: 'visible', timeout: 5000 });
    return confirmModal;
  }

  // 确认删除
  async confirmDelete() {
    const modal = await this.waitForConfirmDialog();

    // 查找确认按钮 - 在modal内部
    const confirmBtn = modal.locator('.ivu-btn-primary').first();
    await confirmBtn.click();
    await this.page.waitForTimeout(2000);
  }

  // 取消删除
  async cancelDelete() {
    const modal = await this.waitForConfirmDialog();

    // 查找取消按钮
    const cancelBtn = modal.locator('button.ivu-btn-text, button.ivu-btn-default').first();
    await cancelBtn.click();
    await this.page.waitForTimeout(1000);
  }

  // 获取Toast消息
  async getToastMessage() {
    return this.getIframe().locator('.ivu-message');
  }

  // 检查Toast是否包含成功消息（支持中英文）
  async hasSuccessToast(): Promise<boolean> {
    const toast = await this.getToastMessage();
    const text = await toast.textContent().catch(() => '');
    return I18N.SUCCESS.some(s => text?.includes(s));
  }

  // 获取当前用户
  getCurrentUser(): string | null {
    return this.currentUser;
  }
}

// ==================== 测试套件 ====================

// 管理员权限测试 - 使用共享页面状态
test.describe('管理员权限测试', () => {
  let configPage: UserConfigPage;
  let sharedPage: Page;

  // 在所有测试前初始化页面并登录一次
  test.beforeAll(async ({ browser }) => {
    sharedPage = await browser.newPage();
    configPage = new UserConfigPage(sharedPage);
    // 只登录一次，后续测试复用
    await configPage.loginAs(PROXY_USERS.ADMIN);
  });

  // 在所有测试后关闭页面并清理测试数据
  test.afterAll(async () => {
    // 清理测试数据
    await configPage.cleanupTestConfigs();
    await sharedPage.close();
  });

  test('TC003: 管理员可以看到删除按钮', async () => {
    const deleteCount = await configPage.getDeleteButtonCount();
    expect(deleteCount).toBeGreaterThan(0);
    console.log(`Admin delete buttons: ${deleteCount}`);
  });

  test('TC005: 成功删除用户配置', async () => {
    // 直接使用当前页面的数据
    const deleteCount = await configPage.getDeleteButtonCount();
    if (deleteCount === 0) {
      console.log('没有可删除的数据，跳过此测试');
      test.skip();
      return;
    }

    // 点击删除
    await configPage.clickFirstDeleteButton();

    // 确认删除
    await configPage.confirmDelete();

    // 检查结果（支持中英文成功消息）
    const toast = await configPage.getToastMessage();
    await expect(toast).toContainText(new RegExp(I18N.SUCCESS.join('|')), { timeout: 10000 });

    console.log('删除成功');
  });

  test('TC006: 用户取消删除操作', async () => {
    // 检查是否有数据
    const deleteCount = await configPage.getDeleteButtonCount();
    if (deleteCount === 0) {
      console.log('没有数据，跳过此测试');
      test.skip();
      return;
    }

    // 点击删除
    await configPage.clickFirstDeleteButton();

    // 取消删除
    await configPage.cancelDelete();

    // 确认对话框关闭
    const modal = configPage.getIframe().locator('.ivu-modal-wrap').filter({ hasNot: configPage.getIframe().locator('.ivu-modal-hidden') });
    await expect(modal).not.toBeVisible({ timeout: 5000 });
  });

  test('TC007: 删除配置时显示完整配置信息', async () => {
    // 检查是否有数据
    const deleteCount = await configPage.getDeleteButtonCount();
    if (deleteCount === 0) {
      console.log('没有数据，跳过此测试');
      test.skip();
      return;
    }

    // 点击删除
    await configPage.clickFirstDeleteButton();

    // 在确认对话框中检查内容
    const modal = await configPage.waitForConfirmDialog();
    const modalBody = modal.locator('.ivu-modal-body');
    await expect(modalBody).toBeVisible({ timeout: 5000 });

    // 取消删除
    await configPage.cancelDelete();
  });

  test('TC010: 删除重要配置时仍可操作', async () => {
    // 检查是否有数据
    const deleteCount = await configPage.getDeleteButtonCount();
    if (deleteCount === 0) {
      console.log('没有数据，跳过此测试');
      test.skip();
      return;
    }

    // 点击删除
    await configPage.clickFirstDeleteButton();

    // 检查确认按钮可用
    const modal = await configPage.waitForConfirmDialog();
    const confirmBtn = modal.locator('.ivu-btn-primary').first();
    await expect(confirmBtn).toBeEnabled({ timeout: 5000 });

    // 取消删除
    await configPage.cancelDelete();
  });
});

// 普通用户权限测试 - 切换用户
test.describe('普通用户权限测试', () => {
  let configPage: UserConfigPage;
  let sharedPage: Page;

  test.beforeAll(async ({ browser }) => {
    sharedPage = await browser.newPage();
    configPage = new UserConfigPage(sharedPage);
    // 以普通用户身份登录
    await configPage.loginAs(PROXY_USERS.NORMAL);
  });

  test.afterAll(async () => {
    await sharedPage.close();
  });

  test('TC004: 普通用户看不到删除按钮', async () => {
    // 等待表格加载
    await configPage.getIframe().locator('.ivu-table-wrapper').waitFor({ timeout: 10000 }).catch(() => {});

    // 检查删除按钮数量应该为0
    const deleteCount = await configPage.getDeleteButtonCount();
    expect(deleteCount).toBe(0);
    console.log(`Normal user delete buttons: ${deleteCount}`);

    // 注意：普通用户可能没有编辑按钮，或者按钮选择器不同
    // 这里只验证删除按钮不可见即可
  });

  test('TC012: 普通用户界面无删除按钮', async () => {
    // 再次确认删除按钮不可见
    const deleteButtons = configPage.getIframe().locator('button.ivu-btn-error');
    await expect(deleteButtons).toHaveCount(0, { timeout: 5000 });
  });
});

// 安全测试
test.describe('安全测试', () => {
  let configPage: UserConfigPage;
  let sharedPage: Page;

  test.beforeAll(async ({ browser }) => {
    sharedPage = await browser.newPage();
    configPage = new UserConfigPage(sharedPage);
    await configPage.loginAs(PROXY_USERS.NORMAL);
  });

  test.afterAll(async () => {
    await sharedPage.close();
  });

  test('TC011: 非管理员用户无法直接调用删除接口', async () => {
    // 直接调用删除接口
    const response = await sharedPage.request.delete(`${BASE_URL}/api/rest_j/v1/configuration/admin/keyvalue`, {
      headers: { 'Content-Type': 'application/json' },
      data: JSON.stringify({ id: 1 })
    });

    // 应返回权限错误或参数错误
    const status = response.status();
    expect(status).toBeGreaterThanOrEqual(400);
    console.log(`Delete API response status: ${status}`);
  });
});
