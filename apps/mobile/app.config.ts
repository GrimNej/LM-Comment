import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

import type { ConfigContext, ExpoConfig } from 'expo/config';

const MOBILE_ENV_ALLOWLIST = [
  'EXPO_PUBLIC_RELAY_BASE_URL',
  'EXPO_PUBLIC_DEMO_TOKEN',
] as const;

type MobileEnvironmentName = (typeof MOBILE_ENV_ALLOWLIST)[number];

function readAllowlistedRootEnvironment(): Partial<Record<MobileEnvironmentName, string>> {
  const rootEnvironmentPath = path.resolve(__dirname, '..', '..', '.env');
  if (!existsSync(rootEnvironmentPath)) return {};

  const allowed = new Set<string>(MOBILE_ENV_ALLOWLIST);
  const values: Partial<Record<MobileEnvironmentName, string>> = {};
  for (const line of readFileSync(rootEnvironmentPath, 'utf8').split(/\r?\n/u)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/u);
    if (!match || !allowed.has(match[1])) continue;
    const name = match[1] as MobileEnvironmentName;
    const rawValue = match[2].trim();
    const quoted = rawValue.match(/^(?:"([\s\S]*)"|'([\s\S]*)')$/u);
    values[name] = (quoted?.[1] ?? quoted?.[2] ?? rawValue).trim();
  }
  return values;
}

function assertJudgeDefaults(relayBaseUrl: string, demoToken: string): void {
  if (process.env.NODE_ENV !== 'production') return;
  let relayUrl: URL;
  try {
    relayUrl = new URL(relayBaseUrl);
  } catch {
    throw new Error('Release mobile relay configuration is missing or invalid.');
  }
  if (
    relayUrl.protocol !== 'https:'
    || !relayUrl.hostname
    || relayUrl.username
    || relayUrl.password
    || relayUrl.search
    || relayUrl.hash
    || demoToken.length < 12
    || demoToken.length > 512
    || /[\u0000-\u001f\u007f]/u.test(demoToken)
  ) {
    throw new Error('Release mobile relay configuration is missing or invalid.');
  }
}

export default ({ config }: ConfigContext): ExpoConfig => {
  // Expo evaluates this file from apps/mobile, while the hackathon's ignored
  // environment lives at the repository root. Read only the two explicitly
  // public demo values; the provider key is never loaded into mobile config.
  const rootEnvironment = readAllowlistedRootEnvironment();
  const relayBaseUrl = (
    process.env.EXPO_PUBLIC_RELAY_BASE_URL
      ?? rootEnvironment.EXPO_PUBLIC_RELAY_BASE_URL
      ?? ''
  ).trim();
  const demoToken = (
    process.env.EXPO_PUBLIC_DEMO_TOKEN
      ?? rootEnvironment.EXPO_PUBLIC_DEMO_TOKEN
      ?? ''
  ).trim();
  assertJudgeDefaults(relayBaseUrl, demoToken);

  return {
    ...config,
    name: 'LM-Comment',
    slug: 'lm-comment',
    scheme: 'lmcomment',
    version: '0.1.0',
    orientation: 'default',
    userInterfaceStyle: 'automatic',
    icon: './assets/images/icon.png',
    android: {
      package: 'com.grimnej.lmcomment',
      versionCode: 1,
      adaptiveIcon: {
        backgroundColor: '#090B10',
        foregroundImage: './assets/images/android-icon-foreground.png',
        monochromeImage: './assets/images/android-icon-monochrome.png',
      },
      predictiveBackGestureEnabled: true,
      permissions: ['SYSTEM_ALERT_WINDOW', 'POST_NOTIFICATIONS'],
      blockedPermissions: [
        'android.permission.READ_EXTERNAL_STORAGE',
        'android.permission.WRITE_EXTERNAL_STORAGE',
      ],
    },
    plugins: [
      'expo-router',
      '@lm-comment/android',
      [
        'expo-build-properties',
        {
          android: {
            compileSdkVersion: 36,
            targetSdkVersion: 36,
            minSdkVersion: 26,
          },
        },
      ],
      [
        'expo-splash-screen',
        {
          backgroundColor: '#090B10',
          image: './assets/images/splash-icon.png',
          imageWidth: 112,
          dark: {
            backgroundColor: '#090B10',
            image: './assets/images/splash-icon.png',
          },
        },
      ],
    ],
    experiments: {
      typedRoutes: true,
      reactCompiler: true,
    },
    extra: {
      lmCommentDemo: {
        relayBaseUrl,
        demoToken,
        defaultTone: 'natural',
        optionCount: 3,
        demoMode: true,
      },
    },
  };
};
