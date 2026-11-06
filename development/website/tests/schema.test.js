'use strict';
const assert = require('assert');
const { addColumn, revokeUnverifiedTokens, prepareTokensIdentityIndexes, hasPlayerNameUnique, hasTokenValueUnique } = require('../../../backend_server/website_backend/schema');

let revoked = false;
const failingPool = {
  async query(sql) {
    if (/ADD COLUMN/.test(sql)) {
      const err = new Error('access denied');
      err.code = 'ER_ACCESS_DENIED_ERROR';
      throw err;
    }
    if (/UPDATE tokens/.test(sql)) {
      revoked = true;
      return [{ affectedRows: 1 }];
    }
    return [{}];
  }
};

(async () => {
  await assert.rejects(() => addColumn(failingPool, 'tokens', 'player_id', 'player_id BIGINT NULL'));
  assert.strictEqual(revoked, false);

  const okPool = {
    async query(sql) {
      if (/ADD COLUMN/.test(sql)) {
        const err = new Error("Duplicate column name 'player_id'");
        err.code = 'ER_DUP_FIELDNAME';
        throw err;
      }
      return [{}];
    }
  };
  await addColumn(okPool, 'tokens', 'player_id', 'player_id BIGINT NULL');

  const revokePool = {
    async query() {
      return [{ affectedRows: 2 }];
    }
  };
  await revokeUnverifiedTokens(revokePool);

  let nameUniquePresent = true;
  const dropped = [];
  const tokenPool = {
    async query(sql, params) {
      if (/SHOW INDEX/.test(sql)) {
        const rows = [
          { Key_name: 'PRIMARY', Column_name: 'id' },
          { Key_name: 'token', Column_name: 'token' }
        ];
        if (nameUniquePresent) rows.push({ Key_name: 'player', Column_name: 'player' });
        return [rows];
      }
      if (/DROP INDEX/.test(sql)) {
        nameUniquePresent = false;
        dropped.push({ sql, params });
        return [{}];
      }
      if (/ADD INDEX idx_tokens_player /.test(sql) || /ADD INDEX idx_tokens_player\(/.test(sql) || /idx_tokens_player/.test(sql)) {
        return [{}];
      }
      return [{}];
    }
  };
  await prepareTokensIdentityIndexes(tokenPool);
  assert.strictEqual(nameUniquePresent, false);
  assert.ok(dropped.some((entry) => (entry.params || []).includes('player') || /player/.test(entry.sql)));
  assert.ok(hasTokenValueUnique({ PRIMARY: ['id'], token: ['token'] }));
  assert.ok(!hasPlayerNameUnique({ PRIMARY: ['id'], token: ['token'] }));
  assert.ok(hasPlayerNameUnique({ PRIMARY: ['id'], player: ['player'] }));

  console.log('schema tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
