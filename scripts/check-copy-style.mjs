import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const failures = [];
const files = [
  path.join(root, 'README.md'),
  path.join(root, 'demo', 'fixtures.json'),
];
const sourceRoots = [
  path.join(root, 'apps', 'mobile', 'src'),
  path.join(root, 'modules', 'lm-comment-android', 'android', 'src', 'main', 'java'),
];
const extensions = new Set(['.ts', '.tsx', '.kt']);

function collect(directory) {
  if (!existsSync(directory)) return;
  for (const entry of readdirSync(directory)) {
    const absolute = path.join(directory, entry);
    if (statSync(absolute).isDirectory()) collect(absolute);
    else if (extensions.has(path.extname(entry))) files.push(absolute);
  }
}

sourceRoots.forEach(collect);

const repositoryTextExtensions = new Set([
  '.cjs', '.js', '.json', '.kt', '.md', '.mjs', '.ts', '.tsx', '.yaml', '.yml',
]);
const repositoryTextFiles = [];

function collectRepositoryText(directory) {
  if (!existsSync(directory)) return;
  for (const entry of readdirSync(directory)) {
    const absolute = path.join(directory, entry);
    if (statSync(absolute).isDirectory()) collectRepositoryText(absolute);
    else if (repositoryTextExtensions.has(path.extname(entry))) repositoryTextFiles.push(absolute);
  }
}

for (const entry of readdirSync(root)) {
  const absolute = path.join(root, entry);
  if (statSync(absolute).isFile() && repositoryTextExtensions.has(path.extname(entry))) {
    repositoryTextFiles.push(absolute);
  }
}
[
  path.join(root, 'apps', 'mobile', 'src'),
  path.join(root, 'contracts'),
  path.join(root, 'demo'),
  path.join(root, 'docs'),
  path.join(root, 'modules', 'lm-comment-android', 'android', 'src'),
  path.join(root, 'scripts'),
].forEach(collectRepositoryText);

const cannedPhrases = [
  /\bjudge-ready\b/i,
  /\bprivate by design\b/i,
  /\breal workflow\b/i,
  /\bsystem check\b/i,
  /\bnative-first\b/i,
  /\bseamless(?:ly)?\b/i,
  /\beffortless(?:ly)?\b/i,
  /\bgame-changing\b/i,
  /\bmoreover\b/i,
  /\bdelve\b/i,
];

const quotedAllCaps = /(['"`])([A-Z][A-Z0-9]*(?:[ ·][A-Z0-9][A-Z0-9 ·…-]*)+)\1/g;

for (const absolute of [...new Set(repositoryTextFiles)]) {
  if (!existsSync(absolute)) continue;
  const relative = path.relative(root, absolute);
  const lines = readFileSync(absolute, 'utf8').split(/\r?\n/);
  lines.forEach((line, index) => {
    if (line.includes('\u2014')) failures.push(`${relative}:${index + 1}: em dash is not allowed in product copy`);
  });
}

for (const absolute of [...new Set(files)]) {
  if (!existsSync(absolute)) continue;
  const relative = path.relative(root, absolute);
  const lines = readFileSync(absolute, 'utf8').split(/\r?\n/);
  lines.forEach((line, index) => {
    for (const pattern of cannedPhrases) {
      if (pattern.test(line)) failures.push(`${relative}:${index + 1}: canned product phrase ${pattern}`);
    }
    for (const match of line.matchAll(quotedAllCaps)) {
      if (match[2].includes('_') || match[2].includes('.')) continue;
      failures.push(`${relative}:${index + 1}: use sentence case instead of "${match[2]}"`);
    }
  });
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('Product copy style passed.');
