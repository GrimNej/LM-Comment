import { buildApp } from './app.js';
import { assertRunnableConfig, loadConfig } from './config.js';

const config = loadConfig();
assertRunnableConfig(config);
const app = buildApp(config);

try {
  await app.listen({ host: config.HOST, port: config.PORT });
} catch (error) {
  app.log.error({ error }, 'Relay failed to start');
  process.exitCode = 1;
}

const shutdown = async (signal: string) => {
  app.log.info({ signal }, 'Relay shutting down');
  await app.close();
};

process.once('SIGINT', () => void shutdown('SIGINT'));
process.once('SIGTERM', () => void shutdown('SIGTERM'));
