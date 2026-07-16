import {
  ActivityIndicator,
  Animated,
  type DimensionValue,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  type StyleProp,
  StyleSheet,
  Text,
  View,
  type ViewStyle,
} from 'react-native';
import {
  type PropsWithChildren,
  type ReactNode,
  useCallback,
  useEffect,
  useState,
} from 'react';
import { SafeAreaView } from 'react-native-safe-area-context';

import { minimumTargetSize } from './tokens';
import { useAppTheme } from './theme';

type ScreenProps = PropsWithChildren<{
  scroll?: boolean;
  contentContainerStyle?: StyleProp<ViewStyle>;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}>;

export function Screen({
  children,
  scroll = true,
  contentContainerStyle,
  style,
  testID,
}: ScreenProps) {
  const { colors } = useAppTheme();
  const content = scroll ? (
    <ScrollView
      contentContainerStyle={[styles.screenContent, contentContainerStyle]}
      keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'}
      keyboardShouldPersistTaps="handled"
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  ) : (
    <View style={[styles.screenContent, styles.nonScrollingContent, contentContainerStyle]}>
      {children}
    </View>
  );

  return (
    <SafeAreaView
      edges={['top', 'bottom']}
      style={[styles.screen, { backgroundColor: colors.canvas }, style]}
      testID={testID}
    >
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.fill}
      >
        {content}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

type MotionPressableProps = PropsWithChildren<{
  onPress?: () => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  accessibilityLabel?: string;
  accessibilityHint?: string;
  accessibilityRole?: 'button' | 'link';
}>;

function MotionPressable({
  children,
  onPress,
  disabled,
  style,
  accessibilityLabel,
  accessibilityHint,
  accessibilityRole = 'button',
}: MotionPressableProps) {
  const { isReduceMotionEnabled, motion } = useAppTheme();
  const [scale] = useState(() => new Animated.Value(1));
  const animate = useCallback(
    (toValue: number) => {
      Animated.timing(scale, {
        toValue,
        duration: isReduceMotionEnabled ? 0 : motion.press,
        useNativeDriver: true,
      }).start();
    },
    [isReduceMotionEnabled, motion.press, scale],
  );

  return (
    <Animated.View style={[style, { transform: [{ scale }] }]}>
      <Pressable
        accessibilityHint={accessibilityHint}
        accessibilityLabel={accessibilityLabel}
        accessibilityRole={accessibilityRole}
        accessibilityState={{ disabled: Boolean(disabled) }}
        disabled={disabled}
        onPress={onPress}
        onPressIn={() => animate(0.985)}
        onPressOut={() => animate(1)}
        style={({ pressed }) => [styles.pressableFill, pressed && styles.pressed]}
      >
        {children}
      </Pressable>
    </Animated.View>
  );
}

export type AppMarkProps = {
  size?: number;
  showWordmark?: boolean;
};

export function AppMark({ size = 54, showWordmark = false }: AppMarkProps) {
  const { colors } = useAppTheme();
  const frameSize = Math.max(size, 36);
  const stroke = Math.max(2, Math.round(frameSize / 25));
  return (
    <View
      accessibilityLabel="LM-Comment"
      accessibilityRole="image"
      style={styles.markRow}
    >
      <View
        style={[
          styles.markFrame,
          {
            borderColor: colors.primary,
            borderRadius: frameSize * 0.3,
            borderBottomRightRadius: frameSize * 0.1,
            borderWidth: stroke,
            height: frameSize,
            width: frameSize,
          },
        ]}
      >
        <View
          style={[
            styles.markLine,
            {
              backgroundColor: colors.secondary,
              height: stroke,
              width: frameSize * 0.42,
            },
          ]}
        />
        <View
          style={[
            styles.markDot,
            {
              backgroundColor: colors.secondary,
              height: frameSize * 0.14,
              width: frameSize * 0.14,
            },
          ]}
        />
      </View>
      {showWordmark ? (
        <Text style={[styles.wordmark, { color: colors.textPrimary }]}>LM-Comment</Text>
      ) : null}
    </View>
  );
}

export type TopBarProps = {
  title?: string;
  subtitle?: string;
  onBack?: () => void;
  right?: ReactNode;
};

export function TopBar({ title, subtitle, onBack, right }: TopBarProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View style={styles.topBar}>
      {onBack ? (
        <MotionPressable
          accessibilityHint="Returns to the previous screen"
          accessibilityLabel="Go back"
          onPress={onBack}
          style={[
            styles.backButton,
            { backgroundColor: colors.surface, borderColor: colors.outline },
          ]}
        >
          <Text style={[styles.backGlyph, { color: colors.textPrimary }]}>‹</Text>
        </MotionPressable>
      ) : null}
      <View style={styles.topBarCopy}>
        {title ? (
          <Text style={[typography.heading, { color: colors.textPrimary }]}>{title}</Text>
        ) : null}
        {subtitle ? (
          <Text style={[typography.small, styles.topBarSubtitle, { color: colors.textMuted }]}>
            {subtitle}
          </Text>
        ) : null}
      </View>
      {right ? <View style={styles.topBarRight}>{right}</View> : null}
    </View>
  );
}

