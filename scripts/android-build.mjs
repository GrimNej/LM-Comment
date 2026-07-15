import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';

const variant = process.argv[2];
if (!['debug', 'release'].includes(variant)) {
  throw new Error('Usage: node scripts/android-build.mjs <debug|release>');
}

const androidDirectory = path.join(process.cwd(), 'apps', 'mobile', 'android');
const wrapper = path.join(androidDirectory, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');
if (!existsSync(wrapper)) {
  throw new Error('Android project missing. Run pnpm mobile:prebuild first.');
}

const javaHome = process.env.JAVA_HOME
  ?? (process.platform === 'win32' ? 'C:\\Program Files\\Android\\Android Studio\\jbr' : undefined);
const androidHome = process.env.ANDROID_HOME
  ?? process.env.ANDROID_SDK_ROOT
  ?? (process.platform === 'win32'
    ? path.join(process.env.LOCALAPPDATA ?? '', 'Android', 'Sdk')
    : undefined);

const task = `:app:assemble${variant[0].toUpperCase()}${variant.slice(1)}`;
const command = process.platform === 'win32' ? 'cmd.exe' : wrapper;
const architecture = process.env.LM_COMMENT_ANDROID_ARCH;
const architectureFlag = architecture ? ` -PreactNativeArchitectures=${architecture}` : '';
const gradleArguments = `${task}${architectureFlag} --console=plain --stacktrace`;
const args = process.platform === 'win32'
  ? ['/d', '/s', '/c', `gradlew.bat ${gradleArguments}`]
  : [task, ...(architecture ? [`-PreactNativeArchitectures=${architecture}`] : []), '--console=plain', '--stacktrace'];
const result = spawnSync(command, args, {
  cwd: androidDirectory,
  env: {
    ...process.env,
    NODE_ENV: variant === 'release' ? 'production' : 'development',
    ...(javaHome ? { JAVA_HOME: javaHome } : {}),
    ...(androidHome ? { ANDROID_HOME: androidHome, ANDROID_SDK_ROOT: androidHome } : {}),
  },
  stdio: 'inherit',
  shell: false,
});
process.exit(result.status ?? 1);
