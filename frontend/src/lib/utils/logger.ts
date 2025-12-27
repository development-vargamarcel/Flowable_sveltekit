/**
 * Production-ready logging utility
 * Provides structured logging with levels and context
 */

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogContext {
  [key: string]: unknown;
}

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: LogContext;
}

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3
};

// Determine minimum log level based on environment
const getMinLevel = (): LogLevel => {
  if (typeof window === 'undefined') {
    // Server-side: use environment variable or default to 'info'
    return (process.env.LOG_LEVEL as LogLevel) || 'info';
  }
  // Client-side: debug in dev, warn in production
  return import.meta.env.DEV ? 'debug' : 'warn';
};

const MIN_LEVEL = getMinLevel();

/**
 * Format log entry for output
 */
function formatLogEntry(entry: LogEntry): string {
  const contextStr = entry.context ? ` ${JSON.stringify(entry.context)}` : '';
  return `[${entry.timestamp}] [${entry.level.toUpperCase()}] ${entry.message}${contextStr}`;
}

/**
 * Check if a log level should be output
 */
function shouldLog(level: LogLevel): boolean {
  return LOG_LEVELS[level] >= LOG_LEVELS[MIN_LEVEL];
}

/**
 * Create a log entry
 */
function createLogEntry(level: LogLevel, message: string, context?: LogContext): LogEntry {
  return {
    timestamp: new Date().toISOString(),
    level,
    message,
    context
  };
}

/**
 * Output log to console
 */
function outputLog(entry: LogEntry): void {
  const formatted = formatLogEntry(entry);

  switch (entry.level) {
    case 'debug':
      // Use console.log for debug to avoid browser-specific behavior
      // eslint-disable-next-line no-console
      console.log(formatted);
      break;
    case 'info':
      // eslint-disable-next-line no-console
      console.info(formatted);
      break;
    case 'warn':
      console.warn(formatted);
      break;
    case 'error':
      console.error(formatted);
      break;
  }
}

/**
 * Logger instance with structured logging
 */
export const logger = {
  /**
   * Debug level logging - only in development
   */
  debug(message: string, context?: LogContext): void {
    if (!shouldLog('debug')) return;
    outputLog(createLogEntry('debug', message, context));
  },

  /**
   * Info level logging - general information
   */
  info(message: string, context?: LogContext): void {
    if (!shouldLog('info')) return;
    outputLog(createLogEntry('info', message, context));
  },

  /**
   * Warn level logging - potential issues
   */
  warn(message: string, context?: LogContext): void {
    if (!shouldLog('warn')) return;
    outputLog(createLogEntry('warn', message, context));
  },

  /**
   * Error level logging - errors and exceptions
   */
  error(message: string, error?: Error | unknown, context?: LogContext): void {
    if (!shouldLog('error')) return;

    const errorContext: LogContext = { ...context };

    if (error instanceof Error) {
      errorContext.errorName = error.name;
      errorContext.errorMessage = error.message;
      if (import.meta.env.DEV) {
        errorContext.stack = error.stack;
      }
    } else if (error !== undefined) {
      errorContext.error = String(error);
    }

    outputLog(createLogEntry('error', message, errorContext));
  },

  /**
   * Create a child logger with additional context
   */
  child(defaultContext: LogContext) {
    return {
      debug: (message: string, context?: LogContext) =>
        logger.debug(message, { ...defaultContext, ...context }),
      info: (message: string, context?: LogContext) =>
        logger.info(message, { ...defaultContext, ...context }),
      warn: (message: string, context?: LogContext) =>
        logger.warn(message, { ...defaultContext, ...context }),
      error: (message: string, error?: Error | unknown, context?: LogContext) =>
        logger.error(message, error, { ...defaultContext, ...context })
    };
  }
};

/**
 * Create a logger for a specific module/component
 */
export function createLogger(moduleName: string) {
  return logger.child({ module: moduleName });
}
