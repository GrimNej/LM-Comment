import LMCommentAndroid, {
  type DemoConfigurationStatus,
  type NativeReadiness,
} from '@lm-comment/android';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AppState,
  Platform,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { bootstrapDemoConfiguration } from '@/lib/demo-configuration';
import {
  AppMark,
  FixtureCard,
  PrimaryButton,
  PrivacyBanner,
  ReadinessCard,
  Screen,
  SecondaryButton,
  SectionHeading,
  StatusHero,
  TopBar,
  type AppColors,
  useAppTheme,
} from '@/ui';

type RelayState = 'checking' | 'ready' | 'unavailable';
type HeroState = 'ready' | 'setup' | 'active' | 'unavailable' | 'checking';

type Fixture = {
  eyebrow: string;
  title: string;
  body: string;
  tags: string[];
  sourceText: string;
  accent: 'lime' | 'terracotta' | 'warm';
};

const FIXTURES: readonly Fixture[] = [
  {
    eyebrow: 'Social post',
    title: 'Celebrate a personal win',
    body: 'A warm post about finishing a first 10K in difficult weather.',
    tags: ['Friendly', 'Social'],
    accent: 'lime',
    sourceText:
      'Synthetic demo post: I finished my first 10K this morning! It rained for most of the route, but the people cheering made every kilometre worth it. Still cannot believe I actually did it.',
  },
  {
    eyebrow: 'Work message',
    title: 'Answer a deadline request',
    body: 'A teammate asks for a revised launch brief before Thursday.',
    tags: ['Professional', 'Work'],
    accent: 'terracotta',
    sourceText:
      'Synthetic demo message: Hi, could you send the revised launch brief by Thursday afternoon? Please include the updated rollout dates and the two open risks so I can review everything before Friday morning.',
  },
  {
    eyebrow: 'Technical thread',
    title: 'Respond to a design tradeoff',
    body: 'A concise engineering discussion about privacy and caching.',
    tags: ['Concise', 'Technical'],
    accent: 'warm',
    sourceText:
      'Synthetic demo thread: Caching OCR output might make repeated edits faster, but it also creates another lifecycle for sensitive text. I would keep the screenshot memory-only and optimize the crop pipeline before adding persistence.',
  },
] as const;

async function getRelayState(): Promise<Exclude<RelayState, 'checking'>> {
  try {
    if (Platform.OS !== 'android') return 'unavailable';
    const diagnostics = await LMCommentAndroid.getSafeDiagnostics();
    return diagnostics.relayHealth === 'healthy' ? 'ready' : 'unavailable';
  } catch {
    return 'unavailable';
  }
}

