import LMCommentAndroid, {
  type CaptureResourceCounts,
  type RelayHealthStatus,
  type SafeDiagnostics,
} from '@lm-comment/android';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import {
  InlineError,
  Screen,
  SecondaryButton,
  SectionHeading,
  SettingRow,
  Skeleton,
  StatusChip,
  TopBar,
  useAppTheme,
} from '@/ui';

const COUNTERS: readonly {
  key: keyof CaptureResourceCounts;
  label: string;
}[] = [
  { key: 'activeCaptureService', label: 'Capture services' },
  { key: 'activeProjection', label: 'Media projections' },
  { key: 'activeVirtualDisplay', label: 'Virtual displays' },
  { key: 'activeImageReader', label: 'Image readers' },
  { key: 'activeImage', label: 'Images' },
  { key: 'activeWorkflowBitmap', label: 'Workflow bitmaps' },
];

export default function DiagnosticsScreen() {
  const { colors, spacing, radii, typography } = useAppTheme();
  const [diagnostics, setDiagnostics] = useState<SafeDiagnostics | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    try {
      setDiagnostics(await LMCommentAndroid.getSafeDiagnostics());
    } catch {
      setDiagnostics(null);
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    void LMCommentAndroid.getSafeDiagnostics().then(
      (result) => {
        if (!active) return;
        setDiagnostics(result);
        setFailed(false);
        setLoading(false);
      },
      () => {
        if (!active) return;
        setDiagnostics(null);
        setFailed(true);
        setLoading(false);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar
        title="Diagnostics"
        subtitle="Safe, content-free signals"
        onBack={() => router.back()}
      />

      <View
        style={[
          styles.safetyCard,
          {
            backgroundColor: colors.secondarySoft,
            borderColor: colors.secondary,
            borderRadius: radii.lg,
          },
        ]}
      >
        <View style={styles.safetyHeader}>
          <Text style={[typography.heading, { color: colors.textPrimary }]}>Safe by construction</Text>
          <StatusChip label="No content" tone="success" />
        </View>
        <Text style={[typography.small, { color: colors.textSecondary }]}>
          This screen cannot display tokens, OCR text, generated replies, request bodies, or
          screenshots.
        </Text>
      </View>

      {failed ? (
        <InlineError
          title="Diagnostics unavailable"
          message="The native diagnostics bridge did not respond."
          onRetry={() => void load()}
        />
      ) : null}

      {loading && !diagnostics ? (
        <View style={{ gap: spacing.sm }} accessibilityLabel="Loading diagnostics">
          <Skeleton height={82} />
          <Skeleton height={82} />
          <Skeleton height={82} />
        </View>
      ) : null}

      {diagnostics ? (
        <>
          <View style={{ gap: spacing.sm }}>
            <SectionHeading title="Application" />
            <SettingRow label="App version" value={diagnostics.appVersion} />
            <SettingRow
              label="Native contract"
              value={`Version ${diagnostics.contractVersion}`}
            />
            <SettingRow
              label="Platform"
              value={diagnostics.androidApi === null ? 'Web preview' : `Android API ${diagnostics.androidApi}`}
            />
            <SettingRow label="Device" value={diagnostics.deviceModel} />
          </View>

          <View style={{ gap: spacing.sm }}>
            <SectionHeading title="Permissions and bubble" />
            <DiagnosticStatusRow
              label="Display over apps"
              status={permissionLabel(diagnostics.overlayPermission)}
              good={diagnostics.overlayPermission === 'granted'}
            />
            <DiagnosticStatusRow
              label="Notifications"
              status={permissionLabel(diagnostics.notificationPermission)}
              good={diagnostics.notificationPermission !== 'denied'}
            />
            <DiagnosticStatusRow
              label="Bubble"
              status={capitalize(diagnostics.bubbleStatus)}
              good={diagnostics.bubbleStatus === 'running'}
              neutral={diagnostics.bubbleStatus === 'stopped'}
            />
          </View>

          <View style={{ gap: spacing.sm }}>
            <SectionHeading title="Relay" />
            <SettingRow
              label="Hostname"
              description="Credentials are intentionally omitted."
              value={diagnostics.relayHostname ?? 'Not configured'}
            />
            <DiagnosticStatusRow
              label="Health"
              status={relayHealthLabel(diagnostics.relayHealth)}
              good={diagnostics.relayHealth === 'healthy'}
              neutral={diagnostics.relayHealth === 'not-configured'}
            />
            <SettingRow
              label="Last stable error"
              description="A fixed code only—never provider or user content."
              value={diagnostics.lastStableErrorCode ?? 'None'}
            />
          </View>

          <View style={{ gap: spacing.sm }}>
            <SectionHeading title="Capture resources" />
            {diagnostics.captureResourceCounts ? (
              COUNTERS.map(({ key, label }) => (
                <SettingRow
                  key={key}
                  label={label}
                  value={String(diagnostics.captureResourceCounts?.[key] ?? 0)}
                />
              ))
            ) : (
              <SettingRow
                label="Resource counters"
                description="Available only in debuggable builds."
                value="Hidden in release"
              />
            )}
          </View>

          <SecondaryButton
            label="Refresh diagnostics"
            onPress={() => void load()}
            loading={loading}
            accessibilityHint="Runs a new content-free native and relay health check"
          />
        </>
      ) : null}
    </Screen>
  );
}

function DiagnosticStatusRow({
  label,
  status,
  good,
  neutral = false,
}: {
  label: string;
  status: string;
  good: boolean;
  neutral?: boolean;
}) {
  return (
    <SettingRow label={label}>
      <StatusChip
        label={status}
        tone={good ? 'success' : neutral ? 'neutral' : 'warning'}
      />
    </SettingRow>
  );
}

function relayHealthLabel(status: RelayHealthStatus): string {
  switch (status) {
    case 'healthy': return 'Healthy';
    case 'degraded': return 'Degraded';
    case 'unavailable': return 'Unavailable';
    case 'not-configured': return 'Not configured';
  }
}

function permissionLabel(value: 'granted' | 'denied' | 'not-required'): string {
  if (value === 'not-required') return 'Not required';
  return capitalize(value);
}

function capitalize(value: string): string {
  return value.length ? value[0].toUpperCase() + value.slice(1) : value;
}

const styles = StyleSheet.create({
  screenContent: { gap: 32, paddingBottom: 48 },
  safetyCard: { borderWidth: 1, padding: 18, gap: 10 },
  safetyHeader: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
  },
});
