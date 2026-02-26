import { test, expect } from '@playwright/test';

test('Verify Process Designer UI Changes', async ({ page }) => {
  // 1. Arrange: Go to the process designer page
  await page.goto('http://localhost:3000/processes/designer');

  // 2. Assert: Verify the "Sync" indicator is present
  const syncButton = page.getByTitle(/Synced with definition/);
  await expect(syncButton).toBeVisible();

  // 3. Act: Open Field Library
  await page.getByText('Field Library').click();

  // 4. Assert: Verify "Load Demo Layout" is visible
  await expect(page.getByText('Load Demo Layout')).toBeVisible();

  // 5. Act: Click Add Field and verify new field types
  await page.getByText('+ Add Field').click();

  // Wait for modal
  await expect(page.getByRole('dialog')).toBeVisible();

  // Check types dropdown contains new types
  const typeSelect = page.locator('select#f_type');
  await typeSelect.selectOption('signature');
  await typeSelect.selectOption('userPicker');
  await typeSelect.selectOption('image');

  // Close modal
  await page.getByText('Cancel').click();

  // 6. Act: Open Expression Tester
  await page.getByTitle('Test Expressions').click();
  await expect(page.getByText('Expression Tester')).toBeVisible();
  await expect(page.getByText('Test Context (JSON)')).toBeVisible();

  // Close tester
  await page.getByTitle('Expression Tester').getByRole('button').first().click(); // Close button usually in header

  // 7. Screenshot
  await page.screenshot({
    path: 'frontend/src/tests/verification/process_designer_verification.png',
    fullPage: true
  });
});
