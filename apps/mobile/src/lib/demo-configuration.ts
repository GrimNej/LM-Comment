import LMCommentAndroid, {
  type DemoConfiguration,
  type Tone,
} from '@lm-comment/android';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

export type DemoBootstrapState =
  | 'configured'
  | 'already-configured'
  | 'not-bundled'
  | 'unavailable';

const TONES = new Set<Tone>(['natural', 'professional', 'friendly', 'witty', 'concise']);
let pendingBootstrap: Promise<DemoBootstrapState> | null = null;

export function getBundledDemoConfiguration(): DemoConfiguration | null {
  const value = Constants.expoConfig?.extra?.lmCommentDemo;
  if (!value || typeof value !== 'object') return null;

  const candidate = value as Record<string, unknown>;
  const relayBaseUrl = typeof candidate.relayBaseUrl === 'string'
    ? candidate.relayBaseUrl.trim()
    : '';
  const demoToken = typeof candidate.demoToken === 'string' ? candidate.demoToken.trim() : '';
  const defaultTone = candidate.defaultTone;
  const optionCount = candidate.optionCount;
  const demoMode = candidate.demoMode;
  if (
    !relayBaseUrl
    || demoToken.length < 12
    || typeof defaultTone !== 'string'
    || !TONES.has(defaultTone as Tone)
    || !Number.isInteger(optionCount)
    || (optionCount as number) < 1
    || (optionCount as number) > 3
    || typeof demoMode !== 'boolean'
  ) {
    return null;
  }

  return {
    relayBaseUrl,
    demoToken,
    defaultTone: defaultTone as Tone,
    optionCount: optionCount as 1 | 2 | 3,
    demoMode,
  };
}

async function runBootstrap(): Promise<DemoBootstrapState> {
  if (Platform.OS !== 'android') return 'unavailable';
  const configuration = getBundledDemoConfiguration();
  if (!configuration) return 'not-bundled';

  const status = await LMCommentAndroid.getDemoConfigurationStatus();
  if (status.relayHostname && status.isDemoTokenConfigured) return 'already-configured';

  await LMCommentAndroid.configureDemo(configuration);
  return 'configured';
}

/** Idempotently persists judge defaults without overwriting hidden settings. */
export function bootstrapDemoConfiguration(): Promise<DemoBootstrapState> {
  pendingBootstrap ??= runBootstrap();
  return pendingBootstrap;
}
