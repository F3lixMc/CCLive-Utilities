'use strict';
const assert = require('assert');
const staffRoles = require('../../../backend_server/website_backend/staff-roles');

const REAL_ADMIN = '47a5c2fc-b317-4180-b96f-ca977c51eb62';
const MOD_UUID = '069a79f4-44e1-4626-a5b0-bac67538bf5a';
const OTHER_UUID = '11111111-1111-1111-1111-111111111111';
const FELIX_UUID = '6a814aa9-acd6-4180-a84c-a816632d7aa1';

function likeEscapeMatch(value, pattern) {
  let i = 0;
  let j = 0;
  while (i < pattern.length) {
    if (pattern[i] === '\\' && i + 1 < pattern.length) {
      if (value[j] !== pattern[i + 1]) return false;
      i += 2;
      j += 1;
      continue;
    }
    if (pattern[i] === '%') return true;
    if (value[j] !== pattern[i]) return false;
    i += 1;
    j += 1;
  }
  return j === value.length;
}

function makeStore(options = {}) {
  const players = [
    { id: 1, display_name: 'Admin', display_name_normalized: 'admin', mc_uuid: REAL_ADMIN },
    { id: 2, display_name: 'ModUser', display_name_normalized: 'moduser', mc_uuid: MOD_UUID },
    { id: 3, display_name: 'Ghost', display_name_normalized: 'ghost', mc_uuid: '' },
    { id: 4, display_name: 'TwinA', display_name_normalized: 'twin', mc_uuid: OTHER_UUID },
    { id: 5, display_name: 'TwinB', display_name_normalized: 'twin', mc_uuid: '22222222-2222-2222-2222-222222222222' },
    { id: 6, display_name: 'F3lixMc_', display_name_normalized: 'f3lixmc_', mc_uuid: FELIX_UUID },
    { id: 7, display_name: 'F3lixMcY', display_name_normalized: 'f3lixmcy', mc_uuid: '33333333-3333-3333-3333-333333333333' },
    { id: 8, display_name: 'foo_bar', display_name_normalized: 'foo_bar', mc_uuid: '44444444-4444-4444-4444-444444444444' },
    { id: 9, display_name: 'foobar', display_name_normalized: 'foobar', mc_uuid: '55555555-5555-5555-5555-555555555555' }
  ];
  const roles = new Map();
  const sessions = [
    { id: 9, account_id: 20, player_id: 2, mod_unlocked_until: new Date(), mod_unlock_key_hash: 'old-hash' }
  ];
  const accounts = [{ id: 20, player_id: 2 }];
  const insertedPlayers = [];
  const likeParams = [];
  let lock = Promise.resolve();

  function snapshot() {
    return {
      roles: new Map(roles),
      sessions: sessions.map((row) => ({ ...row }))
    };
  }

  function restore(copy) {
    roles.clear();
    for (const [key, value] of copy.roles) roles.set(key, value);
    sessions.splice(0, sessions.length, ...copy.sessions.map((row) => ({ ...row })));
  }

  async function query(sql, params) {
    if (/FROM players[\s\S]*display_name_normalized = \?/.test(sql) && /LIMIT 3/.test(sql)) {
      return [players.filter((p) => p.display_name_normalized === params[0]).slice(0, 3)];
    }
    if (/FROM player_display_name_history/.test(sql)) return [[]];
    if (/FROM staff_roles r/.test(sql) && /p.mc_uuid = \?/.test(sql)) {
      const uuid = params[0];
      const player = players.find((p) => p.mc_uuid === uuid);
      if (!player || roles.get(player.id) !== 'moderator') return [[]];
      return [[{ player_id: player.id }]];
    }
    if (/SELECT player_id FROM staff_roles WHERE player_id = \? AND role = \? FOR UPDATE/.test(sql)) {
      return [roles.get(params[0]) === params[1] ? [{ player_id: params[0] }] : []];
    }
    if (/SELECT player_id FROM staff_roles WHERE player_id = \? AND role = \?/.test(sql)) {
      return [roles.get(params[0]) === params[1] ? [{ player_id: params[0] }] : []];
    }
    if (/INSERT IGNORE INTO staff_roles/.test(sql) || /INSERT INTO staff_roles/.test(sql)) {
      if (roles.has(params[0])) return [{ affectedRows: 0 }];
      roles.set(params[0], params[1]);
      return [{ affectedRows: 1 }];
    }
    if (/DELETE FROM staff_roles/.test(sql)) {
      if (options.failOn && options.failOn.test(sql)) {
        throw Object.assign(new Error('injected failure'), { code: 'ER_LOCK_DEADLOCK' });
      }
      const had = roles.get(params[0]) === params[1];
      if (had) roles.delete(params[0]);
      return [{ affectedRows: had ? 1 : 0 }];
    }
    if (/UPDATE website_sessions s/.test(sql) || /SET s.mod_unlocked_until = NULL/.test(sql)) {
      if (options.failOn && options.failOn.test(sql) && options.failArmed) {
        throw Object.assign(new Error('injected failure'), { code: 'ER_LOCK_DEADLOCK' });
      }
      for (const session of sessions) {
        const account = accounts.find((a) => a.id === session.account_id);
        if (account && account.player_id === params[0]) {
          session.mod_unlocked_until = null;
          session.mod_unlock_key_hash = null;
        }
      }
      return [{ affectedRows: 1 }];
    }
    if (/SET mod_unlocked_until = \?/.test(sql)) {
      if (params[4] != null && roles.get(params[4]) !== params[5]) {
        return [{ affectedRows: 0 }];
      }
      const row = sessions.find((entry) => entry.id === params[2] && entry.account_id === params[3]);
      if (!row) return [{ affectedRows: 0 }];
      row.mod_unlocked_until = params[0];
      row.mod_unlock_key_hash = params[1];
      return [{ affectedRows: 1 }];
    }
    if (/FROM staff_roles r[\s\S]*INNER JOIN players/.test(sql) && /ORDER BY/.test(sql)) {
      const rows = [];
      for (const [playerId, role] of roles) {
        if (role !== 'moderator') continue;
        const player = players.find((p) => p.id === playerId);
        if (player) rows.push({ player_id: player.id, display_name: player.display_name, mc_uuid: player.mc_uuid, granted_at: new Date() });
      }
      return [rows];
    }
    if (/INSERT INTO players/.test(sql)) {
      insertedPlayers.push(params);
      return [{ insertId: 99 }];
    }
    if (/LIKE \? ESCAPE/.test(sql)) {
      likeParams.push(params[0]);
      const matched = players
        .filter((p) => p.mc_uuid && likeEscapeMatch(p.display_name_normalized, params[0]))
        .map((p) => ({ id: p.id, display_name: p.display_name }));
      return [matched];
    }
    return [[]];
  }

  const pool = {
    query,
    async getConnection() {
      const prev = lock;
      let release;
      lock = new Promise((resolve) => { release = resolve; });
      await prev;
      const copy = snapshot();
      return {
        query,
        async beginTransaction() {},
        async commit() { release(); },
        async rollback() { restore(copy); release(); },
        release() {}
      };
    }
  };
  return { pool, roles, sessions, insertedPlayers, likeParams, accounts };
}

