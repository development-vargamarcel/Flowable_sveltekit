import { test, expect } from '@playwright/test';

test('should fetch tasks from the backend after logging in', async ({ page }) => {
  await page.goto('/login');
  await page.fill('#username', 'user1');
  await page.fill('#password', 'password');
  await page.click('button[type="submit"]');
  await page.waitForURL('/');
  const response = await page.waitForResponse('**/api/tasks');
  expect(response.status()).toBe(200);
});
