# Etappe 4 — Kontextmenü und Ignore

**Status: Abgeschlossen.** Der Live-Nachtest ist bestätigt. Bei geöffnetem Kontextmenü bleibt der Playerstat-Hover ausgeblendet und erscheint nach dem Schließen wieder.

Stand: 13.11.2026. Etappe 3 bleibt unverändert wirksam. Etappe 5 ist nicht begonnen. Kein Deployment, kein Mod-JAR.

Rechtsklick auf das Präfix `[CCLive]`, den Spielernamen oder `>>` öffnet ein Menü im offenen Chat. Der Treffer kommt aus dem tatsächlich gezeichneten Chat: dieselbe Zeilengeometrie wie beim Rendern, einschließlich umbrochener Zeilen. Der Vanilla-Klickfinder liefert nur Stile mit ClickEvent oder Insertion, deshalb fiel der Name und `>>` bisher durch. Der Name hat weiterhin kein ClickEvent und keine Insertion. Hover-Stats bleiben am Namen, am Präfix und an `>>`. Nachrichtentext, Icons und Shares bleiben ohne dieses Menü. Der bisherige Linksklick-Pfad für Item- und Build-Shares wird dafür nicht abgefangen.

Name und Präfix lösen Spieler und Nachrichten-ID aus derselben Zeile auf, nicht aus der letzten Zeile desselben Spielers. Das gilt für ein einzelnes Buchstabenstück einer umbrochenen Zeile und nach dem Ersetzen durch den Löschplatzhalter. Eine Altzeile ohne `cclive-msg:` lässt Ignore zu, weil sie weiterhin das Präfix `[CCLive]` trägt. „Nachricht löschen“ fehlt dort. Mute und Chatbann übernehmen nur den Namen. Eine Serverzeile im Format `Name >> …` ohne dieses Präfix wird nicht zugeordnet und beim Ignorieren nicht entfernt.

Das Menü gehört zum Chat-Screen, der es geöffnet hat. Schließt dieser Screen, etwa durch Escape, oder wird er gewechselt, ist das Menü zu. Ein neu geöffneter Chat zeigt es nicht wieder.

Normale Spieler sehen „Spieler ignorieren“ oder „Nicht mehr ignorieren“ und „Melden (noch nicht verfügbar)“. Auf der eigenen Chatzeile fehlt „Spieler ignorieren“. Der Meldeeintrag löst keine Anfrage aus. Staff sieht zusätzlich „Nachricht löschen“, „Mute“ und „Chatbann“, wenn die Staff-Rolle sichtbar ist. „Nachricht löschen“ sendet sofort die Löschanfrage für die ID dieser Zeile. Das Aktionsformular öffnet sich dafür nicht. Ein zweiter Klick, solange die Anfrage läuft, sendet nichts erneut. Erfolg und Ablehnung erscheinen im Chat. Mute und Chatbann öffnen weiter das Formular. Grund, Dauer und die ausdrückliche Permanent-Bestätigung bleiben dort Pflicht. Der Server entscheidet den Teamschutz. Eine Ablehnung erscheint als Text, ohne Beleg.

Die Ignore-Liste liegt in `config/cclive-utilities/chat-ignore.json`, getrennt nach der UUID des lokalen Spielerkontos. Sie filtert nur die Anzeige. Server, Belege und Sanktionen bleiben unberührt. Live-Nachrichten und nachgeladene Historie werden vor dem Vorbereiten geprüft. Unmittelbar vor dem Einfügen in den Chat wird dieselbe Liste noch einmal gelesen. Bereits sichtbare Zeilen dieses Namens verschwinden beim Ignorieren. Ein gespeicherter Eintrag des eigenen Namens oder der eigenen Konto-ID blendet die eigenen Nachrichten nicht mehr aus. Der Vergleich der Namen ignoriert Groß- und Kleinschreibung.

`/cclive chat ignore list` sucht nach dem letzten Tastendruck mit kurzer Verzögerung im Hintergrund. Der Spielthread wartet dabei nicht. Verspätete Antworten für einen älteren Suchtext werden verworfen. Die Treffer stehen als kompakte Textzeilen direkt unter dem Suchfeld, mit dezenter Hervorhebung beim Überfahren und wenn der Name schon im Feld steht. Ein Klick übernimmt den Namen nur ins Feld. Enter macht dasselbe wie „Hinzufügen“: nur ein bestätigtes CCLive-Konto, das eigene Konto bleibt ausgeschlossen. Der Button bleibt nutzbar.

