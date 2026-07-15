// Reexport the native module. On web, it will be resolved to LMCommentAndroidModule.web.ts
// and on native platforms to LMCommentAndroidModule.ts
export { default } from './LMCommentAndroidModule';
export * from './LMCommentAndroid.types';
