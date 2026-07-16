import type { TextStyle } from 'react-native';

/**
 * LM-Comment uses an editorial palette: paper, printing ink, one signal color,
 * and one restrained annotation color. Accent fills and readable accent text
 * are separate tokens so contrast does not depend on color alone.
 */
export const darkColors = {
  canvas: '#0D100E',
  surface: '#151915',
  surfaceRaised: '#1C211C',
  inkSurface: '#181D18',
  onInk: '#F4F0E6',
  onInkMuted: '#C5C8BE',
  textPrimary: '#F4F0E6',
  textSecondary: '#C5C8BE',
  textMuted: '#92998F',
  primary: '#B9E84A',
  onPrimary: '#101411',
  secondary: '#FF9A7D',
  onSecondary: '#101411',
  signal: '#B9E84A',
  signalInk: '#101411',
  terracotta: '#E96D4C',
  success: '#9BD59F',
  warning: '#F0B66D',
  danger: '#FF8B78',
  outline: '#444A43',
  outlineSoft: '#2C322C',
  primarySoft: '#293713',
  secondarySoft: '#3A251E',
  successSoft: '#1F3523',
  warningSoft: '#382B1B',
  dangerSoft: '#3B221E',
  warningSurface: '#382B1B',
  dangerSurface: '#3B221E',
  scrim: 'rgba(4, 6, 4, 0.78)',
  shadow: '#000000',
} as const;

export const lightColors = {
  canvas: '#F4F0E6',
  surface: '#FAF7EE',
  surfaceRaised: '#FFFDF7',
  inkSurface: '#101411',
  onInk: '#F4F0E6',
  onInkMuted: '#C9CBC1',
  textPrimary: '#101411',
  textSecondary: '#40473F',
  textMuted: '#646C63',
  primary: '#465D0B',
  onPrimary: '#101411',
  secondary: '#A6422D',
  onSecondary: '#FFFDF7',
  signal: '#B9E84A',
  signalInk: '#101411',
  terracotta: '#E96D4C',
  success: '#2F6B3A',
  warning: '#84500F',
  danger: '#A53529',
  outline: '#B8B7AB',
  outlineSoft: '#D9D5C8',
  primarySoft: '#E5EBCF',
  secondarySoft: '#F3DDD4',
  successSoft: '#DFEBDD',
  warningSoft: '#F2E4C9',
  dangerSoft: '#F2DCD5',
  warningSurface: '#F2E4C9',
  dangerSurface: '#F2DCD5',
  scrim: 'rgba(16, 20, 17, 0.48)',
  shadow: '#101411',
} as const;

export type AppColors = {
  [Key in keyof typeof darkColors]: string;
};

export const spacing = {
  xxs: 4,
  xs: 8,
  sm: 12,
  md: 16,
  lg: 20,
  xl: 24,
  xxl: 32,
  hero: 40,
  section: 48,
} as const;

export const radii = {
  sm: 4,
  md: 7,
  lg: 11,
  xl: 15,
  round: 999,
} as const;

export const typography = {
  display: {
    fontSize: 30,
    lineHeight: 36,
    fontWeight: '700',
    letterSpacing: -0.65,
  } satisfies TextStyle,
  title: {
    fontSize: 22,
    lineHeight: 28,
    fontWeight: '700',
    letterSpacing: -0.3,
  } satisfies TextStyle,
  heading: {
    fontSize: 17,
    lineHeight: 23,
    fontWeight: '600',
    letterSpacing: -0.08,
  } satisfies TextStyle,
  body: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
  } satisfies TextStyle,
  small: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '400',
  } satisfies TextStyle,
  label: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '600',
    letterSpacing: 0,
  } satisfies TextStyle,
  eyebrow: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '600',
    letterSpacing: 0.12,
  } satisfies TextStyle,
} as const;

export const motion = {
  press: 110,
  transition: 180,
} as const;

export const minimumTargetSize = 48;