export default function HomeScreen() {
  const router = useRouter();
  const { colors } = useAppTheme();
  const styles = useMemo(() => createStyles(colors), [colors]);
  const refreshId = useRef(0);
  const [nativeReadiness, setNativeReadiness] = useState<NativeReadiness | null>(null);
  const [demoStatus, setDemoStatus] = useState<DemoConfigurationStatus | null>(null);
  const [relayState, setRelayState] = useState<RelayState>('checking');
  const [operation, setOperation] = useState<'starting' | 'stopping' | 'fixture' | null>(null);
  const [inlineError, setInlineError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const id = ++refreshId.current;
    setRelayState('checking');
    await bootstrapDemoConfiguration().catch(() => undefined);

    const [nativeResult, demoResult, relayResult] = await Promise.allSettled([
      Platform.OS === 'android' ? LMCommentAndroid.getReadiness() : Promise.reject(),
      Platform.OS === 'android'
        ? LMCommentAndroid.getDemoConfigurationStatus()
        : Promise.reject(),
      getRelayState(),
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

  const notificationReady = nativeReadiness?.notificationPermission === 'granted'
    || nativeReadiness?.notificationPermission === 'not-required';
  const overlayReady = nativeReadiness?.overlayPermission === 'granted';
  const tokenReady = demoStatus?.isDemoTokenConfigured === true;
  const nativeReady = typeof nativeReadiness?.contractVersion === 'number';
  const setupReady = nativeReady && overlayReady && notificationReady && tokenReady;
  const bubbleActive = nativeReadiness?.bubbleStatus === 'running'
    || nativeReadiness?.bubbleStatus === 'starting';

  const heroState: HeroState = (() => {
    if (!nativeReadiness || !demoStatus || relayState === 'checking') return 'checking';
    if (bubbleActive) return 'active';
    if (!setupReady) return 'setup';
    if (relayState === 'unavailable') return 'unavailable';
    return 'ready';
  })();

  const heroLabel = {
    active: 'Bubble active',
    checking: 'Checking setup',
    ready: 'Ready to start',
    setup: 'Finish setup',
    unavailable: 'Relay offline',
  }[heroState];
  const heroBody = {
    active: 'Open a post or message, then tap the bubble.',
    checking: 'Checking permissions and the relay connection.',
    ready: 'Start the bubble or try a sample below.',
    setup: 'Allow the missing Android permissions before you start.',
    unavailable: "Capture still works, but the relay can't generate replies right now.",
  }[heroState];

  const toggleBubble = useCallback(async () => {
    if (operation) return;
    setInlineError(null);
    if (!bubbleActive && !setupReady) {
      router.push('/setup');
      return;
    }
    setOperation(bubbleActive ? 'stopping' : 'starting');
    try {
      if (bubbleActive) {
        await LMCommentAndroid.stopBubble();
      } else {
        await LMCommentAndroid.startBubble();
      }
      setNativeReadiness((current) => current
        ? { ...current, bubbleStatus: bubbleActive ? 'stopped' : 'running' }
        : current);
      setTimeout(() => void refresh(), 250);
    } catch {
      setInlineError(
        bubbleActive
          ? 'The bubble could not be stopped. Try once more.'
          : 'The bubble could not start. Review Setup and try again.',
      );
    } finally {
      setOperation(null);
    }
  }, [bubbleActive, operation, refresh, router, setupReady]);

  const openFixture = useCallback(async (fixture: Fixture) => {
    if (operation) return;
    setInlineError(null);
    if (!tokenReady || relayState !== 'ready') {
      router.push('/setup');
      return;
    }
    setOperation('fixture');
    try {
      await LMCommentAndroid.openManualTextWorkflow(fixture.sourceText);
    } catch {
      setInlineError('Manual Text could not open. Check Setup and try again.');
    } finally {
      setOperation(null);
    }
  }, [operation, relayState, router, tokenReady]);

  const checking = heroState === 'checking';
  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar right={<AppMark size={42} showWordmark />} />

      <StatusHero
        status={heroState}
        label={heroLabel}
        title="Write a reply from text on your screen."
        body={heroBody}
      >
        <View style={styles.heroActions}>
          <PrimaryButton
            label={bubbleActive ? 'Stop bubble' : 'Start bubble'}
            onPress={() => void toggleBubble()}
            loading={operation === 'starting' || operation === 'stopping'}
            disabled={checking}
            accessibilityHint={
              bubbleActive
                ? 'Stops the compact overlay bubble.'
                : 'Starts the compact overlay bubble, or opens Setup if permission is needed.'
            }
          />
          <SecondaryButton
            label="Try with text"
            onPress={() => router.push('/demo')}
            disabled={operation !== null}
            accessibilityHint="Opens three synthetic examples that use the real generation workflow."
          />
        </View>
      </StatusHero>

      {inlineError ? (
        <View style={styles.errorCard} accessibilityLiveRegion="polite">
          <Text style={styles.errorTitle}>Something needs attention</Text>
          <Text style={styles.errorBody}>{inlineError}</Text>
        </View>
      ) : null}

      <PrivacyBanner text="The screenshot stays in memory on your phone. Reviewed text and generation choices go to the relay." />

      <SectionHeading
        title="Check permissions and connection"
        description="Tap a missing item to finish setup."
      />
      <View style={styles.cardStack}>
        <ReadinessCard
          label="Display over apps"
          status={!nativeReadiness ? 'checking' : overlayReady ? 'ready' : 'needed'}
          detail={overlayReady ? 'Bubble permission granted' : 'Needed for the compact bubble'}
          onPress={!overlayReady ? () => router.push('/setup') : undefined}
        />
        <ReadinessCard
          label="Notifications"
          status={!nativeReadiness ? 'checking' : notificationReady ? 'ready' : 'needed'}
          detail={notificationReady ? 'Foreground notification available' : 'Needed while the bubble is active'}
          onPress={!notificationReady ? () => router.push('/setup') : undefined}
        />
        <ReadinessCard
          label="Relay connection"
          status={relayState}
          detail={relayState === 'ready' ? 'Generation service reachable' : 'Tap to check setup and retry'}
          onPress={relayState !== 'ready' ? () => router.push('/setup') : undefined}
        />
        <ReadinessCard
          label="Native module"
          status={!nativeReadiness ? 'checking' : nativeReady ? 'ready' : 'unavailable'}
          detail={nativeReady ? `Contract v${nativeReadiness.contractVersion} connected` : 'Native workflow unavailable'}
        />
        <ReadinessCard
          label="Demo token"
          status={!demoStatus ? 'checking' : tokenReady ? 'ready' : 'needed'}
          detail={tokenReady ? 'Demo token configured' : 'Demo token missing'}
          onPress={!tokenReady ? () => router.push('/setup') : undefined}
        />
      </View>

      <SectionHeading
        title="Try a sample reply"
        description="Each sample opens editable text and uses the same generation and copy steps as capture."
      />
      <View style={styles.cardStack}>
        {FIXTURES.map((fixture) => (
          <FixtureCard
            key={fixture.eyebrow}
            eyebrow={fixture.eyebrow}
            title={fixture.title}
            body={fixture.body}
            tags={fixture.tags}
            accent={fixture.accent}
            onPress={() => void openFixture(fixture)}
          />
        ))}
      </View>

      <View style={styles.footerActions}>
        <SecondaryButton label="Browse samples" onPress={() => router.push('/demo')} />
        <SecondaryButton label="Review setup" onPress={() => router.push('/setup')} />
        <SecondaryButton label="Diagnostics" onPress={() => router.push('/diagnostics')} />
        <SecondaryButton label="Settings" onPress={() => router.push('/settings')} />
      </View>
      <Text style={styles.footerNote}>
        LM-Comment never posts a reply. Tap Copy when you want to use one.
      </Text>
    </Screen>
  );
}

function createStyles(colors: AppColors) {
  return StyleSheet.create({
    screenContent: {
      gap: 28,
      paddingBottom: 36,
    },
    heroActions: {
      gap: 10,
    },
    cardStack: {
      gap: 10,
    },
    errorCard: {
      backgroundColor: colors.dangerSurface,
      borderColor: colors.danger,
      borderRadius: 11,
      borderWidth: 1,
      marginTop: 14,
      padding: 16,
    },
    errorTitle: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: '700',
      lineHeight: 20,
    },
    errorBody: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 20,
      marginTop: 3,
    },
    footerActions: {
      gap: 10,
      marginTop: 24,
    },
    footerNote: {
      color: colors.textSecondary,
      fontSize: 13,
      lineHeight: 19,
      marginTop: 18,
      textAlign: 'center',
    },
  });
}
