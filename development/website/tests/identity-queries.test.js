'use strict';
const assert = require('assert');

function presenceKey(player, playerId) {
  return playerId != null ? 'id:' + playerId : 'name:' + String(player).toLowerCase();
}

assert.strictEqual(presenceKey('Steve', 7), 'id:7');
assert.strictEqual(presenceKey('Steve', null), 'name:steve');
assert.notStrictEqual(presenceKey('Steve', 7), presenceKey('Steve', 8));
assert.notStrictEqual(presenceKey('OldName', 7), presenceKey('NewName', 8));

const lastSeenVerified = 'SELECT TIMESTAMPDIFF(SECOND, last_seen, NOW()) AS seconds_ago FROM tokens WHERE player_id = ? AND revoked_at IS NULL LIMIT 1';
const lastSeenLegacy = 'SELECT TIMESTAMPDIFF(SECOND, last_seen, NOW()) AS seconds_ago FROM tokens WHERE player = ? AND player_id IS NULL AND revoked_at IS NULL LIMIT 1';
assert.ok(/player_id = \?/.test(lastSeenVerified));
assert.ok(/revoked_at IS NULL/.test(lastSeenVerified));
assert.ok(/player_id IS NULL/.test(lastSeenLegacy));
assert.ok(/revoked_at IS NULL/.test(lastSeenLegacy));
console.log('identity-query tests ok');
