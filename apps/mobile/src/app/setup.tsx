import LMCommentAndroid, {
  type DemoConfigurationStatus,
  type NativeReadiness,
} from '@lm-comment/android';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppState, Platform, StyleSheet, Text, View, useColorScheme } from 'react-native';

import { bootstrapDemoConfiguration } from '@/lib/demo-configuration';
import {
  PrimaryButton,
  PrivacyBanner,
  ReadinessCard,
  Screen,
  SecondaryButton,
  SectionHeading,
  StatusHero,
  TopBar,
} from '@/ui';

type RelayState = 'checking' | 'ready' | 'unavailable';

async function checkRelay(): Promise<Exclude<RelayState, 'checking'>> {
  try {
    if (Platform.OS !== 'android') return 'unavailable';
    const diagnostics = await LMCommentAndroid.getSafeDiagnostics();
    return diagnostics.relayHealth === 'healthy' ? 'ready' : 'unavailable';
  } catch {
    return 'unavailable';
  }
}

export default function SetupScreen() {
  const router = useRouter();
  const scheme = useColorScheme();
  const styles = useMemo(() => createStyles(scheme === 'light'), [scheme]);
  const refreshId = useRef(0);
  const [nativeReadiness, setNativeReadiness] = useState<NativeReadiness | null>(null);
  const [demoStatus, setDemoStatus] = useState<DemoConfigurationStatus | null>(null);
  const [relayState, setRelayState] = useState<RelayState>('checking');
  const [operation, setOperation] = useState<'overlay' | 'notification' | 'relay' | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const id = ++refreshId.current;
    setRelayState('checking');
    await bootstrapDemoConfiguration().catch(() => undefined);
    const [nativeResult, demoResult, relayResult] = await Promise.allSettled([
      Platform.OS === 'android' ? LMCommentAndroid.getReadiness() : Promise.reject(),
      Platform.OS === 'android'
        ? LMCommentAndroid.getDemoConfigurationStatus()
        : Promise.reject(),
      checkRelay(),
    ]);
    if (id !== refreshId.current) return;
    setNativeReadiness(nativeResult.status === 'fulfilled' ? nativeResult.value : null);
    setDemoStatus(demoResult.status === 'fulfilled' ? demoResult.value : null);
    setRelayState(relayResult.status === 'fulfilled' ? relayResult.value : 'unavailable');
  }, []);

  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') void refresh();
    });
    return () => subscription.remove();
  }, [refresh]);

  const overlayReady = nativeReadiness?.overlayPermission === 'granted';
  const notificationReady = nativeReadiness?.notificationPermission === 'granted'
    || nativeReadiness?.notificationPermission === 'not-required';
  const nativeReady = typeof nativeReadiness?.contractVersion === 'number';
  const tokenReady = demoStatus?.isDemoTokenConfigured === true;
  const fullyReady = overlayReady
    && notificationReady
    && nativeReady
    && tokenReady
    && relayState === 'ready';
  const checking = !nativeReadiness || !demoStatus || relayState === 'checking';

  const openOverlaySettings = useCallback(async () => {
    if (operation) return;
    setOperation('overlay');
    setMessage(null);
    try {
      await LMCommentAndroid.openOverlayPermissionSettings();
    } catch {
      setMessage('Android settings could not open. Return to Home and try again.');
    } finally {
      setOperation(null);
    }
  }, [operation]);

  const requestNotifications = useCallback(async () => {
    if (operation) return;
    setOperation('notification');
    setMessage(null);
    try {
      await LMCommentAndroid.requestNotificationPermission();
      setTimeout(() => void refresh(), 350);
    } catch {
      setMessage('Notification permission could not be requested. Try again from this screen.');
    } finally {
      setOperation(null);
    }
  }, [operation, refresh]);

  const retryRelay = useCallback(async () => {
    if (operation) return;
    setOperation('relay');
    setMessage(null);
    setRelayState('checking');
    const next = await checkRelay();
    setRelayState(next);
    if (next === 'unavailable') {
      setMessage('The demo relay is not reachable yet. Check the phone connection and retry.');
    }
    setOperation(null);
  }, [operation]);

  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar
        title="Setup"
        subtitle="One-time Android readiness"
        onBack={() => router.back()}
      />

      <StatusHero
        status={checking ? 'checking' : fullyReady ? 'ready' : relayState === 'unavailable' ? 'unavailable' : 'setup'}
        label={fullyReady ? 'READY TO DEMO' : 'FOUR QUICK CHECKS'}
        title={fullyReady ? 'Everything is connected.' : 'No account. No provider setup.'}
        body={
          fullyReady
            ? 'Return Home to start the bubble or open a synthetic text fixture.'
            : 'LM-Comment only asks for the Android access required by the judge workflow.'
        }
      >
        <PrimaryButton
          label={fullyReady ? 'Continue to LM-Comment' : 'Refresh checks'}
          onPress={fullyReady ? () => router.replace('/') : () => void refresh()}
          loading={checking}
          accessibilityHint={fullyReady ? 'Returns to the Home screen.' : 'Checks permissions and relay readiness again.'}
        />
      </StatusHero>

      {message ? (
        <View style={styles.messageCard} accessibilityLiveRegion="polite">
          <Text style={styles.messageTitle}>Setup note</Text>
          <Text style={styles.messageBody}>{message}</Text>
        </View>
      ) : null}

      <SectionHeading
        eyebrow="THE COMPLETE SETUP"
        title="Four checks, in order"
        description="The first two are Android controls. Capture consent appears only when you actually capture."
      />

      <View style={styles.stepStack}>
        <View style={styles.stepGroup}>
          <ReadinessCard
            label="1 · Display over apps"
            status={!nativeReadiness ? 'checking' : overlayReady ? 'ready' : 'needed'}
            detail={
              overlayReady
                ? 'Granted — the compact bubble can appear over another app.'
                : 'Android uses this permission only for the compact bubble.'
            }
            onPress={!overlayReady ? () => void openOverlaySettings() : undefined}
          />
          {!overlayReady ? (
            <SecondaryButton
              label="Open Android overlay settings"
              onPress={() => void openOverlaySettings()}
              loading={operation === 'overlay'}
              accessibilityHint="Opens the system page where Display over other apps can be allowed."
            />
          ) : null}
        </View>

        <View style={styles.stepGroup}>
          <ReadinessCard
            label="2 · Foreground notification"
            status={!nativeReadiness ? 'checking' : notificationReady ? 'ready' : 'needed'}
            detail={
              notificationReady
                ? 'Ready — Android can show the bubble service notification.'
                : 'Required on newer Android versions while the bubble is active.'
            }
            onPress={!notificationReady ? () => void requestNotifications() : undefined}
          />
          {!notificationReady ? (
            <SecondaryButton
              label="Allow notifications"
              onPress={() => void requestNotifications()}
              loading={operation === 'notification'}
              accessibilityHint="Shows Android's notification permission request."
            />
          ) : null}
        </View>

        <ReadinessCard
          label="3 · Screen-capture consent"
          status="ready"
          detail="Nothing to grant now. Android asks each time you tap the bubble, before the one-shot capture."
        />

        <View style={styles.stepGroup}>
          <ReadinessCard
            label="4 · Demo backend"
            status={relayState}
            detail={
              relayState === 'ready'
                ? `Reachable${demoStatus?.relayHostname ? ` at ${demoStatus.relayHostname}` : ''}; demo access is ${tokenReady ? 'configured' : 'missing'}.`
                : relayState === 'checking'
                  ? 'Checking the safe health endpoint.'
                  : 'The phone cannot currently reach generation.'
            }
            onPress={relayState === 'unavailable' ? () => void retryRelay() : undefined}
          />
          {relayState === 'unavailable' ? (
            <SecondaryButton
              label="Retry relay connection"
              onPress={() => void retryRelay()}
              loading={operation === 'relay'}
            />
          ) : null}
        </View>
      </View>

      {!tokenReady && demoStatus ? (
        <View style={styles.messageCard}>
          <Text style={styles.messageTitle}>Judge configuration is missing</Text>
          <Text style={styles.messageBody}>
            Reinstall the prepared judge build. This screen never asks for a provider key.
          </Text>
        </View>
      ) : null}

      <PrivacyBanner text="Screenshots stay on this device. Only reviewed text is sent." />

      <View style={styles.boundaryCard}>
        <Text style={styles.boundaryEyebrow}>WHAT HAPPENS AT USE TIME</Text>
        <Text style={styles.boundaryTitle}>Capture remains an explicit Android action.</Text>
        <Text style={styles.boundaryBody}>
          Tap the bubble, approve the system capture prompt, crop on-device, and review the OCR text. Nothing is posted automatically.
        </Text>
      </View>

      <SecondaryButton label="Back to Home" onPress={() => router.replace('/')} />
    </Screen>
  );
}

