# Etappe 1 — Bestandsaufnahme und verbindlicher Entwurf

Stand: 6.11.2026. Nur Dokumentation. Keine Schemaänderung, keine Implementierung von Etappe 2–10, kein Deployment.

Gesamtplan bleibt verbindlich. Dieses Dokument konkretisiert Etappe 1.

---

## 1. Was in Etappe 1 schon erledigt war

Aus Code und Gesamtplan, hier nicht erneut „erfunden“, nur festgehalten:

- Identitätsmodell (`players.mc_uuid`, Mojang join/hasJoined, Mod-Token, Website-Cookie) ist im Code belegt.
- Es gibt **keine** Moderator-Rolle, **kein** `/cclive perms`, **kein** Ingame-Staff-API.
- Website-Admin: UUID-Allowlist + Cookie `cclive_web` + `ADMIN_KEY`-Unlock in `website_sessions`.
- Live-Chat: WebSocket `/ws/chat`; HTTP `/chat/send` und `/chat/messages` existieren, der Mod nutzt sie nicht.
- Clients sehen **keine** persistente Nachrichten-ID. `requestId` ist nur Send-Ack.
- Glyphensperre Client+Server vorhanden (teilweise uncommittet).
- 24h-Hartlöschung von `chat_messages`, 12h-TTL Shares.
- Seasonwechsel: manuelles SQL, kein Backend-Bereinigungsjob für Seasons.
- Control-Center-Live: Übersicht/Leaderboard/Stats/Configs + „Liste anzeigen“. Moderationsbereiche nur Demo.
- Uncommittete Chat-Reparaturen liegen im Arbeitsbaum (siehe Abschnitt 8).

---

## 2. Bestätigter Live-Datenbankbestand (6.11.2026)

Die lesenden Abfragen wurden vom Team ausgeführt. **Nicht raten, diese Typen in Etappe 2/3 übernehmen.**

**chat_messages**

