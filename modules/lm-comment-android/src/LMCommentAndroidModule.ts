import { NativeModule, requireNativeModule } from 'expo';

import type {
  DemoConfiguration,
  DemoConfigurationStatus,
  NativeReadiness,
} from './LMCommentAndroid.types';

declare class LMCommentAndroidModule extends NativeModule<Record<never, never>> {
  getReadiness(): Promise<NativeReadiness>;
  openOverlayPermissionSettings(): Promise<void>;
  requestNotificationPermission(): Promise<NativeReadiness['notificationPermission']>;
  startBubble(): Promise<void>;
  stopBubble(): Promise<void>;
  configureDemo(config: DemoConfiguration): Promise<void>;
  getDemoConfigurationStatus(): Promise<DemoConfigurationStatus>;
  resetDemoConfiguration(): Promise<void>;
  resetBubblePosition(): Promise<void>;
  openManualTextWorkflow(sourceText?: string): Promise<void>;
  getSafeDiagnostics(): Promise<Record<string, unknown>>;
}

export default requireNativeModule<LMCommentAndroidModule>('LMCommentAndroid');
