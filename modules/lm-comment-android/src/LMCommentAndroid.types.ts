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

export type DemoConfigurationStatus = Omit<DemoConfiguration, 'relayBaseUrl' | 'demoToken'> & {
  relayHostname: string | null;
  isDemoTokenConfigured: boolean;
};
