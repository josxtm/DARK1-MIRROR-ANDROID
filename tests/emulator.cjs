const { chromium } = require('playwright');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');

async function run() {
  const assets = path.join(__dirname, '../app/src/main/assets/emu');
  const files = { '/index.html': 'text/html', '/worklet.js': 'text/javascript', '/amb.wasm': 'application/wasm' };
  const server = http.createServer((request, response) => {
    const pathname = new URL(request.url, 'http://127.0.0.1').pathname;
    if (!files[pathname]) return response.writeHead(404).end();
    response.writeHead(200, { 'Content-Type': files[pathname] });
    return response.end(fs.readFileSync(path.join(assets, pathname.slice(1))));
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await chromium.launch({
    ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}),
    args: ['--no-sandbox', '--mute-audio']
  });
  try {
    const page = await browser.newPage({ viewport: { width: 1400, height: 850 }, hasTouch: true });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', item => { if (item.type() === 'error') console.error(item.text()); });
    const origin = `http://127.0.0.1:${server.address().port}`;
    await page.route('**/*', route => route.request().url().startsWith(origin + '/') ? route.continue() : route.abort());
    await page.goto(origin + '/index.html');
    await page.locator('#go').tap();
    try {
      await page.waitForFunction(() => window.ambLast && document.querySelector('#start').hidden);
    } catch (error) {
      throw new Error(await page.locator('#status').textContent(), { cause: error });
    }
    const genre = page.locator('#pr-genres button').nth(1);
    await genre.tap();
    await page.waitForFunction(() => window.ambLast?.g === 1);
    const piece = page.locator('#pr-pieces button').nth(1);
    await piece.tap();
    await page.waitForFunction(() => window.ambLast?.i === 1);
    await page.waitForFunction(() => {
      const histogram = new Set();
      const canvas = document.querySelector('#screen');
      const image = canvas.getContext('2d').getImageData(0, 0, 240, 240).data;
      for (let i = 0; i < image.length; i += 4)
        histogram.add((image[i] << 16) | (image[i + 1] << 8) | image[i + 2]);
      return histogram.size > 20;
    });
    assert.deepEqual(errors, []);
    console.log('DARK-1 WebAssembly: arranque, lienzo RGB 240×240, toque y selección de género y pieza con recursos locales.');
  } finally {
    await browser.close();
    server.close();
  }
}

run().catch(error => { console.error(error); process.exitCode = 1; });
