import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import {
  AppMark,
  AppReadinessProvider,
  AppThemeProvider,
  Screen,
  useAppReadiness,
  useAppTheme,
} from '@/ui';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AppThemeProvider>
        <AppReadinessProvider>
          <AppShell />
        </AppReadinessProvider>
      </AppThemeProvider>
    </SafeAreaProvider>
  );
}

function AppShell() {
  const { phase } = useAppReadiness();
  const { colors, isDark, isReduceMotionEnabled } = useAppTheme();

  return (
    <>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      {phase === 'booting' ? (
        <LaunchGate />
      ) : (
        <Stack
          screenOptions={{
            animation: isReduceMotionEnabled ? 'none' : 'fade',
            contentStyle: { backgroundColor: colors.canvas },
            headerShown: false,
          }}
        />
      )}
    </>
  );
}

function LaunchGate() {
  const { colors, typography } = useAppTheme();
  return (
    <Screen contentContainerStyle={styles.launchScreen} scroll={false}>
      <View style={styles.launchBrand}>
        <AppMark showWordmark size={58} />
        <Text style={[typography.body, styles.launchPromise, { color: colors.textSecondary }]}>
          Review the text before sending it for generation.
        </Text>
      </View>
      <View
        accessibilityLiveRegion="polite"
        accessibilityLabel="Loading app settings"
        style={styles.launchStatus}
      >
        <ActivityIndicator color={colors.secondary} size="small" />
        <Text style={[typography.label, { color: colors.textMuted }]}>Loading settings</Text>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  launchScreen: { justifyContent: 'space-between', paddingBottom: 34, paddingTop: 70 },
  launchBrand: { alignItems: 'flex-start', maxWidth: 360 },
  launchPromise: { marginTop: 24 },
  launchStatus: { alignItems: 'center', flexDirection: 'row', gap: 12 },
});
