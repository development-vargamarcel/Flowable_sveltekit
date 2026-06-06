import { test, expect } from './fixtures';
import { invalidCredentials, login, logout, waitForAppReady } from './support/auth';

test('shows an error message for invalid credentials', async ({ page, log }) => {
  await test.step('Attempt invalid login', async () => {
    await page.goto('/login');
    await waitForAppReady(page);
    await page.getByLabel('Username').fill(invalidCredentials.username);
    await page.getByLabel('Password').fill(invalidCredentials.password);
    log.info('Submitting invalid credentials.');
    await page.getByRole('button', { name: /sign in/i }).click();
  });

  await test.step('Verify error alert', async () => {
    const errorAlert = page.getByRole('alert');
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText(/login failed/i);
    await expect(errorAlert).toContainText(
      /invalid credentials|authentication service|server error|login failed/i
    );
    await expect(page).toHaveURL('/login');
  });
});

test('logout returns the user to the login page', async ({ page, log }) => {
  await test.step('Login', async () => {
    await login(page, log);
  });

  await test.step('Logout', async () => {
    await logout(page, log);
  });

  await test.step('Validate login screen', async () => {
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible();
  });
});
