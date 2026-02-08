/**
 * Centralized Logging Utility
 * Provides consistent logging with different levels and formatting
 * Logs are automatically disabled in production unless explicitly enabled
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogConfig {
  enabled: boolean;
  minLevel: LogLevel;
  showTimestamp: boolean;
  showLevel: boolean;
  prefix: string;
}

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

const LOG_COLORS: Record<LogLevel, string> = {
  debug: '#9CA3AF', // gray
  info: '#3B82F6',  // blue
  warn: '#F59E0B',  // amber
  error: '#EF4444', // red
};

const LOG_ICONS: Record<LogLevel, string> = {
  debug: '🔍',
  info: 'ℹ️',
  warn: '⚠️',
  error: '❌',
};

// Default configuration
const defaultConfig: LogConfig = {
  enabled: import.meta.env.DEV || localStorage.getItem('debug') === 'true',
  minLevel: 'debug',
  showTimestamp: true,
  showLevel: true,
  prefix: '[LoanPlatform]',
};

let config: LogConfig = { ...defaultConfig };

/**
 * Configure the logger
 */
export function configureLogger(options: Partial<LogConfig>): void {
  config = { ...config, ...options };
  logger.info('Logger configured', { config });
}

/**
 * Enable or disable logging
 */
export function enableLogging(enable: boolean): void {
  config.enabled = enable;
  localStorage.setItem('debug', String(enable));
}

/**
 * Format the log message with timestamp and level
 */
function formatMessage(level: LogLevel, message: string): string {
  const parts: string[] = [];

  if (config.showTimestamp) {
    parts.push(`[${new Date().toISOString()}]`);
  }

  if (config.prefix) {
    parts.push(config.prefix);
  }

  if (config.showLevel) {
    parts.push(`[${level.toUpperCase()}]`);
  }

  parts.push(message);

  return parts.join(' ');
}

/**
 * Check if the log level should be output
 */
function shouldLog(level: LogLevel): boolean {
  if (!config.enabled) return false;
  return LOG_LEVELS[level] >= LOG_LEVELS[config.minLevel];
}

/**
 * Core log function
 */
function log(level: LogLevel, message: string, data?: unknown): void {
  if (!shouldLog(level)) return;

  const formattedMessage = formatMessage(level, message);
  const icon = LOG_ICONS[level];
  const color = LOG_COLORS[level];

  const consoleMethod = level === 'error' ? 'error' : level === 'warn' ? 'warn' : 'log';

  if (data !== undefined) {
    console[consoleMethod](
      `%c${icon} ${formattedMessage}`,
      `color: ${color}; font-weight: bold;`,
      data
    );
  } else {
    console[consoleMethod](
      `%c${icon} ${formattedMessage}`,
      `color: ${color}; font-weight: bold;`
    );
  }
}

/**
 * Create a scoped logger for a specific module
 */
export function createLogger(scope: string) {
  return {
    debug: (message: string, data?: unknown) => log('debug', `[${scope}] ${message}`, data),
    info: (message: string, data?: unknown) => log('info', `[${scope}] ${message}`, data),
    warn: (message: string, data?: unknown) => log('warn', `[${scope}] ${message}`, data),
    error: (message: string, data?: unknown) => log('error', `[${scope}] ${message}`, data),
  };
}

/**
 * Main logger instance
 */
export const logger = {
  debug: (message: string, data?: unknown) => log('debug', message, data),
  info: (message: string, data?: unknown) => log('info', message, data),
  warn: (message: string, data?: unknown) => log('warn', message, data),
  error: (message: string, data?: unknown) => log('error', message, data),

  // Utility methods
  group: (label: string) => {
    if (config.enabled) console.group(`${config.prefix} ${label}`);
  },
  groupEnd: () => {
    if (config.enabled) console.groupEnd();
  },
  table: (data: unknown) => {
    if (config.enabled) console.table(data);
  },
  time: (label: string) => {
    if (config.enabled) console.time(`${config.prefix} ${label}`);
  },
  timeEnd: (label: string) => {
    if (config.enabled) console.timeEnd(`${config.prefix} ${label}`);
  },

  // Create scoped logger
  scope: createLogger,
};

// Log initialization
logger.info('Logger initialized', {
  environment: import.meta.env.MODE,
  enabled: config.enabled
});

export default logger;
