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
        onPressIn={() => animate(0.992)}
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

/** Four crop corners and a signal point form the product mark. */
export function AppMark({ size = 54, showWordmark = false }: AppMarkProps) {
  const { colors } = useAppTheme();
  const frameSize = Math.max(size, 36);
  const stroke = Math.max(2, Math.round(frameSize / 24));
  const cornerSize = frameSize * 0.28;
  const cornerColor = colors.textPrimary;
  const sharedCorner = {
    borderColor: cornerColor,
    height: cornerSize,
    width: cornerSize,
  };

  return (
    <View accessibilityLabel="LM-Comment" accessibilityRole="image" style={styles.markRow}>
      <View style={[styles.markFrame, { height: frameSize, width: frameSize }]}>
        <View
          style={[
            styles.markCorner,
            styles.markTopLeft,
            sharedCorner,
            { borderLeftWidth: stroke, borderTopWidth: stroke },
          ]}
        />
        <View
          style={[
            styles.markCorner,
            styles.markTopRight,
            sharedCorner,
            { borderRightWidth: stroke, borderTopWidth: stroke },
          ]}
        />
        <View
          style={[
            styles.markCorner,
            styles.markBottomLeft,
            sharedCorner,
            { borderBottomWidth: stroke, borderLeftWidth: stroke },
          ]}
        />
        <View
          style={[
            styles.markCorner,
            styles.markBottomRight,
            sharedCorner,
            { borderBottomWidth: stroke, borderRightWidth: stroke },
          ]}
        />
        <View
          style={[
            styles.markSignal,
            {
              backgroundColor: colors.signal,
              height: frameSize * 0.18,
              left: frameSize * 0.41,
              top: frameSize * 0.41,
              width: frameSize * 0.18,
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
          <Text
            maxFontSizeMultiplier={1.35}
            style={[styles.backGlyph, { color: colors.textPrimary }]}
          >
            ←
          </Text>
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
          <View style={styles.eyebrowRow}>
            <View style={[styles.eyebrowRule, { backgroundColor: colors.terracotta }]} />
            <Text style={[typography.eyebrow, { color: colors.textMuted }]}>{eyebrow}</Text>
          </View>
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

export type StatusTone = 'neutral' | 'primary' | 'secondary' | 'success' | 'warning' | 'danger';

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
      <Text style={[typography.label, styles.statusLabel, { color: palette.foreground }]}>{label}</Text>
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
    active: 'primary',
    unavailable: 'danger',
    checking: 'neutral',
  }[status] as StatusTone;

  return (
    <Animated.View
      style={[
        styles.hero,
        {
          backgroundColor: colors.inkSurface,
          borderColor: colors.outline,
          opacity: transitionOpacity,
        },
      ]}
    >
      <View
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={[styles.heroRule, { backgroundColor: colors.signal }]}
      />
      <View
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={[styles.heroMarker, { backgroundColor: colors.terracotta }]}
      />
      <StatusChip label={label} tone={tone} />
      <Text style={[typography.display, styles.heroTitle, { color: colors.onInk }]}>{title}</Text>
      {body ? (
        <Text style={[typography.body, styles.heroBody, { color: colors.onInkMuted }]}>{body}</Text>
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
  const foreground = isPrimary ? colors.signalInk : colors.textPrimary;

  return (
    <MotionPressable
      accessibilityHint={accessibilityHint}
      accessibilityLabel={accessibilityLabel ?? label}
      disabled={isDisabled}
      onPress={onPress}
      style={[
        styles.actionButton,
        {
          backgroundColor: isPrimary ? colors.signal : colors.surface,
          borderColor: isPrimary ? colors.signal : colors.outline,
          opacity: isDisabled ? 0.5 : 1,
        },
        style,
      ]}
    >
      <View style={styles.buttonContent}>
        {loading ? <ActivityIndicator color={foreground} size="small" /> : null}
        <Text style={[typography.label, styles.buttonLabel, { color: foreground }]}>
          {loading ? `${label}...` : label}
        </Text>
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
      <View
        style={[
          styles.readinessGlyph,
          { backgroundColor: palette.background, borderColor: palette.border },
        ]}
      >
        <Text
          maxFontSizeMultiplier={1.25}
          style={[styles.readinessGlyphText, { color: palette.foreground }]}
        >
          {copy.glyph}
        </Text>
      </View>
      <View style={styles.readinessCopy}>
        <View style={styles.readinessTitleRow}>
          <Text style={[typography.heading, styles.readinessTitle, { color: colors.textPrimary }]}>
            {label}
          </Text>
          <Text style={[typography.label, styles.readinessStatus, { color: palette.foreground }]}>
            {copy.label}
          </Text>
        </View>
        <Text style={[typography.small, styles.readinessDetail, { color: colors.textSecondary }]}>
          {detail}
        </Text>
      </View>
      {onPress ? (
        <Text
          accessibilityElementsHidden
          maxFontSizeMultiplier={1.25}
          style={[styles.rowArrow, { color: colors.textMuted }]}
        >
          ›
        </Text>
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
  text = 'The screenshot is never sent. Reviewed text and generation choices go to the relay.',
}: PrivacyBannerProps) {
  const { colors, typography } = useAppTheme();
  return (
    <View
      accessibilityLabel={`Privacy. ${text}`}
      style={[
        styles.privacyBanner,
        { backgroundColor: colors.surface, borderColor: colors.outlineSoft },
      ]}
    >
      <View style={[styles.privacyRule, { backgroundColor: colors.signal }]} />
      <View style={styles.privacyCopy}>
        <Text style={[typography.label, { color: colors.textPrimary }]}>Privacy note</Text>
        <Text style={[typography.small, styles.privacyText, { color: colors.textSecondary }]}>
          {text}
        </Text>
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
  accent?: 'lime' | 'terracotta' | 'warm';
};

export function FixtureCard({
  eyebrow,
  title,
  body,
  tags = [],
  onPress,
  accent = 'lime',
}: FixtureCardProps) {
  const { colors, typography } = useAppTheme();
  const accentColor = accent === 'terracotta'
    ? colors.terracotta
    : accent === 'warm'
      ? colors.warning
      : colors.signal;
  const accentTextColor = accent === 'terracotta'
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
        { backgroundColor: colors.surface, borderColor: colors.outlineSoft },
      ]}
    >
      <View style={[styles.fixtureAccent, { backgroundColor: accentColor }]} />
      <View style={styles.fixtureHeader}>
        <Text style={[typography.eyebrow, styles.fixtureEyebrow, { color: accentTextColor }]}>
          {eyebrow}
        </Text>
        <Text
          accessibilityElementsHidden
          maxFontSizeMultiplier={1.25}
          style={[styles.rowArrow, { color: colors.textMuted }]}
        >
          ↗
        </Text>
      </View>
      <Text style={[typography.heading, styles.fixtureTitle, { color: colors.textPrimary }]}>{title}</Text>
      <Text style={[typography.small, styles.fixtureBody, { color: colors.textSecondary }]}>{body}</Text>
      {tags.length ? (
        <View style={styles.tagRow}>
          {tags.map((tag) => (
            <View
              key={tag}
              style={[styles.tag, { backgroundColor: colors.canvas, borderColor: colors.outlineSoft }]}
            >
              <Text style={[typography.small, styles.tagText, { color: colors.textMuted }]}>{tag}</Text>
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
          <Text
            accessibilityElementsHidden
            maxFontSizeMultiplier={1.25}
            style={[styles.rowArrow, { color: colors.textMuted }]}
          >
            ›
          </Text>
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
      <AppMark size={38} />
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

export function Skeleton({ width = '100%', height = 18, radius = 4 }: SkeletonProps) {
  const { colors } = useAppTheme();
  return (
    <View
      accessibilityLabel="Loading"
      style={{
        width,
        height,
        borderRadius: radius,
        backgroundColor: colors.outlineSoft,
        opacity: 0.78,
      }}
    />
  );
}

function statusPalette(colors: ReturnType<typeof useAppTheme>['colors'], tone: StatusTone) {
  switch (tone) {
    case 'primary':
      return { foreground: colors.primary, background: colors.primarySoft, border: colors.primary };
    case 'secondary':
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
    opacity.setValue(isReduceMotionEnabled ? 1 : 0.82);
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
    paddingBottom: 32,
    paddingHorizontal: 18,
  },
  nonScrollingContent: { flex: 1 },
  pressableFill: { flex: 1 },
  pressed: { opacity: 0.82 },
  markRow: { alignItems: 'center', flexDirection: 'row' },
  markFrame: { position: 'relative' },
  markCorner: { position: 'absolute' },
  markTopLeft: { left: 0, top: 0 },
  markTopRight: { right: 0, top: 0 },
  markBottomLeft: { bottom: 0, left: 0 },
  markBottomRight: { bottom: 0, right: 0 },
  markSignal: {
    borderRadius: 999,
    position: 'absolute',
  },
  wordmark: { fontSize: 19, fontWeight: '800', letterSpacing: -0.45, marginLeft: 12 },
  topBar: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    minHeight: 70,
    paddingVertical: 11,
  },
  backButton: {
    borderRadius: 6,
    borderWidth: 1,
    height: minimumTargetSize,
    marginRight: 14,
    width: minimumTargetSize,
  },
  backGlyph: { fontSize: 23, fontWeight: '500', lineHeight: 46, textAlign: 'center' },
  topBarCopy: { flex: 1, justifyContent: 'center', minHeight: minimumTargetSize },
  topBarSubtitle: { marginTop: 2 },
  topBarRight: { justifyContent: 'center', marginLeft: 12, minHeight: minimumTargetSize },
  sectionHeadingRow: { alignItems: 'flex-start', flexDirection: 'row' },
  sectionHeadingCopy: { flex: 1 },
  eyebrowRow: { alignItems: 'center', flexDirection: 'row' },
  eyebrowRule: { height: 2, marginRight: 8, width: 18 },
  sectionHeadingAfterEyebrow: { marginTop: 6 },
  sectionDescription: { marginTop: 7, maxWidth: 600 },
  sectionAction: { justifyContent: 'center', marginLeft: 12, minHeight: minimumTargetSize },
  statusChip: {
    alignItems: 'center',
    alignSelf: 'flex-start',
    borderRadius: 4,
    borderWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    minHeight: 30,
    paddingHorizontal: 9,
    paddingVertical: 4,
  },
  chipDot: { borderRadius: 999, height: 6, marginRight: 7, width: 6 },
  statusLabel: { fontSize: 13, lineHeight: 18 },
  hero: {
    borderRadius: 11,
    borderWidth: 1,
    overflow: 'hidden',
    paddingBottom: 23,
    paddingHorizontal: 22,
    paddingTop: 25,
  },
  heroRule: { height: 4, left: 0, position: 'absolute', top: 0, width: '38%' },
  heroMarker: { height: 10, position: 'absolute', right: 18, top: 18, width: 10 },
  heroTitle: { marginRight: 14, marginTop: 20, maxWidth: 620 },
  heroBody: { marginTop: 11, maxWidth: 560 },
  heroActions: { gap: 10, marginTop: 22 },
  actionButton: {
    borderRadius: 7,
    borderWidth: 1,
    minHeight: minimumTargetSize,
    overflow: 'hidden',
  },
  buttonContent: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    minHeight: minimumTargetSize,
    paddingHorizontal: 17,
    paddingVertical: 11,
  },
  buttonLabel: { fontSize: 15, lineHeight: 21, textAlign: 'center' },
  readinessCard: { borderRadius: 7, borderWidth: 1, minHeight: 92, overflow: 'hidden' },
  readinessContent: { alignItems: 'flex-start', flexDirection: 'row', padding: 15 },
  readinessGlyph: {
    alignItems: 'center',
    borderRadius: 4,
    borderWidth: StyleSheet.hairlineWidth,
    height: 34,
    justifyContent: 'center',
    marginRight: 13,
    width: 34,
  },
  readinessGlyphText: { fontSize: 16, fontWeight: '700', lineHeight: 20 },
  readinessCopy: { flex: 1 },
  readinessTitleRow: { alignItems: 'flex-start', flexDirection: 'row', flexWrap: 'wrap' },
  readinessTitle: { flexGrow: 1, flexShrink: 1, marginRight: 10 },
  readinessDetail: { marginTop: 4 },
  readinessStatus: { fontSize: 13, lineHeight: 18 },
  rowArrow: { fontSize: 22, lineHeight: 26, marginLeft: 10 },
  privacyBanner: {
    alignItems: 'stretch',
    borderRadius: 7,
    borderWidth: 1,
    flexDirection: 'row',
    overflow: 'hidden',
  },
  privacyRule: { width: 4 },
  privacyCopy: { flex: 1, paddingHorizontal: 14, paddingVertical: 13 },
  privacyText: { marginTop: 3 },
  fixtureCard: {
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 174,
    overflow: 'hidden',
    padding: 18,
  },
  fixtureAccent: { bottom: 18, left: 0, position: 'absolute', top: 18, width: 3 },
  fixtureHeader: { alignItems: 'flex-start', flexDirection: 'row', justifyContent: 'space-between' },
  fixtureEyebrow: { flex: 1 },
  fixtureTitle: { marginTop: 12 },
  fixtureBody: { marginTop: 7 },
  tagRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 15 },
  tag: {
    borderRadius: 3,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  tagText: { fontSize: 13, lineHeight: 18 },
  settingRow: { borderRadius: 6, borderWidth: 1, minHeight: 64, overflow: 'hidden' },
  settingInner: { paddingHorizontal: 15, paddingVertical: 13 },
  settingTopLine: { alignItems: 'center', flexDirection: 'row', minHeight: 38 },
  settingCopy: { flex: 1 },
  settingLabel: { fontWeight: '600' },
  settingDescription: { marginTop: 3 },
  settingValue: { flexShrink: 1, marginLeft: 12, maxWidth: '42%', textAlign: 'right' },
  settingControl: { marginTop: 12 },
  inlineError: {
    alignItems: 'center',
    borderLeftWidth: 3,
    borderRadius: 5,
    flexDirection: 'row',
    padding: 14,
  },
  inlineErrorCopy: { flex: 1 },
  inlineErrorMessage: { marginTop: 3 },
  inlineRetry: { justifyContent: 'center', marginLeft: 12, minHeight: minimumTargetSize },
  emptyState: {
    alignItems: 'flex-start',
    borderRadius: 8,
    borderWidth: 1,
    padding: 22,
  },
  emptyTitle: { marginTop: 17 },
  emptyMessage: { marginTop: 6, maxWidth: 380 },
  emptyAction: { alignSelf: 'stretch', marginTop: 19 },
});
