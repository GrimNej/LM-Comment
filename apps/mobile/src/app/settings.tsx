import LMCommentAndroid, {
  type AppearanceMode,
  type DemoConfiguration,
  type Tone,
} from '@lm-comment/android';
import * as Application from 'expo-application';
import Constants from 'expo-constants';
import { router } from 'expo-router';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AccessibilityInfo,
  Platform,
  Pressable,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';

import { getBundledDemoConfiguration } from '@/lib/demo-configuration';
import {
  InlineError,
  PrimaryButton,
  Screen,
  SecondaryButton,
  SectionHeading,
  SettingRow,
  StatusChip,
  TopBar,
  useAppReadiness,
  useAppTheme,
} from '@/ui';

const TONES: readonly { value: Tone; label: string }[] = [
  { value: 'natural', label: 'Natural' },
  { value: 'professional', label: 'Professional' },
  { value: 'friendly', label: 'Friendly' },
  { value: 'witty', label: 'Witty' },
  { value: 'concise', label: 'Concise' },
];

const OPTION_COUNTS = [1, 2, 3] as const;
const APPEARANCE_MODES: readonly { value: AppearanceMode; label: string }[] = [
  { value: 'system', label: 'System' },
  { value: 'light', label: 'Light' },
  { value: 'dark', label: 'Dark' },
];
const ADVANCED_UNLOCK_TAPS = 7;

