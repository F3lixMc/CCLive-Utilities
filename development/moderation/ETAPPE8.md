# Etappe 8 — Chatfilter

Status: implementiert und mit Knotentests sowie `compileJava` geprüft. Keine Live-Abnahme, kein JAR, kein Upload.

Etappe 7 bleibt die Referenz und ist nicht neu geschrieben. Die danach live getesteten Änderungen an Report-Sanktionen, Chat-Aktualisierung und Übersicht bleiben die Grundlage.

## Verhalten

Beide normalen Chat-Einstiege, `POST /chat/send` und WebSocket `chat.send`, laufen durch `acceptPlayerChat`. Die Reihenfolge ist Glyphe, danach Mute oder Chatbann, danach Filter. Eine Filterablehnung speichert die Nachricht nicht, startet keinen Broadcast und setzt `chatBanned` nicht. Lesen und Empfangen bleiben möglich.

Die erste Einrichtung legt genau `youtube.com`, `twitch.tv` und `cclive-utilities.net` einmalig an. `youtu.be` fehlt und kann später im Control Center ergänzt werden. Ein gespeichertes Merkmal verhindert das erneute Anlegen. Ein von einem Admin entfernter Eintrag kommt nach einem Neustart nicht zurück, auch wenn die Liste danach leer ist. Ist die Liste leer, lehnen eingeschaltete Filter jeden erkannten Link ab, bis wieder Domains eingetragen sind.

Links werden nur über den geparsten Hostnamen bewertet, ohne Abruf und ohne DNS. Erlaubt sind der eingetragene Host und eine echte Subdomain. `playlegend.net.evil.example`, `evil-playlegend.net` und der Host in `https://playlegend.net@evil.example` sind ohne eigene Freigabe abgelehnt. Satzzeichen und Klammern gehören nicht zum Host. `1.20.1` ist kein Link. Ein unerlaubter Link lehnt die ganze Nachricht ab. `#ITEM:` und `#BUILD:` bleiben Text.

Ausdrücke gelten an Unicode-Wortgrenzen, ohne Unterschied bei Groß- und Kleinschreibung. Ein Treffer in einem längeren Wort zählt nicht. Eine Ausnahme unterdrückt einen Ausdruck nur, wenn sie ihn vollständig überdeckt. Die Rückmeldung nennt den Listeneintrag nicht. „so eine Scheiße hier“ und „diese Drecksschweine droppen das Item nicht“ bleiben bei den Ausdrücken `scheiß` und `dreck` erlaubt.

Der dritte identische Text desselben `playerId` innerhalb von 10 Sekunden wird abgewiesen und sperrt das Schreiben für 60 Sekunden. HTTP und WebSocket teilen denselben Speicherzähler. Andere Spieler bleiben getrennt. Es entsteht keine `chat_sanctions`-Zeile, und bestehende Mute- oder Bannzeilen werden nicht verändert. Nach 60 Sekunden endet die Sperre. Ein Backend-Neustart löscht sie.

`MODERATION_TOOLS_ENABLED` bleibt der Werkzeugschalter: leer oder ungesetzt an, `0`, `false` oder `off` aus. Er sperrt nur neue Moderationsaktionen und neue Teamnachrichten. `CHAT_FILTERS_ENABLED` hat dieselbe Lesart und ist standardmäßig an. Ausgeschaltet entfallen Domain-, Ausdrucks- und Repeat-Prüfung. Glyphensperre und Sanktionen bleiben aktiv. `CHAT_FILTERS_ENABLED=0` ist nur zum Abschalten nötig, während die Liste angepasst wird.

`GET /chat/rules` mit Spieler-Token liefert die öffentlichen Regeln und die erlaubten Domains, ohne Ausdrücke und Ausnahmen. `/cclive chatrules` zeigt **CCLive Chatregeln** gelb und zentriert. Die Regeln stehen darunter linksbündig mit Zeilenumbruch. **Freigegebene Domains** folgt mit einer Domain pro Zeile und kommt live aus `/chat/rules`. Bei wenig Platz scrollt der Inhalt, Titel und Schließen bleiben stehen. `playlegend.net` gehört nicht zur Startliste und kann im Control Center ergänzt werden.

Im Control Center ist „Chatfilter“ ein eigener Bereich. In der Adminansicht bleibt er beim Wechsel mit „Player Stats“ und „Übersicht“ allein sichtbar. Die Domainliste hat ein Suchfeld. Gesperrte Ausdrücke und Ausnahmen haben je ein Feld zum Hinzufügen. Jede Liste zeigt höchstens zehn Zeilen und scrollt danach in sich. Admins fügen hinzu, bearbeiten und löschen. Vor dem Speichern einer Domain wird der ermittelte Hostname angezeigt. Duplikate und ungültige Eingaben erscheinen am Feld. Dieselbe Duplikatprüfung lehnt `PUT /staff/filters` serverseitig ab. Moderatoren können die Domainliste durchsuchen und alle drei Listen lesen; Schreiben bleibt 403. Cache-Stand der geänderten Oberflächen: `20261113-etappe8-lists`.

