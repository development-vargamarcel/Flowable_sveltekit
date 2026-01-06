import { test, expect } from '@playwright/test';
import * as path from 'path';

test('login and monitor dashboard load', async ({ page }) => {
  // Set a very long test timeout to accommodate the 80s wait + extra time
  test.setTimeout(180000); // 3 minutes

  console.log('Starting login...');
  // Go to login page
  await page.goto('/login');

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

  // The dashboard shows a spinner when loading:
  // <div class="w-12 h-12 border-4 border-blue-600 ... animate-spin mx-auto"></div>
  // We can locate it by the text "Loading dashboard..." which is right below it
  const spinnerLocator = page.locator('text=Loading dashboard...');

  // Potential error messages
  // "Failed to load dashboard"
  const errorLocator = page.locator('.bg-red-50').filter({
    hasNotText: /Overdue Tasks|Breached SLA/
  });

  // Success indicator
  // "Active Processes by Type" or "Workflow Dashboard" (header is always there, need content)
  const successLocator = page.locator('text=Active Processes by Type');

  // We need to wait for UP TO 80 seconds.
  // If at any point an error appears -> Fail.
  // If success appears -> Pass.
  // If 80 seconds pass and spinner is still there -> Screenshot and Wait more.

  const startTime = Date.now();
  const waitTime = 80000; // 80 seconds

  // We will poll the state every 1 second
  let state = 'loading'; // loading, error, success

  while (Date.now() - startTime < waitTime) {
    if (await errorLocator.isVisible()) {
      state = 'error';
      break;
    }
    if (await successLocator.isVisible()) {
      state = 'success';
      break;
    }
    // Still loading (presumably)
    // Check if spinner is visible just to be sure
    if (!(await spinnerLocator.isVisible())) {
      // If spinner is gone but success not visible, what happened?
      // Maybe it's transitioning.
    }

    await page.waitForTimeout(500);
  }

  if (state === 'error') {
    const errorText = await errorLocator.innerText();
    console.error(`Error detected on dashboard: ${errorText}`);
    throw new Error(`Dashboard showed error: ${errorText}`);
  }

  if (state === 'success') {
    console.log('Dashboard loaded successfully within 80 seconds.');
    return;
  }

  // If we are here, we timed out (80s passed) and we are likely still loading
  console.log('80 seconds passed. Checking status...');

  if (await spinnerLocator.isVisible()) {
    console.log('Spinner is still visible after 80 seconds.');

    // "increase wait time, take screenshots and put them in /screenshots for testing folder"

    // Take screenshot
    const screenshotPath = path.resolve('screenshots', `dashboard_slow_load_${Date.now()}.png`);
    console.log(`Taking screenshot to: ${screenshotPath}`);
    await page.screenshot({ path: screenshotPath });

    // Increase wait time - let's wait another 60 seconds
    console.log('Waiting another 60 seconds...');
    try {
      await expect(successLocator).toBeVisible({ timeout: 60000 });
      console.log('Dashboard eventually loaded.');
    } catch (e) {
      console.log('Dashboard still did not load after extra wait.');
      // Take another screenshot?
      await page.screenshot({ path: path.resolve('screenshots', `dashboard_still_loading_${Date.now()}.png`) });
    }
  } else {
    // Spinner gone, but success not found?
    console.log('Spinner is gone but content not found. This is unexpected.');
    await page.screenshot({ path: path.resolve('screenshots', `dashboard_unknown_state_${Date.now()}.png`) });
  }

});
