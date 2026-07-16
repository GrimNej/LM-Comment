import { readFileSync } from 'node:fs';
import path from 'node:path';

const file = path.join(process.cwd(), 'demo', 'quality-set.json');
const document = JSON.parse(readFileSync(file, 'utf8'));
const tones = ['natural', 'professional', 'friendly', 'witty', 'concise'];
const failures = [];

if (document.schemaVersion !== 1) failures.push('schemaVersion must be 1');
if (document.evaluationStatus !== 'unscored') failures.push('quality set must remain unscored until human review');
if (!Array.isArray(document.scenarios) || document.scenarios.length !== 30) {
  failures.push('quality set must contain exactly 30 scenarios');
}

const ids = new Set();
const counts = Object.fromEntries(tones.map((tone) => [tone, 0]));
for (const [index, scenario] of (document.scenarios ?? []).entries()) {
  const label = scenario.id ?? `scenario ${index + 1}`;
  if (typeof scenario.id !== 'string' || !/^[a-z]+-\d{2}$/.test(scenario.id)) failures.push(`${label}: invalid id`);
  if (ids.has(scenario.id)) failures.push(`${label}: duplicate id`);
  ids.add(scenario.id);
  if (!tones.includes(scenario.tone)) failures.push(`${label}: invalid tone`);
  else counts[scenario.tone] += 1;
  if (!Number.isInteger(scenario.optionCount) || scenario.optionCount < 1 || scenario.optionCount > 3) {
    failures.push(`${label}: optionCount must be 1..3`);
  }
  if (typeof scenario.sourceText !== 'string' || scenario.sourceText.length < 20 || scenario.sourceText.length > 8_000) {
    failures.push(`${label}: sourceText length is invalid`);
  }
  if (typeof scenario.instruction !== 'string' || scenario.instruction.length < 5 || scenario.instruction.length > 500) {
    failures.push(`${label}: instruction length is invalid`);
  }
  if (!Array.isArray(scenario.mustReference) || scenario.mustReference.length < 1
    || scenario.mustReference.some((item) => typeof item !== 'string' || !scenario.sourceText.toLowerCase().includes(item.toLowerCase()))) {
    failures.push(`${label}: mustReference must contain source-grounded phrases`);
  }
  if (typeof scenario.mustNotInvent !== 'string' || scenario.mustNotInvent.length < 10) {
    failures.push(`${label}: mustNotInvent is required`);
  }
}

for (const tone of tones) {
  if (counts[tone] !== 6) failures.push(`${tone}: expected 6 scenarios, found ${counts[tone]}`);
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log(`Quality set valid: ${document.scenarios.length} scenarios, 6 per tone.`);
