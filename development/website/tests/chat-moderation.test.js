'use strict';
const assert = require('assert');
const fs = require('fs');
const http = require('http');
const path = require('path');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { sha256Hex } = require('../../../backend_server/website_backend/crypto-util');
const installAdminAuth = require('../../../backend_server/website_backend/admin-auth');
const { installModerationHttp } = require('../../../backend_server/website_backend/moderation-http');
const chatMod = require('../../../backend_server/website_backend/chat-moderation');
const chatFilters = require('../../../backend_server/website_backend/chat-filters');
const staffFeed = require('../../../backend_server/website_backend/staff-chat-feed');
const teamChat = require('../../../backend_server/website_backend/team-chat');

const UUID_A = '47a5c2fc-b317-4180-b96f-ca977c51eb62';
const MOD_UUID = '069a79f4-44e1-4626-a5b0-bac67538bf5a';
const MOD2_UUID = '22222222-2222-2222-2222-222222222222';
const OTHER_UUID = '11111111-1111-1111-1111-111111111111';
const ADMIN_KEY = 'unit-admin-key';
const MOD_KEY = 'unit-mod-key';

function tokenFor(label) {
  return Buffer.from(label.padEnd(32, '0')).toString('hex').slice(0, 64);
}

function request(app, { method = 'GET', path, headers = {}, body }) {
  return new Promise((resolve, reject) => {
    const server = app.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      const req = http.request({
        hostname: '127.0.0.1',
        port,
        path,
        method,
        headers: {
          host: '127.0.0.1:' + port,
          ...headers,
          ...(body ? { 'content-type': 'application/json' } : {})
        }
      }, (res) => {
        const chunks = [];
        res.on('data', (chunk) => chunks.push(chunk));
        res.on('end', () => {
          server.close();
          const raw = Buffer.concat(chunks).toString('utf8');
          let json = null;
          try { json = JSON.parse(raw); } catch { json = null; }
          resolve({ status: res.statusCode, json, raw });
        });
      });
      req.on('error', (err) => { server.close(); reject(err); });
      if (body) req.write(JSON.stringify(body));
      req.end();
    });
  });
}

