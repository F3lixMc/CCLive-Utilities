'use strict';
const assert = require('assert');
const http = require('http');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { sha256Hex } = require('../../../backend_server/website_backend/crypto-util');
const installAdminAuth = require('../../../backend_server/website_backend/admin-auth');
const { installStaffHttp } = require('../../../backend_server/website_backend/staff-http');

const UUID_A = '47a5c2fc-b317-4180-b96f-ca977c51eb62';
const MOD_UUID = '069a79f4-44e1-4626-a5b0-bac67538bf5a';
const OTHER_UUID = '11111111-1111-1111-1111-111111111111';
const ADMIN_KEY = 'unit-admin-key';
const MOD_KEY = 'unit-mod-key';

function tokenFor(label) {
  return Buffer.from(label.padEnd(32, '0')).toString('hex').slice(0, 64);
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

function makeDb() {
  const players = [
    { id: 11, display_name: 'Admin', display_name_normalized: 'admin', mc_uuid: UUID_A },
    { id: 33, display_name: 'ModUser', display_name_normalized: 'moduser', mc_uuid: MOD_UUID },
    { id: 44, display_name: 'Plain', display_name_normalized: 'plain', mc_uuid: OTHER_UUID }
  ];
  const roles = new Map();
  const sessions = {
    admin: {
      token: tokenFor('admin'),
      session_id: 1,
      account_id: 11,
      player_id: 11,
      login_name: 'Admin',
      display_name: 'Admin',
      mc_uuid: UUID_A,
      admin_unlocked_until: new Date(Date.now() + 3600000),
      admin_unlock_key_hash: sha256Hex(ADMIN_KEY),
      mod_unlocked_until: null,
      mod_unlock_key_hash: null
    },
    mod: {
      token: tokenFor('mod'),
      session_id: 2,
      account_id: 33,
      player_id: 33,
      login_name: 'ModUser',
      display_name: 'ModUser',
      mc_uuid: MOD_UUID,
      admin_unlocked_until: null,
      admin_unlock_key_hash: null,
      mod_unlocked_until: null,
      mod_unlock_key_hash: null
    }
  };
  roles.set(33, 'moderator');
  const pool = {
    async query(sql, params) {
      if (/FROM website_sessions/.test(sql)) {
        const row = Object.values(sessions).find((entry) => sha256Hex(entry.token) === params[0]);
        return [row ? [{ ...row }] : []];
      }
      if (/SET admin_unlocked_until = \?/.test(sql)) {
        const row = Object.values(sessions).find((entry) => entry.session_id === params[2]);
        if (row) {
          row.admin_unlocked_until = params[0];
          row.admin_unlock_key_hash = params[1];
        }
        return [{ affectedRows: 1 }];
      }
      if (/SET mod_unlocked_until = \?/.test(sql)) {
        if (params[4] != null && roles.get(params[4]) !== params[5]) {
          return [{ affectedRows: 0 }];
        }
        const row = Object.values(sessions).find((entry) => entry.session_id === params[2]);
        if (row) {
          row.mod_unlocked_until = params[0];
          row.mod_unlock_key_hash = params[1];
        }
        return [{ affectedRows: row ? 1 : 0 }];
      }
      if (/FROM players[\s\S]*display_name_normalized = \?/.test(sql) && /LIMIT 3/.test(sql)) {
        return [players.filter((p) => p.display_name_normalized === params[0])];
      }
      if (/FROM player_display_name_history/.test(sql)) return [[]];
      if (/FROM staff_roles r/.test(sql) && /p.mc_uuid = \?/.test(sql)) {
        const player = players.find((p) => p.mc_uuid === params[0]);
        if (!player || roles.get(player.id) !== 'moderator') return [[]];
        return [[{ player_id: player.id }]];
      }
      if (/SELECT player_id FROM staff_roles WHERE player_id = \? AND role = \? FOR UPDATE/.test(sql)) {
        return [roles.get(params[0]) === params[1] ? [{ player_id: params[0] }] : []];
      }
      if (/SELECT player_id FROM staff_roles WHERE player_id = \? LIMIT 1/.test(sql)) {
        return [roles.has(params[0]) ? [{ player_id: params[0] }] : []];
      }
      if (/INSERT IGNORE INTO staff_roles/.test(sql) || /INSERT INTO staff_roles/.test(sql)) {
        if (roles.has(params[0])) return [{ affectedRows: 0 }];
        roles.set(params[0], params[1]);
        return [{ affectedRows: 1 }];
      }
      if (/DELETE FROM staff_roles/.test(sql)) {
        const had = roles.get(params[0]) === params[1];
        if (had) roles.delete(params[0]);
        return [{ affectedRows: had ? 1 : 0 }];
      }
      if (/UPDATE website_sessions s/.test(sql)) {
        for (const row of Object.values(sessions)) {
          if (row.player_id === params[0]) {
            row.mod_unlocked_until = null;
            row.mod_unlock_key_hash = null;
          }
        }
        return [{ affectedRows: 1 }];
      }
      if (/FROM staff_roles r[\s\S]*INNER JOIN players/.test(sql) && /ORDER BY/.test(sql)) {
        const rows = [];
        for (const [id, role] of roles) {
          if (role !== 'moderator') continue;
          const player = players.find((p) => p.id === id);
          rows.push({ player_id: id, display_name: player.display_name, mc_uuid: player.mc_uuid, granted_at: new Date() });
        }
        return [rows];
      }
      if (/INSERT INTO players/.test(sql)) throw new Error('players must not be created');
      return [[]];
    }
  };
  pool.getConnection = async () => ({
    query: pool.query,
    async beginTransaction() {},
    async commit() {},
    async rollback() {},
    release() {}
  });
  return { pool, sessions, roles };
}

function createApp(db) {
  const app = express();
  app.use(express.json());
  app.use(cookieParser());
  const checkAdmin = installAdminAuth(app, db.pool);
  const checkToken = (req, res, next) => {
    const uuid = req.headers['x-test-uuid'];
    if (!uuid) return res.status(401).json({ error: 'Kein Token angegeben' });
    req.mcUuid = uuid;
    req.playerId = Number(req.headers['x-test-pid'] || 0);
    req.player = req.headers['x-test-player'] || 'Player';
    next();
  };
  installStaffHttp(app, db.pool, {
    checkToken,
    checkAdmin,
    isPlayerOnline: (name) => name === 'ModUser'
  });
  app.get('/admin/overview', checkAdmin, (req, res) => res.json({ ok: true }));
  return app;
}

function adminHeaders(token) {
  return { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin', cookie: 'cclive_web=' + token };
}

(async () => {
  const prevAdmin = process.env.ADMIN_KEY;
  const prevMod = process.env.MOD_KEY;
  process.env.ADMIN_KEY = ADMIN_KEY;
  process.env.MOD_KEY = MOD_KEY;
  try {
    const db = makeDb();
    const app = createApp(db);

    const meMod = await request(app, {
      path: '/moderation/staff/me',
      headers: { 'x-test-uuid': MOD_UUID, 'x-test-pid': '33', 'x-test-player': 'ModUser' }
    });
    assert.strictEqual(meMod.json.staffRole, 'moderator');

    const mePlain = await request(app, {
      path: '/moderation/staff/me',
      headers: { 'x-test-uuid': OTHER_UUID, 'x-test-pid': '44', 'x-test-player': 'Plain' }
    });
    assert.strictEqual(mePlain.status, 403);

    const spoofName = await request(app, {
      path: '/moderation/staff/moderators',
      headers: { 'x-test-uuid': OTHER_UUID, 'x-test-player': 'firestarter03' }
    });
    assert.strictEqual(spoofName.status, 403);

    const listAsMod = await request(app, {
      path: '/moderation/staff/list',
      headers: { 'x-test-uuid': MOD_UUID, 'x-test-pid': '33', 'x-test-player': 'ModUser' }
    });
    assert.strictEqual(listAsMod.status, 200);
    assert.strictEqual(listAsMod.json.online, 1);

    const addAsMod = await request(app, {
      method: 'POST',
      path: '/moderation/staff/moderators',
      headers: { 'x-test-uuid': MOD_UUID, 'x-test-pid': '33', 'x-test-player': 'ModUser' },
      body: { player: 'Plain' }
    });
    assert.strictEqual(addAsMod.status, 403);

    const addAsAdmin = await request(app, {
      method: 'POST',
      path: '/moderation/staff/moderators',
      headers: { 'x-test-uuid': UUID_A, 'x-test-pid': '11', 'x-test-player': 'Admin' },
      body: { player: 'Plain' }
    });
    assert.strictEqual(addAsAdmin.status, 200);
    assert.strictEqual(addAsAdmin.json.player, 'Plain');

    const websiteModApi = await request(app, {
      path: '/admin/staff/moderators',
      headers: adminHeaders(db.sessions.mod.token)
    });
    assert.strictEqual(websiteModApi.status, 403);

    db.sessions.mod.mod_unlocked_until = new Date(Date.now() + 3600000);
    db.sessions.mod.mod_unlock_key_hash = sha256Hex(MOD_KEY);
    const websiteModUnlocked = await request(app, {
      path: '/admin/staff/moderators',
      headers: adminHeaders(db.sessions.mod.token)
    });
    assert.strictEqual(websiteModUnlocked.status, 403);

    const websiteAdmin = await request(app, {
      path: '/admin/staff/moderators',
      headers: adminHeaders(db.sessions.admin.token)
    });
    assert.strictEqual(websiteAdmin.status, 200);

    const unlockMod = await request(app, {
      method: 'POST',
      path: '/staff/unlock',
      headers: adminHeaders(tokenFor('mod')),
      body: { password: MOD_KEY }
    });
    assert.strictEqual(unlockMod.status, 200);
    assert.strictEqual(unlockMod.json.staffRole, 'moderator');
    assert.strictEqual(unlockMod.json.adminUnlocked, false);

    const unlockModWithAdminKey = await request(app, {
      method: 'POST',
      path: '/staff/unlock',
      headers: adminHeaders(tokenFor('mod')),
      body: { password: ADMIN_KEY }
    });
    assert.strictEqual(unlockModWithAdminKey.status, 401);

    const adminViaModRoute = await request(app, {
      method: 'POST',
      path: '/staff/unlock',
      headers: adminHeaders(tokenFor('admin')),
      body: { password: ADMIN_KEY }
    });
    assert.strictEqual(adminViaModRoute.json.staffRole, 'admin');

    process.env.ADMIN_KEY = 'same-key';
    process.env.MOD_KEY = 'same-key';
    const equalKeys = await request(app, {
      method: 'POST',
      path: '/staff/unlock',
      headers: adminHeaders(tokenFor('mod')),
      body: { password: 'same-key' }
    });
    assert.strictEqual(equalKeys.json.staffRole, 'moderator');
    assert.notStrictEqual(equalKeys.json.staffRole, 'admin');

    delete process.env.MOD_KEY;
    process.env.ADMIN_KEY = ADMIN_KEY;
    const missingModKey = await request(app, {
      method: 'POST',
      path: '/staff/unlock',
      headers: adminHeaders(tokenFor('mod')),
      body: { password: MOD_KEY }
    });
    assert.strictEqual(missingModKey.status, 503);
    assert.strictEqual(missingModKey.json.code, 'mod_key_missing');

    const adminStill = await request(app, {
      method: 'POST',
      path: '/admin/unlock',
      headers: adminHeaders(tokenFor('admin')),
      body: { password: ADMIN_KEY }
    });
    assert.strictEqual(adminStill.status, 200);

    process.env.MOD_KEY = 'changed-mod-key';
    db.sessions.mod.mod_unlocked_until = new Date(Date.now() + 3600000);
    db.sessions.mod.mod_unlock_key_hash = sha256Hex(MOD_KEY);
    const afterKeyChange = await request(app, {
      path: '/admin',
      headers: { cookie: 'cclive_web=' + tokenFor('mod') }
    });
    assert.ok(/Control-Center-Key/.test(afterKeyChange.raw));
    assert.ok(!/keine Adminbereiche/.test(afterKeyChange.raw));
  } finally {
    if (prevAdmin == null) delete process.env.ADMIN_KEY;
    else process.env.ADMIN_KEY = prevAdmin;
    if (prevMod == null) delete process.env.MOD_KEY;
    else process.env.MOD_KEY = prevMod;
  }
  console.log('staff-http tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
