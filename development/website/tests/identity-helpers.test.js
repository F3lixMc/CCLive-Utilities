'use strict';
const assert = require('assert');
const {
  normalizeUuid,
  normalizeNameKey,
  isValidLoginName,
  sha256Hex,
  timingSafeEqualHex
} = require('../../../backend_server/website_backend/crypto-util');

assert.strictEqual(normalizeUuid('069a79f444e14626a5b0bac67538bf5a'), '069a79f4-44e1-4626-a5b0-bac67538bf5a');
assert.strictEqual(normalizeUuid('069a79f4-44e1-4626-a5b0-bac67538bf5a'), '069a79f4-44e1-4626-a5b0-bac67538bf5a');
assert.strictEqual(normalizeUuid('nope'), null);
assert.strictEqual(normalizeNameKey('Steve'), 'steve');
assert.ok(isValidLoginName('Steve_1'));
assert.ok(!isValidLoginName('ab'));
assert.ok(timingSafeEqualHex(sha256Hex('a'), sha256Hex('a')));
assert.ok(!timingSafeEqualHex(sha256Hex('a'), sha256Hex('b')));
console.log('identity-helpers tests ok');