export type SectionHeadingProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
};

export function SectionHeading({ eyebrow, title, description, action }: SectionHeadingProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View style={styles.sectionHeadingRow}>
      <View style={styles.sectionHeadingCopy}>
        {eyebrow ? (
          <Text style={[typography.eyebrow, { color: colors.secondary }]}>{eyebrow.toUpperCase()}</Text>
        ) : null}
        <Text
          style={[
            typography.title,
            eyebrow ? styles.sectionHeadingAfterEyebrow : null,
            { color: colors.textPrimary },
          ]}
        >
          {title}
        </Text>
        {description ? (
          <Text style={[typography.body, styles.sectionDescription, { color: colors.textSecondary }]}>
            {description}
          </Text>
        ) : null}
      </View>
      {action ? <View style={styles.sectionAction}>{action}</View> : null}
    </View>
  );
}

export type StatusTone = 'neutral' | 'primary' | 'cyan' | 'success' | 'warning' | 'danger';

export type StatusChipProps = {
  label: string;
  tone?: StatusTone;
};

export function StatusChip({ label, tone = 'neutral' }: StatusChipProps) {
  const { colors, typography } = useAppTheme();
  const palette = statusPalette(colors, tone);
  return (
    <View
      accessibilityLabel={label}
      style={[styles.statusChip, { backgroundColor: palette.background, borderColor: palette.border }]}
    >
      <View style={[styles.chipDot, { backgroundColor: palette.foreground }]} />
      <Text style={[typography.label, { color: palette.foreground }]}>{label}</Text>
    </View>
  );
}

export type HeroStatus = 'ready' | 'setup' | 'active' | 'unavailable' | 'checking';

export type StatusHeroProps = PropsWithChildren<{
  status: HeroStatus;
  label: string;
  title: string;
  body?: string;
}>;

export function StatusHero({ status, label, title, body, children }: StatusHeroProps) {
  const { colors, typography } = useAppTheme();
  const transitionOpacity = useStatusTransition(status);
  const tone: StatusTone = {
    ready: 'success',
    setup: 'warning',
    active: 'cyan',
    unavailable: 'danger',
    checking: 'neutral',
  }[status] as StatusTone;
  return (
    <Animated.View
      style={[
        styles.hero,
        {
          backgroundColor: colors.surfaceRaised,
          borderColor: colors.outline,
          opacity: transitionOpacity,
          shadowColor: colors.shadow,
        },
      ]}
    >
      <View
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={[styles.heroRail, { backgroundColor: colors.primary }]}
      />
      <View
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={[styles.heroCorner, { borderColor: colors.secondary }]}
      />
      <StatusChip label={label} tone={tone} />
      <Text style={[typography.display, styles.heroTitle, { color: colors.textPrimary }]}>{title}</Text>
      {body ? (
        <Text style={[typography.body, styles.heroBody, { color: colors.textSecondary }]}>{body}</Text>
      ) : null}
      {children ? <View style={styles.heroActions}>{children}</View> : null}
    </Animated.View>
  );
}

