import { NativeModule, requireNativeModule } from 'expo';

import type {
  DemoConfiguration,
  DemoDefaults,
  DemoConfigurationStatus,
  NativeReadiness,
  SafeDiagnostics,
} from './LMCommentAndroid.types';

declare class LMCommentAndroidModule extends NativeModule<Record<never, never>> {
  getReadiness(): Promise<NativeReadiness>;
  openOverlayPermissionSettings(): Promise<void>;
  requestNotificationPermission(): Promise<NativeReadiness['notificationPermission']>;
  startBubble(): Promise<void>;
  stopBubble(): Promise<void>;
  configureDemo(config: DemoConfiguration): Promise<void>;
  updateDemoDefaults(defaults: DemoDefaults): Promise<void>;
  getDemoConfigurationStatus(): Promise<DemoConfigurationStatus>;
  resetDemoConfiguration(): Promise<void>;
  resetBubblePosition(): Promise<void>;
  openManualTextWorkflow(sourceText?: string): Promise<void>;
  getSafeDiagnostics(): Promise<SafeDiagnostics>;
}

export default requireNativeModule<LMCommentAndroidModule>('LMCommentAndroid');
