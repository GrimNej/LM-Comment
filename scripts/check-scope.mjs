import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const failures = [];
const requiredScripts = [
  'doctor', 'lint', 'typecheck', 'test', 'mobile:prebuild',
  'mobile:android:debug', 'mobile:android:release', 'relay:build',
];
const rootPackage = JSON.parse(readFileSync(path.join(root, 'package.json'), 'utf8'));
for (const script of requiredScripts) {
  if (!rootPackage.scripts?.[script]) failures.push(`Missing root script: ${script}`);
}

const manifests = [];
function collectManifests(directory) {
  for (const entry of readdirSync(directory)) {
    if (['.git', 'node_modules', 'build', 'dist', '.expo', 'android', 'ios', 'artifacts'].includes(entry)) continue;
    const absolute = path.join(directory, entry);
    if (statSync(absolute).isDirectory()) collectManifests(absolute);
    else if (entry === 'package.json') manifests.push(absolute);
  }
}
collectManifests(root);

const forbiddenPackages = [
  'redux', '@reduxjs/toolkit', 'auth0', '@auth0', 'firebase', 'supabase',
  'prisma', 'typeorm', 'sequelize', 'mongoose', 'redis', 'ioredis',
];
for (const manifest of manifests) {
  const json = JSON.parse(readFileSync(manifest, 'utf8'));
  const deps = { ...json.dependencies, ...json.devDependencies };
  for (const name of Object.keys(deps)) {
    if (forbiddenPackages.some((forbidden) => name === forbidden || name.startsWith(`${forbidden}/`))) {
      failures.push(`${path.relative(root, manifest)} includes excluded package ${name}`);
    }
  }
}

const nativeManifest = path.join(root, 'modules', 'lm-comment-android', 'android', 'src', 'main', 'AndroidManifest.xml');
if (existsSync(nativeManifest)) {
  const xml = readFileSync(nativeManifest, 'utf8');
  const expected = [
    'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
    'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION',
    'android:foregroundServiceType="specialUse"',
    'android:foregroundServiceType="mediaProjection"',
    'android:noHistory="false"',
  ];
  for (const fragment of expected) {
    if (!xml.includes(fragment)) failures.push(`Native manifest missing ${fragment}`);
  }
  const forbiddenPermissions = ['BIND_ACCESSIBILITY_SERVICE', 'READ_EXTERNAL_STORAGE', 'WRITE_EXTERNAL_STORAGE', 'MANAGE_EXTERNAL_STORAGE'];
  for (const permission of forbiddenPermissions) {
    if (xml.includes(permission)) failures.push(`Native manifest includes forbidden permission ${permission}`);
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}
console.log('Frozen hackathon scope passed.');
