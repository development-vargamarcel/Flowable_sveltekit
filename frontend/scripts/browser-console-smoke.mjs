import { chromium } from 'playwright';
import { spawn } from 'node:child_process';

const port = Number(process.env.BPM_FRONTEND_SMOKE_PORT || 4173);
const baseUrl = `http://127.0.0.1:${port}`;
const strictMode = process.env.BPM_FRONTEND_BROWSER_SMOKE_STRICT === '1';

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForServer(url, timeoutMs = 30000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {
      // server not ready yet
    }
    await wait(500);
  }
  throw new Error(`Preview server did not become ready at ${url}`);
}

function shouldTreatAsEnvironmentLimitation(error) {
  const message = String(error?.message ?? error);
  return (
    message.includes("Executable doesn't exist") ||
    message.includes('error while loading shared libraries') ||
    message.includes('Please run the following command to download new browsers')
  );
}

const preview = spawn(
  'npm',
  ['run', 'preview', '--', '--host', '127.0.0.1', '--port', String(port)],
  {
    stdio: 'inherit',
    cwd: process.cwd(),
    env: process.env
  }
);

let browser;
try {
  await waitForServer(baseUrl);

  try {
    browser = await chromium.launch({ headless: true });
  } catch (error) {
    if (shouldTreatAsEnvironmentLimitation(error) && !strictMode) {
      console.warn(
        `Skipping browser console smoke check due to environment limitation: ${String(error.message ?? error)}`
      );
      process.exit(0);
    }
    throw error;
  }

  const page = await browser.newPage();

  const consoleErrors = [];
  const pageErrors = [];

  page.on('console', (message) => {
    if (message.type() === 'error') {
      const text = message.text();
      // Ignore expected 401s from the unauthenticated startup requests
      if (
        text.includes('401 (Unauthorized)') ||
        text.includes('ApiError: Unauthorized') ||
        text.includes('"status":401')
      ) {
        return;
      }
      consoleErrors.push(text);
    }
  });

  page.on('pageerror', (error) => {
    pageErrors.push(error.message);
  });

  await page.goto(baseUrl, { waitUntil: 'networkidle' });

  if (consoleErrors.length || pageErrors.length) {
    console.error('Console smoke test failed.');
    if (consoleErrors.length) {
      console.error(`Console errors (${consoleErrors.length}):`);
      for (const error of consoleErrors) console.error(`- ${error}`);
    }
    if (pageErrors.length) {
      console.error(`Page errors (${pageErrors.length}):`);
      for (const error of pageErrors) console.error(`- ${error}`);
    }
    process.exitCode = 1;
  } else {
    console.log('Browser console smoke check passed.');
  }
} finally {
  if (browser) {
    await browser.close();
  }
  preview.kill('SIGTERM');
}
