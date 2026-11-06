'use strict';
const assert = require('assert');
const http = require('http');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { sha256Hex, randomSecret } = require('../../../backend_server/website_backend/crypto-util');
const installAdminAuth = require('../../../backend_server/website_backend/admin-auth');

const UUID_A = '47a5c2fc-b317-4180-b96f-ca977c51eb62';
const UUID_B = '6a814aa9-acd6-4180-a84c-a816632d7aa1';
const UUID_OTHER = '069a79f4-44e1-4626-a5b0-bac67538bf5a';
const ADMIN_KEY = 'unit-admin-key';

function tokenFor(label) {
  return Buffer.from(label.padEnd(32, '0')).toString('hex').slice(0, 64);
}

function makePool(sessions) {
  return {
    async query(sql, params) {
      if (/FROM website_sessions/.test(sql)) {
        const row = Object.values(sessions).find((entry) => sha256Hex(entry.token) === params[0]);
        return [row ? [{ ...row }] : []];
      }
      if (/SET admin_unlocked_until = NULL/.test(sql)) {
        const row = Object.values(sessions).find((entry) => entry.session_id === params[0]);
        if (row) {
          row.admin_unlocked_until = null;
          row.admin_unlock_key_hash = null;
        }
        return [{ affectedRows: 1 }];
      }
      if (/SET admin_unlocked_until = \?/.test(sql)) {
        const row = Object.values(sessions).find((entry) => entry.session_id === params[2] && entry.account_id === params[3]);
        if (row) {
          row.admin_unlocked_until = params[0];
          row.admin_unlock_key_hash = params[1];
        }
        return [{ affectedRows: 1 }];
      }
      return [[]];
    }
  };
}

function sessionRow({ token, uuid, sessionId, accountId, unlocked }) {
  return {
    token,
    session_id: sessionId,
    account_id: accountId,
    player_id: accountId,
    login_name: 'Admin',
    display_name: 'Admin',
    mc_uuid: uuid,
    admin_unlocked_until: unlocked ? new Date(Date.now() + 60 * 60 * 1000) : null,
    admin_unlock_key_hash: unlocked ? sha256Hex(ADMIN_KEY) : null
  };
}

function createApp(sessions) {
  const app = express();
  app.use(express.json());
  app.use(cookieParser());
  const checkAdmin = installAdminAuth(app, makePool(sessions));
  app.get('/admin/overview', checkAdmin, (req, res) => res.json({ ok: true, session: req.adminAccount.session_id }));
  app.get('/presence/count', (req, res) => res.json({ online: 3 }));
  app.get('/zeichen/data', (req, res) => res.json({ public: true }));
  return app;
}

function request(app, { method = 'GET', path, headers = {}, body }) {
  return new Promise((resolve, reject) => {
    const server = app.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      const req = http.request({
        hostname: '127.0.0.1',
        port,
        path,
        method,
        headers: {
          host: '127.0.0.1:' + port,
          ...headers,
          ...(body ? { 'content-type': 'application/json' } : {})
        }
      }, (res) => {
        const chunks = [];
        res.on('data', (chunk) => chunks.push(chunk));
        res.on('end', () => {
          server.close();
          const raw = Buffer.concat(chunks).toString('utf8');
          let json = null;
          try { json = JSON.parse(raw); } catch { json = null; }
          resolve({ status: res.statusCode, headers: res.headers, raw, json });
        });
      });
      req.on('error', (err) => {
        server.close();
        reject(err);
      });
      if (body) req.write(JSON.stringify(body));
      req.end();
    });
  });
}

function cookieHeader(token) {
  return 'cclive_web=' + token;
}

function adminHeaders(token, extra) {
  return {
    'x-admin-request': '1',
    'sec-fetch-site': 'same-origin',
    cookie: cookieHeader(token),
    ...extra
  };
}

