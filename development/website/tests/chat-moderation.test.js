'use strict';
const assert = require('assert');
const http = require('http');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { sha256Hex } = require('../../../backend_server/website_backend/crypto-util');
const installAdminAuth = require('../../../backend_server/website_backend/admin-auth');
const { installModerationHttp } = require('../../../backend_server/website_backend/moderation-http');
const chatMod = require('../../../backend_server/website_backend/chat-moderation');
const staffFeed = require('../../../backend_server/website_backend/staff-chat-feed');

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
          resolve({ status: res.statusCode, json, raw });
        });
      });
      req.on('error', (err) => { server.close(); reject(err); });
      if (body) req.write(JSON.stringify(body));
      req.end();
    });
  });
}

function makeDb() {
  const now = () => new Date();
  const players = [
    { id: 11, display_name: 'Admin', display_name_normalized: 'admin', mc_uuid: UUID_A },
    { id: 33, display_name: 'ModUser', display_name_normalized: 'moduser', mc_uuid: MOD_UUID },
    { id: 44, display_name: 'Plain', display_name_normalized: 'plain', mc_uuid: OTHER_UUID },
    { id: 55, display_name: 'Ghost', display_name_normalized: 'ghost', mc_uuid: null }
  ];
  const sessions = {
    admin: {
      token: tokenFor('admin'), session_id: 1, account_id: 11, player_id: 11,
      login_name: 'Admin', display_name: 'Admin', mc_uuid: UUID_A,
      admin_unlocked_until: new Date(Date.now() + 3600000),
      admin_unlock_key_hash: sha256Hex(ADMIN_KEY),
      mod_unlocked_until: null, mod_unlock_key_hash: null
    },
    mod: {
      token: tokenFor('mod'), session_id: 2, account_id: 33, player_id: 33,
      login_name: 'ModUser', display_name: 'ModUser', mc_uuid: MOD_UUID,
      admin_unlocked_until: null, admin_unlock_key_hash: null,
      mod_unlocked_until: new Date(Date.now() + 3600000),
      mod_unlock_key_hash: sha256Hex(MOD_KEY)
    }
  };
  const state = {
    seasons: [{ id: 2, is_current: 1 }],
    messages: [],
    events: [],
    redactions: [],
    evidence: [],
    sanctions: [],
    shares: [
      { id: 'ITEM-db2ac5', type: 'ITEM', data: '{"n":1}', expires_at: new Date(Date.now() + 3600000) },
      { id: 'BUILD-709125', type: 'BUILD', data: '{"b":1}', expires_at: new Date(Date.now() + 3600000) }
    ],
    nextId: 1,
    roles: new Map([[33, 'moderator']]),
    leaderboard: [{ season_id: 1, board: 'kills', player_id: 44, score: 9 }],
    sqlLog: [],
    failEvidence: false,
    beforeSeasonDelete: null
  };

  function active(row) {
    return !row.lifted_at && (!row.expires_at || new Date(row.expires_at).getTime() > Date.now());
  }

  function snapshotState() {
    return {
      seasons: state.seasons.map((row) => ({ ...row })),
      messages: state.messages.map((row) => ({ ...row })),
      events: state.events.map((row) => ({ ...row })),
      redactions: state.redactions.map((row) => ({ ...row })),
      evidence: state.evidence.map((row) => ({ ...row })),
      sanctions: state.sanctions.map((row) => ({ ...row })),
      shares: state.shares.map((row) => ({ ...row })),
      nextId: state.nextId,
      roles: new Map(state.roles),
      leaderboard: state.leaderboard.map((row) => ({ ...row })),
      failEvidence: !!state.failEvidence
    };
  }

  function restoreState(saved) {
    state.seasons = saved.seasons;
    state.messages = saved.messages;
    state.events = saved.events;
    state.redactions = saved.redactions;
    state.evidence = saved.evidence;
    state.sanctions = saved.sanctions;
    state.shares = saved.shares;
    state.nextId = saved.nextId;
    state.roles = saved.roles;
    state.leaderboard = saved.leaderboard;
    state.failEvidence = saved.failEvidence;
  }

  function seasonStillEnded(seasonId, currentStart) {
    const season = state.seasons.find((row) => Number(row.id) === Number(seasonId));
    if (!season || Number(season.is_current) !== 0 || season.start_at == null) return false;
    return new Date(season.start_at).getTime() < new Date(currentStart).getTime();
  }

  function keepRows(rows, idField, params) {
    const currentStart = params[params.length - 1];
    const ids = params.slice(0, -1).map(Number);
    const kept = rows.filter((row) => {
      if (!ids.includes(Number(row[idField]))) return true;
      return !seasonStillEnded(row[idField], currentStart);
    });
    return { kept, affected: rows.length - kept.length };
  }

  const pool = {
    async query(sql, params = []) {
      state.sqlLog.push(String(sql));
      if (/SELECT id, is_current, start_at FROM seasons FOR UPDATE/.test(sql)) {
        return [state.seasons.map((row) => ({ ...row }))];
      }
      if (/SELECT id FROM seasons WHERE is_current = 1/.test(sql)) {
        return [state.seasons.filter((s) => s.is_current)];
      }
      if (/SELECT id, display_name, mc_uuid FROM players WHERE id = \?/.test(sql)) {
        const player = players.find((p) => p.id === Number(params[0]));
        return [player ? [{ ...player }] : []];
      }
      if (/FROM staff_roles WHERE player_id = \?/.test(sql)) {
        const id = Number(params[0]);
        if (state.roles.get(id) !== params[1]) return [[]];
        return [[{ player_id: id }]];
      }
      if (/FROM players[\s\S]*display_name_normalized = \?/.test(sql)) {
        return [players.filter((p) => p.display_name_normalized === params[0])];
      }
      if (/FROM player_display_name_history/.test(sql)) return [[]];
      if (/FROM staff_roles r[\s\S]*p.mc_uuid = \?/.test(sql)) {
        const player = players.find((p) => p.mc_uuid === params[0]);
        if (!player || state.roles.get(player.id) !== 'moderator') return [[]];
        return [[{ player_id: player.id }]];
      }
      if (/FROM website_sessions/.test(sql)) {
        if (state.delaySessions) await new Promise((resolve) => setTimeout(resolve, state.delaySessions));
        const row = Object.values(sessions).find((entry) => sha256Hex(entry.token) === params[0]);
        return [row ? [{ ...row }] : []];
      }
      if (/INSERT INTO chat_messages/.test(sql)) {
        const row = {
          id: state.nextId++,
          player: params[0],
          player_id: params[1],
          message: params[2],
          timestamp: now(),
          deleted_at: null,
          deleted_event_id: null
        };
        state.messages.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/FROM chat_messages WHERE id = \? FOR UPDATE/.test(sql)) {
        return [state.messages.filter((m) => m.id === params[0])];
      }
      if (/FROM shared_content WHERE id = \?/.test(sql)) {
        if (params[0] === 'explode') throw new Error('share read failed');
        return [state.shares.filter((s) => s.id === params[0])];
      }
      if (/INSERT INTO moderation_events/.test(sql)) {
        const row = {
          id: state.events.length + 1,
          actor_player_id: params[0],
          target_player_id: params[1],
          source: params[2],
          kind: params[3],
          payload: params[4],
          season_id: params[5],
          created_at: now()
        };
        state.events.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/INSERT INTO chat_message_redactions/.test(sql)) {
        if (state.redactions.some((r) => r.message_id === params[0])) {
          const err = new Error('dup'); err.code = 'ER_DUP_ENTRY'; throw err;
        }
        state.redactions.push({
          message_id: params[0], original_text: params[1], original_player: params[2],
          original_player_id: params[3], original_timestamp: params[4],
          share_ids: params[5], share_snapshots: params[6], actor_player_id: params[7],
          reason_text: params[8], season_id: params[9], event_id: params[10]
        });
        return [{ insertId: state.redactions.length }];
      }
      if (/INSERT INTO player_file_evidence/.test(sql) && /sanction_id/.test(sql)) {
        if (state.failEvidence) throw new Error('evidence failed');
        state.evidence.push({
          target_player_id: params[0],
          message_id: params[1],
          snapshot_text: params[2],
          snapshot_at: params[3],
          action: params[4],
          reason_text: params[5],
          actor_player_id: params[6],
          source: params[7],
          season_id: params[8],
          event_id: params[9],
          sanction_id: params[10],
          share_ids: params[11],
          share_snapshots: params[12]
        });
        return [{ insertId: state.evidence.length }];
      }
      if (/INSERT INTO player_file_evidence/.test(sql)) {
        state.evidence.push({
          target_player_id: params[0],
          message_id: params[1],
          snapshot_text: params[2],
          season_id: params[7],
          event_id: params[8]
        });
        return [{ insertId: state.evidence.length }];
      }
      if (/UPDATE chat_messages/.test(sql) && /deleted_at IS NULL/.test(sql)) {
        const row = state.messages.find((m) => m.id === params[2] && !m.deleted_at);
        if (!row) return [{ affectedRows: 0 }];
        row.message = params[0];
        row.deleted_at = now();
        row.deleted_event_id = params[1];
        return [{ affectedRows: 1 }];
      }
      if (/FROM chat_sanctions[\s\S]*kind = \?/.test(sql) && /FOR UPDATE/.test(sql)) {
        return [state.sanctions.filter((s) => s.player_id === params[0] && s.kind === params[1] && active(s))];
      }
      if (/FROM chat_sanctions[\s\S]*player_id = \? AND/.test(sql) && /lifted_at IS NULL/.test(sql) && /SELECT id, player_id, kind/.test(sql)) {
        return [state.sanctions.filter((s) => s.player_id === params[0] && active(s))];
      }
      if (/INSERT INTO chat_sanctions/.test(sql)) {
        const row = {
          id: state.sanctions.length + 1,
          player_id: params[0],
          kind: params[1],
          starts_at: now(),
          expires_at: params[2],
          lifted_at: null,
          reason_text: params[3],
          actor_player_id: params[4],
          source: params[5],
          created_season_id: params[6],
          created_event_id: null,
          duration_key: params[7],
          message_id: params[8]
        };
        state.sanctions.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/UPDATE chat_sanctions SET created_event_id/.test(sql)) {
        const row = state.sanctions.find((entry) => entry.id === params[1]);
        if (row) row.created_event_id = params[0];
        return [{ affectedRows: row ? 1 : 0 }];
      }
      if (/UPDATE chat_sanctions[\s\S]*lifted_at = NOW/.test(sql)) {
        let n = 0;
        for (const row of state.sanctions) {
          if (row.player_id === params[2] && row.kind === params[3] && active(row)) {
            row.lifted_at = now();
            row.lift_reason = params[0];
            n += 1;
          }
        }
        return [{ affectedRows: n }];
      }
      if (/FROM chat_sanctions s[\s\S]*LEFT JOIN players/.test(sql)) {
        let rows = state.sanctions.filter(active).map((s) => {
          const player = players.find((p) => p.id === s.player_id) || {};
          return { ...s, player: player.display_name || '', mc_uuid: player.mc_uuid || null };
        });
        if (/p\.mc_uuid IS NOT NULL/.test(sql)) rows = rows.filter((s) => s.mc_uuid);
        if (/s\.kind = \?/.test(sql)) rows = rows.filter((s) => s.kind === params[params.length - 1]);
        if (sql.includes('s.player_id = ?')) rows = rows.filter((s) => s.player_id === params[0]);
        return [rows];
      }
      if (/FROM moderation_events e/.test(sql)) {
        return [state.events.slice().reverse().map((e) => ({
          ...e,
          actor: (players.find((p) => p.id === e.actor_player_id) || {}).display_name,
          target: (players.find((p) => p.id === e.target_player_id) || {}).display_name
        }))];
      }
      if (/c\.deleted_at IS NOT NULL AND c\.id IN/.test(sql)) {
        const ids = new Set(params.map(Number));
        return [state.messages
          .filter((m) => m.deleted_at && ids.has(Number(m.id)))
          .sort((a, b) => a.id - b.id)
          .map((m) => ({
            id: m.id,
            player: m.player,
            message: m.message,
            timestamp: m.timestamp && m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
            deleted_at: m.deleted_at
          }))];
      }
      if (/c\.id > \?/.test(sql)) {
        if (state.delayAfterQuery) {
          const wait = state.delayAfterQuery;
          state.delayAfterQuery = 0;
          await new Promise((resolve) => setTimeout(resolve, wait));
        }
        const after = Number(params[0]);
        const limit = Number(params[1]) || 80;
        return [state.messages
          .filter((m) => Number(m.id) > after)
          .sort((a, b) => a.id - b.id)
          .slice(0, limit)
          .map((m) => ({
            id: m.id,
            player: m.player,
            message: m.message,
            timestamp: m.timestamp && m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
            deleted_at: m.deleted_at
          }))];
      }
      if (/FROM chat_messages c[\s\S]*ORDER BY c.id DESC/.test(sql) || /FROM chat_messages c[\s\S]*ORDER BY c.timestamp/.test(sql)) {
        return [state.messages.map((m) => ({
          id: m.id,
          player: m.player,
          message: m.deleted_at ? chatMod.PLACEHOLDER : m.message,
          timestamp: m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
          deleted_at: m.deleted_at
        }))];
      }
      if (/FROM chat_message_redactions/.test(sql) && !/DELETE/.test(sql)) {
        return [state.redactions.filter((r) => Number(r.message_id) === Number(params[0]))];
      }
      if (/DELETE target FROM moderation_events|DELETE target FROM chat_message_redactions|DELETE target FROM player_file_evidence|DELETE s FROM chat_sanctions/.test(sql)) {
        if (state.beforeSeasonDelete) {
          const hook = state.beforeSeasonDelete;
          state.beforeSeasonDelete = null;
          hook();
        }
      }
      if (/DELETE target FROM moderation_events/.test(sql)) {
        const result = keepRows(state.events, 'season_id', params);
        state.events = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM chat_message_redactions/.test(sql)) {
        const result = keepRows(state.redactions, 'season_id', params);
        state.redactions = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM player_file_evidence/.test(sql)) {
        const result = keepRows(state.evidence, 'season_id', params);
        state.evidence = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE s FROM chat_sanctions/.test(sql) && /expires_at < NOW/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        const before = state.sanctions.length;
        state.sanctions = state.sanctions.filter((row) => {
          if (!ids.includes(Number(row.created_season_id))) return true;
          if (!seasonStillEnded(row.created_season_id, currentStart)) return true;
          return !(!row.lifted_at && row.expires_at && new Date(row.expires_at) < now());
        });
        return [{ affectedRows: before - state.sanctions.length }];
      }
      if (/DELETE s FROM chat_sanctions/.test(sql) && /lifted_at IS NOT NULL/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        const before = state.sanctions.length;
        state.sanctions = state.sanctions.filter((row) => {
          if (!ids.includes(Number(row.created_season_id))) return true;
          if (!seasonStillEnded(row.created_season_id, currentStart)) return true;
          return !row.lifted_at;
        });
        return [{ affectedRows: before - state.sanctions.length }];
      }
      if (/FROM leaderboard/.test(sql)) return [state.leaderboard];
      return [[]];
    }
  };
  pool.getConnection = async () => {
    let saved = null;
    return {
      query: (sql, params) => pool.query(sql, params),
      async beginTransaction() { saved = snapshotState(); },
      async commit() { saved = null; },
      async rollback() {
        if (!saved) return;
        restoreState(saved);
        saved = null;
      },
      release() {}
    };
  };
  return { pool, state, players, sessions };
}

