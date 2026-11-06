'use strict';
const assert = require('assert');
const { sha256Hex } = require('../../../backend_server/website_backend/crypto-util');
const { evaluateLinkAccess, firstConfirmWins, alreadyConnected } = require('../../../backend_server/website_backend/link-complete-rules');

const secret = 'a'.repeat(64);
const session = 'b'.repeat(64);
const link = {
  session_secret_hash: sha256Hex(session),
  completion_secret_hash: sha256Hex(secret),
  status: 'confirmed',
  confirmed_uuid: '069a79f4-44e1-4626-a5b0-bac67538bf5a',
  confirm_expires_at: new Date(Date.now() + 60000).toISOString()
};

assert.strictEqual(evaluateLinkAccess(null, { sessionHash: sha256Hex(session), completionSecret: secret }).status, 401);
assert.strictEqual(evaluateLinkAccess(link, { sessionHash: sha256Hex('other'), completionSecret: secret }).status, 401);
assert.strictEqual(evaluateLinkAccess(link, { sessionHash: sha256Hex(session), completionSecret: 'wrong' }).status, 401);
assert.ok(evaluateLinkAccess(link, { sessionHash: sha256Hex(session), completionSecret: secret }).ok);

const expired = { ...link, confirm_expires_at: new Date(Date.now() - 1000).toISOString() };
assert.strictEqual(evaluateLinkAccess(expired, { sessionHash: sha256Hex(session), completionSecret: secret }).status, 409);

assert.ok(firstConfirmWins('pending'));
assert.ok(!firstConfirmWins('confirmed'));

const store = { status: 'confirmed', accounts: 0 };
function completeOnce(attempt) {
  const check = evaluateLinkAccess({ ...link, status: store.status }, attempt);
  if (!check.ok) return check;
  if (store.status !== 'confirmed') return { ok: false, status: 409, error: 'Anfrage bereits abgeschlossen' };
  store.status = 'consumed';
  store.accounts += 1;
  return { ok: true };
}
const first = completeOnce({ sessionHash: sha256Hex(session), completionSecret: secret });
const second = completeOnce({ sessionHash: sha256Hex(session), completionSecret: secret });
assert.ok(first.ok);
assert.strictEqual(second.status, 409);
assert.strictEqual(store.accounts, 1);

const linked = { ...link, status: 'already_linked' };
const linkedCheck = evaluateLinkAccess(linked, { sessionHash: sha256Hex(session), completionSecret: secret });
assert.strictEqual(linkedCheck.status, 409);
assert.strictEqual(linkedCheck.code, 'already_connected');
assert.ok(!linkedCheck.ok);
assert.strictEqual(alreadyConnected().error, 'Dein Profil ist bereits verbunden. Bitte nutze den Login.');
assert.ok(!firstConfirmWins('already_linked'));
console.log('website-auth flow tests ok');
