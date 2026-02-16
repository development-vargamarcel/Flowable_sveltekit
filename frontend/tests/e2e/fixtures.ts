import { test as base, expect } from '@playwright/test';
import { captureScreenshot } from './support/attachments';
import {
  attachBrowserLogging,
  createTestLogger,
  resolveTitlePath,
  shouldCaptureLogs,
  type TestLogger
} from './support/test-logger';

type Fixtures = {
  log: TestLogger;
};

export const test = base.extend<Fixtures>({
  log: async (_unused, use, testInfo) => {
    const logger = createTestLogger(testInfo);
    if (shouldCaptureLogs()) {
      const metadata = {
        titlePath: resolveTitlePath(testInfo),
        file: testInfo.file,
        project: testInfo.project.name,
        baseURL: testInfo.project.use?.baseURL ?? null,
        startTime: new Date().toISOString()
      };
      await testInfo.attach('metadata.json', {
        body: Buffer.from(JSON.stringify(metadata, null, 2)),
        contentType: 'application/json'
      });
    }
    await use(logger);
    await logger.attach();
  },
  page: async ({ page }, use, testInfo) => {
    const cleanup = attachBrowserLogging(page, testInfo);
    await use(page);
    if (testInfo.status !== testInfo.expectedStatus) {
      try {
        await captureScreenshot(page, testInfo, 'failure.png');
      } catch (error) {
        testInfo.annotations.push({
          type: 'warning',
          description: `Failed to capture screenshot: ${String(error)}`
        });
      }
    }
    await cleanup();
  }
});

export { expect };
