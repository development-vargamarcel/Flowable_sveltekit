import { test, expect } from './fixtures';
import { login } from './support/auth';
import { navigateByMenu } from './support/navigation';

test.describe('Page smoke checks', () => {
  test('tasks page renders core heading', async ({ page, log }) => {
    await login(page, log);
    await navigateByMenu(page, 'Tasks', '/tasks');
    await expect(page.getByRole('heading', { name: 'Tasks' })).toBeVisible();
  });

  test('processes page renders core heading', async ({ page, log }) => {
    await login(page, log);
    await navigateByMenu(page, 'Processes', '/processes');
    await expect(page.getByRole('heading', { name: 'Start a New Process' })).toBeVisible();
  });

  test('database page renders core heading', async ({ page, log }) => {
    await login(page, log);
    await navigateByMenu(page, 'Database', '/database');
    await expect(page.getByRole('heading', { name: 'Database Table Viewer' })).toBeVisible();
  });
});