function createApp(db, hooks = {}) {
  const app = express();
  app.use(express.json());
  app.use(cookieParser());
  const checkAdmin = installAdminAuth(app, db.pool);
  const checkToken = (req, res, next) => {
    const uuid = req.headers['x-test-uuid'];
    if (!uuid) return res.status(401).json({ error: 'Kein Token angegeben' });
    const player = db.players.find((p) => p.mc_uuid === uuid);
    if (!player) return res.status(403).json({ error: 'Ungültiger Token' });
    req.mcUuid = uuid;
    req.playerId = player.id;
    req.player = player.display_name;
    next();
  };
  installModerationHttp(app, db.pool, {
    checkToken,
    checkStaff: checkAdmin.checkStaff,
    onSanctionChanged: hooks.onSanctionChanged
  });
  app.post('/chat/send', checkToken, async (req, res) => {
    const stored = await chatMod.persistChatMessage(db.pool, {
      player: req.player, playerId: req.playerId, message: req.body.message
    });
    if (stored.denied) return res.status(403).json({ error: stored.denied.error, code: stored.denied.code });
    res.json({ success: true, id: stored.id });
  });
  chatMod.installChatHistoryRoute(app, db.pool, checkToken);
  return app;
}

function adminHeaders(token) {
  return { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin', cookie: 'cclive_web=' + token };
}

(async () => {
  const prev = { ADMIN_KEY: process.env.ADMIN_KEY, MOD_KEY: process.env.MOD_KEY, TOOLS: process.env.MODERATION_TOOLS_ENABLED };
  process.env.ADMIN_KEY = ADMIN_KEY;
  process.env.MOD_KEY = MOD_KEY;
  delete process.env.MODERATION_TOOLS_ENABLED;
  try {
    assert.deepStrictEqual(chatMod.extractShareIds('hi #ITEM:ITEM-db2ac5 and #BUILD:BUILD-709125.'), [
      { type: 'ITEM', id: 'ITEM-db2ac5' }, { type: 'BUILD', id: 'BUILD-709125' }
    ]);
    assert.deepStrictEqual(chatMod.extractShareIds('#ITEM:ITEM'), []);
    assert.strictEqual(chatMod.parseDuration('5m', false).ms, 5 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('30m', false).key, '30m');
    assert.strictEqual(chatMod.parseDuration('2h', false).ms, 2 * 60 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('14d', false).key, '14d');
    assert.strictEqual(chatMod.parseDuration('10m', false).key, '10m');
    assert.strictEqual(chatMod.parseDuration('366d', false).ms, 366 * 24 * 60 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('0m', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('-1h', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('1', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('367d', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('9007199254740993m', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('permanent', false).code, 'confirm_required');
    assert.strictEqual(chatMod.parseDuration('permanent', true).ms, null);
    assert.strictEqual(chatMod.parseDuration('5m', true).key, '5m');
    const deletedPublic = chatMod.toPublicMessage({ id: 1, player: 'A', message: 'x', deleted_at: new Date() });
    assert.strictEqual(deletedPublic.message, chatMod.PLACEHOLDER);
    assert.strictEqual(deletedPublic.deleted, true);
    assert.strictEqual(deletedPublic.player, 'A');
    const typedPlaceholder = chatMod.toPublicMessage({ id: 2, player: 'A', message: chatMod.PLACEHOLDER, deleted_at: null });
    assert.strictEqual(typedPlaceholder.deleted, false);
    assert.strictEqual(typedPlaceholder.message, chatMod.PLACEHOLDER);

    const db = makeDb();
    const app = createApp(db);
    const token = (uuid) => ({ 'x-test-uuid': uuid });

    const noFeed = await staffFeed.authorizeStaffSocket(db.pool, '');
    assert.strictEqual(noFeed.ok, false);
    const modFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(modFeed.ok, true);
    const adminFeed = await staffFeed.authorizeStaffSocket(db.pool, 'other=1; cclive_web=' + db.sessions.admin.token);
    assert.strictEqual(adminFeed.ok, true);
    const savedUnlock = db.sessions.mod.mod_unlocked_until;
    db.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    const expiredFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(expiredFeed.ok, false);
    db.sessions.mod.mod_unlocked_until = savedUnlock;
    db.state.roles.delete(33);
    const revokedFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(revokedFeed.ok, false);
    db.state.roles.set(33, 'moderator');

    const feed = staffFeed.createStaffFeed(db.pool);
    const viewer = {
      readyState: 1,
      sent: [],
      send(text) { this.sent.push(JSON.parse(text)); },
      close(code) { this.readyState = 3; this.closedCode = code; }
    };
    const accepted = await feed.accept(viewer, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(accepted.ok, true);
    db.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    viewer.staffCheckedAt = 0;
    await feed.publish({ type: 'chat.message', id: 4, player: 'Plain', message: 'live' });
    assert.strictEqual(viewer.sent.length, 0);
    assert.strictEqual(viewer.readyState, 3);
    db.sessions.mod.mod_unlocked_until = savedUnlock;

    const merged = staffFeed.mergeStaffFeed(
      [{ id: 8, player: 'Plain', message: 'geheim', deleted: false }],
      [
        { id: 8, player: 'Plain', message: chatMod.PLACEHOLDER, deleted: true },
        { id: 9, player: 'Plain', message: 'neu', deleted: false },
        { id: 9, player: 'Plain', message: 'neu', deleted: false }
      ]
    );
    assert.strictEqual(merged.length, 2);
    assert.strictEqual(merged[0].deleted, true);
    assert.strictEqual(merged[0].message, chatMod.PLACEHOLDER);
    assert.ok(!JSON.stringify(merged).includes('geheim'));
    assert.strictEqual(merged[1].id, 9);
    db.state.messages.push(
      { id: 70, player: 'Plain', player_id: 44, message: 'geheim-original', timestamp: new Date(), deleted_at: new Date() },
      { id: 71, player: 'Plain', player_id: 44, message: 'sichtbar', timestamp: new Date(), deleted_at: null }
    );
    const caught = await chatMod.staffMessagesAfter(db.pool, 69, 20);
    assert.strictEqual(caught.find((row) => row.id === 70).message, chatMod.PLACEHOLDER);
    assert.strictEqual(caught.find((row) => row.id === 70).deleted, true);
    assert.strictEqual(caught.find((row) => row.id === 71).message, 'sichtbar');
    assert.ok(!JSON.stringify(caught).includes('geheim-original'));
    assert.deepStrictEqual(await chatMod.staffMessagesAfter(db.pool, 0, 20), []);
    db.state.messages = db.state.messages.filter((row) => row.id !== 70 && row.id !== 71);

    const send = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'hello #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125' } });
    assert.strictEqual(send.status, 200);
    assert.ok(send.json.id);

    const spoof = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: { ...token(OTHER_UUID) },
      body: { messageId: send.json.id, reason: 'testgrund', actor: 'Admin' }
    });
    assert.strictEqual(spoof.status, 403);

    const del1 = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(MOD_UUID),
      body: { messageId: send.json.id, reason: 'Beleidigung' }
    });
    assert.strictEqual(del1.status, 200);
    assert.strictEqual(del1.json.already, false);
    const del2 = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(UUID_A),
      body: { messageId: send.json.id, reason: 'nochmal' }
    });
    assert.strictEqual(del2.json.already, true);
    const publicRows = await chatMod.publicHistory(db.pool, {});
    assert.strictEqual(publicRows[0].message, chatMod.PLACEHOLDER);
    assert.strictEqual(publicRows[0].deleted, true);
    assert.strictEqual(publicRows[0].player, 'Plain');
    assert.ok(!JSON.stringify(publicRows).includes('hello'));
    assert.ok(!JSON.stringify(publicRows).includes('#ITEM:'));
    assert.ok(!JSON.stringify(publicRows).includes('#BUILD:'));
    const original = await chatMod.staffOriginal(db.pool, send.json.id);
    assert.ok(original.originalText.includes('#ITEM:ITEM-db2ac5'));
    assert.ok(original.originalText.includes('#BUILD:BUILD-709125'));
    assert.deepStrictEqual(original.shareIds.map((entry) => entry.id), ['ITEM-db2ac5', 'BUILD-709125']);
    assert.strictEqual(original.shareSnapshots.find((entry) => entry.id === 'ITEM-db2ac5').present, true);
    assert.strictEqual(original.shareSnapshots.find((entry) => entry.id === 'BUILD-709125').present, true);

    db.state.messages = db.state.messages.filter((m) => false); // 24h chat cleanup
    assert.strictEqual(db.state.redactions.length, 1);

    const mute = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'Spamtest', actor: 'Plain', actorPlayerId: 44 }
    });
    assert.strictEqual(mute.status, 200);
    assert.strictEqual(mute.json.ok, true);
    assert.strictEqual(mute.json.stored, true);
    assert.strictEqual(mute.json.liveUpdated, true);
    const muteAgain = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'Spamtest2' }
    });
    assert.strictEqual(muteAgain.json.already, true);
    const mutedSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'nochwas' } });
    assert.strictEqual(mutedSend.status, 403);
    assert.strictEqual(mutedSend.json.code, 'muted');
    const mutedHistory = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(mutedHistory.status, 200);

    const ban = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(UUID_A),
      body: { player: 'Plain', duration: 'permanent', confirm: true, reason: 'Schwere Störung' }
    });
    assert.strictEqual(ban.status, 200);
    const bannedSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'x' } });
    assert.strictEqual(bannedSend.json.code, 'banned');
    const bannedHistory = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(bannedHistory.status, 403);

    const unmute = await request(app, {
      method: 'POST', path: '/moderation/actions/unmute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', reason: 'Mute weg' }
    });
    assert.strictEqual(unmute.status, 200);
    const stillBanned = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'x' } });
    assert.strictEqual(stillBanned.json.code, 'banned');

    const webBanLift = await request(app, {
      method: 'POST', path: '/staff/actions/unban',
      headers: adminHeaders(db.sessions.mod.token),
      body: { player: 'Plain', reason: 'Website Aufhebung' }
    });
    assert.strictEqual(webBanLift.status, 200);
    const afterLift = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'wieder da' } });
    assert.strictEqual(afterLift.status, 200);

    const expired = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'mute', duration: '10m', confirmPermanent: false,
      actorPlayerId: 11, source: 'minecraft', reason: 'kurz'
    });
    const row = db.state.sanctions.find((s) => s.id === expired.sanctionId);
    row.expires_at = new Date(Date.now() - 1000);
    const afterExpire = await chatMod.denyChatSend(db.pool, 44);
    assert.strictEqual(afterExpire, null);

    process.env.MODERATION_TOOLS_ENABLED = '0';
    const toolsOff = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'darf nicht' }
    });
    assert.strictEqual(toolsOff.json.code, 'tools_disabled');
    delete process.env.MODERATION_TOOLS_ENABLED;
    const publicStill = chatMod.toPublicMessage({ id: 1, message: 'secret', deleted_at: new Date() });
    assert.strictEqual(publicStill.message, chatMod.PLACEHOLDER);
    assert.strictEqual(publicStill.deleted, true);

    const custom = await request(app, {
      method: 'POST', path: '/staff/actions/mute',
      headers: adminHeaders(db.sessions.admin.token),
      body: { player: 'Plain', duration: '5m', reason: 'eigene dauer', actor: 'Plain', actorPlayerId: 44, confirm: false }
    });
    assert.strictEqual(custom.status, 200);
    assert.strictEqual(custom.json.ok, true);
    const customEvent = (await chatMod.listEvents(db.pool, 20)).find((entry) => entry.reason === 'eigene dauer');
    assert.strictEqual(customEvent.actor, 'Admin');
    assert.strictEqual(customEvent.payload.actorName, 'Admin');
    assert.strictEqual(customEvent.duration, '5m');
    assert.strictEqual(customEvent.permanent, false);
    const remaining = new Date(customEvent.expiresAt).getTime() - Date.now();
    assert.ok(remaining > 4 * 60 * 1000 && remaining < 6 * 60 * 1000);
    const noConfirm = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: 'permanent', reason: 'ohne bestaetigung', confirm: false }
    });
    assert.strictEqual(noConfirm.status, 400);
    assert.strictEqual(noConfirm.json.code, 'confirm_required');
    db.players.push({
      id: 66,
      display_name: 'Citizen',
      display_name_normalized: 'citizen',
      mc_uuid: '33333333-3333-3333-3333-333333333333'
    });
    const twoWeeks = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(MOD_UUID),
      body: { player: 'Citizen', duration: '14d', reason: 'zwei wochen' }
    });
    assert.strictEqual(twoWeeks.status, 200);
    assert.strictEqual(twoWeeks.json.ok, true);
    const weekEvent = (await chatMod.listEvents(db.pool, 20)).find((entry) => entry.reason === 'zwei wochen');
    assert.strictEqual(weekEvent.duration, '14d');
    assert.strictEqual(weekEvent.actor, 'ModUser');
    const weekLeft = new Date(weekEvent.expiresAt).getTime() - Date.now();
    assert.ok(weekLeft > 13 * 24 * 60 * 60 * 1000 && weekLeft < 15 * 24 * 60 * 60 * 1000);

    db.state.sanctions.push({
      id: 77, player_id: 55, kind: 'mute', lifted_at: null, expires_at: null, reason_text: 'geist'
    });
    const muteSuggest = await request(app, {
      path: '/moderation/actions/active?kind=mute',
      headers: token(MOD_UUID)
    });
    assert.strictEqual(muteSuggest.status, 200);
    const muteNames = muteSuggest.json.sanctions.map((entry) => entry.player);
    assert.ok(muteNames.includes('Plain'));
    assert.ok(!muteNames.includes('Ghost'));
    const banSuggest = await request(app, {
      path: '/moderation/actions/active?kind=ban',
      headers: token(MOD_UUID)
    });
    assert.ok(banSuggest.json.sanctions.map((entry) => entry.player).includes('Citizen'));
    assert.ok(!banSuggest.json.sanctions.map((entry) => entry.player).includes('Plain'));
    const clearCustom = await request(app, {
      method: 'POST', path: '/moderation/actions/unmute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', reason: 'Testende' }
    });
    assert.strictEqual(clearCustom.status, 200);

    db.state.seasons = [{ id: 2, is_current: 1 }, { id: 3, is_current: 1 }];
    const skipped = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(skipped.skipped, true);
    db.state.seasons = [];
    const skippedEmpty = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(skippedEmpty.skipped, true);
    db.state.seasons = [{ id: 3, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') }];
    db.state.sanctions.push({
      id: 99, player_id: 44, kind: 'ban', lifted_at: null, expires_at: null,
      created_season_id: 2, reason_text: 'alt'
    });
    const cleaned = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(cleaned.skipped, false);
    assert.ok(db.state.sanctions.some((s) => s.id === 99));
    assert.ok(db.state.leaderboard.length === 1);

    const ambiguous = await chatMod.uniqueCurrentSeason({
      async query() { return [[{ id: 1 }, { id: 2 }]]; }
    });
    assert.strictEqual(ambiguous.ok, false);

    const clock = new Date('2026-08-01T00:00:00Z').getTime();
    const classified = chatMod.classifySeasons([
      { id: 5, is_current: 1, start_at: '2026-06-01T00:00:00Z' },
      { id: 11, is_current: 0, start_at: '2025-01-01T00:00:00Z' },
      { id: 1, is_current: 0, start_at: '2027-01-01T00:00:00Z' },
      { id: 4, is_current: 0, start_at: null },
      { id: 8, is_current: 2, start_at: '2020-01-01T00:00:00Z' },
      { id: 9, is_current: null, start_at: '2020-01-01T00:00:00Z' }
    ], clock);
    assert.deepStrictEqual(classified.endedIds, [11]);
    assert.strictEqual(chatMod.classifySeasons([
      { id: 5, is_current: 1, start_at: null },
      { id: 11, is_current: 0, start_at: '2020-01-01T00:00:00Z' }
    ], clock).reason, 'current_start_unknown');

    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: 'Plain', chatBanned: false }), true);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: 'Plain', chatBanned: true }), false);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: null, chatBanned: false }), false);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 0, player: 'Plain', chatBanned: false }), false);

    const missingHistory = await request(app, { path: '/chat/messages' });
    assert.strictEqual(missingHistory.status, 401);
    assert.ok(!String(missingHistory.raw).includes('wieder da'));
    const invalidHistory = await request(app, {
      path: '/chat/messages',
      headers: { 'x-test-uuid': '00000000-0000-0000-0000-000000000000' }
    });
    assert.strictEqual(invalidHistory.status, 403);
    assert.ok(!String(invalidHistory.raw).includes('wieder da'));

    const notFound = await request(app, {
      method: 'POST',
      path: '/staff/actions/mute',
      headers: adminHeaders(db.sessions.admin.token),
      body: { player: 'Plain', duration: '10m', reason: 'ohne Fund', messageId: 9999 }
    });
    assert.strictEqual(notFound.status, 404);
    assert.strictEqual(notFound.json.code, 'not_found');
    assert.ok(!db.state.sanctions.some((entry) => entry.reason_text === 'ohne Fund'));

    const historyEvents = await chatMod.listEvents(db.pool, 100);
    const deletedEvent = historyEvents.find((entry) => entry.kind === 'message_delete');
    assert.strictEqual(deletedEvent.reason, 'Beleidigung');
    assert.ok(deletedEvent.payload.originalText.includes('hello #ITEM:ITEM-db2ac5'));
    assert.deepStrictEqual(deletedEvent.payload.shareIds.map((entry) => entry.id), ['ITEM-db2ac5', 'BUILD-709125']);
    assert.strictEqual(deletedEvent.actor, 'ModUser');
    assert.strictEqual(deletedEvent.payload.actorName, 'ModUser');
    const muteEvent = historyEvents.find((entry) => entry.kind === 'mute' && entry.reason === 'Spamtest');
    assert.strictEqual(muteEvent.actor, 'ModUser');
    assert.strictEqual(muteEvent.payload.actorName, 'ModUser');
    assert.notStrictEqual(muteEvent.payload.actorName, 'Plain');
    assert.strictEqual(muteEvent.duration, '10m');
    assert.strictEqual(muteEvent.permanent, false);
    assert.ok(muteEvent.expiresAt);
    assert.ok(muteEvent.sanctionId);
    const liftEvent = historyEvents.find((entry) => entry.kind === 'unmute' && entry.reason === 'Mute weg');
    assert.strictEqual(liftEvent.reason, 'Mute weg');
    assert.ok(liftEvent.sanctionIds.length >= 1);
    assert.strictEqual(liftEvent.payload.sanctions[0].duration, '10m');
    assert.ok(liftEvent.payload.sanctions[0].sanctionId);
    const banEvent = historyEvents.find((entry) => entry.kind === 'ban' && entry.reason === 'Schwere Störung');
    assert.strictEqual(banEvent.actor, 'Admin');
    assert.strictEqual(banEvent.payload.actorName, 'Admin');
    const unbanEvent = historyEvents.find((entry) => entry.kind === 'unban');
    assert.strictEqual(unbanEvent.actor, 'ModUser');
    assert.strictEqual(unbanEvent.payload.actorName, 'ModUser');
    assert.strictEqual(banEvent.reason, 'Schwere Störung');
    assert.strictEqual(banEvent.permanent, true);
    assert.strictEqual(banEvent.duration, 'permanent');
    assert.strictEqual(banEvent.expiresAt, null);
    const storedMute = db.state.sanctions.find((entry) => entry.id === muteEvent.sanctionId);
    storedMute.reason_text = 'GEAENDERT';
    const reread = await chatMod.listEvents(db.pool, 100);
    assert.strictEqual(reread.find((entry) => entry.id === muteEvent.id).reason, 'Spamtest');

    db.state.messages.push({
      id: 501,
      player: 'Plain',
      player_id: 44,
      message: 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125',
      timestamp: new Date('2026-08-01T12:00:00Z'),
      deleted_at: null
    });
    db.state.failEvidence = true;
    const sanctionIdsBefore = new Set(db.state.sanctions.map((entry) => entry.id));
    try {
      await assert.rejects(() => chatMod.applySanction(db.pool, {
        playerName: 'Plain', kind: 'mute', duration: '10m', confirmPermanent: false,
        actorPlayerId: 11, source: 'minecraft', reason: 'atomar', messageId: 501
      }));
    } finally {
      db.state.failEvidence = false;
    }
    assert.ok(db.state.sanctions.every((entry) => sanctionIdsBefore.has(entry.id)));
    assert.ok(!db.state.evidence.some((entry) => String(entry.snapshot_text || '').includes('boom')));

    const linked = await request(app, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'wegen Nachricht', messageId: 501 }
    });
    assert.strictEqual(linked.status, 200, JSON.stringify(linked.json));
    assert.strictEqual(linked.json.already, false);
    const linkedRow = db.state.sanctions.find((entry) => entry.reason_text === 'wegen Nachricht');
    assert.strictEqual(linkedRow.message_id, 501);
    const proof = db.state.evidence.find((entry) => entry.sanction_id === linkedRow.id);
    assert.strictEqual(proof.snapshot_text, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    const proofShares = JSON.parse(proof.share_snapshots);
    assert.strictEqual(proofShares.find((entry) => entry.id === 'ITEM-db2ac5').present, true);
    assert.strictEqual(proofShares.find((entry) => entry.id === 'BUILD-709125').present, true);
    const linkedEvent = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.reason === 'wegen Nachricht');
    assert.strictEqual(linkedEvent.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    assert.strictEqual(linkedEvent.messageId, 501);
    linkedRow.reason_text = 'SPAETER';
    db.state.messages.find((entry) => entry.id === 501).message = chatMod.PLACEHOLDER;
    const afterMutation = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.id === linkedEvent.id);
    assert.strictEqual(afterMutation.reason, 'wegen Nachricht');
    assert.strictEqual(afterMutation.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');

    const seededBan = db.state.sanctions.find((entry) => entry.id === 99);
    if (seededBan) seededBan.lifted_at = new Date();
    db.state.messages.push({
      id: 502, player: 'ModUser', player_id: 33, message: 'fremd', timestamp: new Date(), deleted_at: null
    });
    const mismatch = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '1h', confirmPermanent: false,
      actorPlayerId: 11, source: 'website', reason: 'falsche Nachricht', messageId: 502
    });
    assert.strictEqual(mismatch.code, 'message_player_mismatch');
    assert.ok(!db.state.sanctions.some((entry) => entry.reason_text === 'falsche Nachricht'));

    const evidenceBeforePlain = db.state.evidence.length;
    const withoutMessage = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '1h', confirmPermanent: false,
      actorPlayerId: 11, source: 'minecraft', reason: 'ohne Nachricht'
    });
    assert.strictEqual(withoutMessage.already, false);
    assert.strictEqual(withoutMessage.messageId, null);
    assert.strictEqual(db.state.evidence.length, evidenceBeforePlain);
    assert.strictEqual(db.state.sanctions.find((entry) => entry.id === withoutMessage.sanctionId).message_id, null);

    const bannedHistoryAgain = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(bannedHistoryAgain.status, 403);
    const staffWhileBanned = await request(app, {
      path: '/staff/messages',
      headers: adminHeaders(db.sessions.mod.token)
    });
    assert.strictEqual(staffWhileBanned.status, 200);
    const ingameStaffEvents = await request(app, { path: '/moderation/events', headers: token(MOD_UUID) });
    assert.strictEqual(ingameStaffEvents.status, 200);
    assert.ok(ingameStaffEvents.json.events.some((entry) => entry.reason === 'wegen Nachricht'));

    await chatMod.liftSanction(db.pool, {
      playerName: 'Plain', kind: 'ban', actorPlayerId: 11, source: 'minecraft', reason: 'Bann weg'
    });
    db.state.messages.find((entry) => entry.id === 501).message = 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125';
    const removed = await chatMod.deleteChatMessage(db.pool, {
      messageId: 501, actorPlayerId: 33, source: 'minecraft', reason: 'Text sichern'
    });
    assert.strictEqual(removed.placeholder, chatMod.PLACEHOLDER);
    const fromDeleted = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '24h', confirmPermanent: false,
      actorPlayerId: 33, source: 'minecraft', reason: 'Beleg bleibt', messageId: 501
    });
    assert.strictEqual(fromDeleted.already, false);
    const kept = db.state.evidence.find((entry) => entry.sanction_id === fromDeleted.sanctionId);
    assert.strictEqual(kept.snapshot_text, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    assert.notStrictEqual(kept.snapshot_text, chatMod.PLACEHOLDER);
    const deleteRecord = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.reason === 'Text sichern');
    assert.strictEqual(deleteRecord.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');

    const preserved = db.state.events.length;
    db.state.seasons = [
      { id: 5, is_current: 1, start_at: null },
      { id: 11, is_current: 0, start_at: new Date('2025-01-01T00:00:00Z') }
    ];
    const unknownStart = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(unknownStart.skipped, true);
    assert.strictEqual(unknownStart.reason, 'current_start_unknown');
    assert.strictEqual(db.state.events.length, preserved);

    db.state.seasons = [
      { id: 5, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') },
      { id: 11, is_current: 1, start_at: new Date('2025-01-01T00:00:00Z') }
    ];
    const ambiguousCleanup = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(ambiguousCleanup.skipped, true);
    assert.strictEqual(db.state.events.length, preserved);

    db.state.seasons = [
      { id: 5, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') },
      { id: 11, is_current: 0, start_at: new Date('2025-01-01T00:00:00Z') },
      { id: 1, is_current: 0, start_at: new Date('2027-01-01T00:00:00Z') },
      { id: 4, is_current: 0, start_at: null },
      { id: 8, is_current: 2, start_at: new Date('2020-01-01T00:00:00Z') }
    ];
    db.state.events.push(
      { id: 1001, season_id: 11, payload: '{"reason":"alt"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1002, season_id: 1, payload: '{"reason":"spaeter"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1003, season_id: 4, payload: '{"reason":"unklar"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1004, season_id: 100, payload: '{"reason":"waise"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1008, season_id: 8, payload: '{"reason":"flagge"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() }
    );
    db.state.redactions.push(
      { message_id: 77, season_id: 11, original_text: 'alttext' },
      { message_id: 78, season_id: 1, original_text: 'spaetertext' }
    );
    db.state.evidence.push(
      { season_id: 11, snapshot_text: 'altbeleg', message_id: 77 },
      { season_id: 1, snapshot_text: 'spaeterbeleg', message_id: 78 }
    );
    db.state.sanctions.push(
      { id: 201, player_id: 44, kind: 'ban', lifted_at: null, expires_at: null, created_season_id: 11, reason_text: 'aktiv bleibt' },
      { id: 202, player_id: 44, kind: 'mute', lifted_at: new Date(), expires_at: null, created_season_id: 11, reason_text: 'aufgehoben' },
      { id: 203, player_id: 44, kind: 'mute', lifted_at: null, expires_at: new Date(Date.now() - 5000), created_season_id: 1, reason_text: 'zukunft abgelaufen' },
      { id: 204, player_id: 44, kind: 'mute', lifted_at: null, expires_at: new Date(Date.now() - 5000), created_season_id: 11, reason_text: 'alt abgelaufen' }
    );
    db.state.sqlLog.length = 0;
    const seasonCleanup = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(seasonCleanup.skipped, false);
    assert.deepStrictEqual(seasonCleanup.endedSeasonIds.map(Number), [11]);
    assert.ok(!db.state.events.some((entry) => entry.id === 1001));
    assert.ok(db.state.events.some((entry) => entry.id === 1002));
    assert.ok(db.state.events.some((entry) => entry.id === 1003));
    assert.ok(db.state.events.some((entry) => entry.id === 1004));
    assert.ok(db.state.events.some((entry) => entry.id === 1008));
    assert.ok(db.state.events.some((entry) => entry.season_id === 2));
    assert.ok(!db.state.redactions.some((entry) => entry.message_id === 77));
    assert.ok(db.state.redactions.some((entry) => entry.message_id === 78));
    assert.ok(!db.state.evidence.some((entry) => entry.snapshot_text === 'altbeleg'));
    assert.ok(db.state.evidence.some((entry) => entry.snapshot_text === 'spaeterbeleg'));
    assert.ok(db.state.sanctions.some((entry) => entry.id === 201));
    assert.ok(!db.state.sanctions.some((entry) => entry.id === 202));
    assert.ok(db.state.sanctions.some((entry) => entry.id === 203));
    assert.ok(!db.state.sanctions.some((entry) => entry.id === 204));
    assert.strictEqual(db.state.leaderboard.length, 1);
    assert.ok(db.state.sqlLog.some((sql) => sql.includes('is_current = 0') && sql.includes('start_at <')));
    assert.ok(!db.state.sqlLog.some((sql) => sql.includes('season_id <>')));

    db.state.events.push({
      id: 1005, season_id: 11, payload: '{"reason":"wieder da"}', kind: 'mute',
      actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date()
    });
    db.state.beforeSeasonDelete = () => {
      db.state.seasons.find((entry) => entry.id === 11).is_current = 1;
      db.state.seasons.find((entry) => entry.id === 5).is_current = 0;
    };
    const raced = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(raced.skipped, false);
    assert.ok(db.state.events.some((entry) => entry.id === 1005));
    assert.strictEqual(db.state.seasons.find((entry) => entry.id === 11).is_current, 1);

    const liveDb = makeDb();
    liveDb.state.seasons = [{ id: 2, is_current: 1, start_at: new Date('2026-01-01T00:00:00Z') }];
    let liveAttempts = 0;
    const rejectingApp = createApp(liveDb, {
      onSanctionChanged: async () => {
        liveAttempts += 1;
        throw new Error('socket down');
      }
    });
    const liveMute = await request(rejectingApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'live weg' }
    });
    assert.strictEqual(liveMute.status, 200);
    assert.strictEqual(liveMute.json.ok, false);
    assert.strictEqual(liveMute.json.stored, true);
    assert.strictEqual(liveMute.json.liveUpdated, false);
    assert.strictEqual(liveMute.json.code, 'live_update_failed');
    assert.ok(String(liveMute.json.error).includes('gespeichert'));
    assert.strictEqual(liveAttempts, 1);
    assert.ok(liveDb.state.sanctions.some((entry) => entry.reason_text === 'live weg' && !entry.lifted_at));
    const stillMuted = await request(rejectingApp, {
      method: 'POST',
      path: '/chat/send',
      headers: token(OTHER_UUID),
      body: { message: 'darf nicht' }
    });
    assert.strictEqual(stillMuted.status, 403);
    assert.strictEqual(stillMuted.json.code, 'muted');
    const liveAgain = await request(rejectingApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'nochmal live' }
    });
    assert.strictEqual(liveAgain.json.already, true);
    assert.strictEqual(liveAgain.json.ok, true);
    assert.strictEqual(liveAgain.json.stored, true);
    assert.strictEqual(liveAttempts, 1);
    const liveLift = await request(rejectingApp, {
      method: 'POST',
      path: '/staff/actions/unmute',
      headers: adminHeaders(liveDb.sessions.mod.token),
      body: { player: 'Plain', reason: 'live aufheben' }
    });
    assert.strictEqual(liveLift.status, 200);
    assert.strictEqual(liveLift.json.ok, false);
    assert.strictEqual(liveLift.json.stored, true);
    assert.strictEqual(liveLift.json.liveUpdated, false);
    assert.ok(liveDb.state.sanctions.some((entry) => entry.reason_text === 'live weg' && entry.lifted_at));
    const afterLiveLift = await request(rejectingApp, {
      method: 'POST',
      path: '/chat/send',
      headers: token(OTHER_UUID),
      body: { message: 'wieder erlaubt' }
    });
    assert.strictEqual(afterLiveLift.status, 200);
    assert.strictEqual(liveAttempts, 2);

    const fs = require('fs');
    const path = require('path');
    const { WebSocketServer, WebSocket } = require('./backend-require')('ws');
    const { attachWebSocketDispatch } = require('../../../backend_server/website_backend/websocket-dispatch');
    const serverSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/server.js'), 'utf8');
    const feedSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/website_backend/staff-chat-feed.js'), 'utf8');
    const clientSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/public/staff-moderation.js'), 'utf8');
    assert.ok(serverSource.includes('noServer: true'));
    assert.ok(serverSource.includes("'/ws/chat': wss"));
    assert.ok(serverSource.includes("'/ws/staff-chat': staffFeed.wss"));
    assert.ok(!serverSource.includes("path: '/ws/chat'"));
    assert.ok(!feedSource.includes("path: '/ws/staff-chat'"));
    assert.ok(clientSource.includes("data.type === 'staff.ready'"));
    assert.ok(!/addEventListener\('open'[\s\S]{0,240}staff\.resume/.test(clientSource));

    const sockDb = makeDb();
    sockDb.state.delaySessions = 200;
    sockDb.state.delayAfterQuery = 150;
    sockDb.state.messages = [];
    for (let id = 1; id <= 103; id += 1) {
      sockDb.state.messages.push({
        id,
        player: 'Plain',
        player_id: 44,
        message: id === 3 ? 'original-drei' : 'm-' + id,
        timestamp: new Date(),
        deleted_at: id === 3 ? new Date() : null
      });
    }
    const chatWss = new WebSocketServer({ noServer: true });
    chatWss.on('connection', (ws) => {
      ws.on('message', (raw) => {
        const data = JSON.parse(String(raw));
        if (data.type === 'ping') ws.send(JSON.stringify({ type: 'pong', path: 'chat' }));
      });
    });
    const mounted = staffFeed.attachStaffChatFeed(sockDb.pool);
    const httpServer = http.createServer((req, res) => {
      res.writeHead(404);
      res.end();
    });
    attachWebSocketDispatch(httpServer, {
      '/ws/chat': chatWss,
      '/ws/staff-chat': mounted.wss
    });
    await new Promise((resolve) => httpServer.listen(0, '127.0.0.1', resolve));
    const port = httpServer.address().port;
    const sockets = [];
    function openWs(urlPath, headers) {
      return new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + urlPath, { headers });
        sockets.push(ws);
        const messages = [];
        ws.on('message', (buf) => messages.push(JSON.parse(String(buf))));
        ws.on('open', () => resolve({ ws, messages }));
        ws.on('error', reject);
      });
    }
    try {
      const chat = await openWs('/ws/chat');
      const staff = await openWs('/ws/staff-chat', { Cookie: 'cclive_web=' + sockDb.sessions.mod.token });
      staff.ws.send(JSON.stringify({ type: 'staff.resume', afterId: 3, knownIds: [1, 2, 3] }));
      const pong = await new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('chat pong timeout')), 2000);
        chat.ws.once('message', (buf) => {
          clearTimeout(timer);
          resolve(JSON.parse(String(buf)));
        });
        chat.ws.send(JSON.stringify({ type: 'ping' }));
      });
      assert.strictEqual(pong.type, 'pong');
      assert.strictEqual(pong.path, 'chat');
      setTimeout(() => {
        mounted.publish({ type: 'chat.message', id: 200, player: 'Plain', message: 'waehrend-nachladen', deleted: false }).catch(() => {});
      }, 280);
      const deadline = Date.now() + 4000;
      while (Date.now() < deadline) {
        const ids = new Set();
        for (const msg of staff.messages) {
          for (const row of msg.messages || []) ids.add(row.id);
        }
        const pushed = staff.messages.some((msg) => msg.type === 'chat.message' && msg.id === 200);
        if (staff.messages[0] && staff.messages[0].type === 'staff.ready' && ids.has(3) && ids.has(103) && pushed) break;
        await new Promise((resolve) => setTimeout(resolve, 20));
      }
      assert.strictEqual(staff.messages[0].type, 'staff.ready');
      assert.ok(staff.messages.every((msg, index) => index === 0 || msg.type !== 'staff.ready'));
      let live = [
        { id: 1, player: 'Plain', message: 'm-1', deleted: false },
        { id: 2, player: 'Plain', message: 'm-2', deleted: false },
        { id: 3, player: 'Plain', message: 'original-drei', deleted: false }
      ];
      const catchups = staff.messages.filter((msg) => msg.type === 'staff.catchup');
      assert.ok(catchups.length >= 2);
      let delivered = 0;
      for (const frame of catchups) {
        delivered += frame.messages.length;
        assert.ok(!JSON.stringify(frame).includes('original-drei'));
        live = staffFeed.mergeStaffFeed(live, frame.messages);
      }
      assert.ok(delivered > 80);
      assert.strictEqual(live.find((row) => row.id === 3).message, chatMod.PLACEHOLDER);
      assert.strictEqual(live.find((row) => row.id === 3).deleted, true);
      for (let id = 1; id <= 103; id += 1) assert.strictEqual(live.filter((row) => row.id === id).length, 1);
      const pushed = staff.messages.filter((msg) => msg.type === 'chat.message' && msg.id === 200);
      assert.strictEqual(pushed.length, 1);
      live = staffFeed.mergeStaffFeed(live, pushed);
      await mounted.publish({ type: 'chat.message', id: 50, player: 'Plain', message: 'm-50', deleted: false });
      await new Promise((resolve) => setTimeout(resolve, 30));
      const repeated = staff.messages.filter((msg) => msg.type === 'chat.message' && msg.id === 50);
      assert.strictEqual(repeated.length, 1);
      live = staffFeed.mergeStaffFeed(live, repeated);
      live = staffFeed.mergeStaffFeed(live, await chatMod.staffHistory(sockDb.pool, 80));
      live = staffFeed.mergeStaffFeed(live, repeated);
      for (let id = 1; id <= 103; id += 1) assert.strictEqual(live.filter((row) => row.id === id).length, 1);
      assert.strictEqual(live.filter((row) => row.id === 200).length, 1);
      assert.strictEqual(live.find((row) => row.id === 3).deleted, true);
      assert.ok(!JSON.stringify(live).includes('original-drei'));

      const deniedMessages = [];
      const denied = await new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/staff-chat');
        sockets.push(ws);
        const timer = setTimeout(() => reject(new Error('denied timeout')), 2000);
        ws.on('message', (buf) => deniedMessages.push(String(buf)));
        ws.on('close', (code) => { clearTimeout(timer); resolve(code); });
        ws.on('error', () => {});
      });
      assert.strictEqual(denied, 4401);
      assert.strictEqual(deniedMessages.length, 0);

      sockDb.state.roles.delete(33);
      const revokedMessages = [];
      const revoked = await new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/staff-chat', {
          headers: { Cookie: 'cclive_web=' + sockDb.sessions.mod.token }
        });
        sockets.push(ws);
        const timer = setTimeout(() => reject(new Error('revoked timeout')), 2000);
        ws.on('message', (buf) => revokedMessages.push(String(buf)));
        ws.on('close', (code) => { clearTimeout(timer); resolve(code); });
        ws.on('error', () => {});
      });
      assert.strictEqual(revoked, 4401);
      assert.strictEqual(revokedMessages.length, 0);
      assert.ok(!revokedMessages.join('').includes('m-10'));

      const unknown = await new Promise((resolve) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/other');
        sockets.push(ws);
        const timer = setTimeout(() => resolve('timeout'), 1500);
        ws.on('open', () => { clearTimeout(timer); resolve('open'); });
        ws.on('error', (err) => { clearTimeout(timer); resolve(err.message); });
      });
      assert.ok(String(unknown).includes('404'));
    } finally {
      for (const ws of sockets) {
        try { ws.close(); } catch (err) { /* bereits zu */ }
      }
      await new Promise((resolve) => chatWss.close(resolve));
      await new Promise((resolve) => mounted.wss.close(resolve));
      await new Promise((resolve) => httpServer.close(resolve));
    }

    const teamDb = makeDb();
    teamDb.players.push({
      id: 34,
      display_name: 'ModTwo',
      display_name_normalized: 'modtwo',
      mc_uuid: '22222222-2222-2222-2222-222222222222'
    });
    teamDb.state.roles.set(34, 'moderator');
    const teamApp = createApp(teamDb);
    const mc = (uuid) => ({ 'x-test-uuid': uuid });
    const activeSanctions = (id) => teamDb.state.sanctions.filter((row) => row.player_id === id && !row.lifted_at);
    const allowedMute = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(MOD_UUID),
      body: { player: 'Plain', duration: '5m', reason: 'normaler Spieler' }
    });
    assert.strictEqual(allowedMute.status, 200);
    assert.ok(activeSanctions(44).some((row) => row.kind === 'mute'));
    const eventsBefore = teamDb.state.events.length;
    const sanctionsBefore = teamDb.state.sanctions.length;
    const againstMod = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(MOD_UUID),
      body: { player: 'ModTwo', duration: '5m', reason: 'gegen Moderator', role: 'player', actor: 'Admin' }
    });
    assert.strictEqual(againstMod.status, 403);
    assert.strictEqual(againstMod.json.code, 'team_protected');
    assert.ok(String(againstMod.json.error).includes('Teammitglieder'));
    assert.strictEqual(teamDb.state.events.length, eventsBefore);
    assert.strictEqual(teamDb.state.sanctions.length, sanctionsBefore);
    const againstAdmin = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/ban',
      headers: mc(MOD_UUID),
      body: { player: 'Admin', duration: '10m', reason: 'gegen Admin' }
    });
    assert.strictEqual(againstAdmin.status, 403);
    assert.strictEqual(againstAdmin.json.code, 'team_protected');
    assert.strictEqual(teamDb.state.events.length, eventsBefore);
    const adminOnMod = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(UUID_A),
      body: { player: 'ModTwo', duration: '5m', reason: 'Admin darf' }
    });
    assert.strictEqual(adminOnMod.status, 200);
    assert.ok(activeSanctions(34).some((row) => row.kind === 'mute'));
    teamDb.state.messages.push(
      { id: 901, player: 'ModTwo', player_id: 34, message: 'mod-original', timestamp: new Date(), deleted_at: null },
      { id: 902, player: 'Admin', player_id: 11, message: 'admin-original', timestamp: new Date(), deleted_at: null },
      { id: 903, player: 'Plain', player_id: 44, message: 'plain-original', timestamp: new Date(), deleted_at: null }
    );
    const deleteMod = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 901, reason: 'weg damit', player: 'Plain', role: 'user' }
    });
    assert.strictEqual(deleteMod.status, 403);
    assert.strictEqual(deleteMod.json.code, 'team_protected');
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 901).message, 'mod-original');
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 901).deleted_at, null);
    assert.strictEqual(teamDb.state.redactions.length, 0);
    const deleteAdminMsg = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 902, reason: 'admin weg' }
    });
    assert.strictEqual(deleteAdminMsg.status, 403);
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 902).message, 'admin-original');
    const deletePlain = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 903, reason: 'plain weg' }
    });
    assert.strictEqual(deletePlain.status, 200);
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 903).message, chatMod.PLACEHOLDER);
    const websiteAdmin = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/ban',
      headers: adminHeaders(teamDb.sessions.admin.token),
      body: { player: 'ModUser', duration: '10m', reason: 'Website-Admin' }
    });
    assert.strictEqual(websiteAdmin.status, 200);
    assert.ok(activeSanctions(33).some((row) => row.kind === 'ban'));

    console.log('chat-moderation tests ok');
  } finally {
    process.env.ADMIN_KEY = prev.ADMIN_KEY;
    process.env.MOD_KEY = prev.MOD_KEY;
    if (prev.TOOLS == null) delete process.env.MODERATION_TOOLS_ENABLED;
    else process.env.MODERATION_TOOLS_ENABLED = prev.TOOLS;
  }
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
