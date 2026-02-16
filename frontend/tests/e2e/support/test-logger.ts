import fs from 'fs';
import path from 'path';
import util from 'util';
import type { ConsoleMessage, Page, Request, Response, TestInfo } from '@playwright/test';

type LogWriter = {
  write: (message: string) => void;
  close: () => Promise<void>;
};

export type TestLogger = {
  info: (...args: unknown[]) => void;
  warn: (...args: unknown[]) => void;
  error: (...args: unknown[]) => void;
  debug: (...args: unknown[]) => void;
  attach: () => Promise<void>;
};

const formatMessage = (level: string, args: unknown[]) =>
  `[${new Date().toISOString()}] [${level}] ${util.format(...args)}`;

const createWriter = (filePath: string): LogWriter => {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const stream = fs.createWriteStream(filePath, { flags: 'a' });
  return {
    write: (message: string) => {
      stream.write(`${message}\n`);
    },
    close: () =>
      new Promise((resolve) => {
        stream.end(resolve);
      })
  };
};

export const shouldCaptureLogs = () => process.env.E2E_LOGGING !== '0';
const getRequestLoggingMode = () => process.env.E2E_REQUEST_LOGGING ?? '0';

const resolveBaseOrigin = (testInfo: TestInfo) => {
  const baseURL = testInfo.project.use?.baseURL;
  if (!baseURL) {
    return null;
  }
  try {
    return new URL(baseURL).origin;
  } catch {
    return null;
  }
};

export const resolveTitlePath = (testInfo: TestInfo): string[] => {
  const maybeTitlePath = (testInfo as unknown as { titlePath?: string[] | (() => string[]) })
    .titlePath;
  if (typeof maybeTitlePath === 'function') {
    return maybeTitlePath();
  }
  return Array.isArray(maybeTitlePath) ? maybeTitlePath : [];
};

export const createTestLogger = (testInfo: TestInfo): TestLogger => {
  const terminalLogPath = testInfo.outputPath('logs', 'terminal.log');
  const terminalWriter = createWriter(terminalLogPath);
  const enabled = shouldCaptureLogs();

  const write = (level: string, args: unknown[]) => {
    if (!enabled) {
      return;
    }
    terminalWriter.write(formatMessage(level, args));
  };

  if (enabled) {
    terminalWriter.write(
      formatMessage('info', [
        `Test started: ${resolveTitlePath(testInfo).join(' > ')} (${testInfo.file})`
      ])
    );
  }

  return {
    info: (...args: unknown[]) => write('info', args),
    warn: (...args: unknown[]) => write('warn', args),
    error: (...args: unknown[]) => write('error', args),
    debug: (...args: unknown[]) => write('debug', args),
    attach: async () => {
      await terminalWriter.close();
      if (enabled) {
        await testInfo.attach('terminal.log', {
          path: terminalLogPath,
          contentType: 'text/plain'
        });
      }
    }
  };
};

export const attachBrowserLogging = (page: Page, testInfo: TestInfo) => {
  const browserLogPath = testInfo.outputPath('logs', 'browser.log');
  const browserWriter = createWriter(browserLogPath);
  const enabled = shouldCaptureLogs();
  const baseOrigin = resolveBaseOrigin(testInfo);
  const requestLoggingMode = getRequestLoggingMode();
  const errorEntries: string[] = [];
  if (enabled) {
    browserWriter.write(
      formatMessage('info', [
        `Browser logging enabled for: ${resolveTitlePath(testInfo).join(' > ')}`
      ])
    );
  }

  const onConsole = (msg: ConsoleMessage) => {
    const location = msg.location();
    const locationLabel = location.url
      ? ` ${location.url}:${location.lineNumber}:${location.columnNumber}`
      : '';
    if (!enabled) {
      return;
    }
    const entry = formatMessage(msg.type(), [`${msg.text()}${locationLabel}`]);
    browserWriter.write(entry);
    if (msg.type() === 'error') {
      errorEntries.push(entry);
    }
  };

  const onPageError = (error: Error) => {
    if (!enabled) {
      return;
    }
    const entry = formatMessage('pageerror', [error.stack ?? error.message]);
    browserWriter.write(entry);
    errorEntries.push(entry);
  };

  const shouldLogRequest = (url: string) => {
    if (!enabled) {
      return false;
    }
    if (requestLoggingMode === '0') {
      return false;
    }
    if (requestLoggingMode === 'all') {
      return true;
    }
    if (!baseOrigin) {
      return false;
    }
    try {
      return new URL(url).origin === baseOrigin;
    } catch {
      return false;
    }
  };

  const onRequestFailed = (request: Request) => {
    const failure = request.failure();
    if (!enabled) {
      return;
    }
    const entry = formatMessage('requestfailed', [
      `${request.method()} ${request.url()} ${failure?.errorText ?? 'unknown error'}`
    ]);
    browserWriter.write(entry);
    errorEntries.push(entry);
  };

  const onRequest = (request: Request) => {
    if (!shouldLogRequest(request.url())) {
      return;
    }
    browserWriter.write(formatMessage('request', [`${request.method()} ${request.url()}`]));
  };

  const onResponse = (response: Response) => {
    if (!shouldLogRequest(response.url())) {
      return;
    }
    if (response.status() >= 400) {
      const entry = formatMessage('response', [
        `${response.status()} ${response.request().method()} ${response.url()}`
      ]);
      browserWriter.write(entry);
      errorEntries.push(entry);
    }
  };

  page.on('console', onConsole);
  page.on('pageerror', onPageError);
  page.on('request', onRequest);
  page.on('requestfailed', onRequestFailed);
  page.on('response', onResponse);

  return async () => {
    page.off('console', onConsole);
    page.off('pageerror', onPageError);
    page.off('request', onRequest);
    page.off('requestfailed', onRequestFailed);
    page.off('response', onResponse);
    await browserWriter.close();
    if (enabled) {
      await testInfo.attach('browser.log', {
        path: browserLogPath,
        contentType: 'text/plain'
      });
    }
    if (testInfo.status !== testInfo.expectedStatus && errorEntries.length > 0) {
      await testInfo.attach('browser-errors.txt', {
        body: Buffer.from(errorEntries.join('\n')),
        contentType: 'text/plain'
      });
    }
  };
};
