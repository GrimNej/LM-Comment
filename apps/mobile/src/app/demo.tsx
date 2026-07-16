import LMCommentAndroid, { type DemoConfigurationStatus } from '@lm-comment/android';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Platform, StyleSheet, Text, View, useColorScheme } from 'react-native';

import { bootstrapDemoConfiguration } from '@/lib/demo-configuration';
import {
  FixtureCard,
  PrivacyBanner,
  Screen,
  SecondaryButton,
  SectionHeading,
  StatusHero,
  TopBar,
} from '@/ui';

type DemoFixture = {
  id: 'social' | 'professional' | 'technical';
  eyebrow: string;
  title: string;
  body: string;
  tags: string[];
  sourceText: string;
  accent: 'violet' | 'cyan' | 'warm';
};

const DEMO_FIXTURES: readonly DemoFixture[] = [
  {
    id: 'social',
    eyebrow: '01 · SOCIAL REACTION',
    title: 'Celebrate a personal win',
    body: '“I finished my first 10K this morning… It rained, but the people cheering made every kilometre worth it.”',
    tags: ['Synthetic', 'Friendly'],
    accent: 'violet',
    sourceText:
      'Synthetic demo post: I finished my first 10K this morning! It rained for most of the route, but the people cheering made every kilometre worth it. Still cannot believe I actually did it.',
  },
  {
    id: 'professional',
    eyebrow: '02 · PROFESSIONAL REPLY',
    title: 'Answer a deadline request',
    body: '“Could you send the revised launch brief by Thursday afternoon, including the dates and open risks?”',
    tags: ['Synthetic', 'Professional'],
    accent: 'cyan',
    sourceText:
      'Synthetic demo message: Hi — could you send the revised launch brief by Thursday afternoon? Please include the updated rollout dates and the two open risks so I can review everything before Friday morning.',
  },
  {
    id: 'technical',
    eyebrow: '03 · TECHNICAL DISCUSSION',
    title: 'Respond to a design tradeoff',
    body: '“Caching OCR might be faster, but it creates another lifecycle for sensitive text. Keep the screenshot memory-only.”',
    tags: ['Synthetic', 'Concise'],
    accent: 'warm',
    sourceText:
      'Synthetic demo thread: Caching OCR output might make repeated edits faster, but it also creates another lifecycle for sensitive text. I would keep the screenshot memory-only and optimize the crop pipeline before adding persistence.',
  },
] as const;

async function relayIsReachable(): Promise<boolean> {
  try {
    if (Platform.OS !== 'android') return false;
    const diagnostics = await LMCommentAndroid.getSafeDiagnostics();
    return diagnostics.relayHealth === 'healthy';
  } catch {
    return false;
  }
}

export default function DemoScreen() {
  const router = useRouter();
  const scheme = useColorScheme();
  const styles = useMemo(() => createStyles(scheme === 'light'), [scheme]);
  const [configuration, setConfiguration] = useState<DemoConfigurationStatus | null>(null);
  const [relayReady, setRelayReady] = useState<boolean | null>(null);
  const [opening, setOpening] = useState<DemoFixture['id'] | 'blank' | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setRelayReady(null);
    await bootstrapDemoConfiguration().catch(() => undefined);
    const [configurationResult, relayResult] = await Promise.allSettled([
      Platform.OS === 'android'
        ? LMCommentAndroid.getDemoConfigurationStatus()
        : Promise.reject(),
      relayIsReachable(),
    ]);
    setConfiguration(
      configurationResult.status === 'fulfilled' ? configurationResult.value : null,
    );
    setRelayReady(relayResult.status === 'fulfilled' ? relayResult.value : false);
  }, []);

  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const demoConfigured = configuration?.isDemoTokenConfigured === true;
  const ready = demoConfigured && relayReady === true;
  const checking = configuration === null || relayReady === null;

  const openManualText = useCallback(async (
    id: DemoFixture['id'] | 'blank',
    sourceText?: string,
  ) => {
    if (opening) return;
    setMessage(null);
    if (!ready) {
      setMessage('Finish Setup before opening the real generation workflow.');
      return;
    }
    setOpening(id);
    try {
      await LMCommentAndroid.openManualTextWorkflow(sourceText);
    } catch {
      setMessage('Manual Text could not open. Return to Setup and retry the readiness checks.');
    } finally {
      setOpening(null);
    }
  }, [opening, ready]);

  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar
        title="Demo lab"
        subtitle="Synthetic contexts · real workflow"
        onBack={() => router.back()}
      />

      <StatusHero
        status={checking ? 'checking' : ready ? 'ready' : relayReady ? 'setup' : 'unavailable'}
        label={ready ? 'REAL RELAY READY' : checking ? 'CHECKING DEMO' : 'SETUP NEEDED'}
        title="Choose context. Keep control."
        body="Each fixture opens the native Manual Text screen with safe sample content. Review it, choose a tone, then explicitly Generate and Copy."
      />

      {message ? (
        <View style={styles.messageCard} accessibilityLiveRegion="polite">
          <Text style={styles.messageTitle}>Demo needs attention</Text>
          <Text style={styles.messageBody}>{message}</Text>
          <View style={styles.messageAction}>
            <SecondaryButton label="Open Setup" onPress={() => router.push('/setup')} />
          </View>
        </View>
      ) : null}

      <PrivacyBanner text="These examples are synthetic. Screenshots stay on-device; only reviewed text is sent." />

      <SectionHeading
        eyebrow="THREE JUDGE-READY PATHS"
        title="Pick the conversation"
        description="Nothing generates on tap. The fixture first opens an editable review screen."
      />
      <View style={styles.fixtureStack}>
        {DEMO_FIXTURES.map((fixture) => (
          <FixtureCard
            key={fixture.id}
            eyebrow={opening === fixture.id ? 'OPENING MANUAL TEXT…' : fixture.eyebrow}
            title={fixture.title}
            body={fixture.body}
            tags={fixture.tags}
            accent={fixture.accent}
            onPress={() => void openManualText(fixture.id, fixture.sourceText)}
          />
        ))}
      </View>

      <SectionHeading
        eyebrow="WHAT THE JUDGES WILL SEE"
        title="One short, complete loop"
      />
      <View style={styles.journeyCard}>
        <JourneyStep
          styles={styles}
          number="1"
          title="Review"
          body="The synthetic source text is editable before it leaves the phone."
        />
        <JourneyDivider styles={styles} />
        <JourneyStep
          styles={styles}
          number="2"
          title="Generate"
          body="Choose a tone, optional direction, and exactly one to three options."
        />
        <JourneyDivider styles={styles} />
        <JourneyStep
          styles={styles}
          number="3"
          title="Edit and copy"
          body="Refine the visible result and copy it only with an explicit tap."
        />
      </View>

      <View style={styles.footerActions}>
        <SecondaryButton
          label={opening === 'blank' ? 'Opening Manual Text…' : 'Start with blank text'}
          onPress={() => void openManualText('blank')}
          disabled={opening !== null}
          accessibilityHint="Opens Manual Text without a fixture so you can type your own context."
        />
        <SecondaryButton label="Back to Home" onPress={() => router.replace('/')} />
      </View>
      <Text style={styles.footerNote}>
        No auto-posting. No screenshot upload. No account required.
      </Text>
    </Screen>
  );
}

