/**
 * 测试登录和代理用户切换
 */

import { test, expect } from '@playwright/test';
import { DSSLoginHelper, PROXY_USERS } from './utils/dss-login';

test('测试登录流程', async ({ page }) => {
  const helper = new DSSLoginHelper(page);

  console.log('=== 测试登录 ===');
  await helper.login();

  console.log('当前URL:', page.url());
  await page.screenshot({ path: 'test-results/login-result.png', fullPage: true });
});

test('测试代理用户切换 - 管理员', async ({ page }) => {
  const helper = new DSSLoginHelper(page);

  console.log('=== 管理员测试 ===');
  await helper.loginToLinkis(PROXY_USERS.ADMIN);

  console.log('Linkis控制台URL:', page.url());
  await page.screenshot({ path: 'test-results/linkis-console-admin.png', fullPage: true });
});

test('测试代理用户切换 - 普通用户', async ({ page }) => {
  const helper = new DSSLoginHelper(page);

  console.log('=== 普通用户测试 ===');
  await helper.loginToLinkis(PROXY_USERS.NORMAL);

  console.log('Linkis控制台URL:', page.url());
  await page.screenshot({ path: 'test-results/linkis-console-normal.png', fullPage: true });
});
