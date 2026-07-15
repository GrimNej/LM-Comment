import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const expected = {
  node: '22.13.x',
  pnpm: '10.34.5',
  androidPlatform: 'android-36',
};

function command(command, args = []) {
  try {
    return execFileSync(command, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  } catch {
    return null;
  }
}

const home = os.homedir();
const androidHome = process.env.ANDROID_HOME
  ?? process.env.ANDROID_SDK_ROOT
  ?? path.join(home, 'AppData', 'Local', 'Android', 'Sdk');
const studioJdk = path.join('C:', 'Program Files', 'Android', 'Android Studio', 'jbr');
const javaHome = process.env.JAVA_HOME ?? (existsSync(studioJdk) ? studioJdk : null);
const checks = [
  ['Node', process.version, process.version.startsWith('v22.13.')],
  ['pnpm', command('pnpm', ['--version']) ?? 'missing', command('pnpm', ['--version']) === expected.pnpm],
  ['Git', command('git', ['--version']) ?? 'missing', Boolean(command('git', ['--version']))],
  ['Java home', javaHome ?? 'missing', Boolean(javaHome && existsSync(javaHome))],
  ['Android SDK', androidHome, existsSync(androidHome)],
  ['Android API 36', path.join(androidHome, 'platforms', expected.androidPlatform), existsSync(path.join(androidHome, 'platforms', expected.androidPlatform))],
  ['ADB', command('adb', ['version'])?.split(/\r?\n/)[0] ?? 'missing', Boolean(command('adb', ['version']))],
  ['Docker', command('docker', ['--version']) ?? 'missing', Boolean(command('docker', ['--version']))],
];

console.log('LM-Comment environment doctor\n');
for (const [name, value, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'WARN'}  ${name.padEnd(16)} ${value}`);
}
console.log(`\nPinned build runtime: Node ${expected.node}, pnpm ${expected.pnpm}.`);
console.log('Set JAVA_HOME to Android Studio/jbr before running Gradle on Windows.');

// Environment drift is reported rather than failing: the pinned runtime is used in CI,
// while newer local Node versions are often useful for repository maintenance.
process.exitCode = checks.some(([, , ok]) => !ok) ? 0 : 0;
