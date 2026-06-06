import { test, expect } from './fixtures';
import { login } from './support/auth';
import { getNavigationEntries, navigateByMenu } from './support/navigation';

test.describe('Navigation', () => {
  test('verifies all menu items point to the right pages', async ({ page, log }) => {
    await test.step('Login', async () => {
      await login(page, log);
    });

    const navigationSchema = getNavigationEntries();
    await expect(page.locator('nav')).toBeVisible();

    for (const item of navigationSchema) {
      await test.step(`Navigate to ${item.title}`, async () => {
        await navigateByMenu(page, item.title, item.href);
        await expect.soft(page).toHaveURL(item.href);
      });
    }
  });

  test('mobile menu exposes account, theme, and logout actions', async ({ page, log }) => {
    test.skip(
      !test.info().project.name.includes('mobile'),
      'Mobile menu behavior is covered in the mobile project.'
    );

    await page.addInitScript(() => localStorage.setItem('theme', 'light'));
    await login(page, log);

    await page.getByRole('button', { name: 'Open main menu' }).click();
    const menu = page.getByRole('dialog');
    await expect(menu.getByRole('heading', { name: 'Main menu' })).toBeVisible();
    await expect(menu.getByRole('link', { name: 'Profile', exact: true })).toBeVisible();
    await expect(menu.getByRole('button', { name: 'Dark mode', exact: true })).toBeVisible();
    await expect(menu.getByRole('button', { name: 'Logout', exact: true })).toBeVisible();

    await menu.getByRole('button', { name: 'Dark mode', exact: true }).click();
    await expect(page.locator('html')).toHaveClass(/dark/);

    await menu.getByRole('link', { name: 'Profile', exact: true }).click();
    await expect(page).toHaveURL('/profile');
  });
});
