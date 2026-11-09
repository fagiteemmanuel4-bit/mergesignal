const fs = require('fs');
const vm = require('vm');

const html = fs.readFileSync('index.html', 'utf8');
const blocks = [...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)]
  .map(match => match[1])
  .filter(source => source.trim());

if (!blocks.length) throw new Error('No inline application script was found');
blocks.forEach((source, index) => new vm.Script(source, { filename: 'index.html:inline-script-' + (index + 1) }));

for (const file of ['package.json', 'capacitor.config.json', 'manifest.webmanifest']) {
  JSON.parse(fs.readFileSync(file, 'utf8'));
}

console.log('App JavaScript syntax and JSON configuration checks passed.');
