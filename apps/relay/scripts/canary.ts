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
const body: unknown = await response.json();
if (!body || typeof body !== 'object') throw new Error('Canary returned an invalid body.');
const options = Reflect.get(body, 'options');
if (!Array.isArray(options) || options.length !== 1) {
  throw new Error('Canary returned the wrong option count.');
}
const option = options[0];
if (!option || typeof option !== 'object'
  || typeof Reflect.get(option, 'id') !== 'string'
  || typeof Reflect.get(option, 'text') !== 'string'
  || Reflect.get(option, 'text').length < 1
  || Reflect.get(option, 'text').length > 700) {
  throw new Error('Canary returned an invalid option.');
}
console.log('Relay canary passed with one valid option; content intentionally omitted.');
