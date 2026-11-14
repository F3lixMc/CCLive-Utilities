'use strict';
const assert = require('assert');
const { FACTORY_PHASE_SQL, classifyFactoryUpload, factoryLevel, factoryPhaseStatus } = require('../../../backend_server/website_backend/factory-level-upload');
const { classifySmithUpload } = require('../../../backend_server/website_backend/smith-skill-upload');

assert.ok(FACTORY_PHASE_SQL.startsWith('INSERT INTO leaderboard'));
assert.ok(!FACTORY_PHASE_SQL.includes(';'));
assert.ok(FACTORY_PHASE_SQL.includes('GREATEST(score, VALUES(score))'));
assert.ok(FACTORY_PHASE_SQL.includes('s.is_current = 1 AND s.id = ? AND s.is_preseason = ?'));

assert.strictEqual(factoryLevel(1), 1);
assert.strictEqual(factoryLevel(2000), 2000);
assert.strictEqual(factoryLevel('2000'), 2000);
assert.strictEqual(factoryLevel(0), null);
assert.strictEqual(factoryLevel(2001), null);
assert.strictEqual(factoryLevel(1.5), null);

assert.strictEqual(classifyFactoryUpload({ board: 'factory_level', level: 4 }).kind, 'invalid');
assert.strictEqual(classifyFactoryUpload({ seasonId: 9, level: 4 }).kind, 'invalid');
assert.strictEqual(classifyFactoryUpload({ isPreseason: false, level: 4 }).kind, 'invalid');
assert.deepStrictEqual(classifyFactoryUpload({ seasonId: 9, isPreseason: true, level: 4 }), { kind: 'phase', seasonId: 9, preseason: true });
assert.strictEqual(classifySmithUpload({ board: 'smith_skill', level: '4', xp: '1' }).kind, 'legacy');
assert.strictEqual(factoryPhaseStatus(0), 409);
assert.strictEqual(factoryPhaseStatus(1), 200);
assert.strictEqual(factoryPhaseStatus(2), 200);

console.log('factory-level-upload tests ok');