type ButtonProps = {
  label: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  accessibilityLabel?: string;
  accessibilityHint?: string;
  style?: StyleProp<ViewStyle>;
};

export type PrimaryButtonProps = ButtonProps;
export type SecondaryButtonProps = ButtonProps;

function ActionButton({
  label,
  onPress,
  loading = false,
  disabled = false,
  accessibilityLabel,
  accessibilityHint,
  style,
  variant,
}: ButtonProps & { variant: 'primary' | 'secondary' }) {
  const { colors, typography } = useAppTheme();
  const isDisabled = disabled || loading;
  const isPrimary = variant === 'primary';
  return (
    <MotionPressable
      accessibilityHint={accessibilityHint}
      accessibilityLabel={accessibilityLabel ?? label}
      disabled={isDisabled}
      onPress={onPress}
      style={[
        styles.actionButton,
        {
          backgroundColor: isPrimary ? colors.primary : colors.surface,
          borderColor: isPrimary ? colors.primary : colors.outline,
          opacity: isDisabled ? 0.52 : 1,
        },
        style,
      ]}
    >
      <View style={styles.buttonContent}>
        {loading ? (
          <ActivityIndicator
            color={isPrimary ? colors.onPrimary : colors.primary}
            size="small"
          />
        ) : null}
        <Text
          style={[
            typography.label,
            styles.buttonLabel,
            { color: isPrimary ? colors.onPrimary : colors.textPrimary },
          ]}
        >
          {loading ? `${label}…` : label}
        </Text>
        {!loading && isPrimary ? (
          <Text accessibilityElementsHidden style={[styles.buttonArrow, { color: colors.onPrimary }]}>→</Text>
        ) : null}
      </View>
    </MotionPressable>
  );
}

export function PrimaryButton(props: PrimaryButtonProps) {
  return <ActionButton {...props} variant="primary" />;
}

export function SecondaryButton(props: SecondaryButtonProps) {
  return <ActionButton {...props} variant="secondary" />;
}

export type ReadinessStatus = 'ready' | 'needed' | 'checking' | 'unavailable';

export type ReadinessCardProps = {
  label: string;
  status: ReadinessStatus;
  detail: string;
  onPress?: () => void;
};

const readinessCopy: Record<ReadinessStatus, { glyph: string; label: string; tone: StatusTone }> = {
  ready: { glyph: '✓', label: 'Ready', tone: 'success' },
  needed: { glyph: '!', label: 'Action needed', tone: 'warning' },
  checking: { glyph: '…', label: 'Checking', tone: 'neutral' },
  unavailable: { glyph: '×', label: 'Unavailable', tone: 'danger' },
};