(async () => {
  const store = makeStore();
  const unknown = await staffRoles.grantModerator(store.pool, 'Nobody', 1);
  assert.strictEqual(unknown.code, 'unknown');
  assert.strictEqual(store.insertedPlayers.length, 0);

  const unverified = await staffRoles.grantModerator(store.pool, 'Ghost', 1);
  assert.strictEqual(unverified.code, 'unverified');
  assert.strictEqual(store.insertedPlayers.length, 0);

  const ambiguous = await staffRoles.grantModerator(store.pool, 'Twin', 1);
  assert.strictEqual(ambiguous.code, 'ambiguous');

  const adminDenied = await staffRoles.grantModerator(store.pool, 'Admin', 1);
  assert.strictEqual(adminDenied.code, 'admin_unchanged');
  assert.strictEqual(store.roles.size, 0);

  const first = await staffRoles.grantModerator(store.pool, 'ModUser', 1);
  assert.strictEqual(first.ok, true);
  assert.strictEqual(first.already, false);
  assert.strictEqual(store.sessions[0].mod_unlocked_until, null);
  store.sessions[0].mod_unlocked_until = new Date();
  store.sessions[0].mod_unlock_key_hash = 'old-hash';
  const again = await staffRoles.grantModerator(store.pool, 'ModUser', 1);
  assert.strictEqual(again.already, true);
  assert.ok(store.sessions[0].mod_unlocked_until);
  assert.ok(await staffRoles.isModeratorUuid(store.pool, MOD_UUID));

  const list = await staffRoles.listModerators(store.pool, (name) => name === 'ModUser');
  assert.strictEqual(list.length, 1);
  assert.strictEqual(list[0].online, true);

  const revoke = await staffRoles.revokeModerator(store.pool, 'ModUser');
  assert.strictEqual(revoke.already, false);
  assert.strictEqual(store.sessions[0].mod_unlocked_until, null);
  assert.ok(!(await staffRoles.isModeratorUuid(store.pool, MOD_UUID)));
  const revokeAgain = await staffRoles.revokeModerator(store.pool, 'ModUser');
  assert.strictEqual(revokeAgain.already, true);

  store.sessions[0].mod_unlocked_until = new Date();
  store.sessions[0].mod_unlock_key_hash = 'stale';
  const readd = await staffRoles.grantModerator(store.pool, 'ModUser', 1);
  assert.strictEqual(readd.already, false);
  assert.strictEqual(store.sessions[0].mod_unlocked_until, null);
  assert.strictEqual(store.sessions[0].mod_unlock_key_hash, null);

  const adminRevoke = await staffRoles.revokeModerator(store.pool, 'Admin');
  assert.strictEqual(adminRevoke.code, 'admin_unchanged');
  assert.strictEqual(store.insertedPlayers.length, 0);

  const parallelStore = makeStore();
  const parallel = await Promise.all([
    staffRoles.grantModerator(parallelStore.pool, 'ModUser', 1),
    staffRoles.grantModerator(parallelStore.pool, 'ModUser', 1)
  ]);
  assert.ok(parallel.every((row) => row.ok));
  assert.strictEqual(parallel.filter((row) => !row.already).length, 1);
  assert.strictEqual(parallelStore.roles.size, 1);

  const failOpts = { failOn: /UPDATE website_sessions s/, failArmed: false };
  const failing = makeStore(failOpts);
  await staffRoles.grantModerator(failing.pool, 'ModUser', 1);
  failing.sessions[0].mod_unlocked_until = new Date();
  failing.sessions[0].mod_unlock_key_hash = 'live';
  failOpts.failArmed = true;
  await assert.rejects(() => staffRoles.revokeModerator(failing.pool, 'ModUser'));
  assert.strictEqual(failing.roles.get(2), 'moderator');
  assert.ok(failing.sessions[0].mod_unlocked_until);
  assert.strictEqual(failing.sessions[0].mod_unlock_key_hash, 'live');

  const race = makeStore();
  await staffRoles.grantModerator(race.pool, 'ModUser', 1);
  race.sessions[0].mod_unlocked_until = null;
  const raced = await Promise.all([
    staffRoles.revokeModerator(race.pool, 'ModUser'),
    staffRoles.unlockModeratorSession(race.pool, {
      playerId: 2,
      sessionId: 9,
      accountId: 20,
      until: new Date(Date.now() + 1000),
      keyHash: 'new-hash'
    })
  ]);
  const stillMod = await staffRoles.isModeratorUuid(race.pool, MOD_UUID);
  assert.ok(raced.every((row) => row.ok || row.error));
  if (!stillMod) {
    assert.strictEqual(race.sessions[0].mod_unlocked_until, null);
  } else {
    assert.ok(raced.some((row) => row.ok && !row.already));
  }

  const searchStore = makeStore();
  const felix = await staffRoles.searchVerifiedPlayers(searchStore.pool, 'F3lixMc_', 20);
  assert.deepStrictEqual(felix.map((row) => row.player), ['F3lixMc_']);
  const inner = await staffRoles.searchVerifiedPlayers(searchStore.pool, 'foo_', 20);
  assert.deepStrictEqual(inner.map((row) => row.player), ['foo_bar']);
  assert.ok(searchStore.likeParams.every((value) => value.includes('\\_')));
  assert.strictEqual(staffRoles.likePrefix('f3lixmc_'), 'f3lixmc\\_%');

  console.log('staff-roles tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
