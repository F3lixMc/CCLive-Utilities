# Etappe 6 — Akten und Zählungen

**Status: Etappe 6 lokal abgeschlossen.** Kein Deployment, kein JAR, Etappe 7 nicht begonnen.

Stand: 13.11.2026. Etappe 5 bleibt wirksam. Neu ist nur das Speichern der abschließenden Person und die Lesewege für Akten, abgeschlossene Reports und Zählungen. Es gibt keine neue Tabelle und keinen Report-Chat.

## Spielerakte

Beim Öffnen zeigt die Übersicht ein Kachelraster. Darin stehen Spieler, zu denen bestätigte Verstöße oder belegte Direktaktionen vorliegen. Jede Kachel zeigt den Namen und die Anzahl der passenden Einträge. Ein Klick öffnet die Akte. „Alle Akten“ kehrt zum Raster zurück.

Die Filter wirken auf dieses Raster. Ein Moderatorname allein genügt und zeigt die Akten mit passenden Vorgängen dieser Person. Der Filter berücksichtigt die Zuständigkeit, die abschließende Person und den Akteur einer Direktaktion. Spielername und Moderator lassen sich kombinieren. Aktion und Grund grenzen dieselben Einträge zusätzlich ein.

Die Felder „Spielername“ und „Moderator“ in den Spielerakten und im Gesamtprotokoll schlagen beim Fokussieren Namen vor, auch bei leerem Feld. Spielernamen kommen aus registrierten CCLive-Konten. Moderatornamen kommen aus Staff-Rollen, Moderationsereignissen und den Zuständigen beziehungsweise abschließenden Personen der Reports. Tippen verfeinert die Vorschläge. Die Abfrage läuft asynchron. Eine ältere Antwort ersetzt keine neueren Treffer. Die Lesewege bleiben auf berechtigte Staff-Nutzer beschränkt.

Die geöffnete Akte löst ein vorhandenes verifiziertes Konto auf und legt keinen Spieler an. Die Verstoßliste hat zwei Quellen.

Reports gegen diesen Spieler mit `outcome = confirmed_violation` und `confirmed = 1`. Grund, Abschluss und Zeitpunkt kommen aus der Reportzeile. Vor- und Nachkontext kommen aus `chat_report_evidence`. Die Zuständigkeit `owner_player_id` bleibt ein eigenes Feld.

Direkte Aktionen aus `player_file_evidence` ohne Reportbezug. Das sind Löschungen sowie Mute oder Bann, bei denen eine Nachricht mitgesichert wurde. Angezeigt werden Aktion, Grund, Zeitpunkt, Akteur und der gespeicherte Snapshot.

`no_violation` und `season_ended` stehen nicht in der Verstoßliste. Fehlen zu einem bestätigten Report die Belegzeilen, bleibt der Abschluss sichtbar und trägt den Hinweis „Belege nicht mehr verfügbar“. Eine direkte Aktion, deren Belegzeile schon gelöscht wurde, hat keinen verbleibenden Akteneintrag.

## Abschließende Person

Neue Abschlüsse schreiben in `closeReport` die ID der tatsächlich abschließenden Person nach `chat_reports.closed_by_player_id`, zusammen mit Ausgang, `confirmed` und `closed_at`. Die Spalte kommt additiv in `ensureReportSchema`. `season_ended` ist ein Systemabschluss und lässt `closed_by_player_id` leer.

Die Anzeige bestimmt die abschließende Person in dieser Reihenfolge: gespeicherte `closed_by_player_id`, sonst der Akteur eines noch vorhandenen `report_closed`-Ereignisses, sonst der Text „Abschluss durch unbekannt“. Ein Season-Abschluss zeigt „Systemabschluss“.

## Eingereichte Reports

Zweiter Tab derselben Akte, gefiltert auf `reporter_player_id`. `season_ended` zählt nirgends mit.

- „Offen“: `open` und `in_progress`
- „Bestätigt“: `confirmed_violation`
- „Nicht bestätigt“: nur `no_violation`
- „Eingereicht“: die Summe dieser drei

