'use strict';
const assert = require('assert');
const { parseTrustProxy, clientIpFrom } = require('../../../backend_server/website_backend/client-ip');

assert.strictEqual(parseTrustProxy(''), false);
assert.strictEqual(parseTrustProxy('false'), false);
assert.throws(() => parseTrustProxy('true'), /nicht erlaubt/);

const proxy = '10.1.1.8';
const clientA = '203.0.113.10';
const clientB = '203.0.113.20';

assert.strictEqual(
  clientIpFrom(proxy, { 'x-forwarded-for': clientA }, proxy),
  clientA
);
assert.strictEqual(
  clientIpFrom(proxy, { 'x-forwarded-for': clientB }, proxy),
  clientB
);
assert.notStrictEqual(
  clientIpFrom(proxy, { 'x-forwarded-for': clientA }, proxy),
  clientIpFrom(proxy, { 'x-forwarded-for': clientB }, proxy)
);

const direct = '198.51.100.9';
assert.strictEqual(
  clientIpFrom(direct, { 'x-forwarded-for': '203.0.113.99' }, proxy),
  direct
);
assert.strictEqual(
  clientIpFrom(direct, { 'x-forwarded-for': '203.0.113.99' }, ''),
  direct
);
console.log('client-ip tests ok');
