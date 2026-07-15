import type { ConfigContext, ExpoConfig } from 'expo/config';

export default ({ config }: ConfigContext): ExpoConfig => ({
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
    relayBaseUrl: process.env.EXPO_PUBLIC_RELAY_BASE_URL ?? '',
    demoToken: process.env.EXPO_PUBLIC_DEMO_TOKEN ?? '',
  },
});