(async () => {
  const prevKey = process.env.ADMIN_KEY;
  process.env.ADMIN_KEY = ADMIN_KEY;
  try {
    const tokenA = tokenFor('adminA');
    const tokenB = tokenFor('adminB');
    const tokenUser = tokenFor('player');
    const tokenA2 = tokenFor('adminA2');
    const sessions = {
      a: sessionRow({ token: tokenA, uuid: UUID_A, sessionId: 1, accountId: 11, unlocked: false }),
      b: sessionRow({ token: tokenB, uuid: UUID_B, sessionId: 2, accountId: 22, unlocked: false }),
      user: sessionRow({ token: tokenUser, uuid: UUID_OTHER, sessionId: 3, accountId: 33, unlocked: false }),
      a2: sessionRow({ token: tokenA2, uuid: UUID_A, sessionId: 4, accountId: 11, unlocked: false })
    };
    const app = createApp(sessions);

    const anonOverview = await request(app, { path: '/admin/overview', headers: { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin' } });
    assert.strictEqual(anonOverview.status, 401);
    assert.strictEqual(anonOverview.json.code, 'login_required');

    const anonHtml = await request(app, { path: '/admin' });
    assert.strictEqual(anonHtml.status, 200);
    assert.ok(/Anmeldung erforderlich/.test(anonHtml.raw));
    assert.ok(/Control-Center-Key/.test(anonHtml.raw));
    assert.ok(/Zugriff auf das CCLive Control Center bestätigen/.test(anonHtml.raw));

    const htmlRedirect = await request(app, { path: '/admin.html' });
    assert.strictEqual(htmlRedirect.status, 302);
    assert.strictEqual(htmlRedirect.headers.location, '/admin');

    const userOverview = await request(app, {
      path: '/admin/overview',
      headers: adminHeaders(tokenUser, { 'x-player': UUID_A }),
      body: undefined
    });
    assert.strictEqual(userOverview.status, 403);
    assert.strictEqual(userOverview.json.code, 'forbidden');

    const userHtml = await request(app, { path: '/admin', headers: { cookie: cookieHeader(tokenUser) } });
    assert.ok(/Zugriff gesperrt/.test(userHtml.raw));

    const eligibleLocked = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenA) });
    assert.strictEqual(eligibleLocked.status, 401);
    assert.strictEqual(eligibleLocked.json.code, 'admin_unlock_required');

    const spoofUnlock = await request(app, {
      method: 'POST',
      path: '/admin/unlock',
      headers: adminHeaders(tokenUser),
      body: { password: ADMIN_KEY, uuid: UUID_A, player: 'firestarter03' }
    });
    assert.strictEqual(spoofUnlock.status, 403);

    const badKey = await request(app, {
      method: 'POST',
      path: '/admin/unlock',
      headers: adminHeaders(tokenA),
      body: { password: 'wrong-key' }
    });
    assert.strictEqual(badKey.status, 401);
    assert.strictEqual(badKey.json.code, 'invalid_key');
    assert.strictEqual(sessions.a.admin_unlocked_until, null);

    const unlockA = await request(app, {
      method: 'POST',
      path: '/admin/unlock',
      headers: adminHeaders(tokenA),
      body: { password: ADMIN_KEY }
    });
    assert.strictEqual(unlockA.status, 200);
    assert.strictEqual(unlockA.json.adminUnlocked, true);
    assert.ok(sessions.a.admin_unlocked_until);

    const unlockB = await request(app, {
      method: 'POST',
      path: '/admin/unlock',
      headers: adminHeaders(tokenB),
      body: { password: ADMIN_KEY }
    });
    assert.strictEqual(unlockB.status, 200);

    const okA = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenA) });
    assert.strictEqual(okA.status, 200);
    assert.strictEqual(okA.json.session, 1);
    const okB = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenB) });
    assert.strictEqual(okB.status, 200);

    const unlockedHtml = await request(app, { path: '/admin', headers: { cookie: cookieHeader(tokenA) } });
    assert.ok(/CCLive Control Center/.test(unlockedHtml.raw));
    assert.strictEqual(unlockedHtml.headers['cache-control'], 'no-store');

    const otherSession = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenA2) });
    assert.strictEqual(otherSession.status, 401);
    assert.strictEqual(otherSession.json.code, 'admin_unlock_required');

    const headerOnly = await request(app, {
      path: '/admin/overview',
      headers: { 'x-admin-key': ADMIN_KEY }
    });
    assert.notStrictEqual(headerOnly.status, 200);

    const oldCookie = await request(app, {
      path: '/admin/overview',
      headers: {
        'x-admin-request': '1',
        'sec-fetch-site': 'same-origin',
        cookie: 'cclive_admin_session=' + randomSecret(32)
      }
    });
    assert.strictEqual(oldCookie.status, 401);

    sessions.a.admin_unlocked_until = new Date(Date.now() - 1000);
    const expired = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenA) });
    assert.strictEqual(expired.status, 401);
    assert.strictEqual(expired.json.code, 'admin_unlock_required');

    sessions.b.admin_unlocked_until = new Date(Date.now() + 60 * 60 * 1000);
    sessions.b.admin_unlock_key_hash = sha256Hex(ADMIN_KEY);
    const clearUnlock = await request(app, { method: 'POST', path: '/admin/logout', headers: adminHeaders(tokenB) });
    assert.strictEqual(clearUnlock.status, 200);
    assert.strictEqual(sessions.b.admin_unlocked_until, null);
    const afterClear = await request(app, { path: '/admin/overview', headers: adminHeaders(tokenB) });
    assert.strictEqual(afterClear.json.code, 'admin_unlock_required');

    const presence = await request(app, { path: '/presence/count' });
    assert.strictEqual(presence.status, 200);
    assert.strictEqual(presence.json.online, 3);
    const publicData = await request(app, { path: '/zeichen/data' });
    assert.strictEqual(publicData.status, 200);
    assert.strictEqual(publicData.json.public, true);

    const loginRouteGone = await request(app, {
      method: 'POST',
      path: '/admin/login',
      headers: adminHeaders(tokenA),
      body: { password: ADMIN_KEY }
    });
    assert.notStrictEqual(loginRouteGone.status, 200);
  } finally {
    if (prevKey == null) delete process.env.ADMIN_KEY;
    else process.env.ADMIN_KEY = prevKey;
  }
  console.log('admin-auth tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