export function ReadinessCard({ label, status, detail, onPress }: ReadinessCardProps) {
  const { colors, typography } = useAppTheme();
  const transitionOpacity = useStatusTransition(status);
  const copy = readinessCopy[status];
  const palette = statusPalette(colors, copy.tone);
  const content = (
    <Animated.View style={[styles.readinessContent, { opacity: transitionOpacity }]}>
      <View style={[styles.readinessGlyph, { backgroundColor: palette.background }]}>
        <Text style={[styles.readinessGlyphText, { color: palette.foreground }]}>{copy.glyph}</Text>
      </View>
      <View style={styles.readinessCopy}>
        <Text style={[typography.heading, { color: colors.textPrimary }]}>{label}</Text>
        <Text style={[typography.small, styles.readinessDetail, { color: colors.textSecondary }]}>
          {detail}
        </Text>
        <Text style={[typography.label, styles.readinessStatus, { color: palette.foreground }]}>
          {copy.label}
        </Text>
      </View>
      {onPress ? (
        <Text accessibilityElementsHidden style={[styles.rowArrow, { color: colors.textMuted }]}>›</Text>
      ) : null}
    </Animated.View>
  );
  const commonStyle: StyleProp<ViewStyle> = [
    styles.readinessCard,
    { backgroundColor: colors.surface, borderColor: colors.outlineSoft },
  ];

  return onPress ? (
    <MotionPressable
      accessibilityHint="Opens the related setup action"
      accessibilityLabel={`${label}. ${copy.label}. ${detail}`}
      onPress={onPress}
      style={commonStyle}
    >
      {content}
    </MotionPressable>
  ) : (
    <View accessibilityLabel={`${label}. ${copy.label}. ${detail}`} style={commonStyle}>
      {content}
    </View>
  );
}

export type PrivacyBannerProps = {
  text?: string;
};

export function PrivacyBanner({
  text = 'Screenshots stay on this device. Only reviewed text is sent.',
}: PrivacyBannerProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View
      accessibilityLabel={`Privacy. ${text}`}
      style={[
        styles.privacyBanner,
        { backgroundColor: colors.secondarySoft, borderColor: colors.secondary },
      ]}
    >
      <View style={[styles.privacyMark, { borderColor: colors.secondary }]}>
        <View style={[styles.privacyMarkDot, { backgroundColor: colors.secondary }]} />
      </View>
      <View style={styles.privacyCopy}>
        <Text style={[typography.eyebrow, { color: colors.secondary }]}>PRIVATE BY DESIGN</Text>
        <Text style={[typography.small, styles.privacyText, { color: colors.textPrimary }]}>{text}</Text>
      </View>
    </View>
  );
}

export type FixtureCardProps = {
  eyebrow: string;
  title: string;
  body: string;
  tags?: string[];
  onPress: () => void;
  accent?: 'violet' | 'cyan' | 'warm';
};

export function FixtureCard({
  eyebrow,
  title,
  body,
  tags = [],
  onPress,
  accent = 'violet',
}: FixtureCardProps) {
  const { colors, typography } = useAppTheme();
  const accentColor = accent === 'cyan'
    ? colors.secondary
    : accent === 'warm'
      ? colors.warning
      : colors.primary;
  return (
    <MotionPressable
      accessibilityHint="Opens this synthetic example in Manual Text mode"
      accessibilityLabel={`${eyebrow}. ${title}. ${body}`}
      onPress={onPress}
      style={[
        styles.fixtureCard,
        {
          backgroundColor: colors.surface,
          borderColor: colors.outlineSoft,
          shadowColor: colors.shadow,
        },
      ]}
    >
      <View style={[styles.fixtureAccent, { backgroundColor: accentColor }]} />
      <View style={styles.fixtureHeader}>
        <Text style={[typography.eyebrow, styles.fixtureEyebrow, { color: accentColor }]}>
          {eyebrow.toUpperCase()}
        </Text>
        <Text accessibilityElementsHidden style={[styles.rowArrow, { color: colors.textMuted }]}>↗</Text>
      </View>
      <Text style={[typography.heading, styles.fixtureTitle, { color: colors.textPrimary }]}>{title}</Text>
      <Text style={[typography.small, styles.fixtureBody, { color: colors.textSecondary }]}>{body}</Text>
      {tags.length ? (
        <View style={styles.tagRow}>
          {tags.map((tag) => (
            <View key={tag} style={[styles.tag, { backgroundColor: colors.canvas }]}>
              <Text style={[typography.label, { color: colors.textMuted }]}>{tag}</Text>
            </View>
          ))}
        </View>
      ) : null}
    </MotionPressable>
  );
}

