import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { useColorScheme } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { bootstrapDemoConfiguration } from '@/lib/demo-configuration';

export default function RootLayout() {
  const scheme = useColorScheme();
  useEffect(() => {
    // Configuration stays in native private preferences so capture/generation
    // remains available after the React Native activity leaves the foreground.
    void bootstrapDemoConfiguration().catch(() => undefined);
  }, []);

  return (
    <SafeAreaProvider>
      <StatusBar style={scheme === 'light' ? 'dark' : 'light'} />
      <Stack screenOptions={{ headerShown: false, animation: 'fade' }} />
    </SafeAreaProvider>
  );
}
