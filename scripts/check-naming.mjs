import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const ignoredDirectories = new Set(['.git', 'node_modules', 'build', 'dist', '.expo', 'android', 'ios', 'artifacts']);
const allowedFiles = new Set([
  'LM_COMMENT_FINAL_HACKATHON_IMPLEMENTATION_BLUEPRINT.md',
  'hackathon-release-contract.yaml',
  path.join('docs', 'LEGACY_AUDIT.md'),
  path.join('scripts', 'check-naming.mjs'),
]);
const forbidden = ['Peek-A-Comment', 'Peek-a-Prompt', 'peekacomment', 'lmcommentacomment', 'com.lmcommentacomment'];
const textExtensions = new Set(['.json', '.yaml', '.yml', '.md', '.ts', '.tsx', '.js', '.mjs', '.cjs', '.kt', '.kts', '.xml', '.gradle', '.properties']);
const failures = [];

function walk(directory) {
  for (const entry of readdirSync(directory)) {
    if (ignoredDirectories.has(entry)) continue;
    const absolute = path.join(directory, entry);
    const relative = path.relative(root, absolute);
    if (statSync(absolute).isDirectory()) {
      walk(absolute);
      continue;
    }
    if (allowedFiles.has(relative) || !textExtensions.has(path.extname(entry))) continue;
    const content = readFileSync(absolute, 'utf8');
    for (const value of forbidden) {
      if (content.includes(value)) failures.push(`${relative}: forbidden legacy name ${value}`);
    }
  }
}

walk(root);
if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}
console.log('Naming contract passed.');
