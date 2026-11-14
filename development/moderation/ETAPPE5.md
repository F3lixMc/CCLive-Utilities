# Etappe 5 — Reports

**Status: Etappe 5 abgeschlossen.** Übernahme und beide Report-Abschlüsse wurden live bestätigt. Der abschließende Layout-Nachtest in der Moderatoransicht ist bestanden.

Stand: 13.11.2026. Etappe 4 bleibt unverändert wirksam. Der Melder kommt nur aus dem Mod-Token. Der Spielername im Body ist das Ziel.

„Melden“ im Rechtsklick-Menü sendet Spieler und, falls vorhanden, die Nachrichten-ID dieser Zeile. Ohne ID bleibt die Meldung möglich. Ein laufender Klick für dieselbe Zeile sendet nicht doppelt. `/cclive report <spieler> [grund]` nutzt denselben Weg. Der Grund ist optional und darf höchstens 400 Zeichen haben. Erfolg und Ablehnung erscheinen im Client-Chat.

`POST /moderation/reports` prüft nur den Spieler-Token. Das Ziel muss ein vorhandenes verifiziertes Konto sein. Es wird kein Spieler angelegt. Das eigene Konto wird abgelehnt. Eine mitgegebene Nachrichten-ID muss eine öffentliche CCLive-Nachricht genau dieses Ziels sein. Sind die Moderationswerkzeuge aus, blockiert das nur neue Meldungen.

Dieselbe meldende Person und dasselbe Ziel innerhalb von 15 Minuten werden einem noch offenen Report zugeordnet. Prüfung und Anlegen laufen unter derselben Datenbanksperre `GET_LOCK` für dieses Paar, auch wenn keine Nachrichten-ID mitkommt. Die Sperre hängt nicht an einer Chatzeile. Zwei gleichzeitige Meldungen erzeugen genau einen offenen Report. Ein abweichender Grund wird als `report_duplicate` protokolliert. Nachbeobachtung und Belege starten nicht neu. Die Bestätigung sagt, dass die Meldung schon vorliegt.

Beim Anlegen werden bis zu zehn öffentliche Nachrichten des Ziels vor der Meldung kopiert, einschließlich Text und Zeitpunkt. Die angeklickte Nachricht belegt einen dieser zehn Plätze, auch wenn sie älter als die letzten zehn ist, und erscheint nur einmal. Die stündliche Löschung von `chat_messages` nach 24 Stunden fasst diese Kopien nicht an.

Neue öffentliche Nachrichten des Ziels laufen in die Nachbeobachtung, höchstens zehn Stück. Sie endet außerdem nach 30 Minuten oder wenn das Ziel offline geht. Die Gründe sind `limit`, `timeout` und `offline`. Status, Frist und Anzahl werden mit `FOR UPDATE` gelesen. Beleg, Zähler und Endegrund werden in derselben Transaktion geschrieben. Ist die Watch bereits wegen Offline, Zeitlimit oder Anzahl beendet, entsteht kein weiterer Beleg. Bei zwei gleichzeitigen Nachrichten und neun vorhandenen Belegen wird genau eine zum zehnten Beleg. Offline kommt von `POST /presence/leave` und vom Entfernen eines Heartbeats, der älter als 90 Sekunden ist. Ein späterer Login setzt den Endgrund nicht zurück. Der 30-Minuten-Stopp läuft beim Speichern einer Nachricht, beim Presence-Lauf und minütlich auf dem Server.

„Spieler Reports“ ist ein eigener Bereich. In der Admin-Übersicht heißt die Kachel „Eingegangene Reports“ und zeigt die Anzahl der offenen und übernommenen Reports. Ein Klick öffnet den Eingang. Dieselbe Kachel steht auf dem Moderator-Start. Der Bereich liegt in der Navigation beider Ansichten: im Admin unter „Eingegangene Reports“, beim Moderator unter „Bereich wählen“. Die Kachel „Laufende Bestrafungen“ bleibt der spätere Platzhalter. Der Eingang zeigt nur Reports, die nicht `closed` sind. Jede Karte zeigt gemeldeten Spieler, meldenden Spieler, Grund, Vor- und Nachkontext und den zuständigen Moderator. Jedes Staff-Mitglied darf den Kontext auch bei fremder Zuständigkeit lesen.

