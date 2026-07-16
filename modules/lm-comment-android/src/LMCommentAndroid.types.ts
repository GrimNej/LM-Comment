export type BubbleStatus = 'stopped' | 'starting' | 'running' | 'stopping' | 'error';

export type Tone = 'natural' | 'professional' | 'friendly' | 'witty' | 'concise';

export type NativeReadiness = {
  contractVersion: number;
  overlayPermission: 'granted' | 'denied';
  notificationPermission: 'granted' | 'denied' | 'not-required';
  bubbleStatus: BubbleStatus;
};

export type DemoConfiguration = {
  relayBaseUrl: string;
  demoToken: string;
  defaultTone: Tone;
  optionCount: 1 | 2 | 3;
  demoMode: boolean;
};

export type DemoDefaults = Pick<
  DemoConfiguration,
  'defaultTone' | 'optionCount' | 'demoMode'
>;

export type DemoConfigurationStatus = Omit<DemoConfiguration, 'relayBaseUrl' | 'demoToken'> & {
  relayHostname: string | null;
  isDemoTokenConfigured: boolean;
};

export type RelayHealthStatus = 'not-configured' | 'healthy' | 'degraded' | 'unavailable';

export type CaptureResourceCounts = {
  activeCaptureService: number;
  activeProjection: number;
  activeVirtualDisplay: number;
  activeImageReader: number;
  activeImage: number;
  activeWorkflowBitmap: number;
};

/**
 * An intentionally allowlisted diagnostics payload. It must never be expanded
 * with credentials, source text, generated text, requests, or image data.
 */
export type SafeDiagnostics = {
  platform: 'android' | 'web';
  appVersion: string;
  contractVersion: number;
  androidApi: number | null;
  deviceModel: string;
  overlayPermission: NativeReadiness['overlayPermission'];
  notificationPermission: NativeReadiness['notificationPermission'];
  bubbleStatus: BubbleStatus;
  relayHostname: string | null;
  relayHealth: RelayHealthStatus;
  lastStableErrorCode: string | null;
  captureResourceCounts: CaptureResourceCounts | null;
};
