import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const commands = [
  ['check:scope', ['pnpm', 'check:scope']],
  ['check:naming', ['pnpm', 'check:naming']],
  ['check:secrets', ['pnpm', 'check:secrets']],
  ['typecheck', ['pnpm', 'typecheck']],
  ['test', ['pnpm', 'test']],
  ['relay:build', ['pnpm', 'relay:build']],
];
const results = [];
for (const [name, [command, ...args]] of commands) {
  const startedAt = new Date();
  try {
    const output = execFileSync(command, args, { encoding: 'utf8', timeout: 180_000 });
    results.push({ name, status: 'PASS', startedAt, output: output.trim() });
  } catch (error) {
    results.push({
      name,
      status: 'FAIL',
      startedAt,
      output: `${error.stdout ?? ''}${error.stderr ?? ''}`.trim(),
    });
  }
}
const directory = path.join(process.cwd(), 'artifacts', 'evidence');
mkdirSync(directory, { recursive: true });
const stamp = new Date().toISOString().replaceAll(':', '-');
const target = path.join(directory, `quality-${stamp}.json`);
writeFileSync(target, `${JSON.stringify({ createdAt: new Date(), results }, null, 2)}\n`);
console.log(target);
if (results.some((result) => result.status === 'FAIL')) process.exitCode = 1;
