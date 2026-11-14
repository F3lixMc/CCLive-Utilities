# Etappe 7 — Interne Chats

**Status: Etappe 7 lokal abgeschlossen.** Kein Deployment, kein JAR, keine Java-Änderung. Etappe 6 bleibt wirksam.

Stand: 13.11.2026. Interne Nachrichten laufen nur über die bestehende Website-Sitzung und den Kanal `/ws/staff-chat`. Der öffentliche Kanal `/ws/chat` bleibt beim CCLive-Chat.

## Zugang

Cookie `cclive_web`, `checkStaff` und `/ws/staff-chat`. Es gibt keinen Ingame-Token für den Teamchat und keine Route unter `/moderation` dafür. Bei Verbindung, vor dem Senden und im 5-Sekunden-Takt prüft der Kanal Freigabe und Rolle neu. Fehlt eine von beiden, schließt die Verbindung mit 4401 und sendet keinen Inhalt.

## Allgemeiner Teamchat

Tabelle `team_chat_messages`: Spieler, gespeichertes Rollenpräfix, Text, Zeitpunkt. Das Präfix entsteht beim Insert aus der Rolle dieses Augenblicks, `[Admin]` oder `[Mod]`. Eine spätere Rollenänderung schreibt alte Zeilen nicht um.

In der Übersicht von Admin und Moderator steht der Teamchat neben dem öffentlichen Livechat. Beide Protokolle sind 280 Pixel hoch und scrollen intern. Unter 900 Pixel Breite stehen sie untereinander. Das Aktionsformular und das kleine Aktionsprotokoll bleiben unter dem öffentlichen Chat.

`MODERATION_TOOLS_ENABLED` mit `0`, `false` oder `off` blockiert nur neue Teamnachrichten. Vorhandene Zeilen bleiben lesbar. Die stündliche Löschung entfernt `team_chat_messages` nach 24 Stunden, zusammen mit der bestehenden Löschung von `chat_messages`. Report-Chats bleiben dabei stehen.

## Chat pro Report

Tabelle `chat_report_messages` mit `report_id` und `season_id`. Dieselbe Präfixregel.

Ein Button „Teamchat“ öffnet das Panel rechts neben der Reportkarte. Unter 800 Pixel Breite steht es darunter. Bei einem übernommenen Report dürfen der zuständige Bearbeiter, eingetragene Unterstützer und Admins lesen und schreiben, auch wenn noch keine Nachricht existiert. Ein unbeteiligter Moderator sieht den Report weiter und keinen Chatinhalt.

Nach Abschluss bleibt der Chat für dieselben Personen lesbar. `POST` antwortet mit 409. Ist der abgeschlossene Verlauf leer, ist der Button deaktiviert. Ein offener, leerer Chat bleibt bedienbar.

Das Speichern prüft Status und Beteiligung gemeinsam mit dem Insert in einer Transaktion. Der erste Schritt sperrt die Reportzeile mit `SELECT ... FROM chat_reports WHERE id = ? FOR UPDATE`. Dieselbe Sperre ziehen Abschluss und Freigabe, einschließlich des Löschens der Unterstützer. Die Duplikatsperre `cclive:report:Melder:Ziel` bleibt davon getrennt. Wird Abschluss oder Freigabe zuerst wirksam, sieht der danach laufende Schreibversuch den neuen Zustand und wird abgelehnt. Es entsteht keine Nachrichtenzeile. Die Live-Verteilung startet erst, nachdem die Transaktion die Nachricht gespeichert hat.

Report-Nachrichten gehen nur an Verbindungen, deren Beteiligung in diesem Moment noch gilt. Freigeben löscht Zuständigkeit und Unterstützer. Eine noch offene Unterstützer-Verbindung erhält die nächste Report-Nachricht nicht, bekommt `report.chat.denied` ohne Nachrichtentext und verliert die Report-Anmeldung. Die Staff-Verbindung bleibt für den allgemeinen Teamchat offen, solange die Sitzung gültig ist.

Report-Chats laufen in `deleteEndedReportArtifacts` mit dem vorhandenen Season-Schutz mit: nur eine eindeutig beendete Season, `is_current = 0`, gesetzter und zurückliegender `start_at`. Eine unklare aktuelle Season überspringt den ganzen Job.

Cache-Stand der geänderten Oberflächen: `20261113-etappe7`.

## Tatsächlich ausgeführte Tests

Lokal, ohne Minecraft und ohne JAR. Es wurden keine Java-Dateien geändert, deshalb kein Gradle-Lauf.

Aus `backend_server`:

`node ..\development\website\tests\chat-moderation.test.js`

Ausgabe: `chat-moderation tests ok`. Die zwei bestehenden Meldungen `sanction live update error` gehören zum schon vorhandenen Test, der einen fehlgeschlagenen Live-Hinweis absichtlich auslöst.