export default function SettingsScreen() {
  const theme = useAppTheme();
  const { colors, spacing, radii, typography } = theme;
  const { demoConfiguration, refresh } = useAppReadiness();
  const bundledConfiguration = useMemo(() => getBundledDemoConfiguration(), []);
  const hydratedRef = useRef(false);

  const [relayBaseUrl, setRelayBaseUrl] = useState(
    bundledConfiguration?.relayBaseUrl ?? '',
  );
  const [demoToken, setDemoToken] = useState(bundledConfiguration?.demoToken ?? '');
  const [tone, setTone] = useState<Tone>(bundledConfiguration?.defaultTone ?? 'natural');
  const [optionCount, setOptionCount] = useState<1 | 2 | 3>(
    bundledConfiguration?.optionCount ?? 3,
  );
  const [demoMode, setDemoMode] = useState(bundledConfiguration?.demoMode ?? true);
  const [versionTaps, setVersionTaps] = useState(0);
  const [advancedUnlocked, setAdvancedUnlocked] = useState(false);
  const [advancedDirty, setAdvancedDirty] = useState(false);
  const [busyAction, setBusyAction] = useState<
    'save' | 'reset' | 'position' | 'appearance' | null
  >(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const appVersion = Application.nativeApplicationVersion
    ?? Constants.expoConfig?.version
    ?? '0.1.0';
  const androidAvailable = Platform.OS === 'android';

  useEffect(() => {
    if (!demoConfiguration || hydratedRef.current) return;
    hydratedRef.current = true;
    setTone(demoConfiguration.defaultTone);
    setOptionCount(demoConfiguration.optionCount);
    setDemoMode(demoConfiguration.demoMode);
  }, [demoConfiguration]);

  const unlockAdvanced = () => {
    if (advancedUnlocked) return;
    const nextCount = versionTaps + 1;
    setVersionTaps(nextCount);
    if (nextCount < ADVANCED_UNLOCK_TAPS) return;
    setAdvancedUnlocked(true);
    setNotice('Advanced demo configuration unlocked.');
    AccessibilityInfo.announceForAccessibility('Advanced demo configuration unlocked');
  };

  const save = async () => {
    setError(null);
    setNotice(null);
    if (!androidAvailable) {
      setError('Settings can only be saved in the Android application.');
      return;
    }
    if (advancedDirty && (!relayBaseUrl.trim() || demoToken.trim().length < 12)) {
      setError('Enter a valid backend URL and demo token in advanced settings.');
      return;
    }

    const defaults = {
      defaultTone: tone,
      optionCount,
      demoMode,
    };
    setBusyAction('save');
    try {
      if (advancedDirty) {
        const configuration: DemoConfiguration = {
          relayBaseUrl: relayBaseUrl.trim(),
          demoToken: demoToken.trim(),
          ...defaults,
        };
        await LMCommentAndroid.configureDemo(configuration);
        setAdvancedDirty(false);
      } else {
        await LMCommentAndroid.updateDemoDefaults(defaults);
      }
      await refresh();
      setNotice('Settings saved on this device.');
    } catch {
      setError('Settings were not saved. Check the HTTPS URL and demo token, then try again.');
    } finally {
      setBusyAction(null);
    }
  };

  const resetConfiguration = async () => {
    setError(null);
    setNotice(null);
    setBusyAction('reset');
    try {
      await LMCommentAndroid.resetDemoConfiguration();
      setRelayBaseUrl(bundledConfiguration?.relayBaseUrl ?? '');
      setDemoToken(bundledConfiguration?.demoToken ?? '');
      setTone(bundledConfiguration?.defaultTone ?? 'natural');
      setOptionCount(bundledConfiguration?.optionCount ?? 3);
      setDemoMode(bundledConfiguration?.demoMode ?? true);
      setAdvancedDirty(Boolean(bundledConfiguration));
      await refresh();
      setNotice(bundledConfiguration
        ? 'Demo settings cleared. Save to restore the bundled defaults.'
        : 'Demo settings cleared.');
    } catch {
      setError('The demo configuration could not be reset.');
    } finally {
      setBusyAction(null);
    }
  };

  const changeAppearance = async (mode: AppearanceMode) => {
    if (busyAction || mode === theme.appearanceMode) return;
    setError(null);
    setNotice(null);
    setBusyAction('appearance');
    try {
      await theme.setAppearanceMode(mode);
      const label = APPEARANCE_MODES.find((item) => item.value === mode)?.label ?? 'System';
      setNotice(`${label} mode selected.`);
    } catch {
      setError('The appearance setting could not be saved.');
    } finally {
      setBusyAction(null);
    }
  };

  const resetBubblePosition = async () => {
    setError(null);
    setNotice(null);
    setBusyAction('position');
    try {
      await LMCommentAndroid.resetBubblePosition();
      setNotice('Bubble position reset.');
    } catch {
      setError('The bubble position could not be reset.');
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar
        title="Settings"
        subtitle="Writing and connection"
        onBack={() => router.back()}
      />

      <View style={{ gap: spacing.sm }}>
        <Text style={[typography.title, { color: colors.textPrimary }]}>Choose your defaults</Text>
        <Text style={[typography.body, { color: colors.textSecondary }]}>
          Android stores these settings in private app storage and applies them when you use the
          bubble.
        </Text>
      </View>

      {error ? <InlineError title="Could not complete that action" message={error} /> : null}
      {notice ? (
        <View
          accessibilityLiveRegion="polite"
          style={[
            styles.notice,
            {
              backgroundColor: colors.successSoft,
              borderColor: colors.success,
              borderRadius: radii.md,
            },
          ]}
        >
          <Text style={[typography.small, { color: colors.success }]}>{notice}</Text>
        </View>
      ) : null}

      <View style={{ gap: spacing.sm }}>
        <SectionHeading
          title="Appearance"
          description="Light uses paper, ink, lime, and terracotta. Dark restores the graphite, violet, and cyan palette."
        />
        <SettingRow
          label="Color mode"
          description="System follows your phone automatically."
          value={APPEARANCE_MODES.find((item) => item.value === theme.appearanceMode)?.label}
        >
          <View style={styles.wrapControls}>
            {APPEARANCE_MODES.map((item) => (
              <ChoiceButton
                key={item.value}
                label={item.label}
                selected={theme.appearanceMode === item.value}
                onPress={() => void changeAppearance(item.value)}
              />
            ))}
          </View>
        </SettingRow>
      </View>

      <View style={{ gap: spacing.sm }}>
        <SectionHeading title="Writing defaults" />
        <SettingRow label="Default tone" description="Used when a new workflow opens.">
          <View style={styles.wrapControls}>
            {TONES.map((item) => (
              <ChoiceButton
                key={item.value}
                label={item.label}
                selected={tone === item.value}
                onPress={() => setTone(item.value)}
              />
            ))}
          </View>
        </SettingRow>
        <SettingRow label="Option count" description="Generate one, two, or three replies.">
          <View style={styles.wrapControls}>
            {OPTION_COUNTS.map((count) => (
              <ChoiceButton
                key={count}
                label={String(count)}
                selected={optionCount === count}
                onPress={() => setOptionCount(count)}
              />
            ))}
          </View>
        </SettingRow>
        <SettingRow
          label="Demo mode"
          description="Saved with the current demo configuration."
          value={demoMode ? 'On' : 'Off'}
        >
          <Switch
            accessibilityLabel="Demo mode"
            accessibilityRole="switch"
            value={demoMode}
            onValueChange={setDemoMode}
            trackColor={{ false: colors.outline, true: colors.primary }}
            thumbColor={theme.isDark ? colors.textPrimary : colors.surface}
          />
        </SettingRow>
      </View>

      <View style={{ gap: spacing.sm }}>
        <SectionHeading title="Connection" />
        <SettingRow
          label="Relay backend"
          description="Only the hostname is exposed by native status."
          value={demoConfiguration?.relayHostname ?? 'Not configured'}
        />
        <SettingRow
          label="Demo access"
          description="The token itself is never returned by diagnostics."
          value={demoConfiguration?.isDemoTokenConfigured ? 'Configured' : 'Not configured'}
        />

        {advancedUnlocked ? (
          <View
            style={[
              styles.advancedCard,
              {
                backgroundColor: colors.surface,
                borderColor: colors.outline,
                borderRadius: radii.lg,
              },
            ]}
          >
            <View style={styles.advancedHeader}>
              <Text style={[typography.heading, { color: colors.textPrimary }]}>Advanced demo access</Text>
              <StatusChip label="Unlocked" tone="warning" />
            </View>
            <Text style={[typography.small, { color: colors.textSecondary }]}>
              Overrides are validated by Android before they are stored. Production builds require
              HTTPS.
            </Text>
            <TextInput
              accessibilityLabel="Relay backend URL"
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              placeholder="https://relay.example.com"
              placeholderTextColor={colors.textMuted}
              value={relayBaseUrl}
              onChangeText={(value) => {
                setRelayBaseUrl(value);
                setAdvancedDirty(true);
              }}
              style={[
                styles.input,
                typography.body,
                {
                  color: colors.textPrimary,
                  backgroundColor: colors.surfaceRaised,
                  borderColor: colors.outline,
                  borderRadius: radii.md,
                },
              ]}
            />
            <TextInput
              accessibilityLabel="Demo token override"
              autoCapitalize="none"
              autoCorrect={false}
              secureTextEntry
              placeholder="Demo token"
              placeholderTextColor={colors.textMuted}
              value={demoToken}
              onChangeText={(value) => {
                setDemoToken(value);
                setAdvancedDirty(true);
              }}
              style={[
                styles.input,
                typography.body,
                {
                  color: colors.textPrimary,
                  backgroundColor: colors.surfaceRaised,
                  borderColor: colors.outline,
                  borderRadius: radii.md,
                },
              ]}
            />
          </View>
        ) : null}
      </View>

      <View style={{ gap: spacing.sm }}>
        <SectionHeading title="Maintenance" />
        <SettingRow
          label="App version"
          description="Build installed on this device"
          value={appVersion}
          onPress={advancedUnlocked ? undefined : unlockAdvanced}
        />
        <SettingRow
          label="Bubble position"
          description="Return the floating control to its default screen edge."
        >
          <SecondaryButton
            label="Reset position"
            onPress={() => void resetBubblePosition()}
            loading={busyAction === 'position'}
            disabled={busyAction !== null || !androidAvailable}
          />
        </SettingRow>
      </View>

      <View style={{ gap: spacing.sm }}>
        <PrimaryButton
          label="Save settings"
          onPress={() => void save()}
          loading={busyAction === 'save'}
          disabled={busyAction !== null || !androidAvailable}
          accessibilityHint="Validates and saves the demo configuration in Android private storage"
        />
        <SecondaryButton
          label="Reset demo configuration"
          onPress={() => void resetConfiguration()}
          loading={busyAction === 'reset'}
          disabled={busyAction !== null || !androidAvailable}
          accessibilityHint="Clears the native relay URL, demo token, and writing defaults"
        />
      </View>
    </Screen>
  );
}

function ChoiceButton({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  const { colors, radii, typography } = useAppTheme();
  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      onPress={onPress}
      style={({ pressed }) => [
        styles.choice,
        {
          backgroundColor: selected ? colors.primarySoft : colors.surfaceRaised,
          borderColor: selected ? colors.primary : colors.outline,
          borderRadius: radii.md,
          opacity: pressed ? 0.76 : 1,
        },
      ]}
    >
      <Text
        style={[
          typography.label,
          { color: selected ? colors.primary : colors.textSecondary },
        ]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screenContent: { gap: 32, paddingBottom: 48 },
  notice: { borderWidth: 1, paddingHorizontal: 16, paddingVertical: 14 },
  wrapControls: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  choice: {
    minHeight: 48,
    minWidth: 52,
    borderWidth: 1,
    paddingHorizontal: 15,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  advancedCard: { borderWidth: 1, padding: 18, gap: 14 },
  advancedHeader: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
  },
  input: { minHeight: 54, borderWidth: 1, paddingHorizontal: 15, paddingVertical: 12 },
});
