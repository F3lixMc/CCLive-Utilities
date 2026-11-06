'use strict';
const assert = require('assert');
const { describeError, looksConfidential } = require('../../../backend_server/website_backend/safe-log');

assert.ok(looksConfidential('AccessToken leaked'));
const dup = describeError({ code: 'ER_DUP_ENTRY', errno: 1062, sqlState: '23000', message: "Duplicate entry 'Steve' for key 'player'" });
assert.strictEqual(dup.code, 'ER_DUP_ENTRY');
assert.strictEqual(dup.errno, 1062);
assert.ok(!String(dup.message).toLowerCase().includes('token'));

const secret = describeError({ code: 'ER_PARSE_ERROR', message: 'bad token abcdef1234567890' });
assert.strictEqual(secret.message, 'Fehlerdetails unterdrückt');
assert.ok(!describeError({ sql: 'SELECT * FROM tokens', message: 'x' }).sql);
console.log('safe-log tests ok');
