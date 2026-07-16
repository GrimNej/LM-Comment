import type { TextStyle } from 'react-native';

/**
 * LM-Comment uses an editorial palette: paper, printing ink, one signal color,
 * and one restrained annotation color. Accent fills and readable accent text
 * are separate tokens so contrast does not depend on color alone.
 */
export const darkColors = {
  canvas: '#090B10',
  surface: '#141822',
  surfaceRaised: '#1B2030',
  inkSurface: '#0E121A',
  onInk: '#F5F7FB',
  onInkMuted: '#B7BFCE',
  textPrimary: '#F5F7FB',
  textSecondary: '#B7BFCE',
  textMuted: '#8D97A8',
  primary: '#8F83FF',
  onPrimary: '#090B10',
  secondary: '#50D7C5',
  onSecondary: '#090B10',
  signal: '#8F83FF',
  signalInk: '#090B10',
  terracotta: '#50D7C5',
  success: '#49D99A',
  warning: '#FFB85C',
  danger: '#FF7182',
  outline: '#30384A',
  outlineSoft: '#22293A',
  primarySoft: '#292646',
  secondarySoft: '#173B3B',
  successSoft: '#16382D',
  warningSoft: '#3D2E1D',
  dangerSoft: '#3F222D',
  warningSurface: '#3D2E1D',
  dangerSurface: '#3F222D',
  scrim: 'rgba(3, 5, 9, 0.72)',
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
