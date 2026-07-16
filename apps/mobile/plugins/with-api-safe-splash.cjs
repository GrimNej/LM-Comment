const { withAndroidStyles } = require('expo/config-plugins');

/**
 * Expo SDK 57 writes android:windowSplashScreenBehavior into the base values
 * file even though Android lint requires that API 33 attribute to be version
 * qualified. The property is optional, so omit it and keep the same splash
 * icon/background on every supported API level.
 */
module.exports = function withApiSafeSplash(config) {
  return withAndroidStyles(config, (mod) => {
    const styles = mod.modResults.resources.style ?? [];
    for (const style of styles) {
      if (style.$?.name !== 'Theme.App.SplashScreen') continue;
      style.item = (style.item ?? []).filter(
        (item) => item.$?.name !== 'android:windowSplashScreenBehavior',
      );
    }
    return mod;
  });
};
