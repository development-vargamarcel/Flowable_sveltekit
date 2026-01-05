import { test, expect } from '@playwright/test';
import { setupApiForTest } from './helpers/mock-api';

test('login and monitor dashboard load', async ({ page }) => {
  // Set a reasonable test timeout (longer for real backend)
  test.setTimeout(120000); // 2 minutes

  // Setup API - automatically detects if backend is available
  // Uses mock API if backend is not running
  const { useMock } = await setupApiForTest(page);
  console.log(`Running test with ${useMock ? 'mock' : 'real'} API`);

  console.log('Starting login...');
  // Go to login page
  await page.goto('/login');

  // Wait for the page to be fully loaded (auth check completes)
  await page.waitForLoadState('networkidle');

  // Use quick login for John C. (Engineer)
  await page.click('button[title*="eng.john"]');

  // Submit the form
  await page.click('button[type="submit"]');

  // Wait for navigation to home page
  await page.waitForURL('/');
  console.log('Logged in successfully.');

  // Navigate to Dashboard
  console.log('Navigating to dashboard...');
  await page.click('text=Workflow Dashboard');
  await page.waitForURL('/dashboard');

  console.log('Dashboard page loaded (initial HTML). Waiting for content...');

  // Success indicator - "Active Processes by Type" text
  const successLocator = page.locator('text=Active Processes by Type');

  // Wait for the dashboard content to load
  // Use longer timeout for real backend (may need to fetch data from DB)
  const timeout = useMock ? 30000 : 60000;
  await expect(successLocator).toBeVisible({ timeout });
  console.log('Dashboard loaded successfully.');

  // Verify no critical error messages are shown (checking for specific error patterns)
  const errorHeading = page.locator('text=Error Loading Dashboard');
  const backendError = page.locator('text=Backend unavailable');
  await expect(errorHeading).not.toBeVisible();
  await expect(backendError).not.toBeVisible();
});
