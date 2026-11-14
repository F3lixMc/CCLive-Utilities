'use strict';
const assert = require('assert');
const { SYNC_BOARD_IDS, assembleLeaderboardSync, mapSyncRow, smithLevel, smithXp, smithProfileFields, smithStatText } = require('../../../backend_server/website_backend/leaderboard-sync');

const zones = [
  'oak_collection', 'coal_collection', 'raw_copper_collection', 'jungle_collection',
  'spruce_collection', 'bamboo_collection', 'raw_iron_collection', 'mushroom_collection',
  'dark_oak_collection', 'raw_gold_collection', 'mangrove_collection', 'diamond_collection',
  'sulfur_collection', 'quartz_collection', 'obsidian_collection', 'crimson_collection',
  'warped_collection', 'ancient_debris_collection', 'echo_collection', 'amethyst_collection'
];
const expectedIds = ['playtime', 'current_coins', 'all_collections', 'alltime_kills', 'smith_skill', 'factory_level', ...zones];
for (let floor = 1; floor <= 100; floor++) expectedIds.push('floor_' + floor);

assert.deepStrictEqual(SYNC_BOARD_IDS, expectedIds);
assert.strictEqual(SYNC_BOARD_IDS.length, 126);
assert.ok(SYNC_BOARD_IDS.includes('smith_skill'));

const ids = ['playtime', 'floor_1', 'floor_2'];
const tied = assembleLeaderboardSync([
  { board: 'playtime', player: 'Bea', storedPlayer: 'Bea', playerId: 2, normalized: 'bea', score: 10 },
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: 1, normalized: 'ada', score: 10 },
  { board: 'floor_1', player: 'Cem', storedPlayer: 'Cem', playerId: 3, normalized: 'cem', score: 4 }
], ids, 'Bea', 9, true, 1000, 2000);

assert.strictEqual(tied.seasonId, 9);
assert.strictEqual(tied.isPreseason, true);
assert.strictEqual(tied.serverTime, 1000);
assert.strictEqual(tied.nextSyncAt, 2000);
assert.deepStrictEqual(tied.boards.playtime.top, [
  { player: 'Ada', score: 10, rank: 1 },
  { player: 'Bea', score: 10, rank: 2 }
]);
assert.deepStrictEqual(tied.boards.playtime.self, { player: 'Bea', score: 10, rank: 2 });
assert.strictEqual(tied.boards.playtime.self, tied.boards.playtime.top[1]);
assert.deepStrictEqual(tied.boards.floor_2, { top: [], self: null });
assert.deepStrictEqual(tied.boards.floor_1.top, [{ player: 'Cem', score: 4, rank: 1 }]);

const bound = assembleLeaderboardSync([
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: null, normalized: null, score: 99 },
  { board: 'playtime', player: 'Ada', storedPlayer: 'renamed', playerId: 4, normalized: 'ada', score: 3 }
], ['playtime'], 'Ada', 9, true, 1, 2);
assert.deepStrictEqual(bound.boards.playtime.top.map((row) => row.rank), [1, 2]);
assert.deepStrictEqual(bound.boards.playtime.self, { player: 'Ada', score: 3, rank: 2 });

const foreign = assembleLeaderboardSync([
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: 9, normalized: 'other', score: 8 }
], ['playtime'], 'Ada', 9, false, 1, 2);
assert.strictEqual(foreign.isPreseason, false);
assert.strictEqual(foreign.boards.playtime.self, null);
assert.deepStrictEqual(foreign.boards.playtime.top, [{ player: 'Ada', score: 8, rank: 1 }]);

const legacy = assembleLeaderboardSync([
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: null, normalized: null, score: 6 }
], ['playtime'], 'Ada', 9, false, 1, 2);
assert.deepStrictEqual(legacy.boards.playtime.self, { player: 'Ada', score: 6, rank: 1 });

