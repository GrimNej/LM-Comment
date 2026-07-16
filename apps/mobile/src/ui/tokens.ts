import type { TextStyle } from 'react-native';

export const darkColors = {
  canvas: '#090B10',
  surface: '#141822',
  surfaceRaised: '#1B2030',
  textPrimary: '#F5F7FB',
  textSecondary: '#B7BFCE',
  textMuted: '#8D97A8',
  primary: '#8F83FF',
  onPrimary: '#090B10',
  secondary: '#50D7C5',
  onSecondary: '#090B10',
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
  scrim: 'rgba(3, 5, 9, 0.72)',
  shadow: '#000000',
} as const;

export const lightColors = {
  canvas: '#F6F7FB',
  surface: '#FFFFFF',
  surfaceRaised: '#FFFFFF',
  textPrimary: '#121520',
  textSecondary: '#4F586A',
  textMuted: '#687386',
  primary: '#6557E8',
  onPrimary: '#FFFFFF',
  secondary: '#087F73',
  onSecondary: '#FFFFFF',
  success: '#087A51',
  warning: '#9A5700',
  danger: '#B72942',
  outline: '#D5DBE6',
  outlineSoft: '#E8EBF2',
  primarySoft: '#ECE9FF',
  secondarySoft: '#E0F4F0',
  successSoft: '#E1F4EC',
  warningSoft: '#FFF0DC',
  dangerSoft: '#FFE7EB',
  scrim: 'rgba(18, 21, 32, 0.42)',
  shadow: '#182033',
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
  sm: 10,
  md: 14,
  lg: 20,
  xl: 26,
  round: 999,
} as const;

export const typography = {
  display: {
    fontSize: 32,
    lineHeight: 38,
    fontWeight: '600',
    letterSpacing: -0.7,
  } satisfies TextStyle,
  title: {
    fontSize: 24,
    lineHeight: 30,
    fontWeight: '600',
    letterSpacing: -0.35,
  } satisfies TextStyle,
  heading: {
    fontSize: 18,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: -0.12,
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
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '600',
    letterSpacing: 0.18,
  } satisfies TextStyle,
  eyebrow: {
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '700',
    letterSpacing: 1.35,
  } satisfies TextStyle,
} as const;

export const motion = {
  press: 140,
  transition: 220,
} as const;

export const minimumTargetSize = 48;