export type SettingRowProps = PropsWithChildren<{
  label: string;
  description?: string;
  value?: string;
  onPress?: () => void;
  destructive?: boolean;
  disabled?: boolean;
}>;

export function SettingRow({
  label,
  description,
  value,
  onPress,
  children,
  destructive = false,
  disabled = false,
}: SettingRowProps) {
  const { colors, typography } = useAppTheme();
  const content = (
    <View style={styles.settingInner}>
      <View style={styles.settingTopLine}>
        <View style={styles.settingCopy}>
          <Text
            style={[
              typography.body,
              styles.settingLabel,
              { color: destructive ? colors.danger : colors.textPrimary },
            ]}
          >
            {label}
          </Text>
          {description ? (
            <Text style={[typography.small, styles.settingDescription, { color: colors.textMuted }]}>
              {description}
            </Text>
          ) : null}
        </View>
        {value ? (
          <Text style={[typography.label, styles.settingValue, { color: colors.textSecondary }]}>
            {value}
          </Text>
        ) : null}
        {onPress ? (
          <Text accessibilityElementsHidden style={[styles.rowArrow, { color: colors.textMuted }]}>›</Text>
        ) : null}
      </View>
      {children ? <View style={styles.settingControl}>{children}</View> : null}
    </View>
  );
  const rowStyle: StyleProp<ViewStyle> = [
    styles.settingRow,
    {
      backgroundColor: colors.surface,
      borderColor: destructive ? colors.dangerSoft : colors.outlineSoft,
      opacity: disabled ? 0.5 : 1,
    },
  ];
  return onPress ? (
    <MotionPressable
      accessibilityLabel={value ? `${label}, ${value}` : label}
      disabled={disabled}
      onPress={onPress}
      style={rowStyle}
    >
      {content}
    </MotionPressable>
  ) : (
    <View style={rowStyle}>{content}</View>
  );
}

export type InlineErrorProps = {
  title?: string;
  message: string;
  onRetry?: () => void;
};

