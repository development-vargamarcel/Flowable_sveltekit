import { test, expect } from '@playwright/test';
import { setupMockApi } from './helpers/mock-api';

test('login and view dashboard', async ({ page }) => {
  // Setup mock API to simulate backend responses
  await setupMockApi(page);

  // Go to login page
  await page.goto('/login');

  // Wait for the page to be fully loaded (auth check completes)
  await page.waitForLoadState('networkidle');

  // Use quick login for John C. (Engineer)
  // The selector targets the button with the title "John C. (eng.john)"
  await page.click('button[title*="eng.john"]');

  // Submit the form
  await page.click('button[type="submit"]');

  // Wait for navigation to home page
  await page.waitForURL('/');

  // Navigate to Dashboard
  await page.click('text=Workflow Dashboard');
  await page.waitForURL('/dashboard');

  // Check if dashboard content is visible
  // Use a timeout to allow for async data loading
  await expect(page.getByRole('heading', { name: 'Workflow Dashboard' })).toBeVisible({
    timeout: 10000
  });

  // Explicitly check for error messages that indicate failure
  // "Backend unavailable" or "Failed to load dashboard"
  const backendError = page.locator('text=Backend unavailable');
  const loadError = page.locator('text=Failed to load dashboard');

  await expect(backendError).not.toBeVisible();
  await expect(loadError).not.toBeVisible();

  // Verify stats are loaded (checking for a specific element that appears when data is loaded)
  await expect(page.locator('text=Active Processes by Type')).toBeVisible();
});