function createStyles(light: boolean) {
  const colors = light
    ? {
        outline: '#D5DBE6',
        surface: '#FFFFFF',
        textPrimary: '#121520',
        textSecondary: '#4F586A',
        textMuted: '#687386',
        warning: '#9A5700',
        warningSurface: '#FFF7E9',
      }
    : {
        outline: '#30384A',
        surface: '#141822',
        textPrimary: '#F5F7FB',
        textSecondary: '#B7BFCE',
        textMuted: '#8D97A8',
        warning: '#FFB85C',
        warningSurface: '#241C14',
      };
  return StyleSheet.create({
    screenContent: {
      gap: 28,
      paddingBottom: 36,
    },
    stepStack: {
      gap: 12,
    },
    stepGroup: {
      gap: 8,
    },
    messageCard: {
      backgroundColor: colors.warningSurface,
      borderColor: colors.warning,
      borderRadius: 16,
      borderWidth: 1,
      marginTop: 14,
      padding: 16,
    },
    messageTitle: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: '700',
      lineHeight: 20,
    },
    messageBody: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 20,
      marginTop: 3,
    },
    boundaryCard: {
      backgroundColor: colors.surface,
      borderColor: colors.outline,
      borderRadius: 20,
      borderWidth: 1,
      marginBottom: 16,
      marginTop: 4,
      padding: 18,
    },
    boundaryEyebrow: {
      color: colors.textMuted,
      fontSize: 11,
      fontWeight: '700',
      letterSpacing: 1.2,
      lineHeight: 16,
    },
    boundaryTitle: {
      color: colors.textPrimary,
      fontSize: 18,
      fontWeight: '700',
      lineHeight: 24,
      marginTop: 8,
    },
    boundaryBody: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 21,
      marginTop: 7,
    },
  });
}