export function InlineError({ title = 'Something needs attention', message, onRetry }: InlineErrorProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View
      accessibilityLiveRegion="polite"
      style={[styles.inlineError, { backgroundColor: colors.dangerSoft, borderColor: colors.danger }]}
    >
      <View style={styles.inlineErrorCopy}>
        <Text style={[typography.label, { color: colors.danger }]}>{title}</Text>
        <Text style={[typography.small, styles.inlineErrorMessage, { color: colors.textPrimary }]}>
          {message}
        </Text>
      </View>
      {onRetry ? (
        <Pressable
          accessibilityRole="button"
          hitSlop={8}
          onPress={onRetry}
          style={styles.inlineRetry}
        >
          <Text style={[typography.label, { color: colors.danger }]}>Try again</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

export type EmptyStateProps = {
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ title, message, actionLabel, onAction }: EmptyStateProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View style={[styles.emptyState, { backgroundColor: colors.surface, borderColor: colors.outlineSoft }]}>
      <AppMark size={44} />
      <Text style={[typography.heading, styles.emptyTitle, { color: colors.textPrimary }]}>{title}</Text>
      <Text style={[typography.small, styles.emptyMessage, { color: colors.textSecondary }]}>{message}</Text>
      {actionLabel && onAction ? (
        <SecondaryButton label={actionLabel} onPress={onAction} style={styles.emptyAction} />
      ) : null}
    </View>
  );
}

export type SkeletonProps = {
  width?: DimensionValue;
  height?: number;
  radius?: number;
};

export function Skeleton({ width = '100%', height = 18, radius = 8 }: SkeletonProps) {
  const { colors } = useAppTheme();
  return (
    <View
      accessibilityLabel="Loading"
      style={{
        width,
        height,
        borderRadius: radius,
        backgroundColor: colors.outlineSoft,
        opacity: 0.72,
      }}
    />
  );
}

function statusPalette(colors: ReturnType<typeof useAppTheme>['colors'], tone: StatusTone) {
  switch (tone) {
    case 'primary':
      return { foreground: colors.primary, background: colors.primarySoft, border: colors.primary };
    case 'cyan':
      return { foreground: colors.secondary, background: colors.secondarySoft, border: colors.secondary };
    case 'success':
      return { foreground: colors.success, background: colors.successSoft, border: colors.success };
    case 'warning':
      return { foreground: colors.warning, background: colors.warningSoft, border: colors.warning };
    case 'danger':
      return { foreground: colors.danger, background: colors.dangerSoft, border: colors.danger };
    default:
      return { foreground: colors.textSecondary, background: colors.canvas, border: colors.outline };
  }
}

function useStatusTransition(statusKey: string) {
  const { isReduceMotionEnabled, motion } = useAppTheme();
  const [opacity] = useState(() => new Animated.Value(1));
  useEffect(() => {
    opacity.stopAnimation();
    opacity.setValue(isReduceMotionEnabled ? 1 : 0.78);
    Animated.timing(opacity, {
      toValue: 1,
      duration: isReduceMotionEnabled ? 0 : motion.transition,
      useNativeDriver: true,
    }).start();
  }, [isReduceMotionEnabled, motion.transition, opacity, statusKey]);
  return opacity;
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  screen: { flex: 1 },
  screenContent: {
    flexGrow: 1,
    paddingHorizontal: 20,
    paddingBottom: 32,
  },
  nonScrollingContent: { flex: 1 },
  pressableFill: { flex: 1 },
  pressed: { opacity: 0.9 },
  markRow: { alignItems: 'center', flexDirection: 'row' },
  markFrame: { alignItems: 'center', justifyContent: 'center' },
  markLine: { borderRadius: 3, transform: [{ translateY: -4 }] },
  markDot: { borderRadius: 99, position: 'absolute', bottom: '19%', right: '20%' },
  wordmark: { fontSize: 19, fontWeight: '700', letterSpacing: -0.3, marginLeft: 12 },
  topBar: { alignItems: 'center', flexDirection: 'row', minHeight: 72, paddingVertical: 10 },
  backButton: {
    borderRadius: 14,
    borderWidth: 1,
    height: minimumTargetSize,
    marginRight: 14,
    width: minimumTargetSize,
  },
  backGlyph: { fontSize: 35, fontWeight: '300', lineHeight: 43, textAlign: 'center' },
  topBarCopy: { flex: 1, justifyContent: 'center' },
  topBarSubtitle: { marginTop: 1 },
  topBarRight: { marginLeft: 12, minHeight: minimumTargetSize, justifyContent: 'center' },
  sectionHeadingRow: { alignItems: 'flex-start', flexDirection: 'row' },
  sectionHeadingCopy: { flex: 1 },
  sectionHeadingAfterEyebrow: { marginTop: 7 },
  sectionDescription: { marginTop: 8 },
  sectionAction: { marginLeft: 12, minHeight: minimumTargetSize, justifyContent: 'center' },
  statusChip: {
    alignItems: 'center',
    alignSelf: 'flex-start',
    borderRadius: 999,
    borderWidth: 1,
    flexDirection: 'row',
    minHeight: 32,
    paddingHorizontal: 11,
    paddingVertical: 5,
  },
  chipDot: { borderRadius: 99, height: 7, marginRight: 7, width: 7 },
  hero: {
    borderRadius: 26,
    borderWidth: 1,
    elevation: 6,
    overflow: 'hidden',
    padding: 24,
    shadowOffset: { width: 0, height: 14 },
    shadowOpacity: 0.16,
    shadowRadius: 24,
  },
  heroRail: { bottom: 0, left: 0, position: 'absolute', top: 0, width: 4 },
  heroCorner: {
    borderBottomWidth: 2,
    borderRightWidth: 2,
    height: 52,
    opacity: 0.5,
    position: 'absolute',
    right: 18,
    top: 18,
    width: 52,
  },
  heroTitle: { marginRight: 12, marginTop: 22 },
  heroBody: { marginTop: 13, maxWidth: 560 },
  heroActions: { gap: 12, marginTop: 24 },
  actionButton: {
    borderRadius: 14,
    borderWidth: 1,
    minHeight: minimumTargetSize,
    overflow: 'hidden',
  },
  buttonContent: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    minHeight: minimumTargetSize,
    paddingHorizontal: 18,
    paddingVertical: 12,
  },
  buttonLabel: { fontSize: 15, lineHeight: 21, textAlign: 'center' },
  buttonArrow: { fontSize: 18, marginLeft: 9 },
  readinessCard: { borderRadius: 18, borderWidth: 1, minHeight: 112, overflow: 'hidden' },
  readinessContent: { alignItems: 'flex-start', flexDirection: 'row', padding: 18 },
  readinessGlyph: {
    alignItems: 'center',
    borderRadius: 12,
    height: 38,
    justifyContent: 'center',
    marginRight: 14,
    width: 38,
  },
  readinessGlyphText: { fontSize: 18, fontWeight: '700', lineHeight: 22 },
  readinessCopy: { flex: 1 },
  readinessDetail: { marginTop: 3 },
  readinessStatus: { marginTop: 7 },
  rowArrow: { fontSize: 24, lineHeight: 28, marginLeft: 10 },
  privacyBanner: {
    alignItems: 'flex-start',
    borderLeftWidth: 3,
    borderRadius: 16,
    flexDirection: 'row',
    padding: 16,
  },
  privacyMark: {
    alignItems: 'center',
    borderRadius: 11,
    borderWidth: 2,
    height: 34,
    justifyContent: 'center',
    marginRight: 13,
    width: 34,
  },
  privacyMarkDot: { borderRadius: 99, height: 8, width: 8 },
  privacyCopy: { flex: 1 },
  privacyText: { marginTop: 4 },
  fixtureCard: {
    borderRadius: 20,
    borderWidth: 1,
    elevation: 2,
    minHeight: 184,
    overflow: 'hidden',
    padding: 20,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 18,
  },
  fixtureAccent: { height: 3, left: 20, position: 'absolute', top: 0, width: 48 },
  fixtureHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  fixtureEyebrow: { flex: 1 },
  fixtureTitle: { marginTop: 15 },
  fixtureBody: { marginTop: 8 },
  tagRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 16 },
  tag: { borderRadius: 8, paddingHorizontal: 9, paddingVertical: 5 },
  settingRow: { borderRadius: 16, borderWidth: 1, minHeight: 64, overflow: 'hidden' },
  settingInner: { paddingHorizontal: 16, paddingVertical: 14 },
  settingTopLine: { alignItems: 'center', flexDirection: 'row', minHeight: 36 },
  settingCopy: { flex: 1 },
  settingLabel: { fontWeight: '500' },
  settingDescription: { marginTop: 2 },
  settingValue: { flexShrink: 1, marginLeft: 12, maxWidth: '42%', textAlign: 'right' },
  settingControl: { marginTop: 12 },
  inlineError: {
    alignItems: 'center',
    borderLeftWidth: 3,
    borderRadius: 14,
    flexDirection: 'row',
    padding: 14,
  },
  inlineErrorCopy: { flex: 1 },
  inlineErrorMessage: { marginTop: 2 },
  inlineRetry: { justifyContent: 'center', marginLeft: 12, minHeight: minimumTargetSize },
  emptyState: { alignItems: 'center', borderRadius: 20, borderWidth: 1, padding: 26 },
  emptyTitle: { marginTop: 18, textAlign: 'center' },
  emptyMessage: { marginTop: 7, maxWidth: 360, textAlign: 'center' },
  emptyAction: { alignSelf: 'stretch', marginTop: 20 },
});
