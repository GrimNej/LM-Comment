import LMCommentAndroid, {
  type DemoConfigurationStatus,
  type NativeReadiness,
} from '@lm-comment/android';
import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { AppState, Platform } from 'react-native';

import {
  bootstrapDemoConfiguration,
  type DemoBootstrapState,
} from '@/lib/demo-configuration';

export type AppBootstrapPhase = 'booting' | 'ready' | 'setup-needed' | 'unavailable';
export type StableAppErrorCode = 'NATIVE_UNAVAILABLE' | null;

export type AppReadinessValue = {
  phase: AppBootstrapPhase;
  bootstrapResult: DemoBootstrapState | null;
  readiness: NativeReadiness | null;
  demoConfiguration: DemoConfigurationStatus | null;
  readyForJudgeActions: boolean;
  isRefreshing: boolean;
  lastErrorCode: StableAppErrorCode;
  refresh: () => Promise<void>;
  ensureJudgeReady: () => Promise<boolean>;
};

type Snapshot = Pick<
  AppReadinessValue,
  'phase' | 'readiness' | 'demoConfiguration' | 'lastErrorCode'
>;

const initialSnapshot: Snapshot = {
  phase: 'booting',
  readiness: null,
  demoConfiguration: null,
  lastErrorCode: null,
};

const AppReadinessContext = createContext<AppReadinessValue | null>(null);

export function AppReadinessProvider({ children }: PropsWithChildren) {
  const [snapshot, setSnapshot] = useState<Snapshot>(initialSnapshot);
  const [bootstrapResult, setBootstrapResult] = useState<DemoBootstrapState | null>(null);
  const [isRefreshing, setRefreshing] = useState(false);
  const snapshotRef = useRef(snapshot);
  const mountedRef = useRef(true);
  const bootstrapFinishedRef = useRef(false);
  const inFlightRef = useRef<Promise<void> | null>(null);

  const publish = useCallback((next: Snapshot) => {
    snapshotRef.current = next;
    if (mountedRef.current) setSnapshot(next);
  }, []);

  const probe = useCallback(async () => {
    if (Platform.OS !== 'android') {
      publish({
        phase: 'unavailable',
        readiness: null,
        demoConfiguration: null,
        lastErrorCode: 'NATIVE_UNAVAILABLE',
      });
      return;
    }

    try {
      if (!bootstrapFinishedRef.current) {
        const result = await bootstrapDemoConfiguration();
        bootstrapFinishedRef.current = true;
        if (mountedRef.current) setBootstrapResult(result);
      }

      const [readiness, demoConfiguration] = await Promise.all([
        LMCommentAndroid.getReadiness(),
        LMCommentAndroid.getDemoConfigurationStatus(),
      ]);
      const isConfigured = Boolean(
        demoConfiguration.relayHostname && demoConfiguration.isDemoTokenConfigured,
      );
      publish({
        phase: isConfigured ? 'ready' : 'setup-needed',
        readiness,
        demoConfiguration,
        lastErrorCode: null,
      });
    } catch {
      bootstrapFinishedRef.current = true;
      publish({
        phase: 'unavailable',
        readiness: null,
        demoConfiguration: null,
        lastErrorCode: 'NATIVE_UNAVAILABLE',
      });
    }
  }, [publish]);

  const refresh = useCallback((): Promise<void> => {
    if (inFlightRef.current) return inFlightRef.current;
    if (mountedRef.current) setRefreshing(true);
    const request = probe().finally(() => {
      inFlightRef.current = null;
      if (mountedRef.current) setRefreshing(false);
    });
    inFlightRef.current = request;
    return request;
  }, [probe]);

  const ensureJudgeReady = useCallback(async () => {
    await refresh();
    return snapshotRef.current.phase === 'ready';
  }, [refresh]);

  useEffect(() => {
    mountedRef.current = true;
    void refresh();
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') void refresh();
    });
    return () => {
      mountedRef.current = false;
      subscription.remove();
    };
  }, [refresh]);

  const value = useMemo<AppReadinessValue>(
    () => ({
      ...snapshot,
      bootstrapResult,
      readyForJudgeActions: snapshot.phase === 'ready',
      isRefreshing,
      refresh,
      ensureJudgeReady,
    }),
    [bootstrapResult, ensureJudgeReady, isRefreshing, refresh, snapshot],
  );

  return <AppReadinessContext.Provider value={value}>{children}</AppReadinessContext.Provider>;
}

export function useAppReadiness(): AppReadinessValue {
  const value = useContext(AppReadinessContext);
  if (!value) {
    throw new Error('useAppReadiness must be used inside AppReadinessProvider');
  }
  return value;
}
