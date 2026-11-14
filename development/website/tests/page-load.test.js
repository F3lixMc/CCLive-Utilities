'use strict';
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const page = require('../../../backend_server/public/page-load.js');

assert.strictEqual(page.profileStartsImmediately(true, 'Ada', '8'), true);
assert.strictEqual(page.profileStartsImmediately(false, 'Ada', '8'), false);
assert.strictEqual(page.profileStartsImmediately(true, '', '8'), false);
assert.strictEqual(page.profileStartsImmediately(true, 'Ada', ''), false);
assert.strictEqual(page.selectionCurrent('Ada', '8', 'Ada', '8'), true);
assert.strictEqual(page.selectionCurrent('Ada', '8', 'Bo', '8'), false);
assert.strictEqual(page.selectionCurrent('Ada', '8', 'Ada', '9'), false);
assert.strictEqual(page.playerAfterList(['Bo', 'Ada'], 'Ada', 'Bo'), 'Ada');
assert.strictEqual(page.playerAfterList(['Bo', 'Ada'], 'Gone', 'Bo'), 'Bo');
assert.strictEqual(page.playerAfterList(['Bo', 'Ada'], 'Gone', 'Missing'), 'Bo');
assert.strictEqual(page.playerAfterList([], 'Ada', 'Ada'), '');
assert.deepStrictEqual(page.sharedRanks(['2000', '2000', '1999']), [1, 1, 3]);
const started = { tab: 'leaderboards', season: '8', board: 'factory_level' };
assert.strictEqual(page.boardCurrent(started, { tab: 'leaderboards', season: '8', board: 'factory_level' }), true);
assert.strictEqual(page.boardCurrent(started, { tab: 'spieler-statistiken', season: '8', board: 'factory_level' }), false);
assert.strictEqual(page.boardCurrent(started, { tab: 'leaderboards', season: '9', board: 'factory_level' }), false);
assert.strictEqual(page.boardCurrent(started, { tab: 'leaderboards', season: '8', board: 'playtime' }), false);

const root = path.resolve(__dirname, '../../../backend_server/public');
const html = fs.readFileSync(path.join(root, 'main_website.html'), 'utf8');
const select = html.slice(html.indexOf('id="webdata-lb-type">'), html.indexOf('</select>', html.indexOf('id="webdata-lb-type">')));
assert.ok(select.includes('<option value="kills">Alltime Kills</option>'));
assert.ok(select.indexOf('value="kills"') < select.indexOf('value="factory"'));
assert.ok(select.includes('<option value="factory">Fabrik Level</option>'));
const data = fs.readFileSync(path.join(root, 'main_website-data.js'), 'utf8');
assert.ok(data.includes("factory: { key: 'factory_level', title: 'Fabrik Level', label: 'Level' }"));
assert.ok(data.includes('rankTask = loadOwnRank'));
assert.ok(data.includes("['Fabrik Level', data.factory_level]"));
assert.ok(data.includes('const progressTask = loadProgress'));
assert.ok(!data.includes('await loadProgress();'));
assert.ok(data.includes('profileStartsImmediately'));

console.log('page-load tests ok');
