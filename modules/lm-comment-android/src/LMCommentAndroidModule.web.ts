import { NativeModule, registerWebModule } from 'expo';

import type {
  DemoConfiguration,
  DemoDefaults,
  DemoConfigurationStatus,
  SafeDiagnostics,
} from './LMCommentAndroid.types';

const unsupported = (): never => {
  throw new Error('The LM-Comment workflow is available on Android only.');
};

class LMCommentAndroidWebModule extends NativeModule<Record<never, never>> {
  async getReadiness() {
    return {
      contractVersion: 1,
      overlayPermission: 'denied' as const,
      notificationPermission: 'not-required' as const,
      bubbleStatus: 'stopped' as const,
    };
  }

  async getSafeDiagnostics() {
    return {
      platform: 'web',
      appVersion: 'web-preview',
      contractVersion: 1,
      androidApi: null,
      deviceModel: 'Web preview',
      overlayPermission: 'denied',
      notificationPermission: 'not-required',
      bubbleStatus: 'stopped',
      relayHostname: null,
      relayHealth: 'not-configured',
      lastStableErrorCode: null,
      captureResourceCounts: null,
    } satisfies SafeDiagnostics;
  }

  async openOverlayPermissionSettings() { unsupported(); }
  async requestNotificationPermission() { return 'not-required' as const; }
  async startBubble() { unsupported(); }
  async stopBubble() { unsupported(); }
  async configureDemo(_configuration: DemoConfiguration) { unsupported(); }
  async updateDemoDefaults(_defaults: DemoDefaults) { unsupported(); }
  async getDemoConfigurationStatus(): Promise<DemoConfigurationStatus> {
    return {
      relayHostname: null,
      isDemoTokenConfigured: false,
      defaultTone: 'natural',
      optionCount: 3,
      demoMode: true,
    };
  }
  async resetDemoConfiguration() { /* Native private preferences do not exist on web. */ }
  async resetBubblePosition() { /* The overlay bubble does not exist on web. */ }
  async openManualTextWorkflow(_sourceText?: string) { unsupported(); }
}

export default registerWebModule(LMCommentAndroidWebModule, 'LMCommentAndroid');
