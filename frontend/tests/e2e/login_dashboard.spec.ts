import { test, expect } from './fixtures';
import { login } from './support/auth';
import { navigateByMenu } from './support/navigation';

test('login and view dashboard', async ({ page, log }) => {
  await test.step('Login', async () => {
    await login(page, log);
  });

  await test.step('Navigate to dashboard', async () => {
    await navigateByMenu(page, 'Dashboard', '/dashboard');
  });

  await test.step('Verify dashboard content', async () => {
    await expect(page.getByRole('heading', { name: 'Workflow Dashboard' })).toBeVisible({
      timeout: 10000
    });

    const backendError = page.getByText('Backend unavailable');
    const loadError = page.getByText('Failed to load dashboard');

    await expect(backendError).not.toBeVisible();
    await expect(loadError).not.toBeVisible();

    await expect(page.getByText('Active Processes by Type')).toBeVisible();
  });
});
