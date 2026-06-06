import { test, expect } from './fixtures';
import { login } from './support/auth';
import { navigateByMenu } from './support/navigation';

test('should fetch tasks from the backend after logging in', async ({ page, log }) => {
  await test.step('Login', async () => {
    await login(page, log);
  });

  await test.step('Wait for tasks API', async () => {
    const tasksResponse = page.waitForResponse('**/api/tasks', { timeout: 15000 });
    await navigateByMenu(page, 'Tasks', '/tasks');
    const response = await tasksResponse;
    log.info('Tasks response status: %s', response.status());
    if (response.status() !== 200) {
      const bodyText = await response.text();
      log.error('Tasks response body: %s', bodyText);
    }
    expect(response.status()).toBe(200);
  });
});