type RouteStyles = ReturnType<typeof createStyles>;

function JourneyStep({
  styles,
  number,
  title,
  body,
}: {
  styles: RouteStyles;
  number: string;
  title: string;
  body: string;
}) {
  return (
    <View style={styles.journeyRow}>
      <View style={styles.journeyNumber} accessibilityElementsHidden>
        <Text style={styles.journeyNumberText}>{number}</Text>
      </View>
      <View style={styles.journeyCopy}>
        <Text style={styles.journeyTitle}>{title}</Text>
        <Text style={styles.journeyBody}>{body}</Text>
      </View>
    </View>
  );
}

function JourneyDivider({ styles }: { styles: RouteStyles }) {
  return <View style={styles.journeyDivider} />;
}

function createStyles(light: boolean) {
  const colors = light
    ? {
        outline: '#D5DBE6',
        primary: '#6557E8',
        primarySoft: '#F0EEFF',
        surface: '#FFFFFF',
        textPrimary: '#121520',
        textSecondary: '#4F586A',
        textMuted: '#687386',
        warning: '#9A5700',
        warningSurface: '#FFF7E9',
      }
    : {
        outline: '#30384A',
        primary: '#8F83FF',
        primarySoft: '#1E1B34',
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
    fixtureStack: {
      gap: 12,
    },
    messageCard: {
      backgroundColor: colors.warningSurface,
      borderColor: colors.warning,
      borderRadius: 18,
      borderWidth: 1,
      marginTop: 14,
      padding: 16,
    },
    messageTitle: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: '700',
      lineHeight: 21,
    },
    messageBody: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 20,
      marginTop: 4,
    },
    messageAction: {
      marginTop: 12,
    },
    journeyCard: {
      backgroundColor: colors.surface,
      borderColor: colors.outline,
      borderRadius: 22,
      borderWidth: 1,
      padding: 17,
    },
    journeyRow: {
      alignItems: 'flex-start',
      flexDirection: 'row',
      gap: 13,
    },
    journeyNumber: {
      alignItems: 'center',
      backgroundColor: colors.primarySoft,
      borderColor: colors.primary,
      borderRadius: 12,
      borderWidth: 1,
      height: 36,
      justifyContent: 'center',
      width: 36,
    },
    journeyNumberText: {
      color: colors.primary,
      fontSize: 13,
      fontWeight: '800',
    },
    journeyCopy: {
      flex: 1,
      paddingTop: 1,
    },
    journeyTitle: {
      color: colors.textPrimary,
      fontSize: 16,
      fontWeight: '700',
      lineHeight: 22,
    },
    journeyBody: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 20,
      marginTop: 3,
    },
    journeyDivider: {
      backgroundColor: colors.outline,
      height: 18,
      marginBottom: 5,
      marginLeft: 17,
      marginTop: 5,
      width: StyleSheet.hairlineWidth,
    },
    footerActions: {
      gap: 10,
      marginTop: 24,
    },
    footerNote: {
      color: colors.textMuted,
      fontSize: 13,
      lineHeight: 19,
      marginTop: 18,
      textAlign: 'center',
    },
  });
}
