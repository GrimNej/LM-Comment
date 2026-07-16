import LMCommentAndroid, { type DemoConfigurationStatus } from '@lm-comment/android';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';

import { bootstrapDemoConfiguration } from '@/lib/demo-configuration';
import {
  FixtureCard,
  PrivacyBanner,
  Screen,
  SecondaryButton,
  SectionHeading,
  StatusHero,
  TopBar,
  type AppColors,
  useAppTheme,
} from '@/ui';

type DemoFixture = {
  id: 'social' | 'professional' | 'technical';
  eyebrow: string;
  title: string;
  body: string;
  tags: string[];
  sourceText: string;
  accent: 'lime' | 'terracotta' | 'warm';
};

const DEMO_FIXTURES: readonly DemoFixture[] = [
  {
    id: 'social',
    eyebrow: 'Sample 1: Social post',
    title: 'Celebrate a personal win',
    body: '“I finished my first 10K this morning… It rained, but the people cheering made every kilometre worth it.”',
    tags: ['Synthetic', 'Friendly'],
    accent: 'lime',
    sourceText:
      'Synthetic demo post: I finished my first 10K this morning! It rained for most of the route, but the people cheering made every kilometre worth it. Still cannot believe I actually did it.',
  },
  {
    id: 'professional',
    eyebrow: 'Sample 2: Work message',
    title: 'Answer a deadline request',
    body: '“Could you send the revised launch brief by Thursday afternoon, including the dates and open risks?”',
    tags: ['Synthetic', 'Professional'],
    accent: 'terracotta',
    sourceText:
      'Synthetic demo message: Hi, could you send the revised launch brief by Thursday afternoon? Please include the updated rollout dates and the two open risks so I can review everything before Friday morning.',
  },
  {
    id: 'technical',
    eyebrow: 'Sample 3: Technical thread',
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
  const { colors } = useAppTheme();
  const styles = useMemo(() => createStyles(colors), [colors]);
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
      setMessage('Finish setup before generating a reply.');
      return;
    }
    setOpening(id);
    try {
      await LMCommentAndroid.openManualTextWorkflow(sourceText);
    } catch {
      setMessage("The text editor didn't open. Return to Setup and try again.");
    } finally {
      setOpening(null);
    }
  }, [opening, ready]);

  return (
    <Screen scroll contentContainerStyle={styles.screenContent}>
      <TopBar
        title="Samples"
        subtitle="Fictional text to try"
        onBack={() => router.back()}
      />

      <StatusHero
        status={checking ? 'checking' : ready ? 'ready' : relayReady ? 'setup' : 'unavailable'}
        label={ready ? 'Relay ready' : checking ? 'Checking connection' : 'Finish setup'}
        title="Choose a sample"
        body="Each sample opens as editable text. Review it, pick a tone, then tap Generate."
      />

      {message ? (
        <View style={styles.messageCard} accessibilityLiveRegion="polite">
          <Text style={styles.messageTitle}>Finish setup</Text>
          <Text style={styles.messageBody}>{message}</Text>
          <View style={styles.messageAction}>
            <SecondaryButton label="Open setup" onPress={() => router.push('/setup')} />
          </View>
        </View>
      ) : null}

      <PrivacyBanner text="These samples are fictional. Text and generation choices go to the relay after you tap Generate." />

      <SectionHeading
        title="Pick a scenario"
        description="You can edit the sample before you generate anything."
      />
      <View style={styles.fixtureStack}>
        {DEMO_FIXTURES.map((fixture) => (
          <FixtureCard
            key={fixture.id}
            eyebrow={opening === fixture.id ? 'Opening text…' : fixture.eyebrow}
            title={fixture.title}
            body={fixture.body}
            tags={fixture.tags}
            accent={fixture.accent}
            onPress={() => void openManualText(fixture.id, fixture.sourceText)}
          />
        ))}
      </View>

      <SectionHeading
        title="Review, generate, copy"
      />
      <View style={styles.journeyCard}>
        <JourneyStep
          styles={styles}
          number="1"
          title="Review"
          body="Edit the sample before sending it."
        />
        <JourneyDivider styles={styles} />
        <JourneyStep
          styles={styles}
          number="2"
          title="Generate"
          body="Choose a tone, add a note if needed, and request one to three replies."
        />
        <JourneyDivider styles={styles} />
        <JourneyStep
          styles={styles}
          number="3"
          title="Edit and copy"
          body="Edit a reply, then tap Copy."
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
        Nothing is posted from LM-Comment.
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

function createStyles(colors: AppColors) {
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
      borderRadius: 11,
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
      borderRadius: 11,
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
      borderRadius: 7,
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
