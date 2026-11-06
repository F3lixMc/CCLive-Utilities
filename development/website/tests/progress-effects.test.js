'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../../backend_server/public');
const data = JSON.parse(fs.readFileSync(path.join(root, 'statue-maxima.json'), 'utf8'));
const meta = JSON.parse(fs.readFileSync(path.join(root, 'leaderboard-meta.json'), 'utf8'));
const { maximum, line } = require('../../../backend_server/public/progress-effects');
assert.equal(Object.keys(data).length, 67);
assert.equal(Object.values(data).filter(x => x.maximum !== null).length, 66);
assert.equal(maximum(data.Schwert), '+562');
assert.equal(maximum(data.Geschwindigkeits), '41,27%');
assert.equal(maximum(data.Kaktusmeister), '1.638.300%');
assert.equal(maximum(data.Amboss), '+3.240');
assert.equal(maximum(data.Spritzen), '+0,02');
assert.equal(maximum(data.Stundenglas), '+0,5');
assert.equal(maximum(data['Kraftgürtel']), '+0,2');
assert.equal(maximum(data.Spitzhacke), '');
for (const entry of Object.values(data)) if (entry.unit === 'percent') assert.ok(!maximum(entry).startsWith('+'));
assert.equal(line(meta.progress, 1, data), 'Karte: Ausweichchance  |  Statue (Wiki-Max.): Schwertschaden +562');
assert.ok(line(meta.progress, 59, data).includes('Statue: Bergbau-Erfahrung'));
assert.ok(!line(meta.progress, 59, data).includes('undefined'));
assert.deepEqual(data['Geschwindigkeits'].wiki_floors, [6,26]);
assert.deepEqual(meta.progress.statuen['Geschwindigkeits'].floor, [6,27]);
console.log('progress-effects tests ok: maxima, percent/additive formatting, alias, missing values and unchanged drop mappings');

const cards = JSON.parse(fs.readFileSync(path.join(root, 'card-maxima.json'), 'utf8'));
const source = fs.readFileSync(path.resolve(__dirname, '../data/karten-wiki-levelwerte.tsv'), 'utf8').replace(/^\uFEFF/, '').trim().split(/\r?\n/).map(row => row.split('\t'));
assert.equal(source.length, 75);
assert.equal(Object.keys(cards).length, 75);
for (const row of source) {
  assert.equal(row.length, 10);
  const name = row[0].replace(/-Karte$/, '');
  const entry = Object.values(cards).find(entry => entry.wiki_name === name);
  assert.ok(entry, name);
  assert.equal(entry.raw_level_5, row[7], name + ': exact level-5 column');
  assert.equal(entry.level, 5);
  assert.equal(entry.prestige, null);
  const display = maximum(entry);
  if (entry.unit === 'percent') { assert.ok(display.endsWith('%')); assert.ok(!display.startsWith('+')); }
  else { assert.ok(display.startsWith('+')); assert.ok(!display.endsWith('%')); }
}
assert.equal(maximum(cards.Kaninchen), '+24');
assert.equal(maximum(cards.Pferd), '42%');
assert.equal(maximum(cards.Kamel), '3.100%');
assert.equal(maximum(cards.Creeper), '+1.200');
assert.equal(maximum(cards['Verwüster Reiter']), '7.900%');
assert.equal(line(meta.progress, 1, data, cards), 'Karte (Lvl. 5, Wiki): Ausweichchance +24  |  Statue (Wiki-Max.): Schwertschaden +562');
console.log('card maxima tests ok: all 75 source rows use level-5 column, mod units and unknown prestige');
