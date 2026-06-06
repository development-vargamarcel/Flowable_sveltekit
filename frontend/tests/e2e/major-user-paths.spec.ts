import { test, expect } from './fixtures';
import { login } from './support/auth';
import { navigateByMenu } from './support/navigation';

test.describe('Major user paths', () => {
  test.beforeEach(async ({ page, log }) => {
    await page.addInitScript(() => {
      localStorage.removeItem('taskFilters:lastUsed');
      localStorage.removeItem('taskFilters:presets');
    });
    await login(page, log);
  });

  test('filters tasks, saves a preset, and clears the task view', async ({ page }) => {
    await navigateByMenu(page, 'Tasks', '/tasks');

    await page.getByLabel('Search tasks').fill('approval');
    await expect(page).toHaveURL(/\/tasks\?text=approval/);
    await expect(page.getByText('Active filters: 1')).toBeVisible();

    await page.getByLabel('Filter by priority').selectOption('75');
    await expect(page).toHaveURL(/priority=75/);
    await expect(page.getByText('Active filters: 2')).toBeVisible();

    await page.getByLabel('Preset name').fill('High approval work');
    await page.getByRole('button', { name: 'Save Preset' }).click();
    await expect(
      page.getByRole('button', { name: 'High approval work', exact: true })
    ).toBeVisible();

    await page.getByRole('button', { name: 'Clear all filters' }).click();
    await expect(page.getByText('Active filters:')).toHaveCount(0);
    await expect(page).toHaveURL(/\/tasks(\?sortBy=created_desc)?$/);
  });

  test('starts from the process catalog and opens a workflow form', async ({ page }) => {
    await navigateByMenu(page, 'Processes', '/processes');

    await expect(page.getByRole('heading', { name: 'Start a New Process' })).toBeVisible();
    await expect(page.getByRole('link', { name: /Expense Request/ }).first()).toBeVisible();

    await page
      .getByRole('link', { name: /Expense Request/ })
      .first()
      .click();
    await expect(page).toHaveURL('/processes/expense');
    await expect(page.getByRole('heading', { name: 'Submit Expense' })).toBeVisible();
    await expect(page.getByLabel('Amount ($) *')).toBeVisible();
    await expect(page.getByLabel('Category *')).toBeVisible();
    await expect(page.getByLabel('Description *')).toBeVisible();

    await page.getByRole('button', { name: 'Submit Expense' }).click();
    await expect(page.getByText('Please fix the errors below')).toBeVisible();
  });

  test('opens profile settings and notifications from primary navigation', async ({ page }) => {
    await page.goto('/profile');
    await expect(page.getByRole('heading', { name: 'Profile Settings' })).toBeVisible();
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Save Changes' })).toBeVisible();

    await page.goto('/notifications');
    await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible();
    await expect(
      page.getByText(/Loading notifications|No notifications|Error Loading Notifications/)
    ).toBeVisible();
  });
});
