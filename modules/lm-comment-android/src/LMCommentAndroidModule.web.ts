import { NativeModule, registerWebModule } from 'expo';

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
    return { platform: 'web', nativeWorkflowAvailable: false };
  }
}

export default registerWebModule(LMCommentAndroidWebModule, 'LMCommentAndroid');
