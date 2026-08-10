import { test, expect } from '@playwright/test';

test.describe('SSO Platform Happy Path E2E Flow', () => {
  test('should navigate to login page and redirect to SSO Server authorize endpoint', async ({ page }) => {
    // 1. Navigate to frontend portal login page
    await page.goto('/login');
    await expect(page).toHaveTitle(/SSO Platform/i);
    await expect(page.locator('button:has-text("Đăng nhập với SSO")')).toBeVisible();

    // 2. Click login button which should trigger OAuth2 redirect
    const ssoRedirectPromise = page.waitForNavigation({ url: /.*localhost:9000.*/ });
    await page.click('button:has-text("Đăng nhập với SSO")');
    
    // Wait for redirection to SSO Server
    const url = page.url();
    expect(url).toContain('localhost:9000');
  });

  test('should perform login, callback exchange, navigation, product browsing, and logout', async ({ page }) => {
    // Navigate to frontend login
    await page.goto('/login');
    await page.click('button:has-text("Đăng nhập với SSO")');

    // Fill credentials on SSO Server page
    await page.locator('input[name="username"]').fill('admin');
    await page.locator('input[name="password"]').fill('admin123');
    await page.click('button[type="submit"]');

    // Wait to be redirected back to Callback page then Dashboard
    await page.waitForURL('http://localhost:3000/');
    await expect(page.locator('h1')).toContainText('Tổng quan hệ thống');
    await expect(page.locator('text=admin@sso.com')).toBeVisible();

    // Navigate to Products page
    await page.click('text=Sản phẩm');
    await page.waitForURL('**/products');
    await expect(page.locator('h1')).toContainText('Sản phẩm hệ thống');

    // Create a new product
    await page.click('text=Thêm sản phẩm');
    await page.waitForURL('**/products/new');
    await page.locator('input[name="name"]').fill('Sách Lập Trình Java nâng cao');
    await page.locator('input[name="price"]').fill('150000');
    await page.locator('input[name="stock"]').fill('50');
    await page.locator('input[name="category"]').fill('Sách');
    await page.locator('button:has-text("Thêm sản phẩm")').click();

    // Verify returning to product list and product is visible
    await page.waitForURL('**/products');
    await expect(page.locator('text=Sách Lập Trình Java nâng cao')).toBeVisible();

    // Log out
    await page.click('button:has-text("Đăng xuất")');
    await page.waitForURL('**/login');
    await expect(page.locator('button:has-text("Đăng nhập với SSO")')).toBeVisible();
  });
});