Die Übernahme setzt `in_progress` und den Zuständigen nur, solange der Report `open` ist und noch niemandem gehört. Trifft das nicht zu, bleibt die Zeile unverändert, auch für Admins. Abschließen darf der Zuständige, ein eingetragener Unterstützer oder ein Admin. „Verstoß bestätigt“ speichert `confirmed_violation` und `confirmed = 1`. „Kein Verstoß“ speichert `no_violation` und `confirmed = 0`. Danach verschwindet der Report aus dem Eingang. Es entsteht kein Eintrag in `player_file_evidence` und keine Zählung. Freigeben setzt wieder `open`, leert Zuständigkeit und Unterstützer und schaltet die Hilfe aus. Unterstützung anfragen setzt nur `help_requested`. „Unterstützen“ schreibt danach einen Unterstützer. Die Report-Buttons sind kompakt, haben Abstand und brechen bei Bedarf in die nächste Zeile um. Es gibt kein Chatfenster und kein Senden.

Das Protokoll zeigt ungefähr zehn Einträge auf einmal. Weitere Einträge bleiben durch vertikales Scrollen erreichbar. Die Überschrift steht über dem Bereich, die Spaltenköpfe bleiben beim Scrollen stehen. Lange Inhalte, Namen und Aktionsbezeichnungen brechen in der Zelle um. Bei einem Report-Abschluss steht in „Inhalt“ der gespeicherte Ausgang: `no_violation` als „Kein Verstoß“, `confirmed_violation` als „Verstoß bestätigt“, dazu die Report-ID aus dem Ereignis. Abgeschlossene Reports und Spielerakten bleiben Etappe 6.

Dieselben Aktionen liegen für die Website unter `/staff/reports` und für Staff im Spiel unter `/moderation/reports`. Der Handelnde kommt aus der Sitzung oder dem Token.

Eine eindeutig beendete Season schließt ihre offenen und übernommenen Reports mit `season_ended` und `confirmed = NULL`, beendet die Nachbeobachtung und löscht danach nur Belege, Watch und Unterstützer dieser Season. Die Reportzeile bleibt. Ist die Season unklar, bleibt der gesamte Job stehen, einschließlich der Reports. Die aktuelle Season bleibt stehen.

## Tatsächlich ausgeführte Tests

Lokal, ohne Minecraft und ohne JAR.

Aus `backend_server`:

`node ..\development\website\tests\chat-moderation.test.js`

Ausgabe: `chat-moderation tests ok`. Die zwei bestehenden Meldungen `sanction live update error` gehören zum schon vorhandenen Test, der einen fehlgeschlagenen Live-Hinweis absichtlich auslöst.

Die Tests prüfen zwei gleichzeitige Übernahmen mit einem Gewinner, das Schließen durch einen fremden Moderator, durch einen Admin und durch einen Unterstützer, beide Abschlüsse mit `confirmed_violation`/`confirmed = 1` und `no_violation`/`confirmed = 0`, ein Duplikat innerhalb von 15 Minuten, eine angeklickte Nachricht außerhalb der letzten zehn genau einmal im Vorkontext, die elfte Nachbeobachtung, 30 Minuten, Offline und eine spätere Nachricht danach, den stehenbleibenden Snapshot nach gelöschter Chatzeile und den Season-Job: `season_ended` mit `confirmed = NULL`, gelöschte Belege und Watch nur der beendeten Season, Reportzeile bleibt, die aktuelle Season bleibt offen. Ein unklarer Season-Stand überspringt den Job. Zwei gleichzeitige Meldungen ohne Nachrichten-ID erzeugen einen offenen Report, die zweite wird zugeordnet, und die Sperre kommt nicht aus `chat_messages`. Zwei gleichzeitige Nachrichten bei neun Belegen speichern genau einen zehnten Beleg und beenden die Watch mit `limit`; eine weitere Nachricht bleibt ohne Beleg. Zwei Nachrichten nach Ablauf der Frist speichern keinen Beleg. Eine Nachricht parallel zum Offline-Ende und eine weitere danach schreiben keinen Beleg mehr, sobald der Endgrund `offline` steht.

