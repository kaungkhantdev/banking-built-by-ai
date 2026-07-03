// Render any HTML file to PDF with a page-number footer.
// Usage: node render_one.js <input.html> <output.pdf> "<footer left text>"
// Self-discovers chrome-headless-shell (honors CHROME_BIN).
const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');

function discoverChrome() {
  if (process.env.CHROME_BIN && fs.existsSync(process.env.CHROME_BIN)) return process.env.CHROME_BIN;
  const base = path.join(process.env.HOME, '.cache/puppeteer');
  for (const name of ['chrome-headless-shell', 'chrome']) {
    try {
      const out = execSync(`find "${base}" -type f -name '${name}' 2>/dev/null | sort -V | tail -1`).toString().trim();
      if (out && fs.existsSync(out)) return out;
    } catch (_) {}
  }
  throw new Error('No chrome-headless-shell found under ~/.cache/puppeteer');
}

const input = path.resolve(process.argv[2]);
const output = path.resolve(process.argv[3]);
const footerLeft = process.argv[4] || 'Document';

(async () => {
  const browser = await puppeteer.launch({
    executablePath: discoverChrome(), headless: true,
    args: ['--no-sandbox', '--disable-gpu'],
  });
  const page = await browser.newPage();
  await page.goto('file://' + input, { waitUntil: 'networkidle0' });
  await page.pdf({
    path: output, format: 'A4', printBackground: true,
    margin: { top: '18mm', bottom: '16mm', left: '16mm', right: '16mm' },
    displayHeaderFooter: true, headerTemplate: '<div></div>',
    footerTemplate: `
      <div style="width:100%; font-size:8px; color:#b0b6c0; letter-spacing:0.8px;
        font-family:'DMSans',-apple-system,Arial,sans-serif; padding:0 18mm;
        display:flex; justify-content:space-between; text-transform:uppercase;">
        <span>${footerLeft}</span>
        <span><span class="pageNumber"></span> / <span class="totalPages"></span></span>
      </div>`,
  });
  await browser.close();
  console.log('wrote', output);
})().catch(e => { console.error(e); process.exit(1); });
