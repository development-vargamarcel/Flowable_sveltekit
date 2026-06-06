import type { Page } from '@playwright/test';

export type NavigationEntry = {
  title: string;
  href: string;
};

export const getNavigationEntries = (): NavigationEntry[] => [
  { title: 'Dashboard', href: '/dashboard' },
  { title: 'Tasks', href: '/tasks' },
  { title: 'Processes', href: '/processes' },
  { title: 'My Processes', href: '/process-instances' },
  { title: 'Document Types', href: '/document-definitions/types' },
  { title: 'Database', href: '/database' }
];

export const navigateByMenu = async (page: Page, title: string, href: string) => {
  const navLink = page.locator('nav').getByRole('link', { name: title, exact: true });
  if (await navLink.isVisible()) {
    await navLink.click();
  } else {
    await page.getByRole('button', { name: 'Open main menu' }).click();
    await page.getByRole('dialog').getByRole('link', { name: title, exact: true }).click();
  }
  await page.waitForURL(href);
};
