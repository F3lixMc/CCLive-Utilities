'use strict';
const assert = require('assert');
const { sha256Hex } = require('../../../backend_server/website_backend/crypto-util');
const {
  WEB_COOKIE,
  SESSION_MS,
  setWebCookie,
  clearWebCookie,
  webCookieOptions,
  legacyWebCookieOptions,
  loadWebAccount
} = require('../../../backend_server/website_backend/website-session');

const token = 'ab'.repeat(32);
const recorded = [];
const res = {
  cookie(name, value, options) { recorded.push({ op: 'set', name, value, options }); },
  clearCookie(name, options) { recorded.push({ op: 'clear', name, options }); }
};

setWebCookie(res, token);
assert.ok(recorded.some((entry) => entry.op === 'clear' && entry.name === WEB_COOKIE && entry.options.path === '/website'));
assert.ok(recorded.some((entry) => entry.op === 'set' && entry.name === WEB_COOKIE && entry.value === token && entry.options.path === '/' && entry.options.httpOnly && entry.options.maxAge === SESSION_MS));

recorded.length = 0;
clearWebCookie(res);
assert.ok(recorded.some((entry) => entry.op === 'clear' && entry.options.path === '/website'));
assert.ok(recorded.some((entry) => entry.op === 'clear' && entry.options.path === '/'));
assert.strictEqual(webCookieOptions(1).path, '/');
assert.strictEqual(legacyWebCookieOptions().path, '/website');

(async () => {
  const queries = [];
  const pool = {
    async query(sql, params) {
      queries.push({ sql, params });
      return [[{ session_id: 9, mc_uuid: '47a5c2fc-b317-4180-b96f-ca977c51eb62' }]];
    }
  };
  const account = await loadWebAccount(pool, token);
  assert.strictEqual(account.session_id, 9);
  assert.strictEqual(queries[0].params[0], sha256Hex(token));
  assert.strictEqual(await loadWebAccount(pool, 'short'), null);
  console.log('website-session tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
