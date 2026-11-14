'use strict';
const assert = require('assert');
const { leaderboardWindow, nextSyncAt, wantsFullList } = require('../../../backend_server/website_backend/leaderboard-page');

assert.deepStrictEqual(leaderboardWindow(undefined), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow('1'), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow('2'), { page: 2, pageSize: 10, offset: 10 });
assert.deepStrictEqual(leaderboardWindow('3'), { page: 3, pageSize: 10, offset: 20 });
assert.deepStrictEqual(leaderboardWindow('0'), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow('-4'), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow('1.5'), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow('2 OR 1=1'), { page: 1, pageSize: 10, offset: 0 });
assert.deepStrictEqual(leaderboardWindow(['2', '3']), { page: 1, pageSize: 10, offset: 0 });

const at = (hour, minute, second, millis) => Date.UTC(2026, 10, 10, hour, minute, second, millis);
assert.strictEqual(nextSyncAt(at(12, 15, 0, 0)), at(12, 30, 0, 0));
assert.strictEqual(nextSyncAt(at(12, 27, 0, 0)), at(12, 30, 0, 0));
assert.strictEqual(nextSyncAt(at(12, 30, 0, 0)), at(13, 0, 0, 0));
assert.strictEqual(nextSyncAt(at(12, 30, 0, 1)), at(13, 0, 0, 0));
assert.strictEqual(nextSyncAt(at(12, 59, 59, 999)), at(13, 0, 0, 0));
assert.strictEqual(nextSyncAt(at(13, 0, 0, 0)), at(13, 30, 0, 0));
assert.strictEqual(wantsFullList('1'), true);
assert.strictEqual(wantsFullList('true'), true);
assert.strictEqual(wantsFullList('0'), false);
assert.strictEqual(wantsFullList(['1']), false);
console.log('leaderboard-page tests ok');