- `id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY` — vorhanden, in Etappe 2 **nicht** anfassen, keine ID-Übertragung/Chatlöschung.
- `player VARCHAR(255)`, `message TEXT`, `timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `player_id BIGINT(20) NULL`
- Index `idx_timestamp`
- 0 Zeilen, 0 ohne `player_id`

**tokens**

- `id INT(11) AUTO_INCREMENT PRIMARY KEY`
- `token VARCHAR(255) UNIQUE`, `player VARCHAR(255)`
- `player_id BIGINT(20) NULL`, `revoked_at DATETIME NULL`
- `created_at`, `name_color`, `name_colors`, `cactus_rank`, `soul_rank`, `last_seen`, `online_since`
- Indizes `idx_tokens_player_id`, `idx_tokens_player`

**seasons**

- `id INT(11) AUTO_INCREMENT PRIMARY KEY`
- `name VARCHAR(255)`, `is_preseason TINYINT(1)`, `start_at TIMESTAMP`, `is_current TINYINT(1)`

**shared_content**

- `id VARCHAR(32) PRIMARY KEY`
- `owner_player VARCHAR(64)`, `type ENUM('ITEM','BUILD')`
- `data LONGTEXT` mit `JSON_VALID`
- `created_at`, `expires_at`, `owner_player_id BIGINT(20) NULL`

**website_sessions**

- `id BIGINT(20) AUTO_INCREMENT PRIMARY KEY`
- `account_id BIGINT(20)`, `token_hash CHAR(64) UNIQUE`
- `expires_at`, `created_at`
- `admin_unlocked_until DATETIME NULL`, `admin_unlock_key_hash CHAR(64) NULL`
- Index `idx_ws_account`

**players**

- `id BIGINT(20) AUTO_INCREMENT PRIMARY KEY`
- `mc_uuid CHAR(36) NULL` UNIQUE `uk_players_mc_uuid`
- `display_name`, `display_name_normalized VARCHAR(64)`
- `created_at`, `updated_at`
- Index `idx_players_display`

Fremdschlüssel und neue `player_id`-Spalten: **BIGINT**, passend zu `players.id`. `chat_messages.id` bleibt `INT` und wird in Etappe 2 nicht referenziert.

---

## 3. Bestehende Tabellen (Codebeleg) und Nutzung

Erzeugt in `backend_server/website_backend/schema.js`:

- `players` — kanonische Identität (`id`, `mc_uuid` UNIQUE, Anzeigename).
- `player_display_name_history`
- `identity_challenges` — `mod_register` / `website_link`
- `website_accounts` — ein Konto je `player_id`, **keine Rolle**
- `website_link_requests`
- `website_sessions` — `cclive_web`; Spalten `admin_unlocked_until`, `admin_unlock_key_hash`

Nur alteriert / vorausgesetzt:

- `tokens` — Mod-Auth; Join auf `players` mit `mc_uuid IS NOT NULL` und `revoked_at IS NULL`
- `chat_messages` — öffentlicher CCLive-Chat; `player_id` nachgezogen
- `seasons` — aktuelle Season für Leaderboard/Stats, nicht für Chat
- `shared_content` — Item/Build, 12h
- `player_stats`, `leaderboard` — Statistikarchive, Moderationsbereinigung fasst sie **nicht** an
- Config-Tabellen `*_config`, `update_messages`

Presence: In-Memory Heartbeat + `tokens.last_seen` / `online_since`, kein Moderations-Store.

---

## 4. Authentifizierungswege (verbindlich für alle späteren Etappen)

### 4.1 Minecraft / Ingame

1. Mod registriert sich wie heute: Challenge → Mojang `joinServer` → `POST /register` → Zeile in `tokens`.
2. Chat: WebSocket `auth` mit diesem Token (`ChatWebSocket`). Jedes `chat.send` prüft den Token erneut (`resolvePlayerFromToken`).
3. HTTP-Mod-APIs: Header `x-auth-token` (`checkToken`). Liefert `req.playerId` und Anzeigenamen, **heute nicht** `mc_uuid` am Request — Etappe 2 muss `p.mc_uuid` in `checkToken`/`resolvePlayerFromToken` durchreichen.
4. Staff-Aktionen im Spiel (Perms, Mute, Delete, Report, Chatregeln-Admin): **neue** Routen unter `/moderation/...` mit demselben Mod-Token. Danach serverseitig: `players.mc_uuid` → Admin-Allowlist **oder** `staff_roles`.
5. **Kein** Cookie, **kein** Control-Center-Key, **kein** `/admin/unlock` im Spiel.

### 4.2 Website / Control Center

1. Login: Cookie `cclive_web` → `website_sessions` + `website_accounts` + `players.mc_uuid`.
2. Rolle: Admin = `admin-eligible.js` UUID-Set. Moderator = `staff_roles` (neu).
3. Freigabe: gemeinsamer `ADMIN_KEY` bzw. `MOD_KEY` in die jeweilige Session-Spalte (8h, Key-Hash, timing-safe), analog heutiges Admin-Unlock.
4. Moderator mit Admin-Key bleibt Moderator.
5. Jede geschützte Website-Aktion: Cookie + Rolle + gültiges Unlock. Kein Vertrauen in UI-Flags.

### 4.3 Rechteentzug

- Moderator entfernen: Zeile in `staff_roles` weg; alle Sessions des Accounts `mod_unlocked_until = NULL`.
- Ingame: nächste `/moderation/*`-Anfrage und jede Staff-WS-Aktion liest die Rolle neu → 403.
- Offene Staff-/Teamchat-Verbindungen: Rolle periodisch und bei Schreibversuchen prüfen, sonst kappen.
- Admin bleibt Allowlist, unabhängig von `staff_roles`.

---

## 5. Nachrichten-IDs (Ist und Soll)

Heute:

- INSERT ohne `id` in der Spaltenliste; `insertId` wird verworfen.
- Broadcast `chat.message`: `player`, `message`, `timestamp`, Farben, Ränge — **kein** `id`.
- `chat.ack`: `requestId`, `timestamp` — **kein** Persistenz-Key.
- Dedup Client: `player|message|timestamp`.
- History/Admin: dieselben drei Felder ohne `id`.

Soll (additiv, Etappe 3, hier verbindlich):

- Persistente `chat_messages.id` BIGINT PK AUTO_INCREMENT (anlegen falls Live-DDL fehlt).
- Nach INSERT: `id` in `chat.ack` **und** `chat.message`.
- Echo bindet Server-`id` nach Ack (sonst trifft `chat.deleted` die sichtbare Zeile nicht).
- `chat.deleted { id, placeholder }`.
- Öffentliche Selects: Placeholder, nie Original.
- `requestId` bleibt nur Korrelation Sender↔Ack.

---

## 6. Verbindliches Datenmodell (noch nicht anlegen)

Beziehungen: alles hängt an `players.id` / `players.mc_uuid`. Spielernamen nur Anzeige und Suche.

### 6.1 Erweiterung bestehender Tabellen

- `website_sessions.mod_unlocked_until DATETIME NULL`
- `website_sessions.mod_unlock_key_hash CHAR(64) NULL`
- `chat_messages.id` falls fehlend
- `chat_messages.deleted_at DATETIME NULL`
- `chat_messages.deleted_event_id BIGINT NULL` — optional, **kein ON DELETE CASCADE**

### 6.2 Neu

**staff_roles**

- `player_id` UNIQUE → `players.id`
- `role` nur `'moderator'`
- `granted_by_player_id`, `granted_at`
- Admins nicht hier; Allowlist bleibt.

**chat_message_redactions** (Staff only)

- Originaltext, Share-Rohdaten, `message_id`, `created_at`, `season_id`
- `event_id` optional, `ON DELETE SET NULL`
- Jede moderative Löschung, auch ohne Report.

**player_file_evidence** (Spielerakte)

- Ziel-`player_id`, Snapshot (Text, Zeit, message_id), Aktion, Begründung, `actor_player_id`, `source` (`minecraft`|`website`), `season_id`, `report_id` NULL bei Direktaktion.

**chat_sanctions** (Durchsetzung, seasonübergreifend)

- `player_id`, `kind` (`mute`|`ban`|`repeat_mute`)
- `starts_at`, `expires_at` NULL = permanent, `lifted_at`
- `reason_text`, `actor_player_id`, `source`, `created_at`
- `created_event_id` optional **ohne FK-CASCADE** (`ON DELETE SET NULL`)
- Enforcement liest **nur diese Tabelle**, nie `moderation_events`.

**moderation_events** (append-only, saisonal)

- `actor_player_id` nur aus Auth (Token-UUID oder Website-UUID), nie aus dem JSON-Body-Namen
- `source`, `kind`, `payload` JSON, `season_id`, `created_at`
- Kein UPDATE, kein DELETE außer Season-Job.

**chat_reports**

- `season_id`, Ziel, Melder, `status` `open|in_progress|closed`
- `owner_player_id`, `help_requested`
- `confirmed` NULL = ungeklärt, 0 = bewusst kein Verstoß, 1 = bestätigt
- `outcome` `confirmed_violation|no_violation|season_ended|…`
- `season_ended` + `confirmed NULL` zählt **nicht** als „nicht bestätigt“.

**chat_report_watchers** — UNIQUE `(report_id, player_id)`

**chat_report_evidence** — bis 10 vor + 10 nach, Snapshot, `capture_ended_reason`

**chat_report_watch_state** — 30 Minuten, endet bei Offline, kein Resume nach Relog

**team_chat_messages** — 24h, `role_snapshot` zum Sendezeitpunkt

**report_team_chat_messages** — `report_id`; schreiben nur Owner/Watcher/Admin; nach Close schreibgeschützt

**chat_filters** — Domains, Phrasen, Ausnahmen, Repeat-Werte; Version; Änderung → Event. Nutzer-API ohne `phrases`.

Ignore-Liste Etappe 4 lokal, keine Pflicht-DB.

### 6.3 Fremdschlüssel-Regel

Keine Kaskade von Events/Reports/Redactions auf `chat_sanctions`. Season-DELETE auf Protokoll darf aktive Bans und `reason_text` nicht entfernen oder leeren.

---

## 7. Bereinigungsregeln (mit der jeweiligen Tabelle, nicht erst Etappe 10)

| Daten | Wann | Job ab |
|---|---|---|
| Öffentlicher Chat `chat_messages` | 24h hart, inkl. Placeholder-Zeilen | existiert (stündlich) |
| Original nach Löschung (`chat_message_redactions`, `player_file_evidence`) | Ende der `season_id` | Etappe 3 |
| `moderation_events` | Ende der `season_id` | Etappe 3 |
| `chat_sanctions` aktiv | **nie** per Season-Job | — |
| `chat_sanctions` abgelaufen/aufgehoben | optional später, erst wenn Akte den Snapshot hat | Etappe 3/10 prüfen |
| Reports, Report-Evidence, Report-Teamchat | Ende der Report-`season_id`; vorher Close `season_ended` | Etappe 5/7 |
| Allgemeiner Teamchat | 24h | Etappe 7 |
| `leaderboard` / `player_stats` | unangetastet | — |
| Rollen, Websitekonten, Tokens | seasonunabhängig | — |

Season-Erkennung: Backend-Poll auf `seasons.is_current`, unabhängig von Modclients. Vor Löschen der alten Season: offene Reports `outcome=season_ended`, `confirmed=NULL`, Report-Chats freeze.

Öffentliche Selects und Broadcasts dürfen Originaltexte nach Delete **nie** wieder ausgeben, auch wenn `moderationToolsEnabled=false`.

---

## 8. Schnittstellenentwurf (additiv; noch nicht bauen)

### WebSocket Chat (bestehend erweitern)

- `chat.message` + `id`
- `chat.ack` + `id`
- neu `chat.deleted` `{ type, id, placeholder }`
- neu `chat.denied` `{ type, error, code: muted|banned|repeat_mute|filtered|blocked_glyph, requestId }`
- Ban: kein `chat.message` an betroffene Sockets
- Unbekannte Typen: heutige Clients ignorieren sie

### HTTP Ingame (Token)

- `GET/POST/DELETE /moderation/staff/moderators` — nur Admin-UUID
- `GET /moderation/staff/list` — Staff
- `GET /moderation/players/search?name=`
- später `/moderation/actions/*`, `/moderation/reports/*`

### HTTP Website (Cookie + Unlock)

- `POST /staff/unlock` — Key muss zur Rolle passen
- `GET/POST/DELETE /admin/staff/moderators` — nur Admin
- bestehende `/admin/*` unverändert in der Auth-Kette
- `GET /website/me` später `staffRole` ohne Key-Verrat

### Flags

- `moderationToolsEnabled` — neue UIs/Aktionen
- Enforcement von `chat_sanctions` und Placeholder: getrennt, Default an sobald Daten existieren

---

## 9. Uncommitteter Arbeitsstand (nicht verlieren)

Branch aktuell: `chat_moderation-update`.

Geändert u. a.: ChatManager, ChatWebSocket, ChatScreenMixin, ChatHudRenderMixin, Glyph-/Zeichen-Loader, PlayerIcon, Commands.

Untracked: `ChatBlockedGlyphs.java`, `ChatChannelRules.java`, `ChatDisplayDedup.java`, `ChatNameColors.java`, Tests, `admin_icon.png`, `development/`.

Etappe 2 darf diese Chat-Dateien nicht „nebenbei“ umbauen. Vor dem ersten Moderationscode in denselben Dateien: Chat-Stand sichern (Commit durch das Team).

---

## 10. Blocker und Etappe 2

**Blocker für Schema von Etappe 2/3:** Live-`SHOW CREATE TABLE` (Abschnitt 2), besonders `chat_messages.id` und `tokens`-PK.

**Kein Blocker für den Entwurf:** Auth-Wege, Tabellenliste, Bereinigung, WS-Felder, Flags sind festgelegt.

**Empfehlung, ob Etappe 2 beginnen kann:**

- Etappe 1 (Bestand + verbindlicher Entwurf) ist **abgeschlossen**, soweit der Code das hergibt.
- **Etappe 2 noch nicht beginnen** (keine Implementierung, kein ALTER), bis:
  1. die lesenden SQL-Ergebnisse vorliegen, und
  2. das Team Etappe 2 ausdrücklich freigibt.
- Optional parallel: Chat-WIP committen, ohne Moderationsschema.

Nach dieser Etappe: anhalten.

---

## 11. Live-DDL und Etappe-2-Start (6.11.2026)

Live-Tabellen sind bestätigt (Abschnitt 2). `chat_messages.id` existiert als `INT AUTO_INCREMENT` und bleibt in Etappe 2 unangetastet. Neue Rollen-`player_id`-Spalten: `BIGINT` wie `players.id`.

Etappe-2-Code ist umgesetzt: `staff_roles`, Session-Unlock-Spalten, Staff-APIs, Control-Center-Zugang, Ingame-`/cclive perms`, Tests. `chat_messages.id` bleibt ungenutzt. Kein Deployment.
