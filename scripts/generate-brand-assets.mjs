import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const brandRoot = path.join(repoRoot, 'apps', 'mobile', 'assets', 'brand');
const imageRoot = path.join(repoRoot, 'apps', 'mobile', 'assets', 'images');
const require = createRequire(import.meta.url);
const Jimp = require('jimp-compact');

const browserCandidates = process.platform === 'win32'
  ? [
      process.env.CHROME_PATH,
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
      'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
      'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    ]
  : [process.env.CHROME_PATH, 'google-chrome', 'chromium', 'chromium-browser', 'microsoft-edge'];

function findBrowser() {
  for (const candidate of browserCandidates.filter(Boolean)) {
    if (path.isAbsolute(candidate) && !existsSync(candidate)) continue;
    const probe = spawnSync(candidate, ['--version'], { encoding: 'utf8' });
    if (!probe.error && probe.status === 0) return candidate;
  }
  throw new Error('Chrome or Edge is required. Set CHROME_PATH to its executable.');
}

const browser = findBrowser();
mkdirSync(imageRoot, { recursive: true });

function assertPng(file, width, height) {
  const png = readFileSync(file);
  const signature = '89504e470d0a1a0a';
  if (png.subarray(0, 8).toString('hex') !== signature) {
    throw new Error(`${path.basename(file)} is not a PNG.`);
  }
  const actualWidth = png.readUInt32BE(16);
  const actualHeight = png.readUInt32BE(20);
  if (actualWidth !== width || actualHeight !== height) {
    throw new Error(`${path.basename(file)} is ${actualWidth}x${actualHeight}, expected ${width}x${height}.`);
  }
}

async function render(source, destination, width, height) {
  const input = source.startsWith('data:') ? source : pathToFileURL(source).href;
  const output = path.join(imageRoot, destination);
  // Chromium enforces a small minimum viewport on some platforms. Render at
  // least 512 px, then downsample so favicon and monochrome exports stay whole.
  const renderWidth = Math.max(width, 512);
  const renderHeight = Math.max(height, 512);
  const temporaryDirectory = mkdtempSync(path.join(tmpdir(), 'lm-comment-brand-'));
  const temporaryOutput = path.join(temporaryDirectory, 'render.png');
  try {
    const result = spawnSync(
      browser,
      [
        '--headless=new',
        '--disable-gpu',
        '--hide-scrollbars',
        '--force-device-scale-factor=1',
        '--default-background-color=00000000',
        `--window-size=${renderWidth},${renderHeight}`,
        `--screenshot=${temporaryOutput}`,
        input,
      ],
      { encoding: 'utf8' },
    );
    if (result.error || result.status !== 0) {
      throw result.error ?? new Error(result.stderr || `Could not render ${destination}.`);
    }
    if (renderWidth === width && renderHeight === height) {
      copyFileSync(temporaryOutput, output);
    } else {
      const image = await Jimp.read(temporaryOutput);
      image.resize(width, height, Jimp.RESIZE_BICUBIC);
      await image.writeAsync(output);
    }
  } finally {
    rmSync(temporaryDirectory, { force: true, recursive: true });
  }
  assertPng(output, width, height);
}

const icon = path.join(brandRoot, 'lm-comment-icon.svg');
const foreground = path.join(brandRoot, 'lm-comment-foreground.svg');
const monochrome = path.join(brandRoot, 'lm-comment-monochrome.svg');
const inkBackground = `data:image/svg+xml,${encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%" viewBox="0 0 512 512"><rect width="512" height="512" fill="#101411"/></svg>',
)}`;

await render(icon, 'icon.png', 1024, 1024);
await render(icon, 'favicon.png', 96, 96);
await render(foreground, 'android-icon-foreground.png', 512, 512);
copyFileSync(
  path.join(imageRoot, 'android-icon-foreground.png'),
  path.join(imageRoot, 'splash-icon.png'),
);
assertPng(path.join(imageRoot, 'splash-icon.png'), 512, 512);
await render(monochrome, 'android-icon-monochrome.png', 432, 432);
await render(inkBackground, 'android-icon-background.png', 512, 512);

console.log('Generated LM-Comment launcher, adaptive, monochrome, splash, and favicon assets.');