Die Liste zeigt höchstens 100 Einträge: Ziel, Grund, Status beziehungsweise Ausgang und Zeitpunkt. Die Suche nach Moderator, Aktion und Grund filtert diese angezeigten Zeilen. Die vier Zähler kommen per SQL über alle passenden Zeilen des Melders. `season_ended` bleibt ausgeschlossen. Die Anzeigegrenze ändert die Summen nicht.

## Abgeschlossene Reports

Im Report-Bereich steht eine schreibgeschützte Liste mit „Verstoß bestätigt“, „Kein Verstoß“ und „Season beendet“. Ein Eintrag öffnet die vorhandene Reportansicht ohne Übernehmen, Abschließen, Freigeben oder Unterstützen. Zuständigkeit und abschließende Person werden getrennt angezeigt. Der Eingang bleibt auf `status != closed` beschränkt. `readReport` liest einen abgeschlossenen Fall weiter.

## Moderatorübersicht und Gesamtprotokoll

Die Zähler kommen nur aus `moderation_events`, über alle noch vorhandenen relevanten Ereignisse. Die Grenze der sichtbaren Protokollzeilen ändert die Summen nicht.

- „Bearbeitete Reports“: je Moderator und `payload.reportId` höchstens einmal über `report_claimed`, `report_join` und `report_closed`.
- „Bestätigte Reports“: `report_closed` mit bestätigtem Abschluss, gezählt für den Akteur dieses Ereignisses.
- „Ausgeführte Aktionen“: `mute`, `ban`, `message_delete`, `unmute` und `unban`.

Die verfügbaren Moderatoren erscheinen direkt als Kacheln mit den drei Zählern. Darunter listet die Übersicht abgeschlossene Reports aller drei Ausgänge. Ein Klick öffnet denselben schreibgeschützten Fall. Das Gesamtprotokoll hat keine Schreibaktionen und sucht nach Spieler, Moderator, Aktion und Grund in den neuesten 1000 Ereignissen. Dieselbe Suche filtert die Spielerakte. Die Protokollliste im Aktionsformular bleibt unverändert und lädt weiter die neuesten 40 Ereignisse.

Nach einem Season-Job fehlen die Ereignisse der beendeten Season. Die Zähler beschreiben dann nur noch vorhandene Ereignisse. Die abschließende Person bleibt auf der Reportzeile stehen.

## Control Center

Neue Lesewege: `/staff/files`, `/staff/files/tiles`, `/staff/suggest/players`, `/staff/suggest/moderators`, `/staff/reports/closed`, `/staff/stats/moderators` und die Suche auf `/staff/events`. Dieselben Lesewege liegen für Staff im Spiel unter `/moderation/...`. Die Website prüft `checkStaff`. Im Spiel laufen Token und Rollenprüfung. Der Handelnde kommt aus Sitzung oder Token. Ein normaler Spieler und eine Sitzung ohne Staff-Freigabe erhalten keine Akte.

Navigation in Admin und Moderatoransicht: „Spielerakten“ und „Moderatorübersicht“. Suche, Akten und Protokoll nutzen Umbruch und die scrollbare Tabellenhöhe. Die Kachel „Laufende Bestrafungen“ bleibt Platzhalter. Cache-Stand der geänderten Oberflächen: `20261113-etappe6d`.

## Oberflächenkorrektur

„Filtern“ und „Suchen“ stehen auf der Höhe der Eingabefelder. Die Buttons bleiben kompakt und strecken sich im Raster nicht über die Beschriftung. Hover hellt den grünen Button leicht auf. Der Tastaturfokus zeigt einen klaren Rahmen. Bei wenig Platz rutscht die Aktionsgruppe in die nächste Zeile.

Eine Vorschlagszeile ist über die ganze Breite klickbar, auch auf dem freien Platz neben dem Namen. Die Hervorhebung bleibt in der Zeile. Ein Klick übernimmt den Namen. Das gilt für Spieler- und Moderatorvorschläge in den Akten und im Gesamtprotokoll.

Neben „Filtern“ und „Suchen“ steht „Leeren“ in gedämpftem Rot. Der Button leert die vier Felder des jeweiligen Bereichs, schließt die Vorschläge und lädt die ungefilterte Ansicht. Eine noch ausstehende Suchantwort schreibt die geleerten Felder nicht zurück und öffnet die Vorschläge nicht wieder.

