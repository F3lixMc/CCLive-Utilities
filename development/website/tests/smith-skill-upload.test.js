'use strict';
const assert = require('assert');
const {
  SMITH_PHASE_SQL,
  classifySmithUpload,
  smithPhaseStatus,
  smithUploadResult
} = require('../../../backend_server/website_backend/smith-skill-upload');

assert.ok(SMITH_PHASE_SQL.startsWith('INSERT INTO leaderboard'));
assert.ok(SMITH_PHASE_SQL.includes('SELECT s.id'));
assert.ok(SMITH_PHASE_SQL.includes('WHERE s.is_current = 1 AND s.id = ? AND s.is_preseason = ?'));
assert.ok(SMITH_PHASE_SQL.includes('updated_at = NOW()'));
assert.strictEqual(SMITH_PHASE_SQL.includes(';'), false);

const preseason = { id: 9, isCurrent: true, preseason: true };
const season = { id: 9, isCurrent: true, preseason: false };
const matching = { board: 'smith_skill', level: '4', xp: '1', seasonId: 9, isPreseason: true };
const stale = { board: 'smith_skill', level: '4', xp: '1', seasonId: 9, isPreseason: true };
const release = { board: 'smith_skill', level: '4', xp: '1' };

assert.deepStrictEqual(smithUploadResult(matching, preseason), {
  status: 200, written: true, legacy: false, seasonId: 9
});
assert.deepStrictEqual(smithUploadResult(stale, season), { status: 409, written: false });
assert.strictEqual(smithPhaseStatus(0), 409);
assert.strictEqual(smithPhaseStatus(1), 200);
assert.strictEqual(smithPhaseStatus(2), 200);

assert.deepStrictEqual(smithUploadResult(release, season), {
  status: 200, written: true, legacy: true, seasonId: 9
});
assert.strictEqual(classifySmithUpload(release).kind, 'legacy');
assert.strictEqual(smithUploadResult({ seasonId: 9 }, season).status, 400);
assert.strictEqual(smithUploadResult({ isPreseason: false }, season).status, 400);
assert.strictEqual(smithUploadResult({ seasonId: 9, isPreseason: 1 }, preseason).status, 400);
assert.strictEqual(smithUploadResult({ seasonId: 9, isPreseason: false }, preseason).status, 409);

const beforeFlip = smithUploadResult(matching, preseason);
assert.strictEqual(beforeFlip.written, true);
assert.strictEqual(beforeFlip.seasonId, 9);

console.log('smith-skill-upload tests ok');
