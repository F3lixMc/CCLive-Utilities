'use strict';
const assert = require('assert');
const { upsertVerifiedPlayer } = require('../../../backend_server/website_backend/players');

(async () => {
  const queries = [];
  const conn = {
    async query(sql, params) {
      queries.push({ sql, params });
      if (/FROM players WHERE mc_uuid/.test(sql)) {
        return [[{ id: 7, mc_uuid: params[0], display_name: 'Old', display_name_normalized: 'old' }]];
      }
      if (/FROM tokens/.test(sql) && /name_color/.test(sql)) {
        return [[{ name_color: '#12AB34', name_colors: '#12AB34,#34AB12', cactus_rank: 3, soul_rank: 4, last_seen: 'x', online_since: 'y' }]];
      }
      if (/UPDATE tokens SET display_name|UPDATE players|INSERT INTO player_display/.test(sql)) {
        return [{}];
      }
      if (/INSERT INTO tokens/.test(sql)) {
        return [{}];
      }
      if (/UPDATE tokens/.test(sql)) {
        return [{}];
      }
      if (/FROM website_accounts/.test(sql)) {
        return [[]];
      }
      return [{}];
    }
  };
  const result = await upsertVerifiedPlayer(conn, { uuid: '069a79f4-44e1-4626-a5b0-bac67538bf5a', name: 'Old', nameKey: 'old' }, () => 'new-token');
  assert.strictEqual(result.token, 'new-token');
  const insert = queries.find((q) => /INSERT INTO tokens/.test(q.sql));
  assert.ok(insert);
  assert.strictEqual(insert.params[3], '#12AB34');
  assert.strictEqual(insert.params[4], '#12AB34,#34AB12');
  assert.strictEqual(insert.params[5], 3);
  assert.strictEqual(insert.params[6], 4);
  const revoke = queries.find((q) => /revoked_at = NOW()/.test(q.sql));
  assert.ok(revoke);

  const legacy = {
    id: 1,
    token: 'legacy-token',
    player: 'firestarter03',
    player_id: null,
    revoked_at: null
  };
  const tokenRows = [legacy];
  function insertTokenAssumingNameUnique(row) {
    if (tokenRows.some((existing) => existing.player === row.player)) {
      const err = new Error("Duplicate entry 'firestarter03' for key 'player'");
      err.code = 'ER_DUP_ENTRY';
      err.errno = 1062;
      err.sqlState = '23000';
      throw err;
    }
    tokenRows.push(row);
  }
  assert.throws(() => insertTokenAssumingNameUnique({
    token: 'verified-token',
    player: 'firestarter03',
    player_id: 40
  }), /Duplicate entry/);

  const legacyConn = {
    async query(sql, params) {
      if (/FROM players WHERE mc_uuid/.test(sql)) return [[]];
      if (/INSERT INTO players/.test(sql)) {
        return [{ insertId: 40 }];
      }
      if (/FROM tokens/.test(sql) && /name_color/.test(sql)) return [[]];
      if (/UPDATE tokens/.test(sql) && /revoked_at = NOW()/.test(sql)) return [{}];
      if (/INSERT INTO tokens/.test(sql)) {
        tokenRows.push({
          id: 2,
          token: params[0],
          player: params[1],
          player_id: params[2],
          revoked_at: null
        });
        return [{}];
      }
      return [{}];
    }
  };
  const verified = await upsertVerifiedPlayer(
    legacyConn,
    { uuid: '069a79f4-44e1-4626-a5b0-bac67538bf5a', name: 'firestarter03', nameKey: 'firestarter03' },
    () => 'verified-token'
  );
  assert.strictEqual(verified.token, 'verified-token');
  assert.strictEqual(verified.playerRow.id, 40);
  assert.strictEqual(tokenRows.length, 2);
  assert.strictEqual(tokenRows[0].player, 'firestarter03');
  assert.strictEqual(tokenRows[0].player_id, null);
  assert.strictEqual(tokenRows[1].player, 'firestarter03');
  assert.strictEqual(tokenRows[1].player_id, 40);
  assert.strictEqual(tokenRows[1].token, 'verified-token');
  console.log('players rotation tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
