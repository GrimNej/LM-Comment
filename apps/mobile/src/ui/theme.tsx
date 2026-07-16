import {
  AccessibilityInfo,
  Platform,
  useColorScheme,
  useWindowDimensions,
} from 'react-native';
import LMCommentAndroid, { type AppearanceMode } from '@lm-comment/android';
import {
  useCallback,
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
  appearanceMode: AppearanceMode;
  isAppearanceReady: boolean;
  setAppearanceMode: (mode: AppearanceMode) => Promise<void>;
};

const ThemeContext = createContext<AppTheme | null>(null);

export function AppThemeProvider({ children }: PropsWithChildren) {
  const systemColorScheme = useColorScheme();
  const { fontScale } = useWindowDimensions();
  const [isReduceMotionEnabled, setReduceMotionEnabled] = useState(false);
  const [appearanceMode, setAppearanceModeState] = useState<AppearanceMode>('system');
  const [isAppearanceReady, setAppearanceReady] = useState(Platform.OS !== 'android');

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

  useEffect(() => {
    if (Platform.OS !== 'android') return;
    let active = true;
    void LMCommentAndroid.getAppearanceMode().then(
      (mode) => {
        if (active) setAppearanceModeState(mode);
      },
      () => {
        if (active) setAppearanceModeState('system');
      },
    ).finally(() => {
      if (active) setAppearanceReady(true);
    });
    return () => {
      active = false;
    };
  }, []);

  const setAppearanceMode = useCallback(async (mode: AppearanceMode) => {
    const previousMode = appearanceMode;
    setAppearanceModeState(mode);
    try {
      if (Platform.OS === 'android') await LMCommentAndroid.setAppearanceMode(mode);
    } catch (error) {
      setAppearanceModeState(previousMode);
      throw error;
    }
  }, [appearanceMode]);

  const isDark = appearanceMode === 'dark'
    || (appearanceMode === 'system' && systemColorScheme !== 'light');

  const theme = useMemo<AppTheme>(
    () => ({
      colors: isDark ? darkColors : lightColors,
      isDark,
      isReduceMotionEnabled,
      fontScale,
      spacing,
      radii,
      typography,
      motion,
      appearanceMode,
      isAppearanceReady,
      setAppearanceMode,
    }),
    [appearanceMode, fontScale, isAppearanceReady, isDark, isReduceMotionEnabled, setAppearanceMode],
  );

  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}

export function useAppTheme(): AppTheme {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useAppTheme must be used inside AppThemeProvider');
  return value;
}
