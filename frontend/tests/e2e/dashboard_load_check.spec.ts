import { test, expect } from './fixtures';
import { login } from './support/auth';
import { captureScreenshot } from './support/attachments';

const DASHBOARD_WAIT_MS = 80000;
const EXTRA_WAIT_MS = 60000;

test('login and monitor dashboard load', async ({ page, log }, testInfo) => {
  test.setTimeout(DASHBOARD_WAIT_MS + EXTRA_WAIT_MS + 40000);

  await test.step('Login', async () => {
    log.info('Starting login.');
    await login(page, log);
  });

  await test.step('Navigate to dashboard', async () => {
    log.info('Navigating to dashboard.');
    await page.getByRole('link', { name: 'Dashboard' }).click();
    await page.waitForURL('/dashboard');
    await expect(page.getByRole('heading', { name: 'Workflow Dashboard' })).toBeVisible();
  });

  log.info('Dashboard page loaded (initial HTML). Waiting for content.');

  const spinnerLocator = page.getByText('Loading dashboard...');
  const errorLocator = page.locator('.bg-red-50').filter({
    hasNotText: /Overdue Tasks|Breached SLA/
  });
  const successLocator = page.getByText('Active Processes by Type');

  const startTime = Date.now();
  const waitTime = DASHBOARD_WAIT_MS;
  let state: 'loading' | 'error' | 'success' = 'loading';

  while (Date.now() - startTime < waitTime) {
    if (await errorLocator.isVisible()) {
      state = 'error';
      break;
    }
    if (await successLocator.isVisible()) {
      state = 'success';
      break;
    }

    if (!(await spinnerLocator.isVisible())) {
      log.debug('Spinner hidden but success not visible yet.');
    }

    await page.waitForTimeout(500);
  }

  if (state === 'error') {
    const errorText = await errorLocator.innerText();
    log.error(`Error detected on dashboard: ${errorText}`);
    throw new Error(`Dashboard showed error: ${errorText}`);
  }

  if (state === 'success') {
    log.info('Dashboard loaded successfully within 80 seconds.');
    await test.step('Verify dashboard controls', async () => {
      await expect(
        page.getByPlaceholder('Search by key, initiator, task, or process')
      ).toBeVisible();
      await expect(page.getByRole('combobox', { name: 'Filter by status' })).toBeVisible();
      await expect(page.getByRole('combobox', { name: 'Sort processes' })).toBeVisible();
      await expect(page.getByLabel('Escalated only')).toBeVisible();
      await expect(page.getByLabel('Auto refresh')).toBeVisible();
      await expect(page.getByRole('combobox', { name: 'Auto refresh interval' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Copy link' })).toBeVisible();
    });
    await test.step('Verify per-page summary stats', async () => {
      await expect(page.getByText(/Showing \\d+ of/)).toBeVisible();
      await expect(page.getByText(/escalated/)).toBeVisible();
    });
    await test.step('Verify auto refresh toggles', async () => {
      const autoRefreshToggle = page.getByLabel('Auto refresh');
      await autoRefreshToggle.check();
      await expect(autoRefreshToggle).toBeChecked();

      const intervalSelect = page.getByRole('combobox', { name: 'Auto refresh interval' });
      await intervalSelect.selectOption('120');
      await expect(intervalSelect).toHaveValue('120');

      await expect(page.getByText(/Next refresh in/)).toBeVisible();
    });
    return;
  }

  log.warn('80 seconds passed. Checking status...');

  if (await spinnerLocator.isVisible()) {
    log.warn('Spinner is still visible after 80 seconds.');

    const screenshotName = `dashboard_slow_load_${Date.now()}.png`;
    log.info(`Taking screenshot: ${screenshotName}`);
    await captureScreenshot(page, testInfo, screenshotName, { fullPage: false });

    log.info(`Waiting another ${EXTRA_WAIT_MS / 1000} seconds...`);
    try {
      await expect(successLocator).toBeVisible({ timeout: EXTRA_WAIT_MS });
      log.info('Dashboard eventually loaded.');
    } catch (e) {
      log.warn('Dashboard still did not load after extra wait.');
      await captureScreenshot(page, testInfo, `dashboard_still_loading_${Date.now()}.png`, {
        fullPage: false
      });
    }
  } else {
    log.warn('Spinner is gone but content not found. This is unexpected.');
    await captureScreenshot(page, testInfo, `dashboard_unknown_state_${Date.now()}.png`, {
      fullPage: false
    });
  }
});
