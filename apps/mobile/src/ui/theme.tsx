import {
  AccessibilityInfo,
  useColorScheme,
  useWindowDimensions,
} from 'react-native';
import {
  createContext,
  type PropsWithChildren,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import {
  darkColors,
  lightColors,
  motion,
  radii,
  spacing,
  typography,
  type AppColors,
} from './tokens';

export type AppTheme = {
  colors: AppColors;
  isDark: boolean;
  isReduceMotionEnabled: boolean;
  fontScale: number;
  spacing: typeof spacing;
  radii: typeof radii;
  typography: typeof typography;
  motion: typeof motion;
};

const ThemeContext = createContext<AppTheme | null>(null);

export function AppThemeProvider({ children }: PropsWithChildren) {
  const colorScheme = useColorScheme();
  const { fontScale } = useWindowDimensions();
  const [isReduceMotionEnabled, setReduceMotionEnabled] = useState(false);

  useEffect(() => {
    let isMounted = true;
    void AccessibilityInfo.isReduceMotionEnabled().then((value) => {
      if (isMounted) setReduceMotionEnabled(value);
    });
    const subscription = AccessibilityInfo.addEventListener(
      'reduceMotionChanged',
      setReduceMotionEnabled,
    );
    return () => {
      isMounted = false;
      subscription.remove();
    };
  }, []);

  const theme = useMemo<AppTheme>(
    () => ({
      colors: colorScheme === 'light' ? lightColors : darkColors,
      isDark: colorScheme !== 'light',
      isReduceMotionEnabled,
      fontScale,
      spacing,
      radii,
      typography,
      motion,
    }),
    [colorScheme, fontScale, isReduceMotionEnabled],
  );

  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

export function useAppTheme(): AppTheme {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useAppTheme must be used inside AppThemeProvider');
  return value;
}