Dieser Lauf ist nach der Control-Center-Korrektur erneut ausgeführt. Er prüft zusätzlich die Kachel „Eingegangene Reports“, die Navigation, das scrollbare Protokoll, die gespeicherten Abschlusstexte und den Button „Unterstützen“. Der Gradle-Lauf darunter bleibt der vom Menü-Label.

Zusätzlich eine lokale Ansicht mit zwölf Protokolleinträgen, langen Zellinhalten und den beiden Abschlüssen „Kein Verstoß · Report #11“ und „Verstoß bestätigt · Report #12“. Auf der breiten Moderatoransicht ist das Panel 720 px breit, das Protokoll 584 px hoch und der Inhalt 2133 px, also scrollbar, ohne seitlichen Überstand. Bei 360 px Breite bleibt das Admin-Protokoll bei 312 px innerhalb des Panels, weitere Zeilen sind nur durch Scrollen erreichbar. Die Kachel öffnet den Report-Eingang. Die Buttons brechen in drei Zeilen um, ihre Rahmen überschneiden sich nicht, und keiner ragt über das Panel hinaus.

`gradlew test --offline --tests net.felix.chat.ChatContextTargetTest`

BUILD SUCCESSFUL in 21s. `compileJava` lief mit. Das Menü erwartet das Label „Melden“.

Minecraft wurde nicht gestartet. Gegen den laufenden Server wurde nichts hochgeladen.

## Offene Live-Nachtests

Nicht im Spiel und nicht gegen den laufenden Server:

- Rechtsklick „Melden“ an einer Zeile mit Nachrichten-ID und an einer alten Zeile ohne ID. Die Bestätigung erscheint im Chat. Ein zweiter Klick, solange die Anfrage läuft, sendet nicht noch einmal.
- `/cclive report <spieler>` und `/cclive report <spieler> <grund>`.
- Eigenes Konto, unbekannter Name und eine Nachricht eines anderen Spielers werden abgelehnt. Innerhalb von 15 Minuten sagt die zweite Meldung, dass sie zugeordnet wurde.
- Auf dem laufenden Server zeigt die Kachel „Eingegangene Reports“ die Anzahl, und Klick wie Navigation öffnen den Eingang in Admin- und Moderatoransicht. „Verstoß bestätigt“ und „Kein Verstoß“ erscheinen im Protokoll mit der Report-ID. „Unterstützen“ öffnet keinen Chat.
- Nachbeobachtung endet nach zehn weiteren Nachrichten, nach 30 Minuten oder nach Verlassen. Ein erneutes Einloggen setzt sie nicht fort.

## Upload

Diese Serverdateien. Den Client erst mit dem nächsten Mod-Build. Diese Dokumentation und die Tests nicht hochladen.

- `backend_server/server.js` — Offline beendet die Nachbeobachtung, minütlicher 30-Minuten-Lauf
- `backend_server/website_backend/chat-reports.js` — Reports, Belege, Watch, Eingang, Anzahl offener Reports, Season
- `backend_server/website_backend/chat-moderation.js` — Schema, Nachbeobachtung beim Speichern, Season-Abschluss
- `backend_server/website_backend/moderation-http.js` — Melden mit Spieler-Token, Staff-Aktionen für Website und Spiel
- `backend_server/public/staff-moderation.js` — eigener Eingang, Abschlussanzeige, Button „Unterstützen“
- `backend_server/public/staff-home.html` — Navigation, Kachel, scrollbares Protokoll
- `backend_server/public/admin-workspace.js` — Kachel „Eingegangene Reports“ und eigener Bereich
- `backend_server/public/admin-script.js` — Bereich in der Admin-Navigation
- `backend_server/public/admin-style.css` — Protokollhöhe, Umbruch, Buttonabstand
- `backend_server/admin_ui/admin.html` — Navigationseintrag und Cache-Stand

### Geänderte Dateien

- `src/main/java/net/felix/chat/ChatReport.java` — Meldung mit Spieler-Token
- `src/main/java/net/felix/chat/ChatContextMenu.java` — Eintrag „Melden“
- `src/main/java/net/felix/commands/CCLiveCommands.java` — `/cclive report` und Hilfezeile
- `src/test/java/net/felix/chat/ChatContextTargetTest.java` — Label „Melden“
- `development/website/tests/chat-moderation.test.js` — Reports, Watch und Season
