import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
	test('verifies all menu items point to the right pages', async ({ page }) => {
		await page.goto('/login');
		await page.fill('#username', 'user1');
		await page.fill('#password', 'password');
		await page.click('button[type="submit"]');
		await page.waitForURL('/');

		// From `frontend/src/lib/nav-schema.ts`
		const navigationSchema = [
			{
				title: 'Dashboard',
				href: '/dashboard'
			},
			{
				title: 'Tasks',
				href: '/tasks'
			},
			{
				title: 'Processes',
				href: '/processes'
			},
			{
				title: 'Document Types',
				href: '/document-types'
			},
			{
				title: 'Database',
				href: '/database'
			}
		];

		for (const item of navigationSchema) {
			await page.getByRole('link', { name: item.title }).click();
			await expect(page).toHaveURL(item.href);
		}
	});
});
