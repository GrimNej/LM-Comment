const baseUrl = process.env.RELAY_BASE_URL;
const token = process.env.DEMO_TOKEN;
if (!baseUrl || !token) {
  throw new Error('RELAY_BASE_URL and DEMO_TOKEN are required for the canary.');
}

const response = await fetch(new URL('/v1/generate', baseUrl), {
  method: 'POST',
  headers: { 'content-type': 'application/json', 'x-demo-token': token },
  body: JSON.stringify({
    sourceText: 'A teammate shipped a thoughtful, focused prototype.',
    tone: 'friendly',
    instruction: 'Celebrate the progress without exaggerating.',
    optionCount: 1,
  }),
});
if (!response.ok) throw new Error(`Canary failed with ${response.status}.`);
console.log(JSON.stringify(await response.json(), null, 2));