Die Suche selbst lief schon neben dem Spielthread. Das Stocken und die ausgefallenen Tastenkürzel kamen daher, dass jede Antwort das Suchfeld neu erzeugt und dabei Fokus, Cursor und Auswahl verloren hat. Neue Vorschläge ändern nur die gezeichnete Liste. Das Eingabefeld bleibt dasselbe, Strg+A und Einfügen eingeschlossen. Der Admin-Endpunkt `/moderation/players/search` ist für normale Mod-Nutzer gesperrt. Die Liste nutzt dieselbe Spielersuche über `GET /chat/players/search` mit dem normalen Spieler-Token. Ein unbekannter Name wird abgewiesen.

Auf der Website löscht „Nachricht löschen“ die ausgewählte Nachricht auch mit leerem Begründungsfeld. Der Pflichtfeldhinweis gilt dort für Mute, Bann und Aufheben. Beide Lösch-Endpunkte, `/moderation/actions/message/delete` und `/staff/actions/message/delete`, lassen einen fehlenden Grund zu. Teamschutz, Rollenprüfung, Protokoll und Belegsicherung bleiben. Mute und Bann verlangen die Begründung weiterhin.

## Tatsächlich ausgeführte Tests

Lokal, ohne Minecraft und ohne JAR:

`gradlew test --offline --tests net.felix.chat.ChatIgnoreTypingTest --tests net.felix.chat.ChatContextTargetTest --tests net.felix.staff.StaffActionPrefillTest --tests net.felix.chat.ChatShareRegressionTest`

BUILD SUCCESSFUL. `compileJava` lief mit.

Zusätzlich, aus `backend_server`:

`node ..\development\website\tests\chat-moderation.test.js`

Ausgabe: `chat-moderation tests ok`.

Die Tests prüfen die Glyphenreihenfolge zweier Nachrichten desselben Spielers mit Hover und mehrfarbigem Namen. Präfix, jeder Namensbuchstabe und `>>` lösen Spieler und ID genau dieser Zeile auf. Name und `>>` haben kein ClickEvent und keine Insertion, der Vanilla-Klickfinder würde sie verwerfen. Nachrichtentext, Icon und Share bleiben ohne Menü. Die eigene Zeile bietet kein Ignorieren. Vorschläge und manuelles Hinzufügen weisen das eigene Konto ab, ein unbekannter Name ebenfalls. Ein gespeicherter eigener Name und eine gespeicherte eigene Konto-ID blenden die eigenen Nachrichten nicht aus. Direktes Löschen trägt die ID der Zeile und einen leeren Grund, ein zweiter Klick auf dieselbe ID startet keine weitere Anfrage. Eine Löschung ohne Grund bleibt im Protokoll und im Beleg, eine zweite ist `already`. Mute ohne Grund bleibt `reason_required`. Die Website-Löschung ohne Grund trifft die Nachricht. Eine Löschung gegen ein Teammitglied scheitert mit `team_protected`, auch ohne Grund, und lässt Nachricht und Beleg unverändert. Ein normaler Spieler findet über `/chat/players/search` registrierte Konten, das eigene Konto ist markiert, kurze Suchen und Konten ohne UUID bleiben leer. Für das Suchfeld prüft `ChatIgnoreTypingTest` schnelles Tippen gegen eine langsame ältere Antwort, Strg+A und einen eingefügten Text vor und nach dem Eintreffen der Vorschläge, einen Klick, der nur den Namen ins selbe Feld schreibt, sowie Hinzufügen per Enter und per Button. Dabei bleiben Feld, Cursor und Auswahl stehen, unbekannte Namen und das eigene Konto werden abgewiesen. Die Verzögerung liegt bei 200 ms und läuft nicht auf dem Spielthread. Die bestehende Share-Regression ist mitgelaufen. Minecraft wurde nicht gestartet.

## Offene Live-Nachtests

Nicht im Spiel und nicht gegen den laufenden Server:

