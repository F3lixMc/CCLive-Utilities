'use strict';
const assert = require('assert');
const { notifyWebsiteLinked } = require('../../../backend_server/website_backend/website-notify');

const sent = [];
const clients = [
  { readyState: 1, playerId: 7, send: (payload) => sent.push({ id: 7, payload }) },
  { readyState: 1, playerId: 8, send: (payload) => sent.push({ id: 8, payload }) },
  { readyState: 3, playerId: 7, send: () => { throw new Error('closed'); } }
];
assert.strictEqual(notifyWebsiteLinked(clients, 7), 1);
assert.strictEqual(sent.length, 1);
assert.strictEqual(JSON.parse(sent[0].payload).type, 'website.linked');
assert.strictEqual(notifyWebsiteLinked([], 7), 0);
assert.strictEqual(notifyWebsiteLinked(clients, 99), 0);
console.log('website-notify tests ok');