const smith = assembleLeaderboardSync([
  { board: 'smith_skill', player: 'Low', storedPlayer: 'Low', playerId: 1, normalized: 'low', score: 0, level: '9', xp: '9223372036854775807' },
  { board: 'smith_skill', player: 'High', storedPlayer: 'High', playerId: 2, normalized: 'high', score: 0, level: '10', xp: '1' },
  { board: 'smith_skill', player: 'Zero', storedPlayer: 'Zero', playerId: 3, normalized: 'zero', score: 0, level: '10', xp: '0' },
  { board: 'smith_skill', player: 'Nope', storedPlayer: 'Nope', playerId: 4, normalized: 'nope', score: 99 }
], ['smith_skill'], 'High', 9, false, 1, 2);
assert.deepStrictEqual(smith.boards.smith_skill.top.map((row) => row.player), ['High', 'Zero', 'Low']);
assert.deepStrictEqual(smith.boards.smith_skill.top[0], { player: 'High', level: '10', xp: '1', rank: 1 });
assert.strictEqual(typeof smith.boards.smith_skill.top[0].xp, 'string');
assert.strictEqual(smith.boards.smith_skill.self, smith.boards.smith_skill.top[0]);
assert.strictEqual(smithLevel('10'), '10');
assert.strictEqual(smithXp('1'), '1');
assert.strictEqual(smithXp('9223372036854775807'), '9223372036854775807');
assert.strictEqual(smithXp('9223372036854775808'), null);
assert.strictEqual(smithLevel(10), null);
const present = smithProfileFields({ smith_level: '10', smith_xp: '1' });
assert.deepStrictEqual(present, { smith_level: '10', smith_xp: '1' });
assert.strictEqual(smithStatText(present.smith_level), '10');
assert.strictEqual(smithStatText(present.smith_xp), '1');
assert.strictEqual(smithStatText('1000000'), '1.000.000');
assert.deepStrictEqual(smithProfileFields(undefined), { smith_level: null, smith_xp: null });
assert.strictEqual(smithStatText(null), 'Noch nicht erfasst');

const releaseIds = SYNC_BOARD_IDS.filter(id => id !== 'factory_level');
assert.strictEqual(releaseIds.length, 125);
const factoryRows = [
  { board: 'factory_level', player: 'Zed', storedPlayer: 'Zed', playerId: 2, normalized: 'zed', score: 2000, playtime: 1, level: '9', xp: '9' },
  { board: 'factory_level', player: 'Ada', storedPlayer: 'Ada', playerId: 1, normalized: 'ada', score: 2000, playtime: 99999, level: '1', xp: '1' },
  { board: 'factory_level', player: 'Mia', storedPlayer: 'Mia', playerId: 3, normalized: 'mia', score: 1999, playtime: 5 },
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: 1, normalized: 'ada', score: 10 }
];
const ranked = assembleLeaderboardSync(factoryRows, ['factory_level', 'playtime'], 'Ada', 9, false, 1, 2);
assert.deepStrictEqual(ranked.boards.factory_level.top.map(row => [row.player, row.rank, row.score]), [
  ['Ada', 1, 2000],
  ['Zed', 1, 2000],
  ['Mia', 3, 1999]
]);
assert.strictEqual(ranked.boards.factory_level.top[0].level, undefined);
assert.strictEqual(ranked.boards.factory_level.self, ranked.boards.factory_level.top[0]);
assert.deepStrictEqual(ranked.boards.playtime.top, [{ player: 'Ada', score: 10, rank: 1 }]);
const mapped = mapSyncRow({ board: 'factory_level', player: 'Ada', stored_player: 'Ada', player_id: 1, normalized: 'ada', score: 2000, smith_level: 4, smith_xp: '9' });
assert.strictEqual(mapped.level, null);
assert.strictEqual(mapped.xp, null);
const releasePayload = assembleLeaderboardSync([
  { board: 'playtime', player: 'Ada', storedPlayer: 'Ada', playerId: 1, normalized: 'ada', score: 3 },
  { board: 'factory_level', get player() { throw new Error('factory'); }, score: 8 }
], SYNC_BOARD_IDS, 'Ada', 8, false, 1, 2);
for (const id of releaseIds) {
  assert.ok(Array.isArray(releasePayload.boards[id].top), id);
}
assert.deepStrictEqual(releasePayload.boards.factory_level, { top: [], self: null });
assert.deepStrictEqual(releasePayload.boards.playtime.top, [{ player: 'Ada', score: 3, rank: 1 }]);

console.log('leaderboard-sync tests ok');
