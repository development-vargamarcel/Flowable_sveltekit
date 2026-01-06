import { sentrySvelteKit } from '@sentry/sveltekit';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [
      sentrySvelteKit({
        // Source maps upload configuration
        // Set SENTRY_AUTH_TOKEN, SENTRY_ORG, and SENTRY_PROJECT environment variables
        sourceMapsUploadOptions: {
          org: process.env.SENTRY_ORG,
          project: process.env.SENTRY_PROJECT,
          authToken: process.env.SENTRY_AUTH_TOKEN
        }
      }),
      sveltekit()
    ],
    server: {
      port: 3000,
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: env.BACKEND_URL || 'http://localhost:8080',
          changeOrigin: true,
          secure: false
        }
      }
    },
    test: {
      include: ['src/**/*.{test,spec}.{js,ts}'],
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/tests/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        exclude: ['node_modules/', '.svelte-kit/', 'src/tests/']
      }
    }
  };
});
