import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const ignoredDirectories = new Set(['.git', 'node_modules', 'build', 'dist', '.expo', 'android', 'ios', 'artifacts']);
const ignoredFiles = new Set(['LM_COMMENT_FINAL_HACKATHON_IMPLEMENTATION_BLUEPRINT.md']);
const secretPatterns = [
  { label: 'Groq API key', pattern: /\bgsk_[A-Za-z0-9]{20,}\b/g },
  { label: 'Private key', pattern: /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/g },
];
const failures = [];

function walk(directory) {
  for (const entry of readdirSync(directory)) {
    if (ignoredDirectories.has(entry)) continue;
    if (entry === '.env' || (entry.startsWith('.env.') && entry !== '.env.example')) continue;
    const absolute = path.join(directory, entry);
    if (statSync(absolute).isDirectory()) {
      walk(absolute);
      continue;
    }
    if (ignoredFiles.has(path.relative(root, absolute))) continue;
    if (statSync(absolute).size > 2_000_000) continue;
    let content;
    try {
      content = readFileSync(absolute, 'utf8');
    } catch {
      continue;
    }
    for (const { label, pattern } of secretPatterns) {
      pattern.lastIndex = 0;
      if (pattern.test(content)) failures.push(`${path.relative(root, absolute)}: ${label}`);
    }
  }
}

walk(root);
if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}
console.log('Secret canary scan passed.');
