'use strict';
const assert = require('assert');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const blocked = require(path.join(__dirname, '../../../backend_server/website_backend/blocked-glyphs'));

assert.strictEqual(blocked.containsBlocked('Hallo äöüß 123'), false);
assert.strictEqual(blocked.containsBlocked('ѳ'), true);
assert.strictEqual(blocked.containsBlocked('start Ѳ'), true);
assert.strictEqual(blocked.containsBlocked('mit ѳ te'), true);
assert.strictEqual(blocked.containsBlocked('ende ѳ'), true);
assert.strictEqual(blocked.containsBlocked('😀'), false);
assert.strictEqual(blocked.parseUPlus('U+1F000'), 0x1F000);
assert.ok(blocked.snapshot().has(0x0473));
assert.ok(blocked.snapshot().has(0x0472));
const json = blocked.toJson();
assert.strictEqual(json.error_code, 'blocked_glyph');
assert.ok(json.codepoints.includes('U+0473'));

const tmp = path.join(os.tmpdir(), 'blocked-glyphs-test.json');
fs.writeFileSync(tmp, JSON.stringify({ codepoints: ['U+E001'] }));
assert.strictEqual(blocked.loadFromFile(tmp), true);
assert.strictEqual(blocked.containsBlocked(String.fromCodePoint(0xE001)), true);
assert.strictEqual(blocked.containsBlocked('Hallo'), false);
assert.strictEqual(blocked.loadFromFile(path.join(os.tmpdir(), 'missing-blocked-glyphs.json')), false);
assert.strictEqual(blocked.containsBlocked(String.fromCodePoint(0xE001)), true);
fs.unlinkSync(tmp);
console.log('blocked-glyphs tests ok');
