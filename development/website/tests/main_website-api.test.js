'use strict';
const assert = require('assert');
const { createWebsiteApi, remainingMs, formatTimer, playerHeadUrl, isTerminalLinkStatus, httpMessage } = require('../../../backend_server/public/main_website-api.js');

(async () => {
  const calls = [];
  const responses = [];
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    const next = responses.shift();
    if (!next) throw new Error('unexpected fetch ' + url);
    if (next.network) throw new TypeError('failed');
    return {
      ok: next.status >= 200 && next.status < 300,
      status: next.status,
      json: async () => next.body || {}
    };
  };
  const api = createWebsiteApi(fetchImpl);

  responses.push({ status: 200, body: { ok: true } });
  await api.bootstrap();
  assert.strictEqual(calls[0].url, '/website/session/bootstrap');
  assert.strictEqual(calls[0].options.headers['X-Website-Request'], '1');

  responses.push({
    status: 200,
    body: {
      requestId: 'req-1',
      command: '/cclive profil login ABCD1234',
      completionSecret: 'hidden-secret',
      expires_at: new Date(Date.now() + 120000).toISOString()
    }
  });
  const requested = await api.requestLink('Steve');
  assert.ok(requested.command.includes('/cclive profil login'));
  assert.ok(requested.completionSecret);
  assert.notStrictEqual(requested.requestId, requested.completionSecret);

  responses.push({ status: 200, body: { status: 'confirmed', loginNameAvailable: false, confirm_expires_at: requested.expires_at } });
  const status = await api.status('req-1');
  assert.strictEqual(status.status, 'confirmed');
  assert.strictEqual(status.loginNameAvailable, false);

  responses.push({ status: 401, body: { error: 'Sitzung ungültig' } });
  await assert.rejects(() => api.complete({ requestId: 'req-1', completionSecret: 'wrong' }), /Sitzung ungültig/);

  responses.push({ status: 409, body: { code: 'login_name_taken', error: 'belegt' } });
  try {
    await api.complete({ requestId: 'req-1', completionSecret: 'hidden-secret', password: 'x'.repeat(12), passwordConfirm: 'x'.repeat(12) });
    assert.fail('expected 409');
  } catch (err) {
    assert.strictEqual(err.code, 'login_name_taken');
  }

  responses.push({ status: 200, body: { player: 'Steve', loginName: 'Steve2' } });
  const created = await api.complete({
    requestId: 'req-1',
    completionSecret: 'hidden-secret',
    password: 'x'.repeat(12),
    passwordConfirm: 'x'.repeat(12),
    loginName: 'Steve2'
  });
  assert.strictEqual(created.player, 'Steve');

  responses.push({ status: 200, body: { player: 'Steve', loginName: 'Steve2' } });
  const loggedIn = await api.login('Steve2', 'x'.repeat(12));
  assert.strictEqual(loggedIn.player, 'Steve');

  responses.push({ status: 200, body: { ok: true } });
  await api.logout();
  assert.ok(calls.some((c) => c.url === '/website/logout'));

  responses.push({ network: true });
  await assert.rejects(() => api.me(), /Netzwerkfehler/);

  responses.push({ status: 429, body: {} });
  await assert.rejects(() => api.login('Steve2', 'pw'), /Zu viele Versuche/);

  responses.push({ status: 401, body: { error: 'Anmeldung fehlgeschlagen – Anmeldename oder Passwort falsch' } });
  try {
    await api.login('Nope', 'wrong');
    assert.fail('expected 401');
  } catch (err) {
    assert.strictEqual(err.status, 401);
    assert.strictEqual(err.message, 'Anmeldung fehlgeschlagen – Anmeldename oder Passwort falsch');
    assert.ok(!err.network);
  }
  assert.ok(!httpMessage(429, {}).includes('Passwort'));
  assert.ok(httpMessage(0, {}).includes('Netzwerk'));

  assert.strictEqual(
    playerHeadUrl('069a79f4-44e1-4626-a5b0-bac67538bf5a'),
    '/website/avatar/069a79f444e14626a5b0bac67538bf5a'
  );
  assert.strictEqual(playerHeadUrl('not-a-uuid'), null);
  assert.ok(isTerminalLinkStatus({ status: 'already_linked' }));
  assert.ok(isTerminalLinkStatus({ code: 'already_connected' }));
  assert.ok(!isTerminalLinkStatus({ status: 'pending' }));

  assert.strictEqual(formatTimer(remainingMs(Date.now() + 61000, Date.now())), '01:01');
  console.log('frontend api tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