Das zentrierte Moderatorpanel nutzt auf breiten Fenstern bis zu 1200 Pixel. Kurze Protokolleinträge bleiben dort in einer Zeile. Auf schmalen Fenstern brechen lange Texte im Panel um. Das Protokoll bleibt in der Höhe scrollbar.

## Tatsächlich ausgeführte Tests

Lokal, ohne Minecraft und ohne JAR. Es wurden keine Java-Dateien geändert, deshalb kein Gradle-Lauf.

Aus `backend_server`:

`node ..\development\website\tests\chat-moderation.test.js`

Ausgabe: `chat-moderation tests ok`. Die zwei bestehenden Meldungen `sanction live update error` gehören zum schon vorhandenen Test, der einen fehlgeschlagenen Live-Hinweis absichtlich auslöst.

Der Lauf prüft die bisherigen Report-, Watch- und Season-Fälle weiter. Neu: ein bestätigter Report steht in der Akte des Ziels, `no_violation` und `season_ended` nicht. Nach gelöschten Belegen bleibt der bestätigte Abschluss mit `evidenceMissing`. Eine direkte Löschung mit Beleg erscheint ohne Report. Die Zähler des Melders trennen eingereicht, bestätigt, offen und nicht bestätigt und lassen `season_ended` aus. Bei mehr als 100 Reports dieses Melders bleibt die Liste bei 100 Zeilen, die vier Summen zählen alle passenden Zeilen, einschließlich eines älteren bestätigten Abschlusses. Dieselbe Person als Übernehmer, Unterstützer und Abschließender zählt einmal. Ein bestätigter Abschluss gehört dem Abschließenden. Moderator A übernimmt, Admin B schließt ab. Nach dem Entfernen des `report_closed`-Ereignisses zeigt die Akte weiter B, A bleibt nur zuständig, und der Zähler von B fällt auf die noch vorhandenen Ereignisse zurück. Ein relevantes Ereignis hinter den neuesten 5.000 bleibt im Moderatorzähler und fehlt im sichtbaren Protokollfenster. Dieselbe Person übernimmt einen Report, gibt ihn frei, übernimmt ihn erneut und schließt ihn ab. „Bearbeitete Reports“ steigt dabei um eins. Die Aktenübersicht zeigt Plain als Kachel und lässt ModUser aus, weil dort nur „Kein Verstoß“ liegt. Der Moderatorfilter findet Plain über die Zuständigkeit und über den Abschluss. Spielername und Moderator zusammen grenzen die Kachel ein. Leere Vorschläge enthalten registrierte Konten und lassen ein Konto ohne UUID aus. `pl` lässt nur passende Spielernamen übrig, `mod` nur passende Moderatornamen. Ohne Anmeldung bleiben die Kacheln bei 401, ohne Staff-Rolle die Vorschläge bei 403. Ein älterer Abschluss ohne Spalte und ohne Ereignis zeigt „Abschluss durch unbekannt“. Ein normaler Spieler erhält 403. Eine Browser-Anfrage ohne Anmeldung und eine abgelaufene Staff-Freigabe erhalten 401. Ein Staff-Token liest die Akte.

Dieselbe Testdatei prüft die Markup-Stellen für „Leeren“, die Abbruchstellen für ausstehende Antworten und die Panelbreite 1200 Pixel. Ausgabe weiterhin: `chat-moderation tests ok`.

Zusätzlich eine lokale Vorschau der Admin- und Moderatoransicht, ohne Minecraft und ohne den laufenden Server. Geprüft bei 1440 und 390 Pixel Breite:

- In beiden Ansichten sitzen „Filtern“, „Suchen“ und „Leeren“ auf der Höhe der Eingaben. Die Buttons bleiben kompakt. Bei 390 Pixel Breite wechselt die Aktionsgruppe in die nächste Zeile und bleibt im Fenster.
- Die Vorschlagszeile für Spieler und Moderatoren füllt die Feldspalte. Hover färbt die ganze Zeile, einschließlich des freien Platzes neben dem Namen. Ein Klick auf diesen freien Platz übernimmt den Namen in Akten und Gesamtprotokoll.
- „Leeren“ leert alle vier Felder und schließt die Listen. Eine zuvor zurückgehaltene Suchantwort stellt weder Text noch Vorschläge wieder her.
- Bei 1440 Pixel Breite ist das Moderatorpanel 1200 Pixel breit. Kurze Protokollzellen bleiben einzeilig, ein langer Text bricht in der Inhaltsspalte um, und das Protokoll hat einen vertikalen Scrollbalken. Bei 390 Pixel Breite bleibt derselbe lange Text in der Zelle, das Protokoll ragt nicht aus dem Panel und scrollt weiter vertikal.