function makeDb() {
  const now = () => new Date();
  const players = [
    { id: 11, display_name: 'Admin', display_name_normalized: 'admin', mc_uuid: UUID_A },
    { id: 33, display_name: 'ModUser', display_name_normalized: 'moduser', mc_uuid: MOD_UUID },
    { id: 77, display_name: 'ModB', display_name_normalized: 'modb', mc_uuid: MOD2_UUID },
    { id: 44, display_name: 'Plain', display_name_normalized: 'plain', mc_uuid: OTHER_UUID },
    { id: 55, display_name: 'Ghost', display_name_normalized: 'ghost', mc_uuid: null }
  ];
  const sessions = {
    admin: {
      token: tokenFor('admin'), session_id: 1, account_id: 11, player_id: 11,
      login_name: 'Admin', display_name: 'Admin', mc_uuid: UUID_A,
      admin_unlocked_until: new Date(Date.now() + 3600000),
      admin_unlock_key_hash: sha256Hex(ADMIN_KEY),
      mod_unlocked_until: null, mod_unlock_key_hash: null
    },
    mod: {
      token: tokenFor('mod'), session_id: 2, account_id: 33, player_id: 33,
      login_name: 'ModUser', display_name: 'ModUser', mc_uuid: MOD_UUID,
      admin_unlocked_until: null, admin_unlock_key_hash: null,
      mod_unlocked_until: new Date(Date.now() + 3600000),
      mod_unlock_key_hash: sha256Hex(MOD_KEY)
    },
    modB: {
      token: tokenFor('modb'), session_id: 3, account_id: 77, player_id: 77,
      login_name: 'ModB', display_name: 'ModB', mc_uuid: MOD2_UUID,
      admin_unlocked_until: null, admin_unlock_key_hash: null,
      mod_unlocked_until: new Date(Date.now() + 3600000),
      mod_unlock_key_hash: sha256Hex(MOD_KEY)
    }
  };
  const state = {
    seasons: [{ id: 2, is_current: 1 }],
    messages: [],
    events: [],
    redactions: [],
    evidence: [],
    sanctions: [],
    reports: [],
    reportEvidence: [],
    reportWatches: [],
    reportWatchers: [],
    teamMessages: [],
    reportTeamMessages: [],
    nextTeamId: 1,
    nextReportChatId: 1,
    nextReportId: 1,
    shares: [
      { id: 'ITEM-db2ac5', type: 'ITEM', data: '{"n":1}', expires_at: new Date(Date.now() + 3600000) },
      { id: 'BUILD-709125', type: 'BUILD', data: '{"b":1}', expires_at: new Date(Date.now() + 3600000) }
    ],
    nextId: 1,
    filterDomains: [],
    filterPhrases: [],
    filterExceptions: [],
    filterSeeded: false,
    playerTokens: [],
    reportCreateDelay: 0,
    watchDelay: 0,
    reportLockHeld: false,
    reportLockOverlaps: 0,
    watchLockHeld: false,
    watchLockOverlaps: 0,
    reportRowHeld: false,
    reportRowOverlaps: 0,
    pauseAfterReportLock: null,
    reportRowPaused: false,
    roles: new Map([[33, 'moderator'], [77, 'moderator']]),
    leaderboard: [{ season_id: 1, board: 'kills', player_id: 44, score: 9 }],
    sqlLog: [],
    failEvidence: false,
    beforeSeasonDelete: null
  };

  function active(row) {
    return !row.lifted_at && (!row.expires_at || new Date(row.expires_at).getTime() > Date.now());
  }

  function snapshotState() {
    return {
      seasons: state.seasons.map((row) => ({ ...row })),
      messages: state.messages.map((row) => ({ ...row })),
      events: state.events.map((row) => ({ ...row })),
      redactions: state.redactions.map((row) => ({ ...row })),
      evidence: state.evidence.map((row) => ({ ...row })),
      sanctions: state.sanctions.map((row) => ({ ...row })),
      reports: state.reports.map((row) => ({ ...row })),
      reportEvidence: state.reportEvidence.map((row) => ({ ...row })),
      reportWatches: state.reportWatches.map((row) => ({ ...row })),
      reportWatchers: state.reportWatchers.map((row) => ({ ...row })),
      teamMessages: state.teamMessages.map((row) => ({ ...row })),
      reportTeamMessages: state.reportTeamMessages.map((row) => ({ ...row })),
      nextTeamId: state.nextTeamId,
      nextReportChatId: state.nextReportChatId,
      nextReportId: state.nextReportId,
      shares: state.shares.map((row) => ({ ...row })),
      nextId: state.nextId,
      filterDomains: state.filterDomains.slice(),
      filterPhrases: state.filterPhrases.slice(),
      filterExceptions: state.filterExceptions.slice(),
      filterSeeded: state.filterSeeded,
      playerTokens: state.playerTokens.map((row) => ({ ...row })),
      roles: new Map(state.roles),
      leaderboard: state.leaderboard.map((row) => ({ ...row })),
      failEvidence: !!state.failEvidence
    };
  }

  function restoreState(saved) {
    state.seasons = saved.seasons;
    state.messages = saved.messages;
    state.events = saved.events;
    state.redactions = saved.redactions;
    state.evidence = saved.evidence;
    state.sanctions = saved.sanctions;
    state.reports = saved.reports;
    state.reportEvidence = saved.reportEvidence;
    state.reportWatches = saved.reportWatches;
    state.reportWatchers = saved.reportWatchers;
    state.teamMessages = saved.teamMessages;
    state.reportTeamMessages = saved.reportTeamMessages;
    state.nextTeamId = saved.nextTeamId;
    state.nextReportChatId = saved.nextReportChatId;
    state.nextReportId = saved.nextReportId;
    state.shares = saved.shares;
    state.nextId = saved.nextId;
    state.filterDomains = saved.filterDomains;
    state.filterPhrases = saved.filterPhrases;
    state.filterExceptions = saved.filterExceptions;
    state.filterSeeded = saved.filterSeeded;
    state.playerTokens = saved.playerTokens;
    state.roles = saved.roles;
    state.leaderboard = saved.leaderboard;
    state.failEvidence = saved.failEvidence;
  }

  function seasonStillEnded(seasonId, currentStart) {
    const season = state.seasons.find((row) => Number(row.id) === Number(seasonId));
    if (!season || Number(season.is_current) !== 0 || season.start_at == null) return false;
    return new Date(season.start_at).getTime() < new Date(currentStart).getTime();
  }

  function keepRows(rows, idField, params) {
    const currentStart = params[params.length - 1];
    const ids = params.slice(0, -1).map(Number);
    const kept = rows.filter((row) => {
      if (!ids.includes(Number(row[idField]))) return true;
      return !seasonStillEnded(row[idField], currentStart);
    });
    return { kept, affected: rows.length - kept.length };
  }

  function limitedReportRows(sql, rows) {
    let list = rows.slice();
    if (/ORDER BY id DESC/.test(sql)) list.sort((a, b) => Number(b.id) - Number(a.id));
    const match = /LIMIT\s+(\d+)/.exec(sql);
    if (match) list = list.slice(0, Number(match[1]));
    return list;
  }

  const pool = {
    async query(sql, params = []) {
      state.sqlLog.push(String(sql));
      if (/FROM tokens t/.test(sql) && /t\.token = \?/.test(sql)) {
        const row = state.playerTokens.find((entry) => entry.token === params[0] && !entry.revoked_at);
        if (!row) return [[]];
        const player = players.find((entry) => entry.id === row.player_id && entry.mc_uuid);
        if (!player) return [[]];
        return [[{ player: player.display_name, player_id: player.id, mc_uuid: player.mc_uuid }]];
      }
      if (/FROM chat_filter_state/.test(sql)) {
        return state.filterSeeded ? [[{ domains_seeded: 1 }]] : [[]];
      }
      if (/INSERT INTO chat_filter_state/.test(sql) || /UPDATE chat_filter_state SET domains_seeded/.test(sql)) {
        state.filterSeeded = true;
        return [{ affectedRows: 1 }];
      }
      if (/DELETE FROM chat_filter_domains/.test(sql)) {
        state.filterDomains = [];
        return [{ affectedRows: 1 }];
      }
      if (/DELETE FROM chat_filter_phrases/.test(sql)) {
        state.filterPhrases = [];
        return [{ affectedRows: 1 }];
      }
      if (/DELETE FROM chat_filter_exceptions/.test(sql)) {
        state.filterExceptions = [];
        return [{ affectedRows: 1 }];
      }
      if (/INSERT IGNORE INTO chat_filter_domains/.test(sql)) {
        if (!state.filterDomains.includes(params[0])) state.filterDomains.push(params[0]);
        return [{ affectedRows: 1 }];
      }
      if (/INSERT IGNORE INTO chat_filter_phrases/.test(sql)) {
        if (!state.filterPhrases.includes(params[0])) state.filterPhrases.push(params[0]);
        return [{ affectedRows: 1 }];
      }
      if (/INSERT IGNORE INTO chat_filter_exceptions/.test(sql)) {
        if (!state.filterExceptions.includes(params[0])) state.filterExceptions.push(params[0]);
        return [{ affectedRows: 1 }];
      }
      if (/FROM chat_filter_domains/.test(sql)) {
        return [[
          ...state.filterDomains.map(entry => ({ list_name: 'domains', entry })),
          ...state.filterPhrases.map(entry => ({ list_name: 'phrases', entry })),
          ...state.filterExceptions.map(entry => ({ list_name: 'exceptions', entry }))
        ]];
      }
      if (/INSERT INTO team_chat_messages/.test(sql)) {
        const row = {
          id: state.nextTeamId++,
          actor_player_id: params[0],
          actor_name: params[1],
          role_snapshot: params[2],
          body: params[3],
          created_at: now()
        };
        state.teamMessages.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/FROM team_chat_messages/.test(sql) && /SELECT/.test(sql)) {
        const after = Number(params[1]) || 0;
        return [state.teamMessages.filter((row) => row.id > after)];
      }
      if (/DELETE FROM team_chat_messages/.test(sql)) {
        const cutoff = Date.now() - (24 * 60 * 60 * 1000);
        const before = state.teamMessages.length;
        state.teamMessages = state.teamMessages.filter((row) => new Date(row.created_at).getTime() >= cutoff);
        return [{ affectedRows: before - state.teamMessages.length }];
      }
      if (/COUNT\(\*\) AS total FROM chat_report_messages/.test(sql)) {
        const total = state.reportTeamMessages.filter((row) => Number(row.report_id) === Number(params[0])).length;
        return [[{ total }]];
      }
      if (/INSERT INTO chat_report_messages/.test(sql)) {
        const row = {
          id: state.nextReportChatId++,
          report_id: params[0],
          season_id: params[1],
          actor_player_id: params[2],
          actor_name: params[3],
          role_snapshot: params[4],
          body: params[5],
          created_at: now()
        };
        state.reportTeamMessages.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/FROM chat_report_messages/.test(sql) && /SELECT/.test(sql)) {
        return [state.reportTeamMessages.filter((row) => Number(row.report_id) === Number(params[0]))];
      }
      if (/DELETE target FROM chat_report_messages/.test(sql)) {
        const result = keepRows(state.reportTeamMessages, 'season_id', params);
        state.reportTeamMessages = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/SELECT id FROM chat_sanctions/.test(sql) && /kind = \?/.test(sql) && /LIMIT 1/.test(sql)) {
        return [state.sanctions.filter((s) => s.player_id === params[0] && s.kind === params[1] && active(s)).slice(0, 1)];
      }
      if (/SELECT id, is_current, start_at FROM seasons FOR UPDATE/.test(sql)) {
        return [state.seasons.map((row) => ({ ...row }))];
      }
      if (/SELECT id FROM seasons WHERE is_current = 1/.test(sql)) {
        return [state.seasons.filter((s) => s.is_current)];
      }
      if (/SELECT id, display_name, mc_uuid FROM players WHERE id = \?/.test(sql)) {
        const player = players.find((p) => p.id === Number(params[0]));
        return [player ? [{ ...player }] : []];
      }
      if (/FROM staff_roles WHERE player_id = \?/.test(sql)) {
        const id = Number(params[0]);
        if (state.roles.get(id) !== params[1]) return [[]];
        return [[{ player_id: id }]];
      }
      if (/display_name_normalized LIKE \?/.test(sql) && !/suggest-moderators/.test(sql)) {
        let prefix = String(params[0] || '').replace(/\\([%_\\])/g, '$1');
        if (prefix.endsWith('%')) prefix = prefix.slice(0, -1);
        const rows = players
          .filter((p) => p.mc_uuid && p.display_name_normalized.startsWith(prefix))
          .map((p) => ({ id: p.id, display_name: p.display_name }));
        return [rows];
      }
      if (/FROM players[\s\S]*display_name_normalized = \?/.test(sql)) {
        return [players.filter((p) => p.display_name_normalized === params[0])];
      }
      if (/FROM player_display_name_history/.test(sql)) return [[]];
      if (/FROM staff_roles r[\s\S]*p.mc_uuid = \?/.test(sql)) {
        const player = players.find((p) => p.mc_uuid === params[0]);
        if (!player || state.roles.get(player.id) !== 'moderator') return [[]];
        return [[{ player_id: player.id }]];
      }
      if (/FROM website_sessions/.test(sql)) {
        if (state.delaySessions) await new Promise((resolve) => setTimeout(resolve, state.delaySessions));
        const row = Object.values(sessions).find((entry) => sha256Hex(entry.token) === params[0]);
        return [row ? [{ ...row }] : []];
      }
      if (/INSERT INTO chat_messages/.test(sql)) {
        const row = {
          id: state.nextId++,
          player: params[0],
          player_id: params[1],
          message: params[2],
          timestamp: now(),
          deleted_at: null,
          deleted_event_id: null
        };
        state.messages.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/FROM chat_messages WHERE id = \? FOR UPDATE/.test(sql)) {
        return [state.messages.filter((m) => m.id === params[0])];
      }
      if (/FROM shared_content WHERE id = \?/.test(sql)) {
        if (params[0] === 'explode') throw new Error('share read failed');
        return [state.shares.filter((s) => s.id === params[0])];
      }
      if (/INSERT INTO moderation_events/.test(sql)) {
        const row = {
          id: state.events.length + 1,
          actor_player_id: params[0],
          target_player_id: params[1],
          source: params[2],
          kind: params[3],
          payload: params[4],
          season_id: params[5],
          created_at: now()
        };
        state.events.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/INSERT INTO chat_message_redactions/.test(sql)) {
        if (state.redactions.some((r) => r.message_id === params[0])) {
          const err = new Error('dup'); err.code = 'ER_DUP_ENTRY'; throw err;
        }
        state.redactions.push({
          message_id: params[0], original_text: params[1], original_player: params[2],
          original_player_id: params[3], original_timestamp: params[4],
          share_ids: params[5], share_snapshots: params[6], actor_player_id: params[7],
          reason_text: params[8], season_id: params[9], event_id: params[10]
        });
        return [{ insertId: state.redactions.length }];
      }
      if (/INSERT INTO player_file_evidence/.test(sql) && /sanction_id/.test(sql)) {
        if (state.failEvidence) throw new Error('evidence failed');
        state.evidence.push({
          target_player_id: params[0],
          message_id: params[1],
          snapshot_text: params[2],
          snapshot_at: params[3],
          action: params[4],
          reason_text: params[5],
          actor_player_id: params[6],
          source: params[7],
          season_id: params[8],
          event_id: params[9],
          sanction_id: params[10],
          share_ids: params[11],
          share_snapshots: params[12]
        });
        return [{ insertId: state.evidence.length }];
      }
      if (/INSERT INTO player_file_evidence/.test(sql)) {
        state.evidence.push({
          target_player_id: params[0],
          message_id: params[1],
          snapshot_text: params[2],
          snapshot_at: params[3],
          action: 'message_delete',
          reason_text: params[4],
          actor_player_id: params[5],
          source: params[6],
          season_id: params[7],
          event_id: params[8],
          report_id: null
        });
        return [{ insertId: state.evidence.length }];
      }
      if (/UPDATE chat_messages/.test(sql) && /deleted_at IS NULL/.test(sql)) {
        const row = state.messages.find((m) => m.id === params[2] && !m.deleted_at);
        if (!row) return [{ affectedRows: 0 }];
        row.message = params[0];
        row.deleted_at = now();
        row.deleted_event_id = params[1];
        return [{ affectedRows: 1 }];
      }
      if (/FROM chat_sanctions[\s\S]*kind = \?/.test(sql) && /FOR UPDATE/.test(sql)) {
        return [state.sanctions.filter((s) => s.player_id === params[0] && s.kind === params[1] && active(s))];
      }
      if (/FROM chat_sanctions[\s\S]*player_id = \? AND/.test(sql) && /lifted_at IS NULL/.test(sql) && /SELECT id, player_id, kind/.test(sql)) {
        return [state.sanctions.filter((s) => s.player_id === params[0] && active(s))];
      }
      if (/INSERT INTO chat_sanctions/.test(sql)) {
        const row = {
          id: state.sanctions.length + 1,
          player_id: params[0],
          kind: params[1],
          starts_at: now(),
          expires_at: params[2],
          lifted_at: null,
          reason_text: params[3],
          actor_player_id: params[4],
          source: params[5],
          created_season_id: params[6],
          created_event_id: null,
          duration_key: params[7],
          message_id: params[8]
        };
        state.sanctions.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/UPDATE chat_sanctions SET created_event_id/.test(sql)) {
        const row = state.sanctions.find((entry) => entry.id === params[1]);
        if (row) row.created_event_id = params[0];
        return [{ affectedRows: row ? 1 : 0 }];
      }
      if (/UPDATE chat_sanctions[\s\S]*lifted_at = NOW/.test(sql)) {
        let n = 0;
        for (const row of state.sanctions) {
          if (row.player_id === params[2] && row.kind === params[3] && active(row)) {
            row.lifted_at = now();
            row.lift_reason = params[0];
            n += 1;
          }
        }
        return [{ affectedRows: n }];
      }
      if (/FROM chat_sanctions s[\s\S]*LEFT JOIN players/.test(sql)) {
        let rows = state.sanctions.filter(active).map((s) => {
          const player = players.find((p) => p.id === s.player_id) || {};
          return { ...s, player: player.display_name || '', mc_uuid: player.mc_uuid || null };
        });
        if (/p\.mc_uuid IS NOT NULL/.test(sql)) rows = rows.filter((s) => s.mc_uuid);
        if (/s\.kind = \?/.test(sql)) rows = rows.filter((s) => s.kind === params[params.length - 1]);
        if (sql.includes('s.player_id = ?')) rows = rows.filter((s) => s.player_id === params[0]);
        return [rows];
      }
      if (/FROM moderation_events\s+WHERE kind = 'report_closed'/.test(sql)) {
        return [state.events.filter((row) => row.kind === 'report_closed' && Number(row.target_player_id) === Number(params[0]))];
      }
      if (/FROM moderation_events e/.test(sql) && !/suggest-moderators/.test(sql)) {
        let rows = state.events.map((e) => ({
          ...e,
          actor: (players.find((p) => p.id === e.actor_player_id) || {}).display_name,
          target: (players.find((p) => p.id === e.target_player_id) || {}).display_name
        }));
        rows.sort((a, b) => Number(b.id) - Number(a.id));
        if (/LIMIT \?/.test(sql)) {
          const limit = Number(params[params.length - 1]);
          if (Number.isFinite(limit)) rows = rows.slice(0, limit);
        }
        return [rows];
      }
      if (/c\.deleted_at IS NOT NULL AND c\.id IN/.test(sql)) {
        const ids = new Set(params.map(Number));
        return [state.messages
          .filter((m) => m.deleted_at && ids.has(Number(m.id)))
          .sort((a, b) => a.id - b.id)
          .map((m) => ({
            id: m.id,
            player: m.player,
            message: m.message,
            timestamp: m.timestamp && m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
            deleted_at: m.deleted_at
          }))];
      }
      if (/c\.id > \?/.test(sql)) {
        if (state.delayAfterQuery) {
          const wait = state.delayAfterQuery;
          state.delayAfterQuery = 0;
          await new Promise((resolve) => setTimeout(resolve, wait));
        }
        const after = Number(params[0]);
        const limit = Number(params[1]) || 80;
        return [state.messages
          .filter((m) => Number(m.id) > after)
          .sort((a, b) => a.id - b.id)
          .slice(0, limit)
          .map((m) => ({
            id: m.id,
            player: m.player,
            message: m.message,
            timestamp: m.timestamp && m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
            deleted_at: m.deleted_at
          }))];
      }
      if (/FROM chat_messages c[\s\S]*ORDER BY c.id DESC/.test(sql) || /FROM chat_messages c[\s\S]*ORDER BY c.timestamp/.test(sql)) {
        return [state.messages.map((m) => ({
          id: m.id,
          player: m.player,
          message: m.deleted_at ? chatMod.PLACEHOLDER : m.message,
          timestamp: m.timestamp.getTime ? m.timestamp.getTime() : m.timestamp,
          deleted_at: m.deleted_at
        }))];
      }
      if (/FROM chat_message_redactions/.test(sql) && !/DELETE/.test(sql)) {
        return [state.redactions.filter((r) => Number(r.message_id) === Number(params[0]))];
      }
      if (/DELETE target FROM moderation_events|DELETE target FROM chat_message_redactions|DELETE target FROM player_file_evidence|DELETE s FROM chat_sanctions/.test(sql)) {
        if (state.beforeSeasonDelete) {
          const hook = state.beforeSeasonDelete;
          state.beforeSeasonDelete = null;
          hook();
        }
      }
      if (/DELETE target FROM moderation_events/.test(sql)) {
        const result = keepRows(state.events, 'season_id', params);
        state.events = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM chat_message_redactions/.test(sql)) {
        const result = keepRows(state.redactions, 'season_id', params);
        state.redactions = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM player_file_evidence/.test(sql)) {
        const result = keepRows(state.evidence, 'season_id', params);
        state.evidence = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE s FROM chat_sanctions/.test(sql) && /expires_at < NOW/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        const before = state.sanctions.length;
        state.sanctions = state.sanctions.filter((row) => {
          if (!ids.includes(Number(row.created_season_id))) return true;
          if (!seasonStillEnded(row.created_season_id, currentStart)) return true;
          return !(!row.lifted_at && row.expires_at && new Date(row.expires_at) < now());
        });
        return [{ affectedRows: before - state.sanctions.length }];
      }
      if (/DELETE s FROM chat_sanctions/.test(sql) && /lifted_at IS NOT NULL/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        const before = state.sanctions.length;
        state.sanctions = state.sanctions.filter((row) => {
          if (!ids.includes(Number(row.created_season_id))) return true;
          if (!seasonStillEnded(row.created_season_id, currentStart)) return true;
          return !row.lifted_at;
        });
        return [{ affectedRows: before - state.sanctions.length }];
      }
      if (/FROM chat_messages WHERE id = \? AND player_id = \?/.test(sql)) {
        return [state.messages.filter((row) => row.id === params[0] && row.player_id === params[1])];
      }
      if (/FROM chat_messages[\s\S]*WHERE player_id = \?[\s\S]*ORDER BY id DESC/.test(sql)) {
        return [state.messages
          .filter((row) => row.player_id === params[0])
          .sort((a, b) => b.id - a.id)];
      }
      if (/file-tiles-reports/.test(sql)) {
        return [state.reports.filter((row) => row.outcome === 'confirmed_violation' && Number(row.confirmed) === 1).map((row) => ({
          id: row.id,
          target_player_id: row.target_player_id,
          tile_target: (players.find((player) => player.id === row.target_player_id) || {}).display_name || '',
          tile_owner: (players.find((player) => player.id === row.owner_player_id) || {}).display_name || '',
          tile_closed_by: (players.find((player) => player.id === row.closed_by_player_id) || {}).display_name || '',
          outcome: row.outcome,
          reason_text: row.reason_text || ''
        }))];
      }
      if (/file-tiles-direct/.test(sql)) {
        return [state.evidence.filter((row) => row.report_id == null).map((row) => ({
          target_player_id: row.target_player_id,
          tile_target: (players.find((player) => player.id === row.target_player_id) || {}).display_name || '',
          tile_actor: (players.find((player) => player.id === row.actor_player_id) || {}).display_name || '',
          action: row.action || '',
          reason_text: row.reason_text || ''
        }))];
      }
      if (/suggest-moderators/.test(sql)) {
        const ids = new Set();
        for (const [id, role] of state.roles) if (role === 'moderator') ids.add(Number(id));
        for (const event of state.events) if (event.actor_player_id != null) ids.add(Number(event.actor_player_id));
        for (const report of state.reports) {
          if (report.owner_player_id != null) ids.add(Number(report.owner_player_id));
          if (report.closed_by_player_id != null) ids.add(Number(report.closed_by_player_id));
        }
        const adminUuids = new Set(params.filter((value) => typeof value === 'string' && value.includes('-')));
        let rows = players.filter((player) => player.mc_uuid && (ids.has(player.id) || adminUuids.has(player.mc_uuid)));
        if (/LIKE \?/.test(sql)) {
          const like = params.find((value) => typeof value === 'string' && value.endsWith('%'));
          const prefix = String(like || '').replace(/\\([%_\\])/g, '$1').replace(/%$/, '');
          rows = rows.filter((player) => player.display_name_normalized.startsWith(prefix));
        }
        rows.sort((a, b) => a.display_name_normalized.localeCompare(b.display_name_normalized));
        const limit = Number(params[params.length - 1]);
        if (Number.isFinite(limit)) rows = rows.slice(0, limit);
        return [rows.map((player) => ({ id: player.id, display_name: player.display_name }))];
      }
      if (/FROM player_file_evidence[\s\S]*report_id IS NULL/.test(sql)) {
        return [state.evidence.filter((row) => Number(row.target_player_id) === Number(params[0]) && row.report_id == null)];
      }
      if (/SUM\(outcome = 'confirmed_violation'\)/.test(sql) && /reporter_player_id = \?/.test(sql)) {
        const rows = state.reports.filter((row) => Number(row.reporter_player_id) === Number(params[0]) && row.outcome !== 'season_ended');
        return [[{
          confirmed: rows.filter((row) => row.outcome === 'confirmed_violation').length,
          open_count: rows.filter((row) => row.status === 'open' || row.status === 'in_progress').length,
          unconfirmed: rows.filter((row) => row.outcome === 'no_violation').length
        }]];
      }
      if (/FROM chat_reports[\s\S]*outcome = 'confirmed_violation'/.test(sql)) {
        return [limitedReportRows(sql, state.reports.filter((row) => Number(row.target_player_id) === Number(params[0]) && row.outcome === 'confirmed_violation'))];
      }
      if (/FROM chat_reports[\s\S]*outcome <> 'season_ended'/.test(sql)) {
        return [limitedReportRows(sql, state.reports.filter((row) => Number(row.reporter_player_id) === Number(params[0]) && row.outcome !== 'season_ended'))];
      }
      if (/FROM chat_reports[\s\S]*WHERE status = 'closed'/.test(sql)) {
        let rows = state.reports.filter((row) => row.status === 'closed');
        if (/AND outcome = \?/.test(sql)) rows = rows.filter((row) => row.outcome === params[0]);
        return [limitedReportRows(sql, rows)];
      }
      if (/FROM chat_reports[\s\S]*reporter_player_id = \?/.test(sql)) {
        const since = new Date(params[2]).getTime();
        return [state.reports.filter((row) => row.reporter_player_id === params[0]
          && row.target_player_id === params[1]
          && row.status !== 'closed'
          && new Date(row.created_at).getTime() >= since)];
      }
      if (/INSERT INTO chat_reports/.test(sql)) {
        const row = {
          id: state.nextReportId++,
          season_id: params[0],
          target_player_id: params[1],
          reporter_player_id: params[2],
          message_id: params[3],
          reason_text: params[4],
          status: 'open',
          owner_player_id: null,
          help_requested: 0,
          confirmed: null,
          outcome: null,
          source: params[5],
          created_at: now(),
          closed_at: null,
          closed_by_player_id: null
        };
        state.reports.push(row);
        return [{ insertId: row.id, affectedRows: 1 }];
      }
      if (/INSERT INTO chat_report_evidence/.test(sql)) {
        state.reportEvidence.push({
          report_id: params[0],
          season_id: params[1],
          message_id: params[2],
          phase: params[3],
          player: params[4],
          body: params[5],
          message_at: params[6],
          position: params[7]
        });
        return [{ insertId: state.reportEvidence.length, affectedRows: 1 }];
      }
      if (/INSERT INTO chat_report_watch_state/.test(sql)) {
        state.reportWatches.push({
          report_id: params[0],
          season_id: params[1],
          target_player_id: params[2],
          started_at: params[3],
          ends_at: params[4],
          after_count: 0,
          ended_reason: null,
          ended_at: null
        });
        return [{ affectedRows: 1 }];
      }
      if (/FROM chat_report_watch_state[\s\S]*target_player_id = \? AND ended_reason IS NULL/.test(sql)) {
        return [state.reportWatches.filter((row) => row.target_player_id === params[0] && !row.ended_reason)];
      }
      if (/UPDATE chat_report_watch_state[\s\S]*SET ended_reason = \?/.test(sql) && !/INNER JOIN seasons/.test(sql)) {
        const row = state.reportWatches.find((entry) => entry.report_id === params[2] && !entry.ended_reason);
        if (!row) return [{ affectedRows: 0 }];
        row.ended_reason = params[0];
        row.ended_at = params[1];
        return [{ affectedRows: 1 }];
      }
      if (/UPDATE chat_report_watch_state[\s\S]*after_count = after_count \+ 1/.test(sql)) {
        const row = state.reportWatches.find((entry) => entry.report_id === params[0] && !entry.ended_reason);
        if (!row) return [{ affectedRows: 0 }];
        row.after_count += 1;
        return [{ affectedRows: 1 }];
      }
      if (/FROM chat_report_watch_state[\s\S]*ended_reason IS NULL/.test(sql) && /SELECT report_id, ends_at/.test(sql)) {
        return [state.reportWatches.filter((row) => !row.ended_reason)];
      }
      if (/FROM chat_report_evidence[\s\S]*report_id = \?/.test(sql)) {
        return [state.reportEvidence
          .filter((row) => row.report_id === Number(params[0]))
          .sort((a, b) => String(a.phase).localeCompare(String(b.phase)) || a.position - b.position)];
      }
      if (/FROM chat_report_watchers[\s\S]*player_id = \?/.test(sql) && /SELECT/.test(sql)) {
        return [state.reportWatchers.filter((row) => row.report_id === Number(params[0]) && Number(row.player_id) === Number(params[1]))];
      }
      if (/FROM chat_report_watchers[\s\S]*report_id = \?/.test(sql) && /SELECT/.test(sql)) {
        return [state.reportWatchers.filter((row) => row.report_id === Number(params[0]))];
      }
      if (/FROM chat_report_watch_state[\s\S]*report_id = \?/.test(sql)) {
        return [state.reportWatches.filter((row) => row.report_id === Number(params[0]))];
      }
      if (/FROM chat_reports[\s\S]*WHERE id = \?/.test(sql)) {
        return [state.reports.filter((row) => row.id === Number(params[0]))];
      }
      if (/SELECT COUNT\(\*\) AS total FROM chat_reports WHERE status <> 'closed'/.test(sql)) {
        return [[{ total: state.reports.filter((row) => row.status !== 'closed').length }]];
      }
      if (/FROM chat_reports[\s\S]*status <> 'closed'/.test(sql)) {
        return [state.reports.filter((row) => row.status !== 'closed')];
      }
      if (/UPDATE chat_reports[\s\S]*status = 'in_progress'/.test(sql)) {
        const run = async () => {
          if (state.claimDelay) await new Promise((resolve) => setTimeout(resolve, state.claimDelay));
          const row = state.reports.find((entry) => entry.id === Number(params[1]) && entry.status === 'open' && entry.owner_player_id == null);
          if (!row) return [{ affectedRows: 0 }];
          row.status = 'in_progress';
          row.owner_player_id = params[0];
          return [{ affectedRows: 1 }];
        };
        const previous = state.claimChain || Promise.resolve();
        const current = previous.then(run, run);
        state.claimChain = current;
        return current;
      }
      if (/UPDATE chat_reports[\s\S]*status = 'closed', outcome = \?/.test(sql)) {
        const row = state.reports.find((entry) => entry.id === Number(params[4]) && entry.status !== 'closed');
        if (!row) return [{ affectedRows: 0 }];
        row.status = 'closed';
        row.outcome = params[0];
        row.confirmed = params[1];
        row.closed_at = params[2];
        row.closed_by_player_id = params[3];
        return [{ affectedRows: 1 }];
      }
      if (/UPDATE chat_reports[\s\S]*status = 'open', owner_player_id = NULL/.test(sql)) {
        const row = state.reports.find((entry) => entry.id === Number(params[0]) && entry.status !== 'closed');
        if (!row) return [{ affectedRows: 0 }];
        row.status = 'open';
        row.owner_player_id = null;
        row.help_requested = 0;
        return [{ affectedRows: 1 }];
      }
      if (/DELETE FROM chat_report_watchers WHERE report_id = \?/.test(sql)) {
        const before = state.reportWatchers.length;
        state.reportWatchers = state.reportWatchers.filter((row) => row.report_id !== Number(params[0]));
        return [{ affectedRows: before - state.reportWatchers.length }];
      }
      if (/UPDATE chat_reports SET help_requested = 1/.test(sql)) {
        const row = state.reports.find((entry) => entry.id === Number(params[0]) && entry.status !== 'closed');
        if (!row) return [{ affectedRows: 0 }];
        row.help_requested = 1;
        return [{ affectedRows: 1 }];
      }
      if (/INSERT IGNORE INTO chat_report_watchers/.test(sql)) {
        const exists = state.reportWatchers.some((row) => row.report_id === Number(params[0]) && row.player_id === params[1]);
        if (exists) return [{ affectedRows: 0 }];
        state.reportWatchers.push({ report_id: Number(params[0]), player_id: params[1], season_id: params[2] });
        return [{ affectedRows: 1 }];
      }
      if (/UPDATE chat_reports target[\s\S]*season_ended/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        let affected = 0;
        for (const row of state.reports) {
          if (!ids.includes(Number(row.season_id)) || row.status === 'closed') continue;
          if (!seasonStillEnded(row.season_id, currentStart)) continue;
          row.status = 'closed';
          row.outcome = 'season_ended';
          row.confirmed = null;
          row.closed_at = now();
          affected += 1;
        }
        return [{ affectedRows: affected }];
      }
      if (/UPDATE chat_report_watch_state target[\s\S]*season_ended/.test(sql)) {
        const currentStart = params[params.length - 1];
        const ids = params.slice(0, -1).map(Number);
        let affected = 0;
        for (const row of state.reportWatches) {
          if (!ids.includes(Number(row.season_id)) || row.ended_reason) continue;
          if (!seasonStillEnded(row.season_id, currentStart)) continue;
          row.ended_reason = 'season_ended';
          row.ended_at = now();
          affected += 1;
        }
        return [{ affectedRows: affected }];
      }
      if (/DELETE target FROM chat_report_evidence/.test(sql)) {
        const result = keepRows(state.reportEvidence, 'season_id', params);
        state.reportEvidence = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM chat_report_watch_state/.test(sql)) {
        const result = keepRows(state.reportWatches, 'season_id', params);
        state.reportWatches = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/DELETE target FROM chat_report_watchers/.test(sql)) {
        const result = keepRows(state.reportWatchers, 'season_id', params);
        state.reportWatchers = result.kept;
        return [{ affectedRows: result.affected }];
      }
      if (/FROM leaderboard/.test(sql)) return [state.leaderboard];
      return [[]];
    }
  };
  function makeMutex() {
    let tail = Promise.resolve();
    return {
      acquire() {
        const prev = tail;
        let release;
        const gate = new Promise((resolve) => { release = resolve; });
        tail = prev.then(() => gate, () => gate);
        return prev.then(() => release, () => release);
      }
    };
  }
  state.reportMutex = makeMutex();
  state.watchMutex = makeMutex();
  state.reportRowMutex = makeMutex();
  pool.getConnection = async () => {
    let saved = null;
    let namedRelease = null;
    let watchRelease = null;
    let reportRowRelease = null;
    const unlockNamed = () => {
      if (!namedRelease) return;
      const release = namedRelease;
      namedRelease = null;
      state.reportLockHeld = false;
      release();
    };
    const unlockWatch = () => {
      if (!watchRelease) return;
      const release = watchRelease;
      watchRelease = null;
      state.watchLockHeld = false;
      release();
    };
    const unlockReportRow = () => {
      if (!reportRowRelease) return;
      const release = reportRowRelease;
      reportRowRelease = null;
      state.reportRowHeld = false;
      release();
    };
    return {
      async query(sql, params = []) {
        const text = String(sql);
        if (/SELECT GET_LOCK\(/.test(text)) {
          state.sqlLog.push(text);
          if (!namedRelease) {
            if (state.reportLockHeld) state.reportLockOverlaps += 1;
            state.reportLockHeld = true;
            namedRelease = await state.reportMutex.acquire();
            if (state.reportCreateDelay) {
              await new Promise((resolve) => setTimeout(resolve, state.reportCreateDelay));
            }
          }
          return [[{ locked: 1 }]];
        }
        if (/SELECT RELEASE_LOCK\(/.test(text)) {
          state.sqlLog.push(text);
          unlockNamed();
          return [[{ released: 1 }]];
        }
        if (/report-row-lock|report-chat-lock/.test(text) && /FOR UPDATE/.test(text)) {
          if (!reportRowRelease) {
            if (state.reportRowHeld) state.reportRowOverlaps += 1;
            state.reportRowHeld = true;
            reportRowRelease = await state.reportRowMutex.acquire();
            if (/report-row-lock/.test(text) && state.pauseAfterReportLock) {
              state.reportRowPaused = true;
              const pause = state.pauseAfterReportLock;
              state.pauseAfterReportLock = null;
              await pause;
              state.reportRowPaused = false;
            }
          }
          return pool.query(text.replace(/\s+FOR UPDATE/i, ''), params);
        }
        if (/chat_report_watch_state[\s\S]*FOR UPDATE/.test(text)) {
          if (!watchRelease) {
            if (state.watchLockHeld) state.watchLockOverlaps += 1;
            state.watchLockHeld = true;
            watchRelease = await state.watchMutex.acquire();
            if (state.watchDelay) {
              await new Promise((resolve) => setTimeout(resolve, state.watchDelay));
            }
          }
          return pool.query(text, params);
        }
        return pool.query(text, params);
      },
      async beginTransaction() { saved = snapshotState(); },
      async commit() {
        saved = null;
        unlockWatch();
        unlockReportRow();
      },
      async rollback() {
        if (saved) restoreState(saved);
        saved = null;
        unlockWatch();
        unlockReportRow();
      },
      release() {
        unlockNamed();
        unlockWatch();
        unlockReportRow();
      }
    };
  };
  return { pool, state, players, sessions };
}

function createApp(db, hooks = {}) {
  const app = express();
  app.use(express.json());
  app.use(cookieParser());
  const checkAdmin = installAdminAuth(app, db.pool);
  const checkToken = (req, res, next) => {
    const uuid = req.headers['x-test-uuid'];
    if (!uuid) return res.status(401).json({ error: 'Kein Token angegeben' });
    const player = db.players.find((p) => p.mc_uuid === uuid);
    if (!player) return res.status(403).json({ error: 'Ungültiger Token' });
    req.mcUuid = uuid;
    req.playerId = player.id;
    req.player = player.display_name;
    next();
  };
  installModerationHttp(app, db.pool, {
    checkToken,
    checkStaff: checkAdmin.checkStaff,
    onSanctionChanged: hooks.onSanctionChanged,
    onTeamChat: hooks.onTeamChat,
    onReportTeamChat: hooks.onReportTeamChat
  });
  app.post('/chat/send', checkToken, async (req, res) => {
    const accepted = await chatMod.acceptPlayerChat(db.pool, {
      player: req.player, playerId: req.playerId, message: req.body && req.body.message
    });
    if (!accepted.ok) return res.status(accepted.status || 400).json({ error: accepted.error, code: accepted.code });
    res.json({ success: true, id: accepted.id });
  });
  chatMod.installChatHistoryRoute(app, db.pool, checkToken);
  chatMod.installPlayerSearchRoute(app, db.pool, checkToken);
  return app;
}

function adminHeaders(token) {
  return { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin', cookie: 'cclive_web=' + token };
}

async function waitUntil(check) {
  for (let i = 0; i < 80; i += 1) {
    if (check()) return;
    await new Promise((resolve) => setTimeout(resolve, 20));
  }
  throw new Error('Zeitüberschreitung');
}

(async () => {
  const prev = { ADMIN_KEY: process.env.ADMIN_KEY, MOD_KEY: process.env.MOD_KEY, TOOLS: process.env.MODERATION_TOOLS_ENABLED, FILTERS: process.env.CHAT_FILTERS_ENABLED };
  process.env.ADMIN_KEY = ADMIN_KEY;
  process.env.MOD_KEY = MOD_KEY;
  delete process.env.MODERATION_TOOLS_ENABLED;
  try {
    const staffJs = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/staff-moderation.js'), 'utf8');
    const deleteFn = staffJs.slice(staffJs.indexOf('async function deleteSelected'), staffJs.indexOf('async function applyKind'));
    assert.ok(deleteFn.includes('if (deleting) return'));
    assert.ok(!deleteFn.includes('Begründung ist Pflicht'));
    assert.ok(staffJs.includes('Begründung ist Pflicht'));
    const home = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/staff-home.html'), 'utf8');
    const workspace = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/admin-workspace.js'), 'utf8');
    assert.ok(home.includes('Beim Löschen darf sie leer bleiben'));
    assert.ok(workspace.includes('Beim Löschen darf sie leer bleiben'));
    const adminHtml = fs.readFileSync(path.join(__dirname, '../../../backend_server/admin_ui/admin.html'), 'utf8');
    const styleCss = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/admin-style.css'), 'utf8');
    assert.ok(home.includes('Spieler Reports'));
    assert.ok(home.includes('Eingegangene Reports'));
    assert.ok(home.includes('id="staff-view"'));
    assert.ok(home.includes('chatmod-log-scroll'));
    assert.ok(workspace.includes('Spieler Reports'));
    assert.ok(workspace.includes('Eingegangene Reports'));
    assert.ok(workspace.includes('id="reports-open"'));
    assert.ok(workspace.includes('chatmod-log-scroll'));
    assert.ok(workspace.includes('Laufende Bestrafungen'));
    assert.ok(!workspace.includes('Erkannte Regelverstöße'));
    assert.ok(adminHtml.includes('value="reports">Eingegangene Reports'));
    assert.ok(staffJs.includes('Unterstützen'));
    assert.ok(!staffJs.includes('Einklinken'));
    assert.ok(staffJs.includes("payload.outcome === 'no_violation'"));
    assert.ok(staffJs.includes("payload.outcome === 'confirmed_violation'"));
    assert.ok(staffJs.includes('Kein Verstoß'));
    assert.ok(staffJs.includes('Verstoß bestätigt'));
    assert.ok(staffJs.includes("parts.push('Report #' + payload.reportId)"));
    assert.ok(styleCss.includes('.chatmod-log-scroll'));
    assert.ok(styleCss.includes('.report-actions'));
    const pinnedChoice = chatMod.chatReports.selectBeforeMessages(
      [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }],
      1,
      3
    ).map((row) => row.id);
    assert.deepStrictEqual(pinnedChoice, [1, 3, 4]);
    assert.deepStrictEqual(chatMod.extractShareIds('hi #ITEM:ITEM-db2ac5 and #BUILD:BUILD-709125.'), [
      { type: 'ITEM', id: 'ITEM-db2ac5' }, { type: 'BUILD', id: 'BUILD-709125' }
    ]);
    assert.deepStrictEqual(chatMod.extractShareIds('#ITEM:ITEM'), []);
    assert.strictEqual(chatMod.parseDuration('5m', false).ms, 5 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('30m', false).key, '30m');
    assert.strictEqual(chatMod.parseDuration('2h', false).ms, 2 * 60 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('14d', false).key, '14d');
    assert.strictEqual(chatMod.parseDuration('10m', false).key, '10m');
    assert.strictEqual(chatMod.parseDuration('366d', false).ms, 366 * 24 * 60 * 60 * 1000);
    assert.strictEqual(chatMod.parseDuration('0m', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('-1h', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('1', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('367d', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('9007199254740993m', false).code, 'invalid_duration');
    assert.strictEqual(chatMod.parseDuration('permanent', false).code, 'confirm_required');
    assert.strictEqual(chatMod.parseDuration('permanent', true).ms, null);
    assert.strictEqual(chatMod.parseDuration('5m', true).key, '5m');
    const deletedPublic = chatMod.toPublicMessage({ id: 1, player: 'A', message: 'x', deleted_at: new Date() });
    assert.strictEqual(deletedPublic.message, chatMod.PLACEHOLDER);
    assert.strictEqual(deletedPublic.deleted, true);
    assert.strictEqual(deletedPublic.player, 'A');
    const typedPlaceholder = chatMod.toPublicMessage({ id: 2, player: 'A', message: chatMod.PLACEHOLDER, deleted_at: null });
    assert.strictEqual(typedPlaceholder.deleted, false);
    assert.strictEqual(typedPlaceholder.message, chatMod.PLACEHOLDER);

    const db = makeDb();
    const app = createApp(db);
    const token = (uuid) => ({ 'x-test-uuid': uuid });

    const noFeed = await staffFeed.authorizeStaffSocket(db.pool, '');
    assert.strictEqual(noFeed.ok, false);
    const modFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(modFeed.ok, true);
    const adminFeed = await staffFeed.authorizeStaffSocket(db.pool, 'other=1; cclive_web=' + db.sessions.admin.token);
    assert.strictEqual(adminFeed.ok, true);
    const savedUnlock = db.sessions.mod.mod_unlocked_until;
    db.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    const expiredFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(expiredFeed.ok, false);
    db.sessions.mod.mod_unlocked_until = savedUnlock;
    db.state.roles.delete(33);
    const revokedFeed = await staffFeed.authorizeStaffSocket(db.pool, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(revokedFeed.ok, false);
    db.state.roles.set(33, 'moderator');

    const feed = staffFeed.createStaffFeed(db.pool);
    const viewer = {
      readyState: 1,
      sent: [],
      send(text) { this.sent.push(JSON.parse(text)); },
      close(code) { this.readyState = 3; this.closedCode = code; }
    };
    const accepted = await feed.accept(viewer, 'cclive_web=' + db.sessions.mod.token);
    assert.strictEqual(accepted.ok, true);
    db.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    viewer.staffCheckedAt = 0;
    await feed.publish({ type: 'chat.message', id: 4, player: 'Plain', message: 'live' });
    assert.strictEqual(viewer.sent.length, 0);
    assert.strictEqual(viewer.readyState, 3);
    db.sessions.mod.mod_unlocked_until = savedUnlock;

    const merged = staffFeed.mergeStaffFeed(
      [{ id: 8, player: 'Plain', message: 'geheim', deleted: false }],
      [
        { id: 8, player: 'Plain', message: chatMod.PLACEHOLDER, deleted: true },
        { id: 9, player: 'Plain', message: 'neu', deleted: false },
        { id: 9, player: 'Plain', message: 'neu', deleted: false }
      ]
    );
    assert.strictEqual(merged.length, 2);
    assert.strictEqual(merged[0].deleted, true);
    assert.strictEqual(merged[0].message, chatMod.PLACEHOLDER);
    assert.ok(!JSON.stringify(merged).includes('geheim'));
    assert.strictEqual(merged[1].id, 9);
    db.state.messages.push(
      { id: 70, player: 'Plain', player_id: 44, message: 'geheim-original', timestamp: new Date(), deleted_at: new Date() },
      { id: 71, player: 'Plain', player_id: 44, message: 'sichtbar', timestamp: new Date(), deleted_at: null }
    );
    const caught = await chatMod.staffMessagesAfter(db.pool, 69, 20);
    assert.strictEqual(caught.find((row) => row.id === 70).message, chatMod.PLACEHOLDER);
    assert.strictEqual(caught.find((row) => row.id === 70).deleted, true);
    assert.strictEqual(caught.find((row) => row.id === 71).message, 'sichtbar');
    assert.ok(!JSON.stringify(caught).includes('geheim-original'));
    assert.deepStrictEqual(await chatMod.staffMessagesAfter(db.pool, 0, 20), []);
    db.state.messages = db.state.messages.filter((row) => row.id !== 70 && row.id !== 71);

    const send = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'hello #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125' } });
    assert.strictEqual(send.status, 200);
    assert.ok(send.json.id);

    const spoof = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: { ...token(OTHER_UUID) },
      body: { messageId: send.json.id, reason: 'testgrund', actor: 'Admin' }
    });
    assert.strictEqual(spoof.status, 403);

    const del1 = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(MOD_UUID),
      body: { messageId: send.json.id, reason: 'Beleidigung' }
    });
    assert.strictEqual(del1.status, 200);
    assert.strictEqual(del1.json.already, false);
    const del2 = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(UUID_A),
      body: { messageId: send.json.id, reason: 'nochmal' }
    });
    assert.strictEqual(del2.json.already, true);
    const plainSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'ohne grund' } });
    const beforeRedactions = db.state.redactions.length;
    const noReason = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(MOD_UUID),
      body: { messageId: plainSend.json.id }
    });
    assert.strictEqual(noReason.status, 200);
    assert.strictEqual(noReason.json.already, false);
    const storedReason = db.state.redactions.find((row) => row.message_id === plainSend.json.id);
    assert.strictEqual(storedReason.reason_text, '');
    const noReasonAgain = await request(app, {
      method: 'POST', path: '/moderation/actions/message/delete',
      headers: token(MOD_UUID),
      body: { messageId: plainSend.json.id, reason: '' }
    });
    assert.strictEqual(noReasonAgain.json.already, true);
    assert.strictEqual(db.state.redactions.filter((row) => row.message_id === plainSend.json.id).length, 1);
    assert.strictEqual(db.state.redactions.length, beforeRedactions + 1);
    const muteWithoutReason = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '5m' }
    });
    assert.strictEqual(muteWithoutReason.status, 400);
    assert.strictEqual(muteWithoutReason.json.code, 'reason_required');
    const webDelete = await request(app, {
      method: 'POST', path: '/staff/actions/message/delete',
      headers: adminHeaders(db.sessions.mod.token),
      body: { messageId: plainSend.json.id }
    });
    assert.strictEqual(webDelete.json.already, true);
    const siteSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'website leer' } });
    const siteDelete = await request(app, {
      method: 'POST', path: '/staff/actions/message/delete',
      headers: adminHeaders(db.sessions.mod.token),
      body: { messageId: siteSend.json.id }
    });
    assert.strictEqual(siteDelete.status, 200);
    assert.strictEqual(siteDelete.json.already, false);
    assert.strictEqual(db.state.redactions.find((row) => row.message_id === siteSend.json.id).reason_text, '');
    const found = await request(app, { path: '/chat/players/search?name=pl', headers: token(OTHER_UUID) });
    assert.strictEqual(found.status, 200);
    assert.strictEqual(found.json.self_id, 44);
    assert.strictEqual(found.json.players.length, 1);
    assert.strictEqual(found.json.players[0].player, 'Plain');
    assert.strictEqual(found.json.players[0].self, true);
    const others = await request(app, { path: '/chat/players/search?name=mod', headers: token(OTHER_UUID) });
    assert.strictEqual(others.json.players.some((row) => row.player === 'ModUser' && row.self === false), true);
    assert.strictEqual(others.json.players.some((row) => row.player === 'Ghost'), false);
    const shortQuery = await request(app, { path: '/chat/players/search?name=p', headers: token(OTHER_UUID) });
    assert.deepStrictEqual(shortQuery.json.players, []);
    const publicRows = await chatMod.publicHistory(db.pool, {});
    assert.strictEqual(publicRows[0].message, chatMod.PLACEHOLDER);
    assert.strictEqual(publicRows[0].deleted, true);
    assert.strictEqual(publicRows[0].player, 'Plain');
    assert.ok(!JSON.stringify(publicRows).includes('hello'));
    assert.ok(!JSON.stringify(publicRows).includes('#ITEM:'));
    assert.ok(!JSON.stringify(publicRows).includes('#BUILD:'));
    const original = await chatMod.staffOriginal(db.pool, send.json.id);
    assert.ok(original.originalText.includes('#ITEM:ITEM-db2ac5'));
    assert.ok(original.originalText.includes('#BUILD:BUILD-709125'));
    assert.deepStrictEqual(original.shareIds.map((entry) => entry.id), ['ITEM-db2ac5', 'BUILD-709125']);
    assert.strictEqual(original.shareSnapshots.find((entry) => entry.id === 'ITEM-db2ac5').present, true);
    assert.strictEqual(original.shareSnapshots.find((entry) => entry.id === 'BUILD-709125').present, true);

    db.state.messages = db.state.messages.filter((m) => false); // 24h chat cleanup
    assert.strictEqual(db.state.redactions.length, 3);

    const mute = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'Spamtest', actor: 'Plain', actorPlayerId: 44 }
    });
    assert.strictEqual(mute.status, 200);
    assert.strictEqual(mute.json.ok, true);
    assert.strictEqual(mute.json.stored, true);
    assert.strictEqual(mute.json.liveUpdated, true);
    const muteAgain = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'Spamtest2' }
    });
    assert.strictEqual(muteAgain.json.already, true);
    const mutedSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'nochwas' } });
    assert.strictEqual(mutedSend.status, 403);
    assert.strictEqual(mutedSend.json.code, 'muted');
    const mutedHistory = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(mutedHistory.status, 200);

    const ban = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(UUID_A),
      body: { player: 'Plain', duration: 'permanent', confirm: true, reason: 'Schwere Störung' }
    });
    assert.strictEqual(ban.status, 200);
    const bannedSend = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'x' } });
    assert.strictEqual(bannedSend.json.code, 'banned');
    const bannedHistory = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(bannedHistory.status, 403);

    const unmute = await request(app, {
      method: 'POST', path: '/moderation/actions/unmute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', reason: 'Mute weg' }
    });
    assert.strictEqual(unmute.status, 200);
    const stillBanned = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'x' } });
    assert.strictEqual(stillBanned.json.code, 'banned');

    const webBanLift = await request(app, {
      method: 'POST', path: '/staff/actions/unban',
      headers: adminHeaders(db.sessions.mod.token),
      body: { player: 'Plain', reason: 'Website Aufhebung' }
    });
    assert.strictEqual(webBanLift.status, 200);
    const afterLift = await request(app, { method: 'POST', path: '/chat/send', headers: token(OTHER_UUID), body: { message: 'wieder da' } });
    assert.strictEqual(afterLift.status, 200);

    const expired = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'mute', duration: '10m', confirmPermanent: false,
      actorPlayerId: 11, source: 'minecraft', reason: 'kurz'
    });
    const row = db.state.sanctions.find((s) => s.id === expired.sanctionId);
    row.expires_at = new Date(Date.now() - 1000);
    const afterExpire = await chatMod.denyChatSend(db.pool, 44);
    assert.strictEqual(afterExpire, null);

    process.env.MODERATION_TOOLS_ENABLED = '0';
    const toolsOff = await request(app, {
      method: 'POST', path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'darf nicht' }
    });
    assert.strictEqual(toolsOff.json.code, 'tools_disabled');
    delete process.env.MODERATION_TOOLS_ENABLED;
    const publicStill = chatMod.toPublicMessage({ id: 1, message: 'secret', deleted_at: new Date() });
    assert.strictEqual(publicStill.message, chatMod.PLACEHOLDER);
    assert.strictEqual(publicStill.deleted, true);

    const custom = await request(app, {
      method: 'POST', path: '/staff/actions/mute',
      headers: adminHeaders(db.sessions.admin.token),
      body: { player: 'Plain', duration: '5m', reason: 'eigene dauer', actor: 'Plain', actorPlayerId: 44, confirm: false }
    });
    assert.strictEqual(custom.status, 200);
    assert.strictEqual(custom.json.ok, true);
    const customEvent = (await chatMod.listEvents(db.pool, 20)).find((entry) => entry.reason === 'eigene dauer');
    assert.strictEqual(customEvent.actor, 'Admin');
    assert.strictEqual(customEvent.payload.actorName, 'Admin');
    assert.strictEqual(customEvent.duration, '5m');
    assert.strictEqual(customEvent.permanent, false);
    const remaining = new Date(customEvent.expiresAt).getTime() - Date.now();
    assert.ok(remaining > 4 * 60 * 1000 && remaining < 6 * 60 * 1000);
    const noConfirm = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: 'permanent', reason: 'ohne bestaetigung', confirm: false }
    });
    assert.strictEqual(noConfirm.status, 400);
    assert.strictEqual(noConfirm.json.code, 'confirm_required');
    db.players.push({
      id: 66,
      display_name: 'Citizen',
      display_name_normalized: 'citizen',
      mc_uuid: '33333333-3333-3333-3333-333333333333'
    });
    const twoWeeks = await request(app, {
      method: 'POST', path: '/moderation/actions/ban',
      headers: token(MOD_UUID),
      body: { player: 'Citizen', duration: '14d', reason: 'zwei wochen' }
    });
    assert.strictEqual(twoWeeks.status, 200);
    assert.strictEqual(twoWeeks.json.ok, true);
    const weekEvent = (await chatMod.listEvents(db.pool, 20)).find((entry) => entry.reason === 'zwei wochen');
    assert.strictEqual(weekEvent.duration, '14d');
    assert.strictEqual(weekEvent.actor, 'ModUser');
    const weekLeft = new Date(weekEvent.expiresAt).getTime() - Date.now();
    assert.ok(weekLeft > 13 * 24 * 60 * 60 * 1000 && weekLeft < 15 * 24 * 60 * 60 * 1000);

    db.state.sanctions.push({
      id: 77, player_id: 55, kind: 'mute', lifted_at: null, expires_at: null, reason_text: 'geist'
    });
    const muteSuggest = await request(app, {
      path: '/moderation/actions/active?kind=mute',
      headers: token(MOD_UUID)
    });
    assert.strictEqual(muteSuggest.status, 200);
    const muteNames = muteSuggest.json.sanctions.map((entry) => entry.player);
    assert.ok(muteNames.includes('Plain'));
    assert.ok(!muteNames.includes('Ghost'));
    const banSuggest = await request(app, {
      path: '/moderation/actions/active?kind=ban',
      headers: token(MOD_UUID)
    });
    assert.ok(banSuggest.json.sanctions.map((entry) => entry.player).includes('Citizen'));
    assert.ok(!banSuggest.json.sanctions.map((entry) => entry.player).includes('Plain'));
    const clearCustom = await request(app, {
      method: 'POST', path: '/moderation/actions/unmute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', reason: 'Testende' }
    });
    assert.strictEqual(clearCustom.status, 200);

    db.state.seasons = [{ id: 2, is_current: 1 }, { id: 3, is_current: 1 }];
    const skipped = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(skipped.skipped, true);
    db.state.seasons = [];
    const skippedEmpty = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(skippedEmpty.skipped, true);
    db.state.seasons = [{ id: 3, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') }];
    db.state.sanctions.push({
      id: 99, player_id: 44, kind: 'ban', lifted_at: null, expires_at: null,
      created_season_id: 2, reason_text: 'alt'
    });
    const cleaned = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(cleaned.skipped, false);
    assert.ok(db.state.sanctions.some((s) => s.id === 99));
    assert.ok(db.state.leaderboard.length === 1);

    const ambiguous = await chatMod.uniqueCurrentSeason({
      async query() { return [[{ id: 1 }, { id: 2 }]]; }
    });
    assert.strictEqual(ambiguous.ok, false);

    const clock = new Date('2026-08-01T00:00:00Z').getTime();
    const classified = chatMod.classifySeasons([
      { id: 5, is_current: 1, start_at: '2026-06-01T00:00:00Z' },
      { id: 11, is_current: 0, start_at: '2025-01-01T00:00:00Z' },
      { id: 1, is_current: 0, start_at: '2027-01-01T00:00:00Z' },
      { id: 4, is_current: 0, start_at: null },
      { id: 8, is_current: 2, start_at: '2020-01-01T00:00:00Z' },
      { id: 9, is_current: null, start_at: '2020-01-01T00:00:00Z' }
    ], clock);
    assert.deepStrictEqual(classified.endedIds, [11]);
    assert.strictEqual(chatMod.classifySeasons([
      { id: 5, is_current: 1, start_at: null },
      { id: 11, is_current: 0, start_at: '2020-01-01T00:00:00Z' }
    ], clock).reason, 'current_start_unknown');

    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: 'Plain', chatBanned: false }), true);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: 'Plain', chatBanned: true }), false);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 1, player: null, chatBanned: false }), false);
    assert.strictEqual(chatMod.socketMayReceive({ readyState: 0, player: 'Plain', chatBanned: false }), false);

    const missingHistory = await request(app, { path: '/chat/messages' });
    assert.strictEqual(missingHistory.status, 401);
    assert.ok(!String(missingHistory.raw).includes('wieder da'));
    const invalidHistory = await request(app, {
      path: '/chat/messages',
      headers: { 'x-test-uuid': '00000000-0000-0000-0000-000000000000' }
    });
    assert.strictEqual(invalidHistory.status, 403);
    assert.ok(!String(invalidHistory.raw).includes('wieder da'));

    const notFound = await request(app, {
      method: 'POST',
      path: '/staff/actions/mute',
      headers: adminHeaders(db.sessions.admin.token),
      body: { player: 'Plain', duration: '10m', reason: 'ohne Fund', messageId: 9999 }
    });
    assert.strictEqual(notFound.status, 404);
    assert.strictEqual(notFound.json.code, 'not_found');
    assert.ok(!db.state.sanctions.some((entry) => entry.reason_text === 'ohne Fund'));

    const historyEvents = await chatMod.listEvents(db.pool, 100);
    const deletedEvent = historyEvents.find((entry) => entry.kind === 'message_delete' && entry.reason === 'Beleidigung');
    assert.ok(deletedEvent);
    assert.ok(historyEvents.some((entry) => entry.kind === 'message_delete' && entry.reason === ''));
    assert.ok(deletedEvent.payload.originalText.includes('hello #ITEM:ITEM-db2ac5'));
    assert.deepStrictEqual(deletedEvent.payload.shareIds.map((entry) => entry.id), ['ITEM-db2ac5', 'BUILD-709125']);
    assert.strictEqual(deletedEvent.actor, 'ModUser');
    assert.strictEqual(deletedEvent.payload.actorName, 'ModUser');
    const muteEvent = historyEvents.find((entry) => entry.kind === 'mute' && entry.reason === 'Spamtest');
    assert.strictEqual(muteEvent.actor, 'ModUser');
    assert.strictEqual(muteEvent.payload.actorName, 'ModUser');
    assert.notStrictEqual(muteEvent.payload.actorName, 'Plain');
    assert.strictEqual(muteEvent.duration, '10m');
    assert.strictEqual(muteEvent.permanent, false);
    assert.ok(muteEvent.expiresAt);
    assert.ok(muteEvent.sanctionId);
    const liftEvent = historyEvents.find((entry) => entry.kind === 'unmute' && entry.reason === 'Mute weg');
    assert.strictEqual(liftEvent.reason, 'Mute weg');
    assert.ok(liftEvent.sanctionIds.length >= 1);
    assert.strictEqual(liftEvent.payload.sanctions[0].duration, '10m');
    assert.ok(liftEvent.payload.sanctions[0].sanctionId);
    const banEvent = historyEvents.find((entry) => entry.kind === 'ban' && entry.reason === 'Schwere Störung');
    assert.strictEqual(banEvent.actor, 'Admin');
    assert.strictEqual(banEvent.payload.actorName, 'Admin');
    const unbanEvent = historyEvents.find((entry) => entry.kind === 'unban');
    assert.strictEqual(unbanEvent.actor, 'ModUser');
    assert.strictEqual(unbanEvent.payload.actorName, 'ModUser');
    assert.strictEqual(banEvent.reason, 'Schwere Störung');
    assert.strictEqual(banEvent.permanent, true);
    assert.strictEqual(banEvent.duration, 'permanent');
    assert.strictEqual(banEvent.expiresAt, null);
    const storedMute = db.state.sanctions.find((entry) => entry.id === muteEvent.sanctionId);
    storedMute.reason_text = 'GEAENDERT';
    const reread = await chatMod.listEvents(db.pool, 100);
    assert.strictEqual(reread.find((entry) => entry.id === muteEvent.id).reason, 'Spamtest');

    db.state.messages.push({
      id: 501,
      player: 'Plain',
      player_id: 44,
      message: 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125',
      timestamp: new Date('2026-08-01T12:00:00Z'),
      deleted_at: null
    });
    db.state.failEvidence = true;
    const sanctionIdsBefore = new Set(db.state.sanctions.map((entry) => entry.id));
    try {
      await assert.rejects(() => chatMod.applySanction(db.pool, {
        playerName: 'Plain', kind: 'mute', duration: '10m', confirmPermanent: false,
        actorPlayerId: 11, source: 'minecraft', reason: 'atomar', messageId: 501
      }));
    } finally {
      db.state.failEvidence = false;
    }
    assert.ok(db.state.sanctions.every((entry) => sanctionIdsBefore.has(entry.id)));
    assert.ok(!db.state.evidence.some((entry) => String(entry.snapshot_text || '').includes('boom')));

    const linked = await request(app, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'wegen Nachricht', messageId: 501 }
    });
    assert.strictEqual(linked.status, 200, JSON.stringify(linked.json));
    assert.strictEqual(linked.json.already, false);
    const linkedRow = db.state.sanctions.find((entry) => entry.reason_text === 'wegen Nachricht');
    assert.strictEqual(linkedRow.message_id, 501);
    const proof = db.state.evidence.find((entry) => entry.sanction_id === linkedRow.id);
    assert.strictEqual(proof.snapshot_text, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    const proofShares = JSON.parse(proof.share_snapshots);
    assert.strictEqual(proofShares.find((entry) => entry.id === 'ITEM-db2ac5').present, true);
    assert.strictEqual(proofShares.find((entry) => entry.id === 'BUILD-709125').present, true);
    const linkedEvent = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.reason === 'wegen Nachricht');
    assert.strictEqual(linkedEvent.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    assert.strictEqual(linkedEvent.messageId, 501);
    linkedRow.reason_text = 'SPAETER';
    db.state.messages.find((entry) => entry.id === 501).message = chatMod.PLACEHOLDER;
    const afterMutation = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.id === linkedEvent.id);
    assert.strictEqual(afterMutation.reason, 'wegen Nachricht');
    assert.strictEqual(afterMutation.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');

    const seededBan = db.state.sanctions.find((entry) => entry.id === 99);
    if (seededBan) seededBan.lifted_at = new Date();
    db.state.messages.push({
      id: 502, player: 'ModUser', player_id: 33, message: 'fremd', timestamp: new Date(), deleted_at: null
    });
    const mismatch = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '1h', confirmPermanent: false,
      actorPlayerId: 11, source: 'website', reason: 'falsche Nachricht', messageId: 502
    });
    assert.strictEqual(mismatch.code, 'message_player_mismatch');
    assert.ok(!db.state.sanctions.some((entry) => entry.reason_text === 'falsche Nachricht'));

    const evidenceBeforePlain = db.state.evidence.length;
    const withoutMessage = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '1h', confirmPermanent: false,
      actorPlayerId: 11, source: 'minecraft', reason: 'ohne Nachricht'
    });
    assert.strictEqual(withoutMessage.already, false);
    assert.strictEqual(withoutMessage.messageId, null);
    assert.strictEqual(db.state.evidence.length, evidenceBeforePlain);
    assert.strictEqual(db.state.sanctions.find((entry) => entry.id === withoutMessage.sanctionId).message_id, null);

    const bannedHistoryAgain = await request(app, { path: '/chat/messages', headers: token(OTHER_UUID) });
    assert.strictEqual(bannedHistoryAgain.status, 403);
    const staffWhileBanned = await request(app, {
      path: '/staff/messages',
      headers: adminHeaders(db.sessions.mod.token)
    });
    assert.strictEqual(staffWhileBanned.status, 200);
    const ingameStaffEvents = await request(app, { path: '/moderation/events', headers: token(MOD_UUID) });
    assert.strictEqual(ingameStaffEvents.status, 200);
    assert.ok(ingameStaffEvents.json.events.some((entry) => entry.reason === 'wegen Nachricht'));

    await chatMod.liftSanction(db.pool, {
      playerName: 'Plain', kind: 'ban', actorPlayerId: 11, source: 'minecraft', reason: 'Bann weg'
    });
    db.state.messages.find((entry) => entry.id === 501).message = 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125';
    const removed = await chatMod.deleteChatMessage(db.pool, {
      messageId: 501, actorPlayerId: 33, source: 'minecraft', reason: 'Text sichern'
    });
    assert.strictEqual(removed.placeholder, chatMod.PLACEHOLDER);
    const fromDeleted = await chatMod.applySanction(db.pool, {
      playerName: 'Plain', kind: 'ban', duration: '24h', confirmPermanent: false,
      actorPlayerId: 33, source: 'minecraft', reason: 'Beleg bleibt', messageId: 501
    });
    assert.strictEqual(fromDeleted.already, false);
    const kept = db.state.evidence.find((entry) => entry.sanction_id === fromDeleted.sanctionId);
    assert.strictEqual(kept.snapshot_text, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    assert.notStrictEqual(kept.snapshot_text, chatMod.PLACEHOLDER);
    const deleteRecord = (await chatMod.listEvents(db.pool, 100)).find((entry) => entry.reason === 'Text sichern');
    assert.strictEqual(deleteRecord.payload.originalText, 'boom #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');

    const preserved = db.state.events.length;
    db.state.seasons = [
      { id: 5, is_current: 1, start_at: null },
      { id: 11, is_current: 0, start_at: new Date('2025-01-01T00:00:00Z') }
    ];
    const unknownStart = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(unknownStart.skipped, true);
    assert.strictEqual(unknownStart.reason, 'current_start_unknown');
    assert.strictEqual(db.state.events.length, preserved);

    db.state.seasons = [
      { id: 5, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') },
      { id: 11, is_current: 1, start_at: new Date('2025-01-01T00:00:00Z') }
    ];
    const ambiguousCleanup = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(ambiguousCleanup.skipped, true);
    assert.strictEqual(db.state.events.length, preserved);

    db.state.seasons = [
      { id: 5, is_current: 1, start_at: new Date('2026-06-01T00:00:00Z') },
      { id: 11, is_current: 0, start_at: new Date('2025-01-01T00:00:00Z') },
      { id: 1, is_current: 0, start_at: new Date('2027-01-01T00:00:00Z') },
      { id: 4, is_current: 0, start_at: null },
      { id: 8, is_current: 2, start_at: new Date('2020-01-01T00:00:00Z') }
    ];
    db.state.events.push(
      { id: 1001, season_id: 11, payload: '{"reason":"alt"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1002, season_id: 1, payload: '{"reason":"spaeter"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1003, season_id: 4, payload: '{"reason":"unklar"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1004, season_id: 100, payload: '{"reason":"waise"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() },
      { id: 1008, season_id: 8, payload: '{"reason":"flagge"}', kind: 'mute', actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date() }
    );
    db.state.redactions.push(
      { message_id: 77, season_id: 11, original_text: 'alttext' },
      { message_id: 78, season_id: 1, original_text: 'spaetertext' }
    );
    db.state.evidence.push(
      { season_id: 11, snapshot_text: 'altbeleg', message_id: 77 },
      { season_id: 1, snapshot_text: 'spaeterbeleg', message_id: 78 }
    );
    db.state.sanctions.push(
      { id: 201, player_id: 44, kind: 'ban', lifted_at: null, expires_at: null, created_season_id: 11, reason_text: 'aktiv bleibt' },
      { id: 202, player_id: 44, kind: 'mute', lifted_at: new Date(), expires_at: null, created_season_id: 11, reason_text: 'aufgehoben' },
      { id: 203, player_id: 44, kind: 'mute', lifted_at: null, expires_at: new Date(Date.now() - 5000), created_season_id: 1, reason_text: 'zukunft abgelaufen' },
      { id: 204, player_id: 44, kind: 'mute', lifted_at: null, expires_at: new Date(Date.now() - 5000), created_season_id: 11, reason_text: 'alt abgelaufen' }
    );
    db.state.sqlLog.length = 0;
    const seasonCleanup = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(seasonCleanup.skipped, false);
    assert.deepStrictEqual(seasonCleanup.endedSeasonIds.map(Number), [11]);
    assert.ok(!db.state.events.some((entry) => entry.id === 1001));
    assert.ok(db.state.events.some((entry) => entry.id === 1002));
    assert.ok(db.state.events.some((entry) => entry.id === 1003));
    assert.ok(db.state.events.some((entry) => entry.id === 1004));
    assert.ok(db.state.events.some((entry) => entry.id === 1008));
    assert.ok(db.state.events.some((entry) => entry.season_id === 2));
    assert.ok(!db.state.redactions.some((entry) => entry.message_id === 77));
    assert.ok(db.state.redactions.some((entry) => entry.message_id === 78));
    assert.ok(!db.state.evidence.some((entry) => entry.snapshot_text === 'altbeleg'));
    assert.ok(db.state.evidence.some((entry) => entry.snapshot_text === 'spaeterbeleg'));
    assert.ok(db.state.sanctions.some((entry) => entry.id === 201));
    assert.ok(!db.state.sanctions.some((entry) => entry.id === 202));
    assert.ok(db.state.sanctions.some((entry) => entry.id === 203));
    assert.ok(!db.state.sanctions.some((entry) => entry.id === 204));
    assert.strictEqual(db.state.leaderboard.length, 1);
    assert.ok(db.state.sqlLog.some((sql) => sql.includes('is_current = 0') && sql.includes('start_at <')));
    assert.ok(!db.state.sqlLog.some((sql) => sql.includes('season_id <>')));

    db.state.events.push({
      id: 1005, season_id: 11, payload: '{"reason":"wieder da"}', kind: 'mute',
      actor_player_id: 11, target_player_id: 44, source: 'minecraft', created_at: new Date()
    });
    db.state.beforeSeasonDelete = () => {
      db.state.seasons.find((entry) => entry.id === 11).is_current = 1;
      db.state.seasons.find((entry) => entry.id === 5).is_current = 0;
    };
    const raced = await chatMod.cleanupEndedSeasonData(db.pool);
    assert.strictEqual(raced.skipped, false);
    assert.ok(db.state.events.some((entry) => entry.id === 1005));
    assert.strictEqual(db.state.seasons.find((entry) => entry.id === 11).is_current, 1);

    const liveDb = makeDb();
    liveDb.state.seasons = [{ id: 2, is_current: 1, start_at: new Date('2026-01-01T00:00:00Z') }];
    let liveAttempts = 0;
    const rejectingApp = createApp(liveDb, {
      onSanctionChanged: async () => {
        liveAttempts += 1;
        throw new Error('socket down');
      }
    });
    const liveMute = await request(rejectingApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'live weg' }
    });
    assert.strictEqual(liveMute.status, 200);
    assert.strictEqual(liveMute.json.ok, false);
    assert.strictEqual(liveMute.json.stored, true);
    assert.strictEqual(liveMute.json.liveUpdated, false);
    assert.strictEqual(liveMute.json.code, 'live_update_failed');
    assert.ok(String(liveMute.json.error).includes('gespeichert'));
    assert.strictEqual(liveAttempts, 1);
    assert.ok(liveDb.state.sanctions.some((entry) => entry.reason_text === 'live weg' && !entry.lifted_at));
    const stillMuted = await request(rejectingApp, {
      method: 'POST',
      path: '/chat/send',
      headers: token(OTHER_UUID),
      body: { message: 'darf nicht' }
    });
    assert.strictEqual(stillMuted.status, 403);
    assert.strictEqual(stillMuted.json.code, 'muted');
    const liveAgain = await request(rejectingApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '1h', reason: 'nochmal live' }
    });
    assert.strictEqual(liveAgain.json.already, true);
    assert.strictEqual(liveAgain.json.ok, true);
    assert.strictEqual(liveAgain.json.stored, true);
    assert.strictEqual(liveAttempts, 1);
    const liveLift = await request(rejectingApp, {
      method: 'POST',
      path: '/staff/actions/unmute',
      headers: adminHeaders(liveDb.sessions.mod.token),
      body: { player: 'Plain', reason: 'live aufheben' }
    });
    assert.strictEqual(liveLift.status, 200);
    assert.strictEqual(liveLift.json.ok, false);
    assert.strictEqual(liveLift.json.stored, true);
    assert.strictEqual(liveLift.json.liveUpdated, false);
    assert.ok(liveDb.state.sanctions.some((entry) => entry.reason_text === 'live weg' && entry.lifted_at));
    const afterLiveLift = await request(rejectingApp, {
      method: 'POST',
      path: '/chat/send',
      headers: token(OTHER_UUID),
      body: { message: 'wieder erlaubt' }
    });
    assert.strictEqual(afterLiveLift.status, 200);
    assert.strictEqual(liveAttempts, 2);

    const { WebSocketServer, WebSocket } = require('./backend-require')('ws');
    const { attachWebSocketDispatch } = require('../../../backend_server/website_backend/websocket-dispatch');
    const serverSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/server.js'), 'utf8');
    const feedSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/website_backend/staff-chat-feed.js'), 'utf8');
    const clientSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/public/staff-moderation.js'), 'utf8');
    assert.ok(serverSource.includes('noServer: true'));
    assert.ok(serverSource.includes("'/ws/chat': wss"));
    assert.ok(serverSource.includes("'/ws/staff-chat': staffFeed.wss"));
    assert.ok(!serverSource.includes("path: '/ws/chat'"));
    assert.ok(!feedSource.includes("path: '/ws/staff-chat'"));
    assert.ok(clientSource.includes("data.type === 'staff.ready'"));
    assert.ok(!/addEventListener\('open'[\s\S]{0,240}staff\.resume/.test(clientSource));

    const sockDb = makeDb();
    sockDb.state.delaySessions = 200;
    sockDb.state.delayAfterQuery = 150;
    sockDb.state.messages = [];
    for (let id = 1; id <= 103; id += 1) {
      sockDb.state.messages.push({
        id,
        player: 'Plain',
        player_id: 44,
        message: id === 3 ? 'original-drei' : 'm-' + id,
        timestamp: new Date(),
        deleted_at: id === 3 ? new Date() : null
      });
    }
    const chatWss = new WebSocketServer({ noServer: true });
    chatWss.on('connection', (ws) => {
      ws.on('message', (raw) => {
        const data = JSON.parse(String(raw));
        if (data.type === 'ping') ws.send(JSON.stringify({ type: 'pong', path: 'chat' }));
      });
    });
    const mounted = staffFeed.attachStaffChatFeed(sockDb.pool);
    const httpServer = http.createServer((req, res) => {
      res.writeHead(404);
      res.end();
    });
    attachWebSocketDispatch(httpServer, {
      '/ws/chat': chatWss,
      '/ws/staff-chat': mounted.wss
    });
    await new Promise((resolve) => httpServer.listen(0, '127.0.0.1', resolve));
    const port = httpServer.address().port;
    const sockets = [];
    function openWs(urlPath, headers) {
      return new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + urlPath, { headers });
        sockets.push(ws);
        const messages = [];
        ws.on('message', (buf) => messages.push(JSON.parse(String(buf))));
        ws.on('open', () => resolve({ ws, messages }));
        ws.on('error', reject);
      });
    }
    try {
      const chat = await openWs('/ws/chat');
      const staff = await openWs('/ws/staff-chat', { Cookie: 'cclive_web=' + sockDb.sessions.mod.token });
      staff.ws.send(JSON.stringify({ type: 'staff.resume', afterId: 3, knownIds: [1, 2, 3] }));
      const pong = await new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('chat pong timeout')), 2000);
        chat.ws.once('message', (buf) => {
          clearTimeout(timer);
          resolve(JSON.parse(String(buf)));
        });
        chat.ws.send(JSON.stringify({ type: 'ping' }));
      });
      assert.strictEqual(pong.type, 'pong');
      assert.strictEqual(pong.path, 'chat');
      setTimeout(() => {
        mounted.publish({ type: 'chat.message', id: 200, player: 'Plain', message: 'waehrend-nachladen', deleted: false }).catch(() => {});
      }, 280);
      const deadline = Date.now() + 4000;
      while (Date.now() < deadline) {
        const ids = new Set();
        for (const msg of staff.messages) {
          for (const row of msg.messages || []) ids.add(row.id);
        }
        const pushed = staff.messages.some((msg) => msg.type === 'chat.message' && msg.id === 200);
        if (staff.messages[0] && staff.messages[0].type === 'staff.ready' && ids.has(3) && ids.has(103) && pushed) break;
        await new Promise((resolve) => setTimeout(resolve, 20));
      }
      assert.strictEqual(staff.messages[0].type, 'staff.ready');
      assert.ok(staff.messages.every((msg, index) => index === 0 || msg.type !== 'staff.ready'));
      let live = [
        { id: 1, player: 'Plain', message: 'm-1', deleted: false },
        { id: 2, player: 'Plain', message: 'm-2', deleted: false },
        { id: 3, player: 'Plain', message: 'original-drei', deleted: false }
      ];
      const catchups = staff.messages.filter((msg) => msg.type === 'staff.catchup');
      assert.ok(catchups.length >= 2);
      let delivered = 0;
      for (const frame of catchups) {
        delivered += frame.messages.length;
        assert.ok(!JSON.stringify(frame).includes('original-drei'));
        live = staffFeed.mergeStaffFeed(live, frame.messages);
      }
      assert.ok(delivered > 80);
      assert.strictEqual(live.find((row) => row.id === 3).message, chatMod.PLACEHOLDER);
      assert.strictEqual(live.find((row) => row.id === 3).deleted, true);
      for (let id = 1; id <= 103; id += 1) assert.strictEqual(live.filter((row) => row.id === id).length, 1);
      const pushed = staff.messages.filter((msg) => msg.type === 'chat.message' && msg.id === 200);
      assert.strictEqual(pushed.length, 1);
      live = staffFeed.mergeStaffFeed(live, pushed);
      await mounted.publish({ type: 'chat.message', id: 50, player: 'Plain', message: 'm-50', deleted: false });
      await new Promise((resolve) => setTimeout(resolve, 30));
      const repeated = staff.messages.filter((msg) => msg.type === 'chat.message' && msg.id === 50);
      assert.strictEqual(repeated.length, 1);
      live = staffFeed.mergeStaffFeed(live, repeated);
      live = staffFeed.mergeStaffFeed(live, await chatMod.staffHistory(sockDb.pool, 80));
      live = staffFeed.mergeStaffFeed(live, repeated);
      for (let id = 1; id <= 103; id += 1) assert.strictEqual(live.filter((row) => row.id === id).length, 1);
      assert.strictEqual(live.filter((row) => row.id === 200).length, 1);
      assert.strictEqual(live.find((row) => row.id === 3).deleted, true);
      assert.ok(!JSON.stringify(live).includes('original-drei'));

      const deniedMessages = [];
      const denied = await new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/staff-chat');
        sockets.push(ws);
        const timer = setTimeout(() => reject(new Error('denied timeout')), 2000);
        ws.on('message', (buf) => deniedMessages.push(String(buf)));
        ws.on('close', (code) => { clearTimeout(timer); resolve(code); });
        ws.on('error', () => {});
      });
      assert.strictEqual(denied, 4401);
      assert.strictEqual(deniedMessages.length, 0);

      sockDb.state.roles.delete(33);
      const revokedMessages = [];
      const revoked = await new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/staff-chat', {
          headers: { Cookie: 'cclive_web=' + sockDb.sessions.mod.token }
        });
        sockets.push(ws);
        const timer = setTimeout(() => reject(new Error('revoked timeout')), 2000);
        ws.on('message', (buf) => revokedMessages.push(String(buf)));
        ws.on('close', (code) => { clearTimeout(timer); resolve(code); });
        ws.on('error', () => {});
      });
      assert.strictEqual(revoked, 4401);
      assert.strictEqual(revokedMessages.length, 0);
      assert.ok(!revokedMessages.join('').includes('m-10'));

      const unknown = await new Promise((resolve) => {
        const ws = new WebSocket('ws://127.0.0.1:' + port + '/ws/other');
        sockets.push(ws);
        const timer = setTimeout(() => resolve('timeout'), 1500);
        ws.on('open', () => { clearTimeout(timer); resolve('open'); });
        ws.on('error', (err) => { clearTimeout(timer); resolve(err.message); });
      });
      assert.ok(String(unknown).includes('404'));
    } finally {
      for (const ws of sockets) {
        try { ws.close(); } catch (err) { /* bereits zu */ }
      }
      await new Promise((resolve) => chatWss.close(resolve));
      await new Promise((resolve) => mounted.wss.close(resolve));
      await new Promise((resolve) => httpServer.close(resolve));
    }

    const teamDb = makeDb();
    teamDb.players.push({
      id: 34,
      display_name: 'ModTwo',
      display_name_normalized: 'modtwo',
      mc_uuid: '22222222-2222-2222-2222-222222222222'
    });
    teamDb.state.roles.set(34, 'moderator');
    const teamApp = createApp(teamDb);
    const mc = (uuid) => ({ 'x-test-uuid': uuid });
    const activeSanctions = (id) => teamDb.state.sanctions.filter((row) => row.player_id === id && !row.lifted_at);
    const allowedMute = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(MOD_UUID),
      body: { player: 'Plain', duration: '5m', reason: 'normaler Spieler' }
    });
    assert.strictEqual(allowedMute.status, 200);
    assert.ok(activeSanctions(44).some((row) => row.kind === 'mute'));
    const eventsBefore = teamDb.state.events.length;
    const sanctionsBefore = teamDb.state.sanctions.length;
    const againstMod = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(MOD_UUID),
      body: { player: 'ModTwo', duration: '5m', reason: 'gegen Moderator', role: 'player', actor: 'Admin' }
    });
    assert.strictEqual(againstMod.status, 403);
    assert.strictEqual(againstMod.json.code, 'team_protected');
    assert.ok(String(againstMod.json.error).includes('Teammitglieder'));
    assert.strictEqual(teamDb.state.events.length, eventsBefore);
    assert.strictEqual(teamDb.state.sanctions.length, sanctionsBefore);
    const againstAdmin = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/ban',
      headers: mc(MOD_UUID),
      body: { player: 'Admin', duration: '10m', reason: 'gegen Admin' }
    });
    assert.strictEqual(againstAdmin.status, 403);
    assert.strictEqual(againstAdmin.json.code, 'team_protected');
    assert.strictEqual(teamDb.state.events.length, eventsBefore);
    const adminOnMod = await request(teamApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: mc(UUID_A),
      body: { player: 'ModTwo', duration: '5m', reason: 'Admin darf' }
    });
    assert.strictEqual(adminOnMod.status, 200);
    assert.ok(activeSanctions(34).some((row) => row.kind === 'mute'));
    teamDb.state.messages.push(
      { id: 901, player: 'ModTwo', player_id: 34, message: 'mod-original', timestamp: new Date(), deleted_at: null },
      { id: 902, player: 'Admin', player_id: 11, message: 'admin-original', timestamp: new Date(), deleted_at: null },
      { id: 903, player: 'Plain', player_id: 44, message: 'plain-original', timestamp: new Date(), deleted_at: null }
    );
    const deleteMod = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 901, reason: 'weg damit', player: 'Plain', role: 'user' }
    });
    assert.strictEqual(deleteMod.status, 403);
    assert.strictEqual(deleteMod.json.code, 'team_protected');
    const deleteModEmpty = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 901 }
    });
    assert.strictEqual(deleteModEmpty.status, 403);
    assert.strictEqual(deleteModEmpty.json.code, 'team_protected');
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 901).deleted_at, null);
    assert.strictEqual(teamDb.state.redactions.length, 0);
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 901).message, 'mod-original');
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 901).deleted_at, null);
    assert.strictEqual(teamDb.state.redactions.length, 0);
    const deleteAdminMsg = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 902, reason: 'admin weg' }
    });
    assert.strictEqual(deleteAdminMsg.status, 403);
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 902).message, 'admin-original');
    const deletePlain = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(teamDb.sessions.mod.token),
      body: { messageId: 903, reason: 'plain weg' }
    });
    assert.strictEqual(deletePlain.status, 200);
    assert.strictEqual(teamDb.state.messages.find((row) => row.id === 903).message, chatMod.PLACEHOLDER);
    const websiteAdmin = await request(teamApp, {
      method: 'POST',
      path: '/staff/actions/ban',
      headers: adminHeaders(teamDb.sessions.admin.token),
      body: { player: 'ModUser', duration: '10m', reason: 'Website-Admin' }
    });
    assert.strictEqual(websiteAdmin.status, 200);
    assert.ok(activeSanctions(33).some((row) => row.kind === 'ban'));

    const MOD2 = '22222222-2222-2222-2222-222222222222';
    const reportDb = makeDb();
    reportDb.players.push({
      id: 34, display_name: 'ModTwo', display_name_normalized: 'modtwo', mc_uuid: MOD2
    });
    reportDb.state.roles.set(34, 'moderator');
    const reportApp = createApp(reportDb);
    const reportToken = (uuid) => ({ 'x-test-uuid': uuid });
    const sentIds = [];
    for (let i = 1; i <= 12; i++) {
      const sent = await request(reportApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(OTHER_UUID),
        body: { message: 'vor-' + i }
      });
      assert.strictEqual(sent.status, 200);
      sentIds.push(sent.json.id);
    }
    const oldestId = sentIds[0];
    const created = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'Plain', messageId: oldestId, reason: 'Beleidigung' }
    });
    assert.strictEqual(created.status, 200);
    assert.strictEqual(created.json.duplicate, false);
    assert.strictEqual(created.json.message, 'Meldung gesendet.');
    const reportId = created.json.id;
    const opened = await request(reportApp, {
      method: 'GET',
      path: '/staff/reports/' + reportId,
      headers: adminHeaders(reportDb.sessions.mod.token)
    });
    assert.strictEqual(opened.status, 200);
    const beforeIds = opened.json.report.before.map((row) => row.message_id);
    assert.strictEqual(beforeIds.length, 10);
    assert.strictEqual(beforeIds.filter((id) => id === oldestId).length, 1);
    assert.ok(beforeIds.includes(oldestId));
    assert.ok(!beforeIds.includes(sentIds[1]));
    assert.ok(!beforeIds.includes(sentIds[2]));
    assert.strictEqual(opened.json.report.before.find((row) => row.message_id === oldestId).body, 'vor-1');
    const duplicate = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'Plain', reason: 'anderer Grund' }
    });
    assert.strictEqual(duplicate.status, 200);
    assert.strictEqual(duplicate.json.duplicate, true);
    assert.strictEqual(duplicate.json.id, reportId);
    assert.strictEqual(reportDb.state.reportWatches.length, 1);
    assert.strictEqual(reportDb.state.reportEvidence.filter((row) => row.phase === 'before').length, 10);
    assert.ok(reportDb.state.events.some((row) => row.kind === 'report_duplicate'));
    const selfReport = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(OTHER_UUID),
      body: { player: 'Plain', reason: 'ich' }
    });
    assert.strictEqual(selfReport.status, 400);
    assert.strictEqual(selfReport.json.code, 'self');
    const unknownReport = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(OTHER_UUID),
      body: { player: 'Niemand' }
    });
    assert.strictEqual(unknownReport.status, 400);
    assert.strictEqual(unknownReport.json.code, 'unknown');
    const wrongMessage = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(OTHER_UUID),
      body: { player: 'ModUser', messageId: oldestId, reason: 'falsche zeile' }
    });
    assert.strictEqual(wrongMessage.status, 400);
    assert.strictEqual(wrongMessage.json.code, 'message_mismatch');
    assert.strictEqual(reportDb.state.reports.length, 1);

    reportDb.state.claimDelay = 30;
    const [claimA, claimB] = await Promise.all([
      request(reportApp, {
        method: 'POST',
        path: '/staff/reports/' + reportId + '/claim',
        headers: adminHeaders(reportDb.sessions.admin.token),
        body: {}
      }),
      request(reportApp, {
        method: 'POST',
        path: '/staff/reports/' + reportId + '/claim',
        headers: adminHeaders(reportDb.sessions.mod.token),
        body: {}
      })
    ]);
    const claimStatuses = [claimA.status, claimB.status].sort();
    assert.deepStrictEqual(claimStatuses, [200, 409]);
    assert.strictEqual(reportDb.state.reports[0].status, 'in_progress');
    assert.ok(reportDb.state.reports[0].owner_player_id === 11 || reportDb.state.reports[0].owner_player_id === 33);
    reportDb.state.claimDelay = 0;

    const foreignClose = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports/' + reportId + '/close',
      headers: reportToken(MOD2),
      body: { outcome: 'no_violation' }
    });
    assert.strictEqual(foreignClose.status, 403);
    assert.strictEqual(reportDb.state.reports[0].status, 'in_progress');

    const ownerId = reportDb.state.reports[0].owner_player_id;
    const ownerSession = ownerId === 11 ? reportDb.sessions.admin : reportDb.sessions.mod;
    const helperSession = ownerId === 11 ? reportDb.sessions.mod : reportDb.sessions.admin;
    const help = await request(reportApp, {
      method: 'POST',
      path: '/staff/reports/' + reportId + '/help',
      headers: adminHeaders(ownerSession.token),
      body: {}
    });
    assert.strictEqual(help.status, 200);
    const joined = await request(reportApp, {
      method: 'POST',
      path: '/staff/reports/' + reportId + '/join',
      headers: adminHeaders(helperSession.token),
      body: {}
    });
    assert.strictEqual(joined.status, 200);
    const watcherClose = await request(reportApp, {
      method: 'POST',
      path: '/staff/reports/' + reportId + '/close',
      headers: adminHeaders(helperSession.token),
      body: { outcome: 'no_violation' }
    });
    assert.strictEqual(watcherClose.status, 200);
    assert.strictEqual(reportDb.state.reports[0].outcome, 'no_violation');
    assert.strictEqual(reportDb.state.reports[0].confirmed, 0);
    assert.strictEqual(reportDb.state.reports[0].status, 'closed');
    const inboxAfterClose = await request(reportApp, {
      method: 'GET',
      path: '/staff/reports',
      headers: adminHeaders(reportDb.sessions.admin.token)
    });
    assert.ok(!inboxAfterClose.json.reports.some((row) => row.id === reportId));
    assert.strictEqual(inboxAfterClose.json.openCount, inboxAfterClose.json.reports.length);

    const violation = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(MOD_UUID),
      body: { player: 'Plain', reason: 'zweiter' }
    });
    assert.strictEqual(violation.status, 200);
    const violationId = violation.json.id;
    const adminClose = await request(reportApp, {
      method: 'POST',
      path: '/staff/reports/' + violationId + '/close',
      headers: adminHeaders(reportDb.sessions.admin.token),
      body: { outcome: 'confirmed_violation' }
    });
    assert.strictEqual(adminClose.status, 200);
    const violationRow = reportDb.state.reports.find((row) => row.id === violationId);
    assert.strictEqual(violationRow.outcome, 'confirmed_violation');
    assert.strictEqual(violationRow.confirmed, 1);
    assert.strictEqual(reportDb.state.evidence.filter((row) => row.action).length >= 0, true);

    const watched = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'Plain', reason: 'nachlauf' }
    });
    const watchedId = watched.json.id;
    for (let i = 1; i <= 11; i++) {
      const sent = await request(reportApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(OTHER_UUID),
        body: { message: 'danach-' + i }
      });
      assert.strictEqual(sent.status, 200);
    }
    const afterRows = reportDb.state.reportEvidence.filter((row) => row.report_id === watchedId && row.phase === 'after');
    assert.strictEqual(afterRows.length, 10);
    assert.ok(!afterRows.some((row) => row.body === 'danach-11'));
    assert.strictEqual(reportDb.state.reportWatches.find((row) => row.report_id === watchedId).ended_reason, 'limit');

    const timed = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'ModUser', reason: 'zeit' }
    });
    const timedId = timed.json.id;
    const timedWatch = reportDb.state.reportWatches.find((row) => row.report_id === timedId);
    timedWatch.ends_at = new Date(Date.now() - 1000);
    const late = await request(reportApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(MOD_UUID),
      body: { message: 'zu spaet' }
    });
    assert.strictEqual(late.status, 200);
    assert.strictEqual(reportDb.state.reportEvidence.filter((row) => row.report_id === timedId && row.phase === 'after').length, 0);
    assert.strictEqual(timedWatch.ended_reason, 'timeout');
    const swept = await chatMod.chatReports.sweepReportWatches(reportDb.pool, Date.now());
    assert.strictEqual(swept, 0);
    assert.strictEqual(timedWatch.ended_reason, 'timeout');

    const offline = await request(reportApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'ModTwo', reason: 'weg' }
    });
    const offlineId = offline.json.id;
    const endedOffline = await chatMod.chatReports.endWatchesForPlayers(reportDb.pool, [34], 'offline');
    assert.strictEqual(endedOffline, 1);
    const relog = await request(reportApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(MOD2),
      body: { message: 'wieder da' }
    });
    assert.strictEqual(relog.status, 200);
    const offlineWatch = reportDb.state.reportWatches.find((row) => row.report_id === offlineId);
    assert.strictEqual(offlineWatch.ended_reason, 'offline');
    assert.strictEqual(reportDb.state.reportEvidence.filter((row) => row.report_id === offlineId && row.phase === 'after').length, 0);

    const snapshotBody = reportDb.state.reportEvidence.find((row) => row.report_id === reportId && row.message_id === oldestId);
    reportDb.state.messages = reportDb.state.messages.filter((row) => row.id !== oldestId);
    assert.strictEqual(snapshotBody.body, 'vor-1');
    assert.ok(!reportDb.state.messages.some((row) => row.id === oldestId));
    const closePath = '/staff/reports/' + watchedId + '/close';
    const closeHeaders = adminHeaders(reportDb.sessions.admin.token);
    const invalid = await request(reportApp, {
      method: 'POST', path: closePath, headers: closeHeaders,
      body: { outcome: 'confirmed_violation', sanction: { kind: 'mute', duration: 'invalid', reason: 'Test' } }
    });
    assert.strictEqual(invalid.status, 400);
    const watchedReport = reportDb.state.reports.find((row) => row.id === watchedId);
    assert.notStrictEqual(watchedReport.status, 'closed');
    const sanctioned = await request(reportApp, {
      method: 'POST', path: closePath, headers: closeHeaders,
      body: { outcome: 'confirmed_violation', sanction: { kind: 'mute', duration: '5m', reason: 'Test' } }
    });
    assert.strictEqual(sanctioned.status, 200);
    assert.strictEqual(watchedReport.status, 'closed');
    assert.strictEqual(sanctioned.json.sanction.kind, 'mute');
    assert.ok(sanctioned.json.sanction.sanctionId);
    const archived = await request(reportApp, {
      method: 'GET', path: '/staff/reports/' + watchedId, headers: closeHeaders
    });
    assert.strictEqual(archived.status, 200);
    assert.strictEqual(archived.json.report.sanctionKind, 'mute');

    const seasonDb = makeDb();
    seasonDb.state.seasons = [{ id: 2, is_current: 1, start_at: new Date('2026-01-01T00:00:00Z') }];
    const seasonApp = createApp(seasonDb);
    const seasonPlain = await request(seasonApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(OTHER_UUID),
      body: { message: 'season-plain' }
    });
    assert.strictEqual(seasonPlain.status, 200);
    const endedReport = await request(seasonApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'Plain', reason: 'alte season' }
    });
    assert.strictEqual(endedReport.status, 200);
    seasonDb.state.seasons = [
      { id: 9, is_current: 1, start_at: new Date('2026-08-01T00:00:00Z') },
      { id: 2, is_current: 0, start_at: new Date('2026-01-01T00:00:00Z') }
    ];
    const seasonMod = await request(seasonApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(MOD_UUID),
      body: { message: 'season-mod' }
    });
    assert.strictEqual(seasonMod.status, 200);
    const keptReport = await request(seasonApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'ModUser', reason: 'aktuelle season' }
    });
    assert.strictEqual(keptReport.status, 200);
    seasonDb.state.seasons.push({ id: 3, is_current: 1, start_at: new Date('2026-09-01T00:00:00Z') });
    const skippedSeason = await chatMod.cleanupEndedSeasonData(seasonDb.pool);
    assert.strictEqual(skippedSeason.skipped, true);
    assert.strictEqual(seasonDb.state.reports.find((row) => row.id === endedReport.json.id).status, 'open');
    assert.ok(seasonDb.state.reportEvidence.some((row) => row.report_id === endedReport.json.id));
    seasonDb.state.seasons = seasonDb.state.seasons.filter((row) => row.id !== 3);
    const reportSeasonCleanup = await chatMod.cleanupEndedSeasonData(seasonDb.pool);
    assert.strictEqual(reportSeasonCleanup.skipped, false);
    const closedSeason = seasonDb.state.reports.find((row) => row.id === endedReport.json.id);
    assert.strictEqual(closedSeason.status, 'closed');
    assert.strictEqual(closedSeason.outcome, 'season_ended');
    assert.strictEqual(closedSeason.confirmed, null);
    assert.ok(!seasonDb.state.reportEvidence.some((row) => row.report_id === endedReport.json.id));
    assert.ok(!seasonDb.state.reportWatches.some((row) => row.report_id === endedReport.json.id));
    const currentSeason = seasonDb.state.reports.find((row) => row.id === keptReport.json.id);
    assert.strictEqual(currentSeason.status, 'open');
    assert.ok(seasonDb.state.reportEvidence.some((row) => row.report_id === keptReport.json.id));
    assert.ok(seasonDb.state.reportWatches.some((row) => row.report_id === keptReport.json.id));

    const parallelDb = makeDb();
    const parallelApp = createApp(parallelDb);
    parallelDb.state.sqlLog.length = 0;
    parallelDb.state.reportCreateDelay = 80;
    const [parallelA, parallelB] = await Promise.all([
      request(parallelApp, {
        method: 'POST',
        path: '/moderation/reports',
        headers: reportToken(UUID_A),
        body: { player: 'Plain', reason: 'gleichzeitig eins' }
      }),
      request(parallelApp, {
        method: 'POST',
        path: '/moderation/reports',
        headers: reportToken(UUID_A),
        body: { player: 'Plain', reason: 'gleichzeitig zwei' }
      })
    ]);
    parallelDb.state.reportCreateDelay = 0;
    assert.strictEqual(parallelA.status, 200);
    assert.strictEqual(parallelB.status, 200);
    assert.deepStrictEqual([parallelA.json.duplicate, parallelB.json.duplicate].sort(), [false, true]);
    assert.strictEqual(parallelA.json.id, parallelB.json.id);
    assert.strictEqual(parallelDb.state.reports.length, 1);
    assert.strictEqual(parallelDb.state.reportWatches.length, 1);
    assert.strictEqual(parallelDb.state.events.filter((row) => row.kind === 'report_duplicate').length, 1);
    assert.ok(parallelDb.state.reportLockOverlaps >= 1);
    assert.ok(parallelDb.state.sqlLog.some((sql) => sql.includes('GET_LOCK')));
    assert.ok(!parallelDb.state.sqlLog.some((sql) => /FROM chat_messages[\s\S]*FOR UPDATE/.test(sql)));
    const parallelId = parallelA.json.id;
    const parallelWatch = parallelDb.state.reportWatches.find((row) => row.report_id === parallelId);
    for (let i = 1; i <= 9; i++) {
      parallelDb.state.reportEvidence.push({
        report_id: parallelId,
        season_id: parallelWatch.season_id,
        message_id: null,
        phase: 'after',
        player: 'Plain',
        body: 'beleg-' + i,
        message_at: new Date(),
        position: i
      });
    }
    parallelWatch.after_count = 9;
    parallelDb.state.watchLockOverlaps = 0;
    parallelDb.state.watchDelay = 80;
    const [tenthA, tenthB] = await Promise.all([
      request(parallelApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(OTHER_UUID),
        body: { message: 'parallel-a' }
      }),
      request(parallelApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(OTHER_UUID),
        body: { message: 'parallel-b' }
      })
    ]);
    parallelDb.state.watchDelay = 0;
    assert.strictEqual(tenthA.status, 200);
    assert.strictEqual(tenthB.status, 200);
    const tenthRows = parallelDb.state.reportEvidence.filter((row) => row.report_id === parallelId && row.phase === 'after');
    assert.strictEqual(tenthRows.length, 10);
    const captured = tenthRows.filter((row) => row.body === 'parallel-a' || row.body === 'parallel-b');
    assert.strictEqual(captured.length, 1);
    assert.strictEqual(captured[0].position, 10);
    assert.strictEqual(parallelWatch.after_count, 10);
    assert.strictEqual(parallelWatch.ended_reason, 'limit');
    assert.ok(parallelDb.state.watchLockOverlaps >= 1);
    const afterLimit = await request(parallelApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(OTHER_UUID),
      body: { message: 'nach dem limit' }
    });
    assert.strictEqual(afterLimit.status, 200);
    assert.strictEqual(parallelDb.state.reportEvidence.filter((row) => row.report_id === parallelId && row.phase === 'after').length, 10);
    assert.ok(!parallelDb.state.reportEvidence.some((row) => row.body === 'nach dem limit'));

    const timedRace = await request(parallelApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'ModUser', reason: 'zeit parallel' }
    });
    const timedRaceWatch = parallelDb.state.reportWatches.find((row) => row.report_id === timedRace.json.id);
    timedRaceWatch.ends_at = new Date(Date.now() - 1000);
    parallelDb.state.watchDelay = 80;
    const [lateA, lateB] = await Promise.all([
      request(parallelApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(MOD_UUID),
        body: { message: 'zeit-a' }
      }),
      request(parallelApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(MOD_UUID),
        body: { message: 'zeit-b' }
      })
    ]);
    parallelDb.state.watchDelay = 0;
    assert.strictEqual(lateA.status, 200);
    assert.strictEqual(lateB.status, 200);
    assert.strictEqual(parallelDb.state.reportEvidence.filter((row) => row.report_id === timedRace.json.id && row.phase === 'after').length, 0);
    assert.strictEqual(timedRaceWatch.ended_reason, 'timeout');

    parallelDb.players.push({
      id: 34, display_name: 'ModTwo', display_name_normalized: 'modtwo', mc_uuid: MOD2
    });
    const offlineRace = await request(parallelApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: reportToken(UUID_A),
      body: { player: 'ModTwo', reason: 'offline parallel' }
    });
    const offlineRaceId = offlineRace.json.id;
    parallelDb.state.watchDelay = 80;
    const offlineBefore = parallelDb.state.reportEvidence.filter((row) => row.report_id === offlineRaceId && row.phase === 'after').length;
    await Promise.all([
      chatMod.chatReports.endWatchesForPlayers(parallelDb.pool, [34], 'offline'),
      request(parallelApp, {
        method: 'POST',
        path: '/chat/send',
        headers: reportToken(MOD2),
        body: { message: 'noch online' }
      })
    ]);
    parallelDb.state.watchDelay = 0;
    const offlineMid = parallelDb.state.reportEvidence.filter((row) => row.report_id === offlineRaceId && row.phase === 'after').length;
    assert.ok(offlineMid === offlineBefore || offlineMid === offlineBefore + 1);
    assert.strictEqual(parallelDb.state.reportWatches.find((row) => row.report_id === offlineRaceId).ended_reason, 'offline');
    const offlineAgain = await request(parallelApp, {
      method: 'POST',
      path: '/chat/send',
      headers: reportToken(MOD2),
      body: { message: 'wieder da parallel' }
    });
    assert.strictEqual(offlineAgain.status, 200);
    assert.strictEqual(parallelDb.state.reportEvidence.filter((row) => row.report_id === offlineRaceId && row.phase === 'after').length, offlineMid);
    assert.ok(!parallelDb.state.reportEvidence.some((row) => row.body === 'wieder da parallel'));

    assert.ok(home.includes('Spielerakten'));
    assert.ok(home.includes('Moderatorübersicht'));
    assert.ok(home.includes('id="file-search"'));
    assert.ok(home.includes('id="protocol-body"'));
    assert.ok(home.includes('Laufende Aktionen'));
    assert.ok(workspace.includes('Spielerakten'));
    assert.ok(workspace.includes('Moderatorübersicht'));
    assert.ok(workspace.includes('Laufende Bestrafungen'));
    assert.ok(adminHtml.includes('value="files">Spielerakten'));
    assert.ok(adminHtml.includes('value="stats">Moderatorübersicht'));
    assert.ok(home.includes('id="file-player-suggest"'));
    assert.ok(home.includes('id="mod-stats-tiles"'));
    assert.ok(home.includes('>Filtern</button>'));
    assert.ok(home.includes('data-filter-clear'));
    assert.ok(home.includes('>Leeren</button>'));
    assert.ok(home.includes('>Suchen</button>'));
    assert.ok(workspace.includes('data-filter-clear'));
    assert.ok(workspace.includes('>Leeren</button>'));
    assert.ok(staffJs.includes('cancelSuggest'));
    assert.ok(staffJs.includes('ticket !== fileViewTicket'));
    assert.ok(staffJs.includes('ticket !== protocolTicket'));
    assert.ok(styleCss.includes('max-width:min(1200px,100%)'));
    assert.ok(styleCss.includes('.file-search-actions'));
    assert.ok(styleCss.includes('.filter-clear'));
    assert.ok(workspace.includes('id="file-player-suggest"'));
    assert.ok(workspace.includes('id="mod-stats-tiles"'));
    assert.ok(staffJs.includes('className = \'file-tile\''));
    assert.ok(staffJs.includes('Alle Akten'));
    assert.ok(staffJs.includes('if (ticket !== seq) return'));
    assert.ok(styleCss.includes('.file-tile-grid'));
    assert.ok(staffJs.includes('Belege nicht mehr verfügbar'));
    assert.ok(staffJs.includes('Abschluss durch unbekannt'));
    assert.ok(staffJs.includes('Unterstützen'));
    assert.ok(styleCss.includes('.file-search'));
    assert.ok(styleCss.includes('chatmod-log-scroll'));

    const fileDb = makeDb();
    const fileApp = createApp(fileDb);
    const fileToken = (uuid) => ({ 'x-test-uuid': uuid });
    const posted = await request(fileApp, {
      method: 'POST',
      path: '/chat/send',
      headers: fileToken(OTHER_UUID),
      body: { message: 'gelöschte zeile' }
    });
    assert.strictEqual(posted.status, 200);
    const confirmed = await request(fileApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: fileToken(UUID_A),
      body: { player: 'Plain', messageId: posted.json.id, reason: 'Beleidigung' }
    });
    assert.strictEqual(confirmed.status, 200);
    const confirmedId = confirmed.json.id;
    const claimed = await request(fileApp, {
      method: 'POST',
      path: '/staff/reports/' + confirmedId + '/claim',
      headers: adminHeaders(fileDb.sessions.mod.token),
      body: {}
    });
    assert.strictEqual(claimed.status, 200);
    const closedViolation = await request(fileApp, {
      method: 'POST',
      path: '/staff/reports/' + confirmedId + '/close',
      headers: adminHeaders(fileDb.sessions.admin.token),
      body: { outcome: 'confirmed_violation' }
    });
    assert.strictEqual(closedViolation.status, 200);
    const storedViolation = fileDb.state.reports.find((row) => row.id === confirmedId);
    assert.strictEqual(storedViolation.closed_by_player_id, 11);
    assert.strictEqual(storedViolation.owner_player_id, 33);
    const cleared = await request(fileApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: fileToken(UUID_A),
      body: { player: 'ModUser', reason: 'unklar' }
    });
    assert.strictEqual(cleared.status, 200);
    const clearedId = cleared.json.id;
    const closedClear = await request(fileApp, {
      method: 'POST',
      path: '/staff/reports/' + clearedId + '/close',
      headers: adminHeaders(fileDb.sessions.admin.token),
      body: { outcome: 'no_violation' }
    });
    assert.strictEqual(closedClear.status, 200);
    const stillOpen = await request(fileApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: fileToken(UUID_A),
      body: { player: 'ModUser', reason: 'noch offen' }
    });
    assert.strictEqual(stillOpen.status, 200);
    assert.notStrictEqual(stillOpen.json.id, clearedId);
    fileDb.state.reports.push({
      id: 880,
      season_id: 2,
      target_player_id: 44,
      reporter_player_id: 11,
      message_id: null,
      reason_text: 'Season vorbei',
      status: 'closed',
      owner_player_id: null,
      help_requested: 0,
      confirmed: null,
      outcome: 'season_ended',
      source: 'website',
      created_at: new Date(),
      closed_at: new Date(),
      closed_by_player_id: null
    });
    const deletedLine = await request(fileApp, {
      method: 'POST',
      path: '/staff/actions/message/delete',
      headers: adminHeaders(fileDb.sessions.admin.token),
      body: { messageId: posted.json.id, reason: 'Aktenzeile' }
    });
    assert.strictEqual(deletedLine.status, 200);
    const plainFile = await request(fileApp, {
      path: '/staff/files?player=Plain',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(plainFile.status, 200);
    const confirmedEntry = plainFile.json.violations.find((item) => item.kind === 'report' && item.report.id === confirmedId);
    assert.ok(confirmedEntry);
    assert.strictEqual(confirmedEntry.report.closedBy, 'Admin');
    assert.strictEqual(confirmedEntry.report.owner, 'ModUser');
    assert.strictEqual(confirmedEntry.report.ownerPlayerId, 33);
    assert.strictEqual(confirmedEntry.evidenceMissing, false);
    assert.ok(plainFile.json.violations.some((item) => item.kind === 'direct' && item.snapshot === 'gelöschte zeile' && item.action === 'message_delete'));
    assert.ok(!plainFile.json.violations.some((item) => item.kind === 'report' && item.report.outcome !== 'confirmed_violation'));
    assert.ok(!plainFile.json.violations.some((item) => item.report && item.report.id === 880));
    const modTargetFile = await request(fileApp, {
      path: '/staff/files?player=ModUser',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(!modTargetFile.json.violations.some((item) => item.report && (item.report.outcome === 'no_violation' || item.report.outcome === 'season_ended')));
    const adminFile = await request(fileApp, {
      path: '/staff/files?player=Admin',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(adminFile.json.counts.submitted, 3);
    assert.strictEqual(adminFile.json.counts.confirmed, 1);
    assert.strictEqual(adminFile.json.counts.open, 1);
    assert.strictEqual(adminFile.json.counts.unconfirmed, 1);
    assert.ok(!adminFile.json.submitted.some((row) => row.outcome === 'season_ended' || row.id === 880));
    const filteredSubmitted = await request(fileApp, {
      path: '/staff/files?player=Admin&reason=Beleidigung',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(filteredSubmitted.json.counts.submitted, 3);
    assert.strictEqual(filteredSubmitted.json.submitted.length, 1);
    fileDb.state.events.push(
      {
        id: fileDb.state.events.length + 1,
        actor_player_id: 33,
        target_player_id: 44,
        source: 'website',
        kind: 'report_claimed',
        payload: { reportId: 77 },
        season_id: 2,
        created_at: new Date()
      },
      {
        id: fileDb.state.events.length + 2,
        actor_player_id: 33,
        target_player_id: 44,
        source: 'website',
        kind: 'report_join',
        payload: { reportId: 77 },
        season_id: 2,
        created_at: new Date()
      },
      {
        id: fileDb.state.events.length + 3,
        actor_player_id: 33,
        target_player_id: 44,
        source: 'website',
        kind: 'report_closed',
        payload: { reportId: 77, outcome: 'confirmed_violation', confirmed: 1 },
        season_id: 2,
        created_at: new Date()
      }
    );
    const stats = await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(stats.status, 200);
    const modStats = stats.json.moderators.find((row) => row.playerId === 33);
    const adminStats = stats.json.moderators.find((row) => row.playerId === 11);
    assert.strictEqual(modStats.handledReports, 2);
    assert.strictEqual(modStats.confirmedReports, 1);
    assert.strictEqual(adminStats.confirmedReports, 1);
    assert.ok(adminStats.actions >= 1);
    fileDb.state.reportEvidence = fileDb.state.reportEvidence.filter((row) => row.report_id !== confirmedId);
    fileDb.state.events = fileDb.state.events.filter((row) => {
      if (row.kind !== 'report_closed') return true;
      const payload = typeof row.payload === 'string' ? JSON.parse(row.payload) : row.payload;
      return Number(payload.reportId) !== Number(confirmedId);
    });
    const afterPurge = await request(fileApp, {
      path: '/staff/files?player=Plain',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    const keptViolation = afterPurge.json.violations.find((item) => item.kind === 'report' && item.report.id === confirmedId);
    assert.ok(keptViolation);
    assert.strictEqual(keptViolation.evidenceMissing, true);
    assert.strictEqual(keptViolation.report.closedBy, 'Admin');
    assert.strictEqual(keptViolation.report.owner, 'ModUser');
    assert.strictEqual(keptViolation.report.closeLabel, null);
    const statsAfter = await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(statsAfter.json.moderators.find((row) => row.playerId === 11).confirmedReports, 0);
    fileDb.state.reports.push({
      id: 901,
      season_id: 2,
      target_player_id: 44,
      reporter_player_id: 11,
      message_id: null,
      reason_text: 'alt',
      status: 'closed',
      owner_player_id: 33,
      help_requested: 0,
      confirmed: 1,
      outcome: 'confirmed_violation',
      source: 'website',
      created_at: new Date(),
      closed_at: new Date(),
      closed_by_player_id: null
    });
    const unknownFile = await request(fileApp, {
      path: '/staff/files?player=Plain',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    const unknown = unknownFile.json.violations.find((item) => item.kind === 'report' && item.report.id === 901);
    assert.ok(unknown);
    assert.strictEqual(unknown.report.closeLabel, 'Abschluss durch unbekannt');
    assert.strictEqual(unknown.report.owner, 'ModUser');
    assert.strictEqual(unknown.report.closedBy, null);
    const closedList = await request(fileApp, {
      path: '/staff/reports/closed',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(closedList.json.reports.some((row) => row.outcome === 'confirmed_violation'));
    assert.ok(closedList.json.reports.some((row) => row.outcome === 'no_violation'));
    assert.ok(closedList.json.reports.some((row) => row.outcome === 'season_ended' && row.closeLabel === 'Systemabschluss'));
    const onlyClear = await request(fileApp, {
      path: '/staff/reports/closed?outcome=no_violation',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(onlyClear.json.reports.length >= 1);
    assert.ok(onlyClear.json.reports.every((row) => row.outcome === 'no_violation'));
    const closedRead = await request(fileApp, {
      path: '/staff/reports/' + confirmedId,
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(closedRead.status, 200);
    assert.strictEqual(closedRead.json.report.canClaim, false);
    assert.strictEqual(closedRead.json.report.canClose, false);
    assert.strictEqual(closedRead.json.report.canRelease, false);
    assert.strictEqual(closedRead.json.report.canJoin, false);
    assert.strictEqual(closedRead.json.report.closedBy, 'Admin');
    const inbox = await request(fileApp, {
      path: '/staff/reports',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(!inbox.json.reports.some((row) => row.id === confirmedId || row.id === 880 || row.id === 901));
    assert.ok(inbox.json.reports.some((row) => row.id === stillOpen.json.id));
    const byReason = await request(fileApp, {
      path: '/staff/events?reason=Aktenzeile&limit=50',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(byReason.json.events.some((row) => row.kind === 'message_delete'));
    const noHit = await request(fileApp, {
      path: '/staff/events?player=Niemand&limit=50',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(noHit.json.events.length, 0);
    for (let i = 0; i < 101; i++) {
      fileDb.state.reports.push({
        id: 3000 + i,
        season_id: 2,
        target_player_id: 44,
        reporter_player_id: 11,
        message_id: null,
        reason_text: 'sammel ' + i,
        status: 'open',
        owner_player_id: null,
        help_requested: 0,
        confirmed: null,
        outcome: null,
        source: 'website',
        created_at: new Date(),
        closed_at: null,
        closed_by_player_id: null
      });
    }
    fileDb.state.reports.push({
      id: 3200,
      season_id: 2,
      target_player_id: 44,
      reporter_player_id: 11,
      message_id: null,
      reason_text: 'Season sammel',
      status: 'closed',
      owner_player_id: null,
      help_requested: 0,
      confirmed: null,
      outcome: 'season_ended',
      source: 'website',
      created_at: new Date(),
      closed_at: new Date(),
      closed_by_player_id: null
    });
    const manyReports = await request(fileApp, {
      path: '/staff/files?player=Admin',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(manyReports.status, 200);
    assert.strictEqual(manyReports.json.counts.submitted, 105);
    assert.strictEqual(manyReports.json.counts.confirmed, 2);
    assert.strictEqual(manyReports.json.counts.open, 102);
    assert.strictEqual(manyReports.json.counts.unconfirmed, 1);
    assert.strictEqual(manyReports.json.submitted.length, 100);
    assert.ok(!manyReports.json.submitted.some((row) => row.outcome === 'season_ended'));
    const beforeOutside = await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    const modBefore = beforeOutside.json.moderators.find((row) => row.playerId === 33);
    fileDb.state.events.push({
      id: 0,
      actor_player_id: 33,
      target_player_id: 44,
      source: 'website',
      kind: 'mute',
      payload: { reason: 'ausserhalb' },
      season_id: 2,
      created_at: new Date(0)
    });
    const highestEvent = fileDb.state.events.reduce((max, row) => Math.max(max, Number(row.id) || 0), 0);
    for (let i = 1; i <= 5000; i++) {
      fileDb.state.events.push({
        id: highestEvent + i,
        actor_player_id: 11,
        target_player_id: 44,
        source: 'website',
        kind: 'report_opened',
        payload: { reportId: 10000 + i },
        season_id: 2,
        created_at: new Date()
      });
    }
    const outsideStats = await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    const modOutside = outsideStats.json.moderators.find((row) => row.playerId === 33);
    assert.strictEqual(modOutside.actions, modBefore.actions + 1);
    assert.strictEqual(modOutside.handledReports, modBefore.handledReports);
    assert.strictEqual(modOutside.confirmedReports, modBefore.confirmedReports);
    const protocolWindow = await request(fileApp, {
      path: '/staff/events?limit=50',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(!protocolWindow.json.events.some((row) => row.reason === 'ausserhalb'));
    for (const row of fileDb.state.reports) {
      if (row.target_player_id === 44 && row.status !== 'closed') {
        row.created_at = new Date(Date.now() - 60 * 60 * 1000);
      }
    }
    const handledBefore = (await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    })).json.moderators.find((row) => row.playerId === 33).handledReports;
    const cycle = await request(fileApp, {
      method: 'POST',
      path: '/moderation/reports',
      headers: fileToken(UUID_A),
      body: { player: 'Plain', reason: 'erneut uebernehmen' }
    });
    assert.strictEqual(cycle.status, 200);
    assert.strictEqual(cycle.json.duplicate, false);
    const cycleId = cycle.json.id;
    for (const action of ['claim', 'release', 'claim']) {
      const step = await request(fileApp, {
        method: 'POST',
        path: '/staff/reports/' + cycleId + '/' + action,
        headers: adminHeaders(fileDb.sessions.mod.token),
        body: {}
      });
      assert.strictEqual(step.status, 200);
    }
    const cycleClose = await request(fileApp, {
      method: 'POST',
      path: '/staff/reports/' + cycleId + '/close',
      headers: adminHeaders(fileDb.sessions.mod.token),
      body: { outcome: 'no_violation' }
    });
    assert.strictEqual(cycleClose.status, 200);
    const claimEvents = fileDb.state.events.filter((row) => {
      if (row.kind !== 'report_claimed') return false;
      const payload = typeof row.payload === 'string' ? JSON.parse(row.payload) : row.payload;
      return Number(payload.reportId) === Number(cycleId) && Number(row.actor_player_id) === 33;
    });
    assert.strictEqual(claimEvents.length, 2);
    const handledAfter = (await request(fileApp, {
      path: '/staff/stats/moderators',
      headers: adminHeaders(fileDb.sessions.admin.token)
    })).json.moderators.find((row) => row.playerId === 33).handledReports;
    assert.strictEqual(handledAfter, handledBefore + 1);
    const tiles = await request(fileApp, {
      path: '/staff/files/tiles',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(tiles.status, 200);
    const plainTile = tiles.json.tiles.find((row) => row.name === 'Plain');
    assert.ok(plainTile);
    assert.ok(plainTile.count >= 2);
    assert.ok(!tiles.json.tiles.some((row) => row.name === 'ModUser'));
    const byOwner = await request(fileApp, {
      path: '/staff/files/tiles?moderator=ModUser',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    const ownerTile = byOwner.json.tiles.find((row) => row.name === 'Plain');
    assert.ok(ownerTile);
    assert.ok(ownerTile.count >= 1);
    const byCloser = await request(fileApp, {
      path: '/staff/files/tiles?moderator=Admin',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(byCloser.json.tiles.some((row) => row.name === 'Plain'));
    const combined = await request(fileApp, {
      path: '/staff/files/tiles?player=Plain&moderator=ModUser',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(combined.json.tiles.some((row) => row.name === 'Plain'));
    assert.ok(!combined.json.tiles.some((row) => row.name !== 'Plain'));
    const otherPlayer = await request(fileApp, {
      path: '/staff/files/tiles?player=Admin&moderator=ModUser',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(!otherPlayer.json.tiles.some((row) => row.name === 'Plain'));
    const nobody = await request(fileApp, {
      path: '/staff/files/tiles?moderator=Niemand',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.strictEqual(nobody.json.tiles.length, 0);
    const playerNames = await request(fileApp, {
      path: '/staff/suggest/players?q=',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(playerNames.json.names.includes('Plain'));
    assert.ok(playerNames.json.names.includes('Admin'));
    assert.ok(!playerNames.json.names.includes('Ghost'));
    const narrowed = await request(fileApp, {
      path: '/staff/suggest/players?q=pl',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(narrowed.json.names.includes('Plain'));
    assert.ok(!narrowed.json.names.includes('Admin'));
    const moderatorNames = await request(fileApp, {
      path: '/staff/suggest/moderators?q=',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(moderatorNames.json.names.includes('ModUser'));
    assert.ok(moderatorNames.json.names.includes('Admin'));
    const narrowedMod = await request(fileApp, {
      path: '/staff/suggest/moderators?q=mod',
      headers: adminHeaders(fileDb.sessions.admin.token)
    });
    assert.ok(narrowedMod.json.names.includes('ModUser'));
    assert.ok(!narrowedMod.json.names.includes('Admin'));
    const playerDenied = await request(fileApp, {
      path: '/moderation/files?player=Plain',
      headers: fileToken(OTHER_UUID)
    });
    assert.strictEqual(playerDenied.status, 403);
    const tilesDenied = await request(fileApp, {
      path: '/staff/files/tiles',
      headers: { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin' }
    });
    assert.strictEqual(tilesDenied.status, 401);
    const suggestDenied = await request(fileApp, {
      path: '/moderation/suggest/players?q=',
      headers: fileToken(OTHER_UUID)
    });
    assert.strictEqual(suggestDenied.status, 403);
    const anonFile = await request(fileApp, {
      path: '/staff/files?player=Plain',
      headers: { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin' }
    });
    assert.strictEqual(anonFile.status, 401);
    assert.ok(!anonFile.json.violations);
    const fileUnlock = fileDb.sessions.mod.mod_unlocked_until;
    fileDb.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    const lockedFile = await request(fileApp, {
      path: '/staff/files?player=Plain',
      headers: adminHeaders(fileDb.sessions.mod.token)
    });
    fileDb.sessions.mod.mod_unlocked_until = fileUnlock;
    assert.strictEqual(lockedFile.status, 401);
    const tokenFile = await request(fileApp, {
      path: '/moderation/files?player=Plain',
      headers: fileToken(MOD_UUID)
    });
    assert.strictEqual(tokenFile.status, 200);
    assert.ok(tokenFile.json.violations.some((item) => item.kind === 'report' && item.report.id === confirmedId));

    const live = [];
    const chatTeamDb = makeDb();
    const chatTeamApp = createApp(chatTeamDb, {
      onTeamChat: (message) => live.push({ type: 'team.message', message }),
      onReportTeamChat: (reportId, message) => live.push({ type: 'team.report', reportId: Number(reportId), message })
    });
    function inProgress(id, owner) {
      return {
        id, season_id: 2, target_player_id: 44, reporter_player_id: 44, message_id: null,
        reason_text: 'test', status: 'in_progress', owner_player_id: owner, help_requested: 0,
        confirmed: null, outcome: null, source: 'website', created_at: new Date(),
        closed_at: null, closed_by_player_id: null
      };
    }
    const anonTeam = await request(chatTeamApp, {
      path: '/staff/team-chat',
      headers: { 'x-admin-request': '1', 'sec-fetch-site': 'same-origin' }
    });
    assert.strictEqual(anonTeam.status, 401);
    const sent = await request(chatTeamApp, {
      method: 'POST', path: '/staff/team-chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token),
      body: { message: 'Interner Hinweis' }
    });
    assert.strictEqual(sent.status, 200);
    assert.strictEqual(sent.json.message.prefix, '[Mod]');
    assert.ok(live.some((row) => row.type === 'team.message'));
    chatTeamDb.sessions.mod.mod_unlocked_until = new Date(Date.now() - 1000);
    const revoked = await request(chatTeamApp, {
      path: '/staff/team-chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token)
    });
    assert.strictEqual(revoked.status, 401);
    chatTeamDb.sessions.mod.mod_unlocked_until = new Date(Date.now() + 3600000);

    chatTeamDb.state.reports.push(inProgress(9, 33));
    const outsider = await request(chatTeamApp, {
      path: '/staff/reports/9/chat',
      headers: adminHeaders(chatTeamDb.sessions.modB.token)
    });
    assert.strictEqual(outsider.status, 403);
    const reportInbox = await request(chatTeamApp, {
      path: '/staff/reports',
      headers: adminHeaders(chatTeamDb.sessions.modB.token)
    });
    assert.ok(reportInbox.json.reports.some((row) => row.id === 9 && row.teamChat && row.teamChat.readable === false));
    const ownerChat = await request(chatTeamApp, {
      method: 'POST', path: '/staff/reports/9/chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token),
      body: { message: 'Wir prüfen das.' }
    });
    assert.strictEqual(ownerChat.status, 200);
    const adminChat = await request(chatTeamApp, {
      method: 'POST', path: '/staff/reports/9/chat',
      headers: adminHeaders(chatTeamDb.sessions.admin.token),
      body: { message: 'Admin sieht mit.' }
    });
    assert.strictEqual(adminChat.status, 200);
    chatTeamDb.state.reportWatchers.push({ report_id: 9, player_id: 77, season_id: 2 });
    const helperChat = await request(chatTeamApp, {
      method: 'POST', path: '/staff/reports/9/chat',
      headers: adminHeaders(chatTeamDb.sessions.modB.token),
      body: { message: 'Unterstützer schreibt.' }
    });
    assert.strictEqual(helperChat.status, 200);
    const closedEmpty = inProgress(10, 33);
    closedEmpty.status = 'closed';
    closedEmpty.outcome = 'no_violation';
    chatTeamDb.state.reports.push(closedEmpty);
    const emptyClosed = await request(chatTeamApp, {
      path: '/staff/reports/10',
      headers: adminHeaders(chatTeamDb.sessions.mod.token)
    });
    assert.strictEqual(emptyClosed.json.report.teamChat.count, 0);
    assert.strictEqual(emptyClosed.json.report.teamChat.writable, false);
    assert.ok(staffJs.includes("report.status === 'closed' && !access.count"));
    assert.ok(staffJs.includes("button.textContent = 'Teamchat'"));
    assert.ok(!staffJs.includes('/ws/staff-team'));
    assert.ok(staffJs.includes('/staff/filters'));
    assert.ok(staffJs.includes('/staff/filters/preview'));
    assert.ok(staffJs.includes("kind === 'domain'"));
    assert.ok(home.includes('Chatfilter'));
    assert.ok(home.includes('data-filter-kind="domain"'));
    assert.ok(home.includes('type="search"'));
    assert.ok(home.includes('Freigegebene Domains'));
    assert.ok(!home.includes('<textarea'));
    assert.ok(adminHtml.includes('value="filters"'));
    assert.ok(workspace.includes('filtersView'));
    assert.ok(workspace.includes('data-filter-kind="phrase"'));
    assert.ok(styleCss.includes('max-height:320px'));
    assert.ok(styleCss.includes('height:32px'));
    const adminJs = fs.readFileSync(path.join(__dirname, '../../../backend_server/public/admin-script.js'), 'utf8');
    const applyBody = adminJs.slice(adminJs.indexOf('function applyView'), adminJs.indexOf('function applyConfigView'));
    assert.ok(applyBody.includes('currentView === "filters"'));
    assert.ok(applyBody.indexOf('currentView === "filters"') < applyBody.indexOf('playerstatsContainer.style.display = "block"'));
    assert.ok(!adminHtml.includes('value="teamchat"'));
    assert.ok(home.includes('id="team-chat-log"'));
    assert.ok(workspace.includes('Liste anzeigen'));
    assert.ok(workspace.includes('overview-team-chat'));

    chatTeamDb.state.reports[0].status = 'closed';
    const frozen = await request(chatTeamApp, {
      method: 'POST', path: '/staff/reports/9/chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token),
      body: { message: 'zu spät' }
    });
    assert.strictEqual(frozen.status, 409);

    async function raceAgainst(actionPath, actionBody, reportId, beforeCount) {
      live.length = 0;
      let resume;
      chatTeamDb.state.pauseAfterReportLock = new Promise((resolve) => { resume = resolve; });
      const actionP = request(chatTeamApp, {
        method: 'POST', path: actionPath,
        headers: adminHeaders(chatTeamDb.sessions.mod.token),
        body: actionBody
      });
      await waitUntil(() => chatTeamDb.state.reportRowPaused);
      const sendP = request(chatTeamApp, {
        method: 'POST', path: '/staff/reports/' + reportId + '/chat',
        headers: adminHeaders(chatTeamDb.sessions.admin.token),
        body: { message: 'parallel' }
      });
      resume();
      const closed = await actionP;
      const written = await sendP;
      assert.strictEqual(closed.status, 200);
      assert.ok(written.status === 409 || written.status === 403, written.status + ' ' + JSON.stringify(written.json));
      assert.strictEqual(chatTeamDb.state.reportTeamMessages.filter((row) => row.report_id === reportId).length, beforeCount);
      assert.ok(!live.some((row) => row.type === 'team.report' && row.message && row.message.message === 'parallel'));
    }
    chatTeamDb.state.reports[0].status = 'in_progress';
    chatTeamDb.state.reports[0].owner_player_id = 33;
    const beforeClose = chatTeamDb.state.reportTeamMessages.filter((row) => row.report_id === 9).length;
    await raceAgainst('/staff/reports/9/close', { outcome: 'no_violation' }, 9, beforeClose);

    chatTeamDb.state.reports.push(inProgress(11, 33));
    chatTeamDb.state.reportWatchers.push({ report_id: 11, player_id: 77, season_id: 2 });
    await request(chatTeamApp, {
      method: 'POST', path: '/staff/reports/11/chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token),
      body: { message: 'vor Freigabe' }
    });
    const helperWs = {
      readyState: 1,
      staffAuthorized: true,
      staffCheckedAt: Date.now(),
      staffPlayerId: 77,
      staffRole: 'moderator',
      reportChats: new Set([11]),
      sent: [],
      send(text) { this.sent.push(JSON.parse(text)); }
    };
    const staffWs = {
      readyState: 1,
      staffAuthorized: true,
      staffCheckedAt: Date.now(),
      staffPlayerId: 33,
      staffRole: 'moderator',
      reportChats: new Set(),
      sent: [],
      send(text) { this.sent.push(JSON.parse(text)); }
    };
    assert.strictEqual(await teamChat.socketMayReadReport(chatTeamDb.pool, 11, helperWs), true);
    const beforeRelease = chatTeamDb.state.reportTeamMessages.filter((row) => row.report_id === 11).length;
    await raceAgainst('/staff/reports/11/release', {}, 11, beforeRelease);
    assert.strictEqual(await teamChat.socketMayReadReport(chatTeamDb.pool, 11, helperWs), false);
    const teamFeedTest = staffFeed.createStaffFeed(chatTeamDb.pool);
    teamFeedTest.reportAccess = (ws, reportId) => teamChat.socketMayReadReport(chatTeamDb.pool, reportId, ws);
    teamFeedTest.sockets.add(helperWs);
    teamFeedTest.sockets.add(staffWs);
    await teamFeedTest.publish({ type: 'team.message', message: { id: 99, message: 'intern bleibt' } });
    await teamFeedTest.publish({ type: 'team.report', reportId: 11, message: { id: 100, message: 'nach Freigabe' } }, (ws) => (
      teamChat.socketMayReadReport(chatTeamDb.pool, 11, ws)
    ));
    assert.ok(staffWs.sent.some((row) => row.type === 'team.message'));
    assert.ok(helperWs.sent.some((row) => row.type === 'team.message'));
    assert.ok(!helperWs.sent.some((row) => row.type === 'team.report'));

    chatTeamDb.state.teamMessages.push({
      id: 500, actor_player_id: 33, actor_name: 'ModUser', role_snapshot: 'moderator',
      body: 'alt', created_at: new Date(Date.now() - 25 * 60 * 60 * 1000)
    });
    assert.strictEqual(await teamChat.cleanupGeneral24h(chatTeamDb.pool), 1);
    chatTeamDb.state.seasons = [
      { id: 1, is_current: 0, start_at: new Date('2026-01-01') },
      { id: 2, is_current: 1, start_at: new Date('2026-06-01') }
    ];
    chatTeamDb.state.reportTeamMessages.push({
      id: 800, report_id: 9, season_id: 1, actor_player_id: 33, actor_name: 'ModUser',
      role_snapshot: 'moderator', body: 'alte season', created_at: new Date()
    });
    const seasonJob = await chatMod.cleanupEndedSeasonData(chatTeamDb.pool);
    assert.ok(!seasonJob.skipped);
    assert.ok(seasonJob.reportChats >= 1);
    assert.ok(!chatTeamDb.state.reportTeamMessages.some((row) => row.season_id === 1));

    const successLive = [];
    const okApp = createApp(chatTeamDb, {
      onReportTeamChat: (reportId, message) => successLive.push({ reportId: Number(reportId), after: chatTeamDb.state.reportTeamMessages.length, message })
    });
    chatTeamDb.state.reports.push(inProgress(12, 33));
    const okSend = await request(okApp, {
      method: 'POST', path: '/staff/reports/12/chat',
      headers: adminHeaders(chatTeamDb.sessions.mod.token),
      body: { message: 'nach Insert' }
    });
    assert.strictEqual(okSend.status, 200);
    assert.ok(successLive[0].after >= 1);

    chatTeamDb.state.reports.push(inProgress(13, 33));
    chatTeamDb.state.reportWatchers.push({ report_id: 13, player_id: 77, season_id: 2 });
    const openFeed = staffFeed.createStaffFeed(chatTeamDb.pool);
    openFeed.reportAccess = (ws, reportId) => teamChat.socketMayReadReport(chatTeamDb.pool, reportId, ws);
    function openSocket() {
      return {
        readyState: 1,
        sent: [],
        send(text) { this.sent.push(JSON.parse(text)); },
        close(code) { this.readyState = 3; this.closedCode = code; }
      };
    }
    const supporter = openSocket();
    const supporterAuth = await openFeed.accept(supporter, 'cclive_web=' + chatTeamDb.sessions.modB.token);
    assert.strictEqual(supporterAuth.ok, true);
    await openFeed.watchReport(supporter, 13);
    assert.ok(supporter.reportChats.has(13));
    const releasedOpen = await request(chatTeamApp, {
      method: 'POST',
      path: '/staff/reports/13/release',
      headers: adminHeaders(chatTeamDb.sessions.mod.token)
    });
    assert.strictEqual(releasedOpen.status, 200);
    supporter.sent = [];
    await openFeed.recheckAll();
    assert.strictEqual(supporter.readyState, 1);
    assert.strictEqual(supporter.sent.length, 1);
    assert.strictEqual(supporter.sent[0].type, 'report.chat.denied');
    assert.strictEqual(supporter.sent[0].reportId, 13);
    assert.ok(!Object.prototype.hasOwnProperty.call(supporter.sent[0], 'message'));
    assert.ok(!supporter.reportChats.has(13));
    await openFeed.publish({ type: 'team.message', message: { id: 70, message: 'team bleibt' } });
    assert.ok(supporter.sent.some((row) => row.type === 'team.message' && row.message.message === 'team bleibt'));
    const roleViewer = openSocket();
    await openFeed.accept(roleViewer, 'cclive_web=' + chatTeamDb.sessions.mod.token);
    roleViewer.sent = [];
    chatTeamDb.state.roles.delete(33);
    roleViewer.staffCheckedAt = 0;
    await openFeed.publish({ type: 'team.message', message: { id: 71, message: 'darf nicht' } });
    assert.strictEqual(roleViewer.sent.length, 0);
    assert.strictEqual(roleViewer.closedCode, 4401);
    chatTeamDb.state.roles.set(33, 'moderator');

    const moderationSource = fs.readFileSync(path.resolve(__dirname, '../../../backend_server/website_backend/chat-moderation.js'), 'utf8');
    const commandSource = fs.readFileSync(path.resolve(__dirname, '../../../src/main/java/net/felix/commands/CCLiveCommands.java'), 'utf8');
    const rulesScreen = fs.readFileSync(path.resolve(__dirname, '../../../src/main/java/net/felix/chat/ChatRulesScreen.java'), 'utf8');
    assert.strictEqual(serverSource.split('acceptPlayerChat').length - 1, 2);
    const acceptAt = moderationSource.indexOf('async function acceptPlayerChat');
    const persistAt = moderationSource.indexOf('async function persistChatMessage');
    const acceptBody = moderationSource.slice(acceptAt, persistAt);
    assert.ok(acceptBody.indexOf('containsBlocked') < acceptBody.indexOf('persistChatMessage'));
    const persistBody = moderationSource.slice(persistAt, moderationSource.indexOf('async function', persistAt + 20));
    assert.ok(persistBody.indexOf('denyChatSend') < persistBody.indexOf('inspect'));
    assert.ok(commandSource.includes('literal("chatrules")'));
    assert.ok(commandSource.includes('ChatRulesScreen.open()'));
    assert.ok(rulesScreen.includes('"/chat/rules"'));
    assert.ok(rulesScreen.includes('CCLive Chatregeln'));
    assert.ok(rulesScreen.includes('mouseScrolled'));
    assert.ok(rulesScreen.includes('Freigegebene Domains'));
    assert.ok(rulesScreen.includes('getPlayerToken'));
    assert.ok(!rulesScreen.includes('StaffClientAccess'));
    assert.deepStrictEqual(chatFilters.SEED_DOMAINS, ['youtube.com', 'twitch.tv', 'cclive-utilities.net']);
    assert.ok(!chatFilters.SEED_DOMAINS.includes('youtu.be'));
    assert.strictEqual(chatFilters.REPEAT_WINDOW_MS, 10000);
    assert.strictEqual(chatFilters.REPEAT_LOCK_MS, 60000);

    const filterDb = makeDb();
    const filterApp = createApp(filterDb);
    const adminActor = { id: 11, name: 'Admin', role: 'admin' };
    await chatFilters.ensureFilterSchema(filterDb.pool);
    const seeded = await chatFilters.readLists(filterDb.pool);
    assert.deepStrictEqual(seeded.domains.slice().sort(), ['cclive-utilities.net', 'twitch.tv', 'youtube.com']);
    assert.ok(!seeded.domains.includes('youtu.be'));
    await chatFilters.saveFilters(filterDb.pool, {
      domains: ['youtube.com', 'cclive-utilities.net'],
      phrases: [],
      exceptions: []
    }, adminActor);
    await chatFilters.ensureFilterSchema(filterDb.pool);
    const seededKept = await chatFilters.readLists(filterDb.pool);
    assert.deepStrictEqual(seededKept.domains.slice().sort(), ['cclive-utilities.net', 'youtube.com']);
    assert.ok(!seededKept.domains.includes('twitch.tv'));
    assert.ok(!seededKept.domains.includes('youtu.be'));

    async function say(uuid, message) {
      return request(filterApp, {
        method: 'POST',
        path: '/chat/send',
        headers: token(uuid),
        body: { message }
      });
    }
    async function expectSend(uuid, message, code) {
      const before = filterDb.state.messages.length;
      const sanctions = filterDb.state.sanctions.length;
      const response = await say(uuid, message);
      if (code) {
        assert.strictEqual(response.status, code === 'blocked_glyph' ? 400 : 403, message);
        assert.strictEqual(response.json.code, code, message);
        assert.strictEqual(filterDb.state.messages.length, before, message);
      } else {
        assert.strictEqual(response.status, 200, message + ' ' + JSON.stringify(response.json));
        assert.strictEqual(filterDb.state.messages.length, before + 1, message);
      }
      assert.strictEqual(filterDb.state.sanctions.length, sanctions, message);
      return response;
    }

    const changed = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: ['youtube.com', 'twitch.tv', 'cclive-utilities.net', 'playlegend.net'],
        phrases: ['scheiß', 'dreck'],
        exceptions: []
      }
    });
    assert.strictEqual(changed.status, 200);
    assert.ok(changed.json.domains.includes('playlegend.net'));
    assert.ok(changed.json.domains.includes('twitch.tv'));
    assert.ok(filterDb.state.events.some((event) => event.kind === 'filter_changed'));
    const duplicateDomain = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: ['youtube.com', 'https://www.youtube.com/watch?v=abc'],
        phrases: ['scheiß', 'dreck'],
        exceptions: []
      }
    });
    assert.strictEqual(duplicateDomain.status, 400);
    assert.strictEqual(duplicateDomain.json.code, 'duplicate');
    assert.ok(filterDb.state.filterDomains.includes('playlegend.net'));
    const domainPreview = await request(filterApp, {
      method: 'POST',
      path: '/staff/filters/preview',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: { kind: 'domain', value: 'https://www.youtube.com/watch?v=abc' }
    });
    assert.strictEqual(domainPreview.status, 200);
    assert.strictEqual(domainPreview.json.value, 'youtube.com');
    assert.strictEqual(domainPreview.json.code, 'duplicate');
    const domainEdit = await request(filterApp, {
      method: 'POST',
      path: '/staff/filters/preview',
      headers: adminHeaders(filterDb.sessions.mod.token),
      body: { kind: 'domain', value: 'https://www.youtube.com/watch?v=abc', ignore: 'youtube.com' }
    });
    assert.strictEqual(domainEdit.json.ok, true);
    assert.strictEqual(domainEdit.json.value, 'youtube.com');
    const invalidDomain = await request(filterApp, {
      method: 'POST',
      path: '/staff/filters/preview',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: { kind: 'domain', value: 'keine domain' }
    });
    assert.strictEqual(invalidDomain.json.ok, false);
    assert.strictEqual(invalidDomain.json.code, 'invalid');
    const removedDomain = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: ['youtube.com', 'twitch.tv', 'cclive-utilities.net'],
        phrases: ['scheiß', 'dreck'],
        exceptions: []
      }
    });
    assert.strictEqual(removedDomain.status, 200);
    assert.ok(!removedDomain.json.domains.includes('playlegend.net'));
    const restored = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: ['youtube.com', 'twitch.tv', 'cclive-utilities.net', 'playlegend.net'],
        phrases: ['scheiß', 'dreck'],
        exceptions: []
      }
    });
    assert.strictEqual(restored.status, 200);
    const modWrite = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.mod.token),
      body: { domains: ['evil.example'], phrases: [], exceptions: [] }
    });
    assert.strictEqual(modWrite.status, 403);
    assert.ok(!filterDb.state.filterDomains.includes('evil.example'));
    const modRead = await request(filterApp, {
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.mod.token)
    });
    assert.strictEqual(modRead.status, 200);
    assert.strictEqual(modRead.json.canEdit, false);
    assert.ok(modRead.json.phrases.includes('scheiß'));
    assert.ok(modRead.json.exceptions);
    const rules = await request(filterApp, { path: '/chat/rules', headers: token(OTHER_UUID) });
    assert.strictEqual(rules.status, 200);
    assert.ok(Array.isArray(rules.json.rules));
    assert.strictEqual(rules.json.rules.length, 5);
    assert.ok(rules.json.rules[0].startsWith('Keine harten Beleidigungen'));
    assert.ok(rules.json.domains.includes('youtube.com'));
    assert.ok(!rules.json.domains.includes('youtu.be'));
    assert.ok(!Object.prototype.hasOwnProperty.call(rules.json, 'phrases'));
    assert.ok(!Object.prototype.hasOwnProperty.call(rules.json, 'exceptions'));
    assert.ok(!rules.raw.includes('scheiß'));
    assert.ok(!rules.raw.includes('dreck'));

    await expectSend(OTHER_UUID, 'https://youtube.com');
    await expectSend(OTHER_UUID, 'https://www.youtube.com/watch');
    await expectSend(OTHER_UUID, 'https://shop.youtube.com/offer');
    await expectSend(OTHER_UUID, '(https://youtube.com/watch?v=1)');
    await expectSend(OTHER_UUID, 'https://youtube.com.');
    await expectSend(OTHER_UUID, 'https://youtube.com,');
    await expectSend(OTHER_UUID, 'https://youtube.com und https://twitch.tv');
    await expectSend(OTHER_UUID, 'https://youtube.com und https://evil.example', 'domain_blocked');
    await expectSend(OTHER_UUID, 'https://youtube.com.evil.example', 'domain_blocked');
    await expectSend(OTHER_UUID, 'https://evil-youtube.com', 'domain_blocked');
    await expectSend(OTHER_UUID, 'https://playlegend.net/shop');
    await expectSend(OTHER_UUID, 'https://shop.playlegend.net');
    await expectSend(OTHER_UUID, 'https://playlegend.net.evil.example', 'domain_blocked');
    await expectSend(OTHER_UUID, 'https://playlegend.net@evil.example', 'domain_blocked');
    await expectSend(OTHER_UUID, 'Version 1.20.1 ist da');
    await expectSend(OTHER_UUID, 'so eine Scheiße hier');
    await expectSend(OTHER_UUID, 'diese Drecksschweine droppen das Item nicht');
    await expectSend(OTHER_UUID, 'hello #ITEM:ITEM-db2ac5 #BUILD:BUILD-709125');
    const phraseSave = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: ['youtube.com', 'twitch.tv', 'cclive-utilities.net', 'playlegend.net'],
        phrases: ['verbotswort'],
        exceptions: ['das verbotswort bleibt']
      }
    });
    assert.strictEqual(phraseSave.status, 200);
    const duplicatePhrase = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: phraseSave.json.domains,
        phrases: ['verbotswort', 'Verbotswort'],
        exceptions: ['das verbotswort bleibt']
      }
    });
    assert.strictEqual(duplicatePhrase.status, 400);
    assert.strictEqual(duplicatePhrase.json.code, 'duplicate');
    assert.deepStrictEqual(filterDb.state.filterPhrases, ['verbotswort']);
    const duplicateException = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: {
        domains: phraseSave.json.domains,
        phrases: ['verbotswort'],
        exceptions: ['das verbotswort bleibt', 'Das verbotswort bleibt']
      }
    });
    assert.strictEqual(duplicateException.status, 400);
    assert.strictEqual(duplicateException.json.code, 'duplicate');
    assert.deepStrictEqual(filterDb.state.filterExceptions, ['das verbotswort bleibt']);
    const blockedPhrase = await expectSend(OTHER_UUID, 'nur verbotswort', 'filtered');
    assert.ok(!blockedPhrase.json.error.includes('verbotswort'));
    await expectSend(OTHER_UUID, 'das verbotswort bleibt hier');
    const eventsBeforeAmbiguous = filterDb.state.events.filter((event) => event.kind === 'filter_changed').length;
    filterDb.state.seasons = [{ id: 2, is_current: 1 }, { id: 3, is_current: 1 }];
    const ambiguousSave = await chatFilters.saveFilters(filterDb.pool, {
      domains: ['example.com'],
      phrases: [],
      exceptions: []
    }, adminActor);
    assert.strictEqual(ambiguousSave.ok, true);
    assert.deepStrictEqual(ambiguousSave.domains, ['example.com']);
    assert.strictEqual(filterDb.state.events.filter((event) => event.kind === 'filter_changed').length, eventsBeforeAmbiguous);
    filterDb.state.seasons = [{ id: 2, is_current: 1 }];
    const emptied = await request(filterApp, {
      method: 'PUT',
      path: '/staff/filters',
      headers: adminHeaders(filterDb.sessions.admin.token),
      body: { domains: [], phrases: [], exceptions: [] }
    });
    assert.strictEqual(emptied.status, 200);
    await chatFilters.ensureFilterSchema(filterDb.pool);
    assert.deepStrictEqual(filterDb.state.filterDomains, []);
    await expectSend(OTHER_UUID, 'https://youtube.com/leer', 'domain_blocked');
    await expectSend(OTHER_UUID, 'Text ohne Link');
    process.env.CHAT_FILTERS_ENABLED = '0';
    try {
      await expectSend(OTHER_UUID, 'https://evil.example/aus');
    } finally {
      delete process.env.CHAT_FILTERS_ENABLED;
    }
    await expectSend(OTHER_UUID, 'https://evil.example/an', 'domain_blocked');
    await expectSend(OTHER_UUID, 'Hallo \u0473', 'blocked_glyph');

    const filterMute = await request(filterApp, {
      method: 'POST',
      path: '/moderation/actions/mute',
      headers: token(MOD_UUID),
      body: { player: 'Plain', duration: '10m', reason: 'Spamtest' }
    });
    assert.strictEqual(filterMute.status, 200);
    const mutedTry = await say(OTHER_UUID, 'darf nicht schreiben');
    assert.strictEqual(mutedTry.status, 403);
    assert.strictEqual(mutedTry.json.code, 'muted');
    const frozenSanction = JSON.stringify(filterDb.state.sanctions);
    chatFilters.clearRepeats();
    const same = 'dreimal-gleich';
    assert.strictEqual((await say(MOD2_UUID, same)).status, 200);
    assert.strictEqual((await say(MOD2_UUID, same)).status, 200);
    const thirdRepeat = await say(MOD2_UUID, same);
    assert.strictEqual(thirdRepeat.status, 403);
    assert.strictEqual(thirdRepeat.json.code, 'repeat_blocked');
    const fourthRepeat = await say(MOD2_UUID, 'danach-anders');
    assert.strictEqual(fourthRepeat.json.code, 'repeat_blocked');
    const otherRepeat = await say(UUID_A, same);
    assert.strictEqual(otherRepeat.status, 200);
    assert.strictEqual(JSON.stringify(filterDb.state.sanctions), frozenSanction);
    chatFilters.expireRepeatLocks();
    assert.strictEqual((await say(MOD2_UUID, same)).status, 200);

    chatFilters.clearRepeats();
    const filterSockDb = makeDb();
    await chatFilters.ensureFilterSchema(filterSockDb.pool);
    filterSockDb.state.playerTokens.push(
      { token: 'plain-token', player_id: 44, revoked_at: null },
      { token: 'modb-token', player_id: 77, revoked_at: null }
    );
    const chatClients = new Set();
    function broadcastChat(payload) {
      const text = JSON.stringify(payload);
      for (const client of chatClients) {
        if (chatMod.socketMayReceive(client)) client.send(text);
      }
    }
    const filterWss = new WebSocketServer({ noServer: true });
    filterWss.on('connection', (ws) => {
      chatClients.add(ws);
      ws.player = null;
      ws.playerId = null;
      ws.chatBanned = true;
      ws.on('message', (raw) => {
        Promise.resolve().then(async () => {
          const data = JSON.parse(String(raw));
          if (data.type === 'auth') {
            const [rows] = await filterSockDb.pool.query(
              `SELECT p.display_name AS player, t.player_id, p.mc_uuid
                 FROM tokens t
                 INNER JOIN players p ON p.id = t.player_id
                WHERE t.token = ? AND t.revoked_at IS NULL AND p.mc_uuid IS NOT NULL
                LIMIT 1`,
              [data.token]
            );
            if (!rows.length) {
              ws.send(JSON.stringify({ type: 'auth.error' }));
              return;
            }
            ws.player = rows[0].player;
            ws.playerId = rows[0].player_id;
            ws.chatBanned = !(await chatMod.canReceiveChat(filterSockDb.pool, ws.playerId));
            ws.send(JSON.stringify({ type: 'auth.ok', player: ws.player }));
            return;
          }
          if (data.type !== 'chat.send') return;
          const accepted = await chatMod.acceptPlayerChat(filterSockDb.pool, {
            player: ws.player,
            playerId: ws.playerId,
            message: data.message
          });
          if (!accepted.ok) {
            if (accepted.status === 403) {
              ws.chatBanned = !(await chatMod.canReceiveChat(filterSockDb.pool, ws.playerId));
              ws.send(JSON.stringify({ type: 'chat.denied', code: accepted.code, error: accepted.error }));
            } else {
              ws.send(JSON.stringify({ type: 'chat.error', error: accepted.error }));
            }
            return;
          }
          const payload = { type: 'chat.message', id: accepted.id, player: accepted.player, message: accepted.message };
          ws.send(JSON.stringify({ type: 'chat.ack', id: accepted.id }));
          broadcastChat(payload);
        }).catch(() => {
          ws.send(JSON.stringify({ type: 'chat.error', error: 'Serverfehler' }));
        });
      });
      ws.on('close', () => chatClients.delete(ws));
    });
    const filterHttp = http.createServer((req, res) => {
      if (req.method !== 'POST' || req.url !== '/chat/send') {
        res.writeHead(404);
        res.end();
        return;
      }
      const chunks = [];
      req.on('data', (chunk) => chunks.push(chunk));
      req.on('end', () => {
        Promise.resolve().then(async () => {
          const body = JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}');
          const player = filterSockDb.players.find((entry) => entry.mc_uuid === req.headers['x-test-uuid']);
          const accepted = await chatMod.acceptPlayerChat(filterSockDb.pool, {
            player: player && player.display_name,
            playerId: player && player.id,
            message: body.message
          });
          if (!accepted.ok) {
            res.writeHead(accepted.status || 400, { 'content-type': 'application/json' });
            res.end(JSON.stringify({ error: accepted.error, code: accepted.code }));
            return;
          }
          broadcastChat({ type: 'chat.message', id: accepted.id, player: accepted.player, message: accepted.message });
          res.writeHead(200, { 'content-type': 'application/json' });
          res.end(JSON.stringify({ success: true, id: accepted.id }));
        }).catch(() => {
          res.writeHead(500);
          res.end();
        });
      });
    });
    attachWebSocketDispatch(filterHttp, { '/ws/chat': filterWss });
    await new Promise((resolve) => filterHttp.listen(0, '127.0.0.1', resolve));
    const filterPort = filterHttp.address().port;
    function openFilterSocket() {
      return new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://127.0.0.1:' + filterPort + '/ws/chat');
        const messages = [];
        ws.on('message', (buf) => messages.push(JSON.parse(String(buf))));
        ws.on('open', () => resolve({ ws, messages }));
        ws.on('error', reject);
      });
    }
    function waitSocket(bag, pred) {
      const found = bag.messages.find(pred);
      if (found) return Promise.resolve(found);
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('socket timeout')), 2000);
        const onMessage = () => {
          const row = bag.messages.find(pred);
          if (!row) return;
          clearTimeout(timer);
          bag.ws.off('message', onMessage);
          resolve(row);
        };
        bag.ws.on('message', onMessage);
      });
    }
    const filterSockets = [];
    try {
      const plainSock = await openFilterSocket();
      filterSockets.push(plainSock.ws);
      plainSock.ws.send(JSON.stringify({ type: 'auth', token: 'plain-token' }));
      await waitSocket(plainSock, (row) => row.type === 'auth.ok');
      const line = 'socket-gleich';
      const firstPost = await new Promise((resolve, reject) => {
        const req = http.request({
          hostname: '127.0.0.1',
          port: filterPort,
          path: '/chat/send',
          method: 'POST',
          headers: { 'content-type': 'application/json', 'x-test-uuid': OTHER_UUID, host: '127.0.0.1:' + filterPort }
        }, (res) => {
          const parts = [];
          res.on('data', (chunk) => parts.push(chunk));
          res.on('end', () => resolve({ status: res.statusCode, json: JSON.parse(Buffer.concat(parts).toString('utf8')) }));
        });
        req.on('error', reject);
        req.write(JSON.stringify({ message: line }));
        req.end();
      });
      assert.strictEqual(firstPost.status, 200);
      plainSock.ws.send(JSON.stringify({ type: 'chat.send', message: line }));
      const ack = await waitSocket(plainSock, (row) => row.type === 'chat.ack');
      assert.ok(ack.id);
      const storedBeforeThird = filterSockDb.state.messages.length;
      const thirdPost = await new Promise((resolve, reject) => {
        const req = http.request({
          hostname: '127.0.0.1',
          port: filterPort,
          path: '/chat/send',
          method: 'POST',
          headers: { 'content-type': 'application/json', 'x-test-uuid': OTHER_UUID, host: '127.0.0.1:' + filterPort }
        }, (res) => {
          const parts = [];
          res.on('data', (chunk) => parts.push(chunk));
          res.on('end', () => resolve({ status: res.statusCode, json: JSON.parse(Buffer.concat(parts).toString('utf8')) }));
        });
        req.on('error', reject);
        req.write(JSON.stringify({ message: line }));
        req.end();
      });
      assert.strictEqual(thirdPost.status, 403);
      assert.strictEqual(thirdPost.json.code, 'repeat_blocked');
      assert.strictEqual(filterSockDb.state.messages.length, storedBeforeThird);
      assert.strictEqual(plainSock.ws.readyState, 1);
      const serverSock = [...chatClients][0];
      assert.strictEqual(serverSock.chatBanned, false);
      assert.strictEqual(chatMod.socketMayReceive(serverSock), true);
      const later = await new Promise((resolve, reject) => {
        const req = http.request({
          hostname: '127.0.0.1',
          port: filterPort,
          path: '/chat/send',
          method: 'POST',
          headers: { 'content-type': 'application/json', 'x-test-uuid': MOD2_UUID, host: '127.0.0.1:' + filterPort }
        }, (res) => {
          const parts = [];
          res.on('data', (chunk) => parts.push(chunk));
          res.on('end', () => resolve({ status: res.statusCode, json: JSON.parse(Buffer.concat(parts).toString('utf8')) }));
        });
        req.on('error', reject);
        req.write(JSON.stringify({ message: 'von modb' }));
        req.end();
      });
      assert.strictEqual(later.status, 200);
      const received = await waitSocket(plainSock, (row) => row.type === 'chat.message' && row.player === 'ModB' && row.message === 'von modb');
      assert.ok(received);
      assert.strictEqual(plainSock.ws.readyState, 1);
    } finally {
      for (const ws of filterSockets) ws.close();
      filterWss.close();
      await new Promise((resolve) => filterHttp.close(resolve));
    }

    console.log('chat-moderation tests ok');
  } finally {
    process.env.ADMIN_KEY = prev.ADMIN_KEY;
    process.env.MOD_KEY = prev.MOD_KEY;
    if (prev.TOOLS == null) delete process.env.MODERATION_TOOLS_ENABLED;
    else process.env.MODERATION_TOOLS_ENABLED = prev.TOOLS;
    if (prev.FILTERS == null) delete process.env.CHAT_FILTERS_ENABLED;
    else process.env.CHAT_FILTERS_ENABLED = prev.FILTERS;
  }
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
