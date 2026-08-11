import { test, expect } from '@playwright/test';

test.describe('Microservice Next.js Portal Happy Path E2E Flow', () => {
  test('should redirect to SSO login page when unauthenticated', async ({ page }) => {
    // 1. Access secure dashboard page
    await page.goto('/');
    
    // 2. Middleware should redirect to login page
    await page.waitForURL('**/login**');
    await expect(page.locator('button:has-text("Đăng nhập với SSO Server")')).toBeVisible();

    // 3. Trigger SSO signin redirect flow
    const ssoRedirectPromise = page.waitForNavigation({ url: /.*localhost:9000.*/ });
    await page.click('button:has-text("Đăng nhập với SSO Server")');
    
    const url = page.url();
    expect(url).toContain('localhost:9000');
  });

  test('should log in, browse products, place orders, setup 2FA, and sign out', async ({ page }) => {
    // 1. Start sign in flow
    await page.goto('/login');
    await page.click('button:has-text("Đăng nhập với SSO Server")');

    // 2. Perform credential submission on SSO Server login page
    await page.locator('input[name="username"]').fill('admin');
    await page.locator('input[name="password"]').fill('admin123');
    await page.click('button[type="submit"]');

    // 3. Callback exchange should redirect back to dashboard index
    await page.waitForURL('http://localhost:3001/');
    await expect(page.locator('h1')).toContainText('Tổng quan Hệ thống');
    await expect(page.locator('text=admin@sso.com')).toBeVisible();

    // 4. Navigate to products listing page
    await page.click('text=Sản phẩm');
    await page.waitForURL('**/products');
    await expect(page.locator('h1')).toContainText('Sản phẩm hệ thống');

    // 5. Open new product creation form
    await page.click('text=Thêm sản phẩm');
    await page.waitForURL('**/products/new');
    await page.locator('input[name="name"]').fill('Sony PlayStation 5 Slim');
    await page.locator('input[name="price"]').fill('12500000');
    await page.locator('input[name="stock"]').fill('15');
    await page.locator('button:has-text("Thêm sản phẩm")').click();

    // 6. Verify product listing updates
    await page.waitForURL('**/products');
    await expect(page.locator('text=Sony PlayStation 5 Slim')).toBeVisible();

    // 7. Go to Order checkout page
    await page.click('text=Đơn hàng');
    await page.waitForURL('**/orders');
    await page.click('text=Tạo đơn hàng mới');
    await page.waitForURL('**/orders/new');
    await page.locator('input[name="shippingAddress"]').fill('789 CMT8, Q. Tân Bình, TP. HCM');
    await page.locator('input[name="quantity"]').fill('2');
    await page.locator('button:has-text("Xác nhận Đặt hàng")').click();

    // 8. Verify order listing updates
    await page.waitForURL('**/orders');
    await expect(page.locator('text=789 CMT8, Q. Tân Bình, TP. HCM')).toBeVisible();

    // 9. Navigate to Profile page and verify Setup 2FA configuration flow
    await page.click('text=Hồ sơ của tôi');
    await page.waitForURL('**/profile');
    await page.click('button:has-text("Cấu hình 2FA")');
    await expect(page.locator('h3:has-text("Thiết lập bảo mật 2 lớp")')).toBeVisible();
    await page.locator('input[id="otp"]').fill('123456');
    await page.click('button:has-text("Kích hoạt 2FA")');
    await expect(page.locator('text=Đã bật')).toBeVisible();

    // 10. Perform logout sign out
    await page.click('button:has-text("Đăng xuất")');
    await page.waitForURL('**/login');
    await expect(page.locator('button:has-text("Đăng nhập với SSO Server")')).toBeVisible();
  });
});