- Rechtsklick auf Präfix, mehrfarbigen Namen mit Hover und `>>`, jeweils bei zwei Nachrichten desselben Spielers. Jede öffnet Spieler und ID genau dieser Zeile.
- Umgebrochene Zeile und eine bereits gelöschte Zeile behalten Name und ID.
- Nachrichtentext und Shares behalten Linksklick und Hover. Eine normale Serverzeile `Name >> …` bleibt beim Ignorieren stehen.
- Nach Escape und erneutem Öffnen des Chats ist das Kontextmenü zu.
- Im Ignore-Suchfeld schnell tippen, auch wenn die Antwort langsam kommt. Strg+A und Einfügen bleiben vor und nach den Vorschlägen im selben Feld möglich. Ein Treffer füllt nur den Namen. Enter und „Hinzufügen“ übernehmen nur ein bestätigtes fremdes CCLive-Konto.
- „Nachricht löschen“ im Spiel sendet sofort, ohne Formular, und zeigt Erfolg oder die Teamschutz-Ablehnung. Ein schneller zweiter Klick sendet nicht noch einmal.
- Auf der Website löscht der Button die ausgewählte Nachricht mit leerem Begründungsfeld. Mute und Bann verlangen Begründung, Dauer und bei Permanent die Bestätigung.

## Upload

Ein neuer Client-Build und diese Serverdateien. Diese Dokumentation und die Tests nicht hochladen.

- `backend_server/server.js` — `GET /chat/players/search` für jeden gültigen Spieler-Token
- `backend_server/website_backend/chat-moderation.js` — Löschen ohne Grund, dieselbe Spielersuche
- `backend_server/public/staff-moderation.js` — Website-Löschen ohne Begründungszwang, kein Doppelklick
- `backend_server/public/staff-home.html` — Hinweis, dass Löschen ohne Begründung geht
- `backend_server/public/admin-workspace.js` — derselbe Hinweis in der Admin-Übersicht
- `backend_server/admin_ui/admin.html` — Cache-Stand der Skripte

### Geänderte Dateien

- `src/main/java/net/felix/chat/ChatLineTarget.java` — Präfix, Name und `>>` derselben Zeile
- `src/main/java/net/felix/chat/ChatStyleHelper.java` — Glyphen-Treffer im gerenderten Chat
- `src/main/java/net/felix/chat/ChatContextMenu.java` — Menü, eigenes Konto, direktes Löschen
- `src/main/java/net/felix/chat/ChatMessageDelete.java` — Löschanfrage ohne Formular
- `src/main/java/net/felix/chat/ChatIgnoreList.java` — lokale Liste, eigener Name und Konto-ID
- `src/main/java/net/felix/chat/ChatIgnoreRules.java` — bestätigter Treffer, Ausschluss des eigenen Kontos
- `src/main/java/net/felix/chat/ChatPlayerSearch.java` — Suche nach dem letzten Tastendruck, ältere Antworten verfallen
- `src/main/java/net/felix/chat/ChatIgnoreTyping.java` — Vorschläge lassen das Eingabefeld stehen
- `src/main/java/net/felix/chat/ChatIgnoreScreen.java` — Text-Dropdown, Enter und Hinzufügen
- `src/test/java/net/felix/chat/ChatIgnoreTypingTest.java`
- `src/main/java/net/felix/chat/ChatManager.java` — blendet eigene Nachrichten nicht aus
- `src/main/java/net/felix/chat/ChatHudMessageIds.java` — entfernt sichtbare Zeilen eines ignorierten Namens
- `src/main/java/net/felix/mixin/ChatScreenMixin.java` — Rechtsklick, ohne den Linksklick-Pfad der Shares zu ändern
- `src/main/java/net/felix/mixin/ChatHudHoverMixin.java` — zeichnet das Menü im Chat
- `src/main/java/net/felix/staff/StaffActionScreen.java` — übernimmt Spieler und Nachrichten-ID
- `src/main/java/net/felix/commands/CCLiveCommands.java` — Befehl und Hilfezeile
- `src/test/java/net/felix/chat/ChatContextTargetTest.java`
- `src/test/java/net/felix/staff/StaffActionPrefillTest.java`

Die blauen, grünen und lila Chat-Icons und `PlayerIconUtility` sind nicht verändert.
