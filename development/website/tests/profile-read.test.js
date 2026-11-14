'use strict';
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { profileBoardSql, readProfileBoards, formatProfileTiming, openProfileSession, readSeasonPhase, createProfileGate, loadSharedProfile, loadPlayerNames } = require('../../../backend_server/website_backend/profile-read');

const byId = profileBoardSql(true);
const byName = profileBoardSql(false);
assert.ok(byId.includes("'playtime', 'smith_skill', 'factory_level'"));
assert.ok(byId.includes('player_id = ?'));
assert.ok(!byId.includes('player_id IS NULL'));
assert.ok(byName.includes('player = ? AND player_id IS NULL'));
assert.strictEqual(byId.split('SELECT').length, 2);

const ada = readProfileBoards([
  { board: 'playtime', score: '3600' },
  { board: 'smith_skill', smith_level: '4', smith_xp: '9' },
  { board: 'factory_level', score: '2000' }
]);
assert.strictEqual(ada.playtime, 3600);
assert.strictEqual(ada.smith_level, '4');
assert.strictEqual(ada.smith_xp, '9');
assert.strictEqual(ada.factory_level, 2000);

const missing = readProfileBoards([{ board: 'playtime', score: '12' }]);
assert.strictEqual(missing.factory_level, null);
assert.strictEqual(missing.smith_level, null);

const otherSeason = readProfileBoards([{ board: 'factory_level', score: '15' }]);
assert.strictEqual(otherSeason.factory_level, 15);
assert.strictEqual(otherSeason.playtime, null);

const line = formatProfileTiming('profile', 200, 1200, 7, [
  { label: 'season', sql: 2 },
  { label: 'boards', sql: 3 }
]);
assert.ok(line.includes('pool=7ms'));
assert.ok(line.includes('season: sql=2ms'));
assert.ok(line.includes('boards: sql=3ms'));
assert.strictEqual(line.split('pool=').length, 2);
assert.ok(!line.includes('pool=12ms'));

(async () => {
  let connections = 0;
  const connection = {
    async query(sql, args) {
      assert.strictEqual(args[0], 8);
      return [[{ board: 'factory_level', score: '40', smith_level: null, smith_xp: null }]];
    },
    release() { connections -= 1; }
  };
  const pool = { async getConnection() { connections += 1; return connection; } };
  const session = await openProfileSession(pool);
  assert.strictEqual(connections, 1);
  await session.query('season', 'SELECT id FROM seasons WHERE id = ?', [8]);
  await session.query('boards', profileBoardSql(true), [8, 4]);
  assert.strictEqual(connections, 1);
  assert.strictEqual(session.queries.length, 2);
  assert.ok(session.queries.every(item => item.sql >= 0));
  const text = session.line('profile', 200, 1500);
  assert.ok(text.includes('pool=' + session.poolWait + 'ms'));
  session.release();
  assert.strictEqual(connections, 0);
  const server = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/server.js'), 'utf8');
  assert.ok(server.includes('connectionLimit: 10'));
  assert.ok(server.includes('profileGate.invalidate(req.player, seasonId)'));
  const browser = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/public/main_website-data.js'), 'utf8');
  assert.ok(browser.includes('12000'));

  const seasons = { current: [{ id: 9, is_preseason: 1 }] };
  const historical = { id: 8, is_preseason: 0 };
  let opened = 0;
  let released = 0;
  const livePool = {
    async getConnection() {
      opened += 1;
      return {
        async query(sql, args) {
          if (String(sql).includes('is_current')) return [seasons.current];
          if (String(sql).includes('WHERE id = ?')) return [args[0] === 8 ? [historical] : []];
          if (String(sql).includes('FROM player_stats')) return [[{ player: 'Ada' }, { player: 'Bo' }]];
          return [[]];
        },
        release() { released += 1; }
      };
    }
  };
  const phase = await readSeasonPhase(async (label, sql, args) => livePool.getConnection().then(connection => connection.query(sql, args)), { query: {} });
  assert.deepStrictEqual(phase, { seasonId: 9, preseason: true });
  opened = 0;
  const named = await readSeasonPhase(async (label, sql, args) => {
    const connection = await livePool.getConnection();
    return connection.query(sql, args);
  }, { query: { season_id: '8' } });
  assert.deepStrictEqual(named, { seasonId: 8, preseason: false });
  await assert.rejects(readSeasonPhase(async () => [[]], { query: { season_id: '0' } }), error => error.status === 400);
  await assert.rejects(readSeasonPhase(async () => [[]], { query: { season_id: '99' } }), error => error.status === 404);

  opened = 0;
  released = 0;
  const names = await loadPlayerNames(livePool, { query: {} });
  assert.deepStrictEqual(names, ['Ada', 'Bo']);
  assert.strictEqual(opened, 1);
  assert.strictEqual(released, 1);

  const gate = createProfileGate({ limit: 2, ttlMs: 30_000 });
  let stored = null;
  let reads = 0;
  opened = 0;
  released = 0;
  const req = { params: { player: 'Ada' }, query: {} };
  async function readBody() {
    reads += 1;
    return stored;
  }
  const parallel = await Promise.all([
    loadSharedProfile(gate, livePool, req, readBody),
    loadSharedProfile(gate, livePool, { params: { player: 'ada' }, query: {} }, readBody),
    loadSharedProfile(gate, livePool, req, readBody)
  ]);
  assert.ok(parallel.every(result => result.status === 404));
  assert.strictEqual(opened, 1);
  assert.strictEqual(released, 1);
  assert.strictEqual(reads, 1);
  opened = 0;
  await loadSharedProfile(gate, livePool, req, readBody);
  assert.strictEqual(reads, 1);
  assert.strictEqual(opened, 1);
  const other = await loadSharedProfile(gate, livePool, { params: { player: 'Ada' }, query: { season_id: '8' } }, readBody);
  assert.strictEqual(other.status, 404);
  assert.strictEqual(reads, 2);
  stored = { player: 'Ada', season_id: 9 };
  gate.invalidate('Ada', 9);
  const visible = await loadSharedProfile(gate, livePool, req, readBody);
  assert.strictEqual(visible.status, 200);
  assert.strictEqual(visible.body.player, 'Ada');
  assert.strictEqual(reads, 3);

  let failures = 0;
  await assert.rejects(loadSharedProfile(gate, livePool, { params: { player: 'Bo' }, query: {} }, async () => {
    failures += 1;
    throw new Error('Datenbank nicht erreichbar');
  }));
  await assert.rejects(loadSharedProfile(gate, livePool, { params: { player: 'Bo' }, query: {} }, async () => {
    failures += 1;
    throw new Error('Datenbank nicht erreichbar');
  }));
  assert.strictEqual(failures, 2);
  assert.strictEqual(gate.hasMiss('Bo', 9, true), false);
  const limited = createProfileGate({ limit: 2, ttlMs: 30_000 });
  assert.strictEqual(limited.rememberMiss('C', 1, false, 0), true);
  assert.strictEqual(limited.rememberMiss('D', 1, false, 0), true);
  assert.strictEqual(limited.rememberMiss('E', 1, false, 0), true);
  assert.strictEqual(limited.size(), 2);
  assert.strictEqual(limited.hasMiss('C', 1, false), false);
  limited.invalidate('E', 1);
  assert.strictEqual(limited.hasMiss('E', 1, false), false);
  assert.strictEqual(limited.rememberMiss('F', 1, false, 0), false);
  console.log('profile-read tests ok');
})().catch(err => { console.error(err); process.exitCode = 1; });
