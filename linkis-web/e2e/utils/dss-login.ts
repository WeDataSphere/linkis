/**
 * DSS门户登录辅助类
 * 简化版：登录 -> 选择代理用户 -> 进入Linkis主页
 */

import { Page, BrowserContext } from '@playwright/test';

// 配置
const BASE_URL = process.env.TEST_BASE_URL || 'http://10.107.97.166:8088';
const LOGIN_URL = `${BASE_URL}/#/login`;

// 登录用户配置
const LOGIN_USER = {
  username: process.env.TEST_LOGIN_USER || 'v_kkhuang',
  password: process.env.TEST_LOGIN_PASS || 'kkhuang~1'
};

// 代理用户配置
export const PROXY_USERS = {
  ADMIN: 'hadoop',      // 管理员代理用户
  NORMAL: 'hduser05',   // 普通用户代理用户
  CURRENT: 'v_kkhuang'  // 当前登录用户
};

/**
 * DSS门户登录辅助类
 */
export class DSSLoginHelper {
  readonly page: Page;
  readonly context: BrowserContext;

  constructor(page: Page) {
    this.page = page;
    this.context = page.context();
  }

  /**
   * 清除会话状态（cookies + storage）
   */
  async clearSession() {
    await this.context.clearCookies();
    try {
      await this.page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      });
    } catch { }
    console.log('会话已清除');
  }

  /**
   * 1. 登录DSS门户
   */
  async login() {
    // 清除会话
    await this.clearSession();

    // 访问登录页
    await this.page.goto(LOGIN_URL);
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForTimeout(2000);

    // 填写用户名密码
    await this.page.locator('input[type="text"]').first().fill(LOGIN_USER.username);
    await this.page.locator('input[type="password"]').first().fill(LOGIN_USER.password);

    // 点击登录
    await this.page.locator('button:has-text("Login")').first().click();
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForTimeout(3000);

    console.log(`登录成功: ${LOGIN_USER.username}`);
  }

  /**
   * 2. 选择代理用户
   * @param proxyUser 代理用户名（hadoop, hduser05, v_kkhuang）
   */
  async selectProxyUser(proxyUser: string) {
    await this.page.waitForTimeout(1000);
    await this.page.getByText(proxyUser, { exact: true }).click();
    await this.page.getByRole('button', { name: 'Confirm' }).click();
  }

  /**
   * 3. 进入Linkis主页（点击Console按钮）
   */
  async enterLinkisConsole() {
    await this.page.waitForTimeout(1000);
    console.log('点击 Console 按钮进入Linkis');
    await this.page.getByText('Console', { exact: true }).click();
    await this.page.waitForLoadState('domcontentloaded');
    await this.page.waitForTimeout(3000);
    console.log(`当前URL: ${this.page.url()}`);
  }

  /**
   * 完整流程：登录 -> 选择代理用户 -> 进入Linkis主页
   */
  async loginToLinkis(proxyUser: string) {
    await this.login();
    await this.selectProxyUser(proxyUser);
    await this.enterLinkisConsole();
  }
}

/**
 * 创建DSS登录辅助实例
 */
export function createDSSLoginHelper(page: Page): DSSLoginHelper {
  return new DSSLoginHelper(page);
}
