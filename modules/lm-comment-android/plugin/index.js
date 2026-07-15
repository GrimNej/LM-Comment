const { withAndroidManifest } = require('expo/config-plugins');

const permissions = [
  'android.permission.SYSTEM_ALERT_WINDOW',
  'android.permission.FOREGROUND_SERVICE',
  'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
  'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION',
  'android.permission.POST_NOTIFICATIONS',
  'android.permission.INTERNET',
];

function upsertByName(items, item) {
  const name = item.$['android:name'];
  const index = items.findIndex((entry) => entry?.$?.['android:name'] === name);
  if (index >= 0) items[index] = { ...items[index], ...item };
  else items.push(item);
}

module.exports = function withLMCommentAndroid(config) {
  return withAndroidManifest(config, (mod) => {
    const manifest = mod.modResults.manifest;
    manifest['uses-permission'] = manifest['uses-permission'] ?? [];
    for (const name of permissions) {
      const found = manifest['uses-permission'].some(
        (entry) => entry?.$?.['android:name'] === name,
      );
      if (!found) manifest['uses-permission'].push({ $: { 'android:name': name } });
    }

    const application = manifest.application?.[0];
    if (!application) throw new Error('LM-Comment requires an Android application node.');
    application.activity = application.activity ?? [];
    application.service = application.service ?? [];
    upsertByName(application.activity, {
      $: {
        'android:name': 'com.grimnej.lmcomment.workflow.CaptureWorkflowActivity',
        'android:exported': 'false',
        'android:excludeFromRecents': 'true',
        'android:noHistory': 'false',
        'android:taskAffinity': '',
        'android:launchMode': 'singleTop',
        'android:theme': '@style/Theme.LMComment.CaptureWorkflow',
      },
    });
    upsertByName(application.service, {
      $: {
        'android:name': 'com.grimnej.lmcomment.bubble.BubbleOverlayService',
        'android:exported': 'false',
        'android:foregroundServiceType': 'specialUse',
      },
      property: [{
        $: {
          'android:name': 'android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE',
          'android:value': 'User-started floating LM-Comment writing assistant bubble',
        },
      }],
    });
    upsertByName(application.service, {
      $: {
        'android:name': 'com.grimnej.lmcomment.capture.OneShotCaptureService',
        'android:exported': 'false',
        'android:stopWithTask': 'true',
        'android:foregroundServiceType': 'mediaProjection',
      },
    });
    return mod;
  });
};