Minecraft wurde nicht gestartet. Gegen den laufenden Server wurde nichts hochgeladen.

## Offene Live-Nachtests

Nicht im Spiel und nicht gegen den laufenden Server:

- Admin schließt einen von einem Moderator übernommenen Report mit „Verstoß bestätigt“. Die Akte des Ziels zeigt den Moderator als Zuständigen und den Admin als abschließende Person.
- Der Tab „Eingereichte Reports“ trennt die vier Zähler. Ein Season-Abschluss erhöht keinen davon.
- „Abgeschlossene Reports“ zeigt die drei Ausgänge. Der geöffnete Fall hat keine Buttons zum Übernehmen, Abschließen, Freigeben oder Unterstützen.
- Die Moderatorübersicht zählt bearbeitete Reports, bestätigte Abschlüsse und ausgeführte Aktionen. Das Gesamtprotokoll filtert nach Spieler, Moderator, Aktion und Grund. Dieselbe Suche filtert die Akte.
- In einem schmalen Fenster bleiben Akten, Suche und Protokoll im Panel. „Laufende Bestrafungen“ bleibt der Platzhalter.
- Ein normaler Spieler und eine Sitzung ohne Staff-Freigabe öffnen keine Akte.

## Upload

Diese Serverdateien. Kein Client-Build in diesem Schritt. Diese Dokumentation und die Tests nicht hochladen.

### `backend_server/website_backend`

- `backend_server/website_backend/moderation-stats.js` — Aktenkacheln, Moderatorfilter, Vorschläge, eingereichte Zähler über alle Zeilen, Moderatorzähler über alle relevanten Ereignisse
- `backend_server/website_backend/staff-roles.js` — Spielernamen-Vorschläge auch bei leerem oder kurzem Suchtext
- `backend_server/website_backend/chat-reports.js` — Spalte `closed_by_player_id`, Abschluss speichert die Person, Anzeige der abschließenden Person
- `backend_server/website_backend/chat-moderation.js` — Protokollsuche und Akteur-ID an den Ereignissen
- `backend_server/website_backend/moderation-http.js` — Kacheln, Namensvorschläge und die übrigen Staff-Lesewege für Website und Spiel

### `backend_server/public`

- `backend_server/public/staff-moderation.js` — Aktenkacheln, Livesuche, „Leeren“ und Schutz vor veralteten Suchantworten
- `backend_server/public/staff-home.html` — Navigation, Filteraktionen und Vorschlagslisten
- `backend_server/public/admin-workspace.js` — dieselben Filteraktionen in der Admin-Ansicht
- `backend_server/public/admin-script.js` — die neuen Bereiche verdecken die übrigen Panels
- `backend_server/public/admin-style.css` — kompakte Filterbuttons, zeilenbreite Vorschläge, Panelbreite 1200 Pixel, umbrechendes Protokoll

### `backend_server/admin_ui`

- `backend_server/admin_ui/admin.html` — Navigationseinträge und Cache-Stand `20261113-etappe6d`

## Upload dieser Korrekturrunde

Nur die Dateien dieser Oberflächenkorrektur. Kein Client-Build. Diese Dokumentation und die Tests nicht hochladen.

### `backend_server/public`

- `backend_server/public/admin-style.css` — Filterbuttons, Vorschlagszeilen, Moderatorpanel und Protokollumbruch
- `backend_server/public/staff-moderation.js` — „Leeren“ und das Verwerfen ausstehender Suchantworten
- `backend_server/public/staff-home.html` — Filteraktionen der Moderatoransicht, Cache-Stand `20261113-etappe6d`
- `backend_server/public/admin-workspace.js` — dieselben Filteraktionen in der Admin-Ansicht

### `backend_server/admin_ui`

- `backend_server/admin_ui/admin.html` — Cache-Stand `20261113-etappe6d` für Stylesheet, Workspace und Staff-Skript
