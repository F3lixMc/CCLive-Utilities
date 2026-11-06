'use strict';
const assert = require('assert');

(async () => {
  let listenCalled = false;
  let revoked = false;
  async function start({ migrate, revoke, listen }) {
    try {
      await migrate();
      await revoke();
      await listen();
    } catch (err) {
      const error = new Error('start-failed:' + (err && err.code));
      throw error;
    }
  }
  await assert.rejects(() => start({
    async migrate() {
      const err = new Error('schema');
      err.code = 'ER_BAD_TABLE_ERROR';
      throw err;
    },
    async revoke() { revoked = true; },
    async listen() { listenCalled = true; }
  }), /start-failed:ER_BAD_TABLE_ERROR/);
  assert.strictEqual(listenCalled, false);
  assert.strictEqual(revoked, false);
  console.log('init-start tests ok');
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
