import * as Sentry from '@sentry/sveltekit';

// Initialize Sentry for client-side error tracking
// Set PUBLIC_SENTRY_DSN environment variable to enable Sentry
const sentryDsn = import.meta.env.PUBLIC_SENTRY_DSN;

if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,

    // Performance Monitoring
    tracesSampleRate: 1.0, // Capture 100% of transactions for performance monitoring

    // Session Replay
    replaysSessionSampleRate: 0.1, // 10% of sessions for general sampling
    replaysOnErrorSampleRate: 1.0, // 100% of sessions with errors

    // Environment configuration
    environment: import.meta.env.MODE,

    // Integrations
    integrations: [Sentry.replayIntegration()]
  });
}

// Export Sentry's handleError hook
export const handleError = Sentry.handleErrorWithSentry();