Der Lauf prüft die bisherigen Report-, Watch- und Season-Fälle weiter. Neu: Admin, zuständiger Moderator, Unterstützer und unbeteiligter Moderator. Anfragen ohne Staff-Freigabe und mit abgelaufener Freigabe erhalten 401. Ein offener leerer Chat ist beschreibbar. Ein leerer abgeschlossener Verlauf liefert `count = 0` und `writable = false`; der Button ist im Skript deaktiviert. Schreiben nach Abschluss liefert 409, der vorhandene Verlauf bleibt lesbar. Das Präfix `[Admin]` bleibt nach einer späteren Rollenänderung in der Zeile stehen. `MODERATION_TOOLS_ENABLED=0` blockiert neue Teamnachrichten und lässt den Verlauf lesbar. Zwei parallele Fälle halten den Schreibversuch, bis Abschluss beziehungsweise Freigabe committed hat. Danach ist der Schreibversuch abgelehnt, die Nachricht fehlt und es wurde nichts live verteilt. Ein erfolgreicher Schreibweg verteilt erst nach dem gespeicherten Insert. Die 24-Stunden-Löschung entfernt nur alte Teamnachrichten. Eine unklare aktuelle Season lässt Report-Chats stehen. Eine eindeutig beendete Season löscht deren Report-Chat. Rechteentzug und abgelaufene Freigabe schließen eine offene Verbindung mit 4401 und senden keinen Inhalt. Freigeben, während ein Unterstützer verbunden ist, schickt `report.chat.denied` ohne Text, nimmt die Report-Anmeldung weg und lässt den allgemeinen Teamchat auf derselben Verbindung zu.

Minecraft wurde nicht gestartet. Gegen den laufenden Server wurde nichts hochgeladen.

## Offene Live-Nachtests

Nicht im Spiel und nicht gegen den laufenden Server:

- In einem breiten Fenster stehen Teamchat und Livechat nebeneinander, gleich hoch, und scrollen intern. Unter etwa 800 Pixel Breite stehen sie untereinander. Das Aktionsformular bleibt darunter.
- Der Button „Teamchat“ öffnet das Panel rechts neben der Reportkarte und darunter auf einem schmalen Fenster. Ein offener leerer Chat lässt die erste Nachricht zu. Ein abgeschlossener leerer Verlauf hat einen deaktivierten Button.
- Ein unbeteiligter Moderator sieht den Report und keinen Chatinhalt. Nach Abschluss lesen dieselben Personen weiter, Schreiben liefert 409.
- Freigeben, während ein Unterstützer verbunden ist, beendet nur die Report-Anmeldung. Der allgemeine Teamchat bleibt auf der Verbindung.
- Eine entzogene Rolle oder eine abgelaufene Freigabe schließt die offene Staff-Verbindung mit 4401.
- Nach 24 Stunden ist der allgemeine Teamchat weg. Report-Chats folgen der Season-Löschung.

## Upload

Diese Serverdateien. Kein Client-Build in diesem Schritt. Diese Dokumentation und die Tests nicht hochladen.

### `backend_server/website_backend`

- `backend_server/website_backend/team-chat.js` — Tabellen, Präfix, Team- und Report-Chat, gemeinsame Reportzeilensperre beim Schreiben
- `backend_server/website_backend/chat-reports.js` — dieselbe Sperre bei Abschluss und Freigabe, Zugriffsfeld `teamChat`, Season-Löschung der Report-Chats
- `backend_server/website_backend/chat-moderation.js` — Schema der Teamchats und Zähler `reportChats` im Season-Job
- `backend_server/website_backend/moderation-http.js` — `GET`/`POST` für `/staff/team-chat` und `/staff/reports/:id/chat`, Live-Verteilung erst nach dem Speichern
- `backend_server/website_backend/staff-chat-feed.js` — Freigabe, Rolle und Report-Beteiligung auch bei offenen Verbindungen, `report.chat.watch` und `report.chat.denied`
- `backend_server/server.js` — Verteilung über `/ws/staff-chat`, 24-Stunden-Löschung des allgemeinen Teamchats

### `backend_server/public`

- `backend_server/public/staff-moderation.js` — Teamchat, Report-Button, eine Staff-Verbindung, Button bei leerem abgeschlossenem Verlauf deaktiviert
- `backend_server/public/staff-home.html` — Teamchat neben dem Livechat, Cache-Stand `20261113-etappe7`
- `backend_server/public/admin-workspace.js` — derselbe Teamchat in der Admin-Übersicht
- `backend_server/public/admin-style.css` — gleiche Protokollhöhe 280 Pixel, Report-Chat rechts, untereinander auf schmalen Fenstern

### `backend_server/admin_ui`

- `backend_server/admin_ui/admin.html` — Cache-Stand `20261113-etappe7` für Stylesheet, Workspace und Staff-Skript