## Tests

Aus `backend_server`:

`node ..\development\website\tests\chat-moderation.test.js`

Ausgabe:

```
sanction live update error
sanction live update error
chat-moderation tests ok
```

Die zwei Zeilen `sanction live update error` stammen aus dem bestehenden Sanktionstest ohne verbundene Chatsockets. Abgedeckt sind die einmalige Startliste ohne `youtu.be`, ein danach entfernt bleibender Eintrag, erlaubte Domain und Subdomain, ähnlich aussehende Fremddomains, mehrere Links, Satzzeichen und Klammern, `https://playlegend.net@evil.example`, `1.20.1`, die beiden harmlosen Sätze, gesperrte Ausdrücke, Ausnahmen, zwei Spieler, das Ende der 60 Sekunden, eine unveränderte manuelle Sanktion, Admin-Änderungen, Lesen und 403 für Moderatoren, öffentliche Regeln ohne interne Liste, eine leere Domainliste, der Schalter `CHAT_FILTERS_ENABLED=0` sowie die bisherigen Moderations- und Share-Fälle. Der Repeat-Guard wurde zusätzlich an einem echten WebSocket-`chat.send` gemischt mit `POST /chat/send` geprüft. Dieselbe Verbindung hat danach eine Nachricht eines anderen Spielers empfangen.

`compileJava` endete mit `BUILD SUCCESSFUL`. Es gab keinen Gradle-`build` und kein JAR.

## Offene Live-Checks

- Das Backend muss neu gestartet werden, damit die Tabellen und die einmalige Startliste ankommen.
- `CHAT_FILTERS_ENABLED` muss nur gesetzt werden, wenn die Prüfung abgeschaltet werden soll.
- Ein Mod-Build ist nötig, damit `/cclive chatrules` im Spiel ankommt. Dieser Auftrag hat das JAR nicht gebaut und nichts hochgeladen.
- Im Control Center prüfen, dass „Chatfilter“ nicht unter „Player Stats“ liegt, Moderatoren nur lesen und suchen und Admins hinzufügen, bearbeiten und löschen.
- Im Spiel `/cclive chatrules` bei kleiner Fenstergröße prüfen. Der Screen ist kompiliert, aber nicht ingame geöffnet.
- Einen entfernten Domain-Eintrag nach einem Neustart kontrollieren und danach eine leere Liste mit einem Link prüfen.

## Upload

Differenz zum live getesteten Etappe-7-Stand. Unveränderte Etappe-7-Dateien stehen hier nicht noch einmal. Diese Notiz und die Knotentests nicht hochladen.

### `backend_server/website_backend`

- `backend_server/website_backend/chat-filters.js` — Startliste, Wortgrenzen, Ausnahmen, Repeat-Speicher, Tabellen, `CHAT_FILTERS_ENABLED` und serverseitige Duplikatprüfung
- `backend_server/website_backend/chat-moderation.js` — gemeinsame Annahme, Filter nach Mute und Bann, Schema
- `backend_server/website_backend/moderation-http.js` — `GET /chat/rules`, `GET` und `PUT /staff/filters`, `POST /staff/filters/preview`
- `backend_server/server.js` — HTTP und WebSocket nutzen dieselbe Annahme

### `backend_server/public`

- `backend_server/public/admin-script.js` — „Chatfilter“ blendet Player Stats und die übrigen Bereiche aus
- `backend_server/public/staff-moderation.js` — Suche nur bei Domains, Listenaktionen, Vorschau und Duplikate
- `backend_server/public/staff-home.html` — Bereich Chatfilter, Cache-Stand `20261113-etappe8-lists`
- `backend_server/public/admin-workspace.js` — derselbe Bereich in der Admin-Ansicht
- `backend_server/public/admin-style.css` — höchstens zehn sichtbare Listenzeilen

### `backend_server/admin_ui`

- `backend_server/admin_ui/admin.html` — Auswahl Chatfilter, Cache-Stand `20261113-etappe8-lists`

### `src/main/java`

- `src/main/java/net/felix/chat/ChatRulesScreen.java` — gelbe Überschrift, umbrochene Regeln, live geladene Domains, Scrollen
- `src/main/java/net/felix/commands/CCLiveCommands.java` — `/cclive chatrules`
