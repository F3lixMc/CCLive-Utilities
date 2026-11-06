# Etappe 3 — Übergabe

Stand: 6.11.2026. Etappe 3 ist im Code korrigiert. Kein Deployment, kein Mod-JAR. Etappe 4 ist nicht begonnen. Etappe 2 bleibt unverändert wirksam: Rollen, Control-Center-Zugang, Modverwaltung, Adminbefehle einschließlich Sichtbarkeit und Autocomplete.

## Nachbesserung nach dem zweiten Live-Test

Bereits live in Ordnung und hier nicht angefasst: Mod-Chat einmalig, Website-Chat sofort und ohne Duplikate, Löschen behält Absender und Präfix, eigene Dauern ohne `confirm`, Permanent mit `confirm`, Namensvorschläge, Leeren setzt nur Felder zurück, Mitlaufen aus hält die Scrollposition, Rechteentzug beendet den Website-Livechat innerhalb von fünf Sekunden. Keine Etappe 4, kein Deployment, kein JAR.

### Ursache und Lösung

1. **Leeren.** Der Button stand bei den Aktionsbuttons. Er steht jetzt oben rechts im Moderationsformular, in Admin- und Moderatoransicht. Die Farbe ist ein gedämpftes Rot in der Art der vorhandenen roten Umriss-Buttons, mit eigenem Hover und Fokus. In schmalen Fenstern bleibt er rechts und wird nicht auf die volle Breite gezogen. Er setzt weiter nur Eingaben, Fehler und Auswahl zurück.

2. **Hellgrauer Löschtext.** Der ersetzte Inhalt nutzte die normale Nachrichtenfarbe. Nur der Platzhalter `Nachricht von Moderatoren gelöscht!` wird hellgrau (`#C8C8C8`), im Spiel bei der Live-Ersetzung und in der nachgeladenen Historie, auf der Website im hellen Layout als lesbares Grau und im Darkmode als `#C8C8C8`. Präfix und Spielername bleiben unverändert. Maßgeblich ist `deleted: true`. Derselbe Wortlaut ohne dieses Feld behält die normale Nachrichtenfarbe. Alte Share-Hover und Klickaktionen bleiben entfernt.

3. **Teamschutz.** Moderatoren konnten andere Moderatoren und Admins muten, bannen und deren Nachrichten löschen. Mute, Bann und Löschen prüfen jetzt zentral im Backend die verifizierte Spieler-ID des Akteurs und des Ziels. Die Rolle kommt aus der Admin-UUID-Liste beziehungsweise `staff_roles`, nicht aus Formularfeldern. Beim Löschen zählt der Autor der Nachricht. Ein Moderator gegen einen anderen Moderator oder einen Admin erhält `403` und den Text „Moderatoren können keine Aktionen gegen Teammitglieder ausführen.“ Dabei werden Nachricht, Sanktion und Ereignis nicht geschrieben. Ein Admin darf diese Aktionen auf Moderatoren weiter ausführen. Aufheben und Aktionen gegen normale Spieler bleiben wie bisher. Die Formulare erklären die Grenze zusätzlich; die Buttons bleiben sichtbar.

4. **Completion für permanent.** Nach `permanent` hing direkt das freie Begründungsargument, deshalb zeigte die Vervollständigung nur `<reason>`. Mute und Ban haben jetzt das Literal `permanent` und darunter das Literal `confirm`, danach `<reason>`. Zeitliche Dauern führen weiter direkt zur Begründung, ohne `confirm`. Die Staff-Sichtbarkeit und die Namensvorschläge an `name`, `unmute` und `unban` bleiben.

### Tatsächlich ausgeführte Tests

Lokal ausgeführt, nicht live und nicht gegen den laufenden Server:

- `node development/website/tests/chat-moderation.test.js` — erfolgreich. `sanction live update error` erscheint zweimal, weil ein Test den Live-Callback absichtlich fehlschlagen lässt.
- `gradlew test --tests net.felix.chat.ChatShareRegressionTest --tests net.felix.commands.ModSanctionCompletionTest --offline` — erfolgreich. `compileJava` lief mit. Ein JAR wurde nicht erzeugt.

Die Node-Suite prüft über `/moderation/actions` und `/staff/actions`: ein Moderator darf einen normalen Spieler muten und dessen Nachricht löschen; Mute, Bann und Löschen gegen einen anderen Moderator oder einen Admin werden abgelehnt, auch wenn Formularfelder eine andere Rolle oder einen anderen Akteur behaupten; ein Admin darf einen Moderator muten und bannen. Abgelehnte Fälle ändern weder Nachricht noch Sanktion und legen kein Ereignis an. Der Java-Test prüft die hellgraue Farbe nur am ersetzten Inhalt, die unveränderte Farbe einer normal geschriebenen Zeile mit demselben Wortlaut, das Literal `confirm` nach `permanent` bei Mute und Ban, kein `confirm` nach `5m`, die Dauer- und Permanent-Vorschläge, die Namensvorschläge und dass der Mod-Befehl ohne Staff-Rolle nicht sichtbar ist.

### Offen, weil nicht live ausgeführt

- Sitzung im Spiel: Tab nach `permanent` zeigt `confirm`, nach `5m` nur die Begründung. Namensvorschläge und die rollenabhängige Sichtbarkeit im echten Client.
- Sichtprüfung: Leeren oben rechts, Hover, Fokus und schmales Fenster. Hellgrauer Platzhalter im Minecraft-Chat und im Website-Chat, Präfix und Name unverändert, derselbe Satz ohne Löschstatus normal gefärbt.

### Uploadliste nur dieser Runde

Nichts davon wurde hochgeladen. Tests und diese Datei nicht hochladen.

Backend:

- `backend_server/website_backend/staff-roles.js` — prüft Moderator gegen anderen Moderator oder Admin anhand verifizierter Spieler-IDs
- `backend_server/website_backend/chat-moderation.js` — wendet diese Prüfung auf Mute, Bann und Löschen an, vor dem Schreiben
- `backend_server/website_backend/moderation-http.js` — antwortet darauf mit 403

Website:

- `backend_server/public/admin-style.css` — Leeren oben rechts, gedämpftes Rot, hellgrauer Löschtext
- `backend_server/public/admin-workspace.js` — Button und Hinweis im Control Center
- `backend_server/public/staff-home.html` — Button und Hinweis in der Moderatoransicht
- `backend_server/public/staff-moderation.js` — färbt nur den ersetzten Inhalt, wenn `deleted` gesetzt ist
- `backend_server/admin_ui/admin.html` — Cache-Stand `20261106-etappe3g`

Mod, nur als Quelltext. Der Benutzer baut den Client selbst. Es wird kein JAR erzeugt.

- `src/main/java/net/felix/chat/ChatManager.java` — hellgrauer Platzhalter nur bei ausdrücklichem Löschstatus
- `src/main/java/net/felix/chat/ChatHudMessageIds.java` — dieselbe Farbe bei der Live-Ersetzung
- `src/main/java/net/felix/commands/CCLiveCommands.java` — Literal `confirm` nach `permanent` für Mute und Ban

### Neustarts

Backend neu starten, danach die Website hart neu laden, damit `?v=20261106-etappe3g` gilt. Für Farbe und Befehlsvervollständigung den Mod selbst bauen und Minecraft neu starten.

### Gezielte Nachtests

1. Leeren sitzt oben rechts, wirkt gedämpft rot, reagiert auf Hover und Fokus und bleibt in einem schmalen Fenster rechts. Es wird keine Sanktion ausgelöst, Eingaben und Auswahl sind danach leer.
2. Eine gelöschte Zeile ist im Spiel und auf der Website hellgrau, Präfix und Name nicht. Eine selbst geschriebene Zeile mit dem Wortlaut `Nachricht von Moderatoren gelöscht!` bleibt normal gefärbt. Historie und Live-Ersetzung zeigen dasselbe. Der alte Share ist nicht mehr klickbar.
3. Ein Moderator mutet, bannt und löscht einen normalen Spieler. Dieselben Aktionen gegen einen anderen Moderator und gegen einen Admin werden mit dem Team-Hinweis abgelehnt, ohne Protokolleintrag. Ein Admin mutet einen Moderator erfolgreich. Mitgeschickte Felder wie `actor` oder `role` ändern das nicht.
4. Tab nach `/cclive mod mute <name> permanent` bietet `confirm`. Nach `5m` erscheint direkt die Begründung, ohne `confirm`. Unmute und Unban schlagen weiter passende Namen vor. Ohne Staff-Rolle fehlt der Mod-Befehl.

## Korrekturrunde nach dem Live-Test

Die bereits live bestandenen Regeln bleiben: eine Nachricht erscheint genau einmal, Löschen ersetzt nur die betroffene Zeile, Mute lässt lesen und sperrt Schreiben mit Grund und Dauer, Bann sperrt Senden und Empfang, Unban lässt einen parallelen Mute bestehen, zeitlich begrenzte Sanktionen laufen ab. Keine Etappe 4, kein Deployment, kein JAR.

### Ursache und Lösung

1. **Absender bei gelöschten Nachrichten.** `chat.deleted` hat die ganze Zeile durch `[CCLive]` und den Platzhalter ersetzt. Die neue Ersetzung behält Präfix, Spielername, Farben und das vorhandene `>>` und tauscht nur den Text danach gegen `Nachricht von Moderatoren gelöscht!`. Hover, Klick und Share-Insertion des alten Inhalts entfallen. Öffentliche Historie liefert weiterhin den Spielernamen und denselben Platzhalter, zusätzlich `deleted: true`. Eine Nachricht, die zufällig so lautet, bleibt eine normale Zeile, solange `deleted` nicht gesetzt ist.

2. **Moderator im Protokoll.** Gespeichert wurde die Akteur-ID aus der Anmeldung, die Anzeige hat die Spalte aber nicht gezeigt. Der Name wird jetzt beim Schreiben aus der angemeldeten Sitzung in den Ereignis-Payload gelegt (`req.player` im Spiel, `adminAccount.display_name` auf der Website) und über die gespeicherte Spieler-ID wieder ausgelesen. Formularfelder wie `actor` werden ignoriert. Die Tabelle zeigt die Spalte Moderator.

3. **Autocomplete für unmute und unban.** Die Namensargumente hatten keine Vorschläge. `/cclive mod unmute` fragt asynchron nur registrierte Spieler mit aktivem Mute ab, `/cclive mod unban` nur solche mit aktivem Bann. Die Staff-Prüfung der Befehle bleibt. Nach einer eigenen Aktion wird der Cache sofort verworfen, sonst spätestens nach 1,5 Sekunden neu geladen.

4. **confirm nur für permanent.** Die Befehlszweige haben `confirm` wie einen Pflichtbestandteil aussehen lassen, und das Testfenster hat bei der Dauer `permanent` die Bestätigung selbst gesetzt. Zeitlich begrenzte Sanktionen laufen ohne `confirm`. Permanent verlangt im Befehl das Wort `confirm` vor dem Grund und im Testfenster den Schalter „Permanent: an“. Löschen und Aufheben haben kein `confirm`. Begründungen bleiben Pflicht. `/cclive help` nennt dieselbe Syntax.

5. **Eigene Laufzeiten.** Erlaubt waren nur `10m`, `1h`, `24h`, `7d` und `permanent`. Dieselben vier Werte und `permanent` bleiben Schnellvorschläge. Zusätzlich gilt jede positive ganze Zahl mit `m`, `h` oder `d`, einheitlich in Befehl, Testfenster, Website und Backend. `0`, negativ, ohne Einheit, zu groß oder nicht sicher als ganze Millisekundenzahl darstellbar wird abgelehnt. Die Obergrenze ist 366 Tage, damit Ablaufzeit und `duration_key` in `VARCHAR(16)` sicher bleiben. Permanent ist keine Zahl. Protokoll und Sanktion speichern die eingegebene Dauer und den berechneten Ablauf.

6. **Kompakter Livechat.** Jede Nachricht war ein großer hellgrüner Button. Jetzt ist jede Nachricht eine eigene Zeile `[#0001] Spielername » Nachricht`. Nur die ID ist ein kleiner Button und wählt die Nachricht aus. Die Anzeige füllt auf mindestens vier Stellen auf und schneidet längere IDs nicht ab. Intern bleibt die numerische ID. Lange Texte umbrechen. Mitlaufen bleibt im Adminbereich an den Schalter gebunden und springt nur dann ans Ende. Dieselbe Zeile gilt in Admin- und Moderatoransicht.

7. **Formulare leeren.** Nach einer gespeicherten Aktion, auch bei `stored: true` und `live_update_failed`, werden Eingaben und Auswahl geleert. Der Hinweis, dass die Sanktion gespeichert ist und die Verbindungen nicht aktualisiert wurden, bleibt stehen, damit dieselbe Eingabe nicht erneut abgeschickt wird. Schlägt die Aktion fehl, bleiben die Eingaben. „Leeren“ setzt nur die Felder und die Auswahl zurück. Rote Feldfehler und die Pflichtsternchen bleiben.

8. **Share-IDs.** `extractShareIds` hat nach dem Doppelpunkt nur Buchstaben und Ziffern gelesen und bei `#ITEM:ITEM-db2ac5` daher `ITEM` statt `ITEM-db2ac5` behalten. Die ID umfasst jetzt Präfix, Bindestrich und den folgenden Teil, analog `#BUILD:BUILD-709125`. Löschen und eine Sanktion mit Nachrichtenbeleg speichern Originaltext, diese Share-ID und den vorhandenen Share-Datensatz.

9. **Website-Livechat.** Neue Nachrichten kamen nur mit der Seitenabfrage alle 15 Sekunden. Der Spieler-WebSocket `/ws/chat` bleibt am Mod-Token. Die Website öffnet daneben `/ws/staff-chat` und sendet nur das vorhandene Sitzungscookie. Dieselbe Website-Sitzung und die gültige Control-Center-Freigabe gelten für Admin und Moderator. Ein Mod-Token liegt nicht im Browser, und es gibt keinen öffentlichen Staff-Endpunkt. Offene Verbindungen werden spätestens alle 5 Sekunden erneut geprüft; entzogene Rechte und abgelaufene Freigaben schließen den Kanal. Die erste Fassung hat die beiden Kanäle mit je einem eigenen Upgrade-Listener an denselben HTTP-Server gehängt. Die Bibliothek `ws` lehnt dabei den nicht passenden Pfad selbst ab, `/ws/staff-chat` kam deshalb mit HTTP 400 nicht zustande. Beide Server laufen jetzt mit `noServer`. Ein gemeinsamer Dispatcher in `websocket-dispatch.js` wählt anhand des Pfads genau einen Server und weist unbekannte Pfade mit HTTP 404 ab. Die Mod-Anmeldung auf `/ws/chat` bleibt. Die Nachladeanfrage geht erst nach `staff.ready` heraus. Kommt sie während der noch laufenden Autorisierung an, bleibt sie liegen und wird danach verarbeitet. Vor der Freigabe geht keine Chatzeile hinaus. Das Nachladen holt verpasste Nachrichten in Seiten zu 80, bis eine kürzere Seite kommt, und fragt zusätzlich die bereits bekannten älteren IDs ab. Eine davon gelöschte Zeile kommt als Platzhalter zurück, auch wenn ihre ID kleiner als die höchste bekannte ist. Dieselbe Zusammenführung nach ID gilt für Nachladen, Push und die HTTP-Abfrage alle 15 Sekunden. Eine gelöschte Zeile wird dabei nicht wieder zum Original. Mitlaufen scrollt nur, wenn der Schalter an ist und die Ansicht bereits unten steht. Die 15-Sekunden-Abfrage öffnet keine zweite Verbindung. Beim Verlassen der Seite wird der Kanal geschlossen. Teamchat und Reports sind nicht enthalten.

### Tatsächlich ausgeführte Tests

Lokal ausgeführt, nicht live und nicht gegen den laufenden Server:

- `node development/website/tests/chat-moderation.test.js` — in dieser Runde erneut ausgeführt, erfolgreich. Die Ausgabe `sanction live update error` gehört zum bereits vorhandenen Test, der einen fehlschlagenden Live-Callback erwartet. Der Java-Test wurde in dieser Runde nicht erneut ausgeführt.

Die Node-Suite prüft die echten Share-IDs `ITEM-db2ac5` und `BUILD-709125` beim Löschen und bei einer Sanktion mit Nachrichtenbeleg, eigene Dauern `5m` und `14d` samt Ablauf, die Ablehnung von `0m`, `-1h`, einer Zahl ohne Einheit, `367d`, einer nicht sicher darstellbaren Dauer und von `permanent` ohne Bestätigung, den Moderator aus der Anmeldung statt aus dem Formular, sowie Vorschläge nur für registrierte Spieler mit passender aktiver Sanktion. Dieselbe Suite öffnet jetzt einen echten HTTP-Server mit WebSocket-Clients. `/ws/chat` und `/ws/staff-chat` bestehen am selben Anschluss gleichzeitig, ein unbekannter Pfad endet mit 404. Die Nachladeanfrage wird vor Abschluss der verzögerten Autorisierung gesendet und danach vollständig beantwortet; die erste Servermeldung ist `staff.ready`. Mehr als 80 verpasste Nachrichten kommen in mehreren Seiten an. Eine bereits bekannte ältere Zeile, die während der Unterbrechung gelöscht wurde, erscheint als Platzhalter, der Originaltext nicht. Nachladen, Push und der HTTP-Abgleich ergeben je ID eine Zeile. Ohne Sitzung und nach entzogener Moderatorrolle schließt der Kanal mit 4401 und ohne Chatzeile. Die Ansicht sendet die Nachladeanfrage erst nach `staff.ready`. Zuvor geprüft und hier nicht erneut gelaufen: `gradlew test --tests net.felix.chat.ChatShareRegressionTest --offline` war erfolgreich, `compileJava` lief mit, ein JAR wurde nicht erzeugt. Der Java-Test prüft, dass nur der Nachrichtentext der passenden Zeile ersetzt wird, Name und Präfix bleiben, die Share-Markierung entfällt und eine andere Zeile mit dem Platzhaltertext unverändert bleibt. `deleted: true` wird als Löschstatus gelesen, der gleiche Text ohne dieses Feld nicht.

### Uploadliste der vorherigen Korrekturrunde

Nichts davon wurde hochgeladen. Tests und diese Datei nicht hochladen.

Backend:

- `backend_server/server.js` — `chat.deleted` trägt `deleted: true`; `/ws/chat` und `/ws/staff-chat` hängen am gemeinsamen Upgrade-Dispatcher, die Mod-Anmeldung bleibt
- `backend_server/website_backend/websocket-dispatch.js` — wählt anhand des Pfads genau einen WebSocket-Server und lehnt unbekannte Pfade ab
- `backend_server/website_backend/chat-moderation.js` — Platzhalter, Löschstatus, Share-IDs, eigene Dauern, Akteursname, Vorschlagsliste, seitenweises Nachladen und gelöschte bekannte IDs
- `backend_server/website_backend/moderation-http.js` — Akteur nur aus der Anmeldung, Dauerart für die Vorschläge
- `backend_server/website_backend/staff-chat-feed.js` — Website-WebSocket `/ws/staff-chat` ohne eigenen Upgrade-Listener, Sitzung und Control-Center-Freigabe, Nachladen erst nach der Freigabe, erneute Prüfung offener Verbindungen

Website:

- `backend_server/public/staff-moderation.js` — kompakte Zeilen, Moderatorenspalte, Leeren, Zurücksetzen, eigene Dauer, eine Website-Verbindung; Nachladen erst nach `staff.ready`, einschließlich bekannter älterer IDs
- `backend_server/public/staff-home.html` — Moderatorformular, Protokollspalte und Mitlaufen
- `backend_server/public/admin-workspace.js` — dasselbe Formular im Control Center
- `backend_server/public/admin-style.css` — kompakte Chatzeile, kleiner ID-Button
- `backend_server/admin_ui/admin.html` — bindet die geänderten Dateien mit neuem Cache-Stand ein

Mod, nur als Quelltext. Der Benutzer baut den Client selbst. Es wird kein JAR erzeugt.

- `src/main/java/net/felix/chat/ChatManager.java` — Löschstatus statt Textvergleich, Platzhalter mit Ausrufezeichen
- `src/main/java/net/felix/chat/ChatHudMessageIds.java` — ersetzt nur den Nachrichtentext und behält den Absender
- `src/main/java/net/felix/commands/CCLiveCommands.java` — `confirm` nur bei permanent, eigene Dauer, Vorschläge für unmute und unban
- `src/main/java/net/felix/staff/StaffNameSuggestions.java` — asynchrone Vorschläge aktiver Sanktionen
- `src/main/java/net/felix/staff/StaffModerationCommands.java` — verwirft die Vorschläge nach einer gespeicherten Aktion
- `src/main/java/net/felix/staff/StaffActionScreen.java` — ausdrückliche Permanent-Bestätigung, Leeren, Zurücksetzen nur nach Erfolg

### Neustarts

Den Backend-Prozess neu starten, nachdem die Backend-Dateien liegen, einschließlich `websocket-dispatch.js` und `staff-chat-feed.js`. Damit gelten Platzhalter, Share-Auflösung, eigene Dauern, der Akteursname, `deleted: true` und beide WebSocket-Pfade am selben Anschluss. Die Website danach hart neu laden, damit `staff-moderation.js?v=20261106-etappe3f` gilt. Ein Datenbank-Skript ist nicht nötig. `duration_key` bleibt `VARCHAR(16)`. Die Obergrenze von 366 Tagen passt dort hinein.

Für die sichtbare Zeile mit Absender, die Befehle und das Testfenster den aktuellen Mod selbst bauen und Minecraft neu starten. Der laufende Client zeigt die neue Ersetzung und die neue Befehlssyntax erst danach. Es wird kein JAR mitgeliefert.

### Gezielte Nachtests

1. Eine Chatzeile mit Share löschen. Auf allen Clients bleibt `[CCLive]`, der Spielername und `>>`, der Text wird `Nachricht von Moderatoren gelöscht!`. Eine andere Zeile und eine Vanilla-Zeile bleiben stehen. Hover und Klick des alten Shares sind weg.
2. Dieselbe gelöschte Zeile in der öffentlichen Historie und im Staff-Chat prüfen: Spielername bleibt, Inhalt ist der Platzhalter, die ID ist nur der kleine Button.
3. Mute und Bann je einmal im Spiel und auf der Website. Im Protokoll steht der angemeldete Moderator, nicht der Zielspieler. Eine mitgeschickte andere Identität ändert das nicht.
4. `/cclive mod mute <name> 5m <grund>` ohne `confirm`. `/cclive mod mute <name> permanent <grund>` wird abgelehnt. Mit `permanent confirm` und angehaktem Schalter im Testfenster wird es gespeichert. Löschen und Aufheben ohne `confirm`.
5. Tab bei unmute nur für gemutete registrierte Spieler, bei unban nur für gebannte. Nach Aufheben verschwindet der Name aus den Vorschlägen.
6. `30m` und `14d` setzen. Protokoll zeigt die eingegebene Dauer und den Ablauf. `0m`, `367d` und `permanent` ohne Bestätigung werden abgelehnt, die Felder bleiben gefüllt.
7. Nach einer erfolgreichen Aktion und nach `live_update_failed` sind die Felder leer und die Auswahl weg. Der Speicherhinweis bleibt bei der unvollständigen Live-Aktualisierung sichtbar. „Leeren“ führt keine Aktion aus. Eine abgelehnte Aktion lässt die Eingaben stehen.
8. Eine Nachricht mit `#ITEM:ITEM-db2ac5` oder `#BUILD:BUILD-709125` löschen oder als Beleg einer Sanktion verwenden. Originaltext, die vollständige ID und der Share-Datensatz liegen im Beleg.
9. Admin- und Moderatoransicht offen lassen. Eine neue Chatzeile und eine Löschung erscheinen ohne Seitenreload. Dieselbe Zeile steht nicht doppelt, auch nach der 15-Sekunden-Abfrage. Nach einer längeren Unterbrechung mit mehr als 80 neuen Zeilen sind alle da. Eine schon angezeigte ältere Zeile, die in der Pause gelöscht wurde, steht danach als Platzhalter. Mitlaufen aus und nach oben scrollen lässt die Position stehen. Freigabe ablaufen lassen oder die Moderatorrolle entziehen: die offene Verbindung liefert danach nichts mehr. Die Ansicht schließen und im Netzwerk prüfen, dass `/ws/staff-chat` beendet ist. Im Browser liegt kein Mod-Token.

## Umgesetzter Umfang

- Persistente Nachrichten-ID ist `chat_messages.id`. `requestId` bleibt nur die Korrelation einer Sendeanfrage.
- `chat.ack` und `chat.message` enthalten `id`. Die eigene Zeile entsteht nach dem Ack. Die WebSocket-Kopie derselben ID wird übersprungen.
- `chat.deleted` ersetzt nur den Nachrichtentext der Zeile mit `insertion = cclive-msg:<id>`. Präfix und Spielername bleiben. Der neue Text ist `Nachricht von Moderatoren gelöscht!`. Der übrige Chat bleibt stehen. Gelöscht wird am Feld `deleted` erkannt, nicht am Wortlaut.
- Mute und Bann dürfen parallel existieren. Eine zweite aktive Maßnahme derselben Art wird nicht verlängert. Aufheben wirkt nur auf die genannte Art. Permanent nur mit ausdrücklicher Bestätigung, `expires_at` bleibt dann leer.
- `MODERATION_TOOLS_ENABLED=0` sperrt neue Moderationsaktionen. Vorhandene Sanktionen und der öffentliche Platzhalter bleiben wirksam.
- Temporäre Befehle bis Etappe 4: `/cclive mod`, `/cclive mod delete <id> <grund>`, `/cclive mod mute|ban <name> <dauer> <grund>`, bei permanent `/cclive mod mute|ban <name> permanent confirm <grund>`, `/cclive mod unmute|unban <name> <grund>`. `confirm` gilt nur für permanent. Dauer ist eine positive ganze Zahl mit `m`, `h` oder `d`, höchstens 366 Tage. Das Rechtsklick-Menü bleibt Etappe 4.

## Korrekturen dieser Prüfung

### 1. Chatabruf verlangt eine gültige Identität

`GET /chat/messages` verlangt zuerst einen gültigen Mod-Token (`x-auth-token`) und prüft danach den Chatbann.

- Kein Token: 401.
- Ungültiger oder widerrufener Token: 403, keine Historie.
- Gültiger Token und aktiver Chatbann: 403 `banned`.
- Gültiger Token mit Mute oder ohne Sanktion: öffentliche Historie, gelöschte Zeilen nur als Platzhalter.

Aufrufer geprüft: Der Mod holt den Chat über WebSocket `/ws/chat`, nicht über diesen Endpunkt. Die Website nutzt für Staff `/staff/messages` und im Spiel `/moderation/messages`. Diese geschützten Sichten bleiben an Staff-Berechtigung gebunden und werden nicht zusätzlich vom Chatbann abhängig gemacht. Es musste kein bestehender Staff-Aufrufer umgestellt werden.

WebSocket: Ungültige Tokens und noch nicht geprüfte Verbindungen empfangen keine Chat- oder Löschereignisse. Erst nach der Sanktionsprüfung wird der Empfang freigegeben. Ein Chatbann sperrt Empfang und Senden, ein Mute nur das Senden.

### 2. Ereignisse bleiben für sich lesbar

`moderation_events` wird nicht nachträglich geändert. Grund, Sanktions-ID, Dauer und Ablauf stehen im Payload. Bei permanenter Laufzeit ist `permanent: true` und `expiresAt: null`. `listEvents` liest diese Felder nur aus dem Payload, nicht aus der später veränderbaren Sanktionszeile.

Nachrichtenlöschungen speichern im Ereignis Grund, Nachrichten-ID, Originaltext, Spieler und Share-IDs. Nach dem Ersetzen der Chatzeile bleibt das Ereignis verständlich.

Aufheben speichert den Aufhebungsgrund, die Sanktions-IDs und je Sanktion den ursprünglichen Grund, die Dauer, den Ablauf oder die permanente Laufzeit.

### 3. Optionale Nachricht bei direkter Sanktion

Mute und Bann akzeptieren optional `messageId`. Ohne ID bleibt die Sanktion möglich; es wird keine Nachricht erfunden und kein Beleg angelegt.

Mit ID prüft der Server in derselben Transaktion, dass die Nachricht dem betroffenen Spieler gehört (`player_id`). Originaltext und Share-Belege landen atomar in `player_file_evidence` und im Ereignis. Gehört die Nachricht nicht dem Spieler, wird abgelehnt und nichts gespeichert. Ist die Zeile schon ersetzt, wird der Originaltext aus dem Löschbeleg verwendet, nicht der Platzhalter. Fehlt dieser Beleg, wird die Sanktion abgelehnt.

Die Befehle bleiben ohne Nachrichten-ID. Das Testfenster `/cclive mod` und die Staff-Formulare senden die ID nur, wenn das optionale Feld gefüllt ist. Kontextmenüs bleiben Etappe 4.

### 4. Nur eindeutig beendete Seasons

Die Bereinigung sperrt die Season-Zeilen und löscht nur Daten, deren Season alle folgenden Punkte erfüllt:

- genau eine Zeile mit `is_current = 1`
- die aktuelle Season hat ein lesbares `start_at`
- die andere Season hat exakt `is_current = 0`
- ihr `start_at` liegt vor dem Start der aktuellen Season und in der Vergangenheit

Season-IDs werden nicht als Season-Nummern gelesen. Eine höhere ID kann eine ältere Season sein, eine niedrigere ID eine künftige. Künftige Seasons, fehlendes `start_at`, andere Flag-Werte, fehlende Season-Zeilen und mehrdeutige aktuelle Seasons bleiben unangetastet. Wird eine Season noch während der Löschung wieder aktuell, bleibt sie durch die erneute Prüfung in derselben Transaktion erhalten.

Aktive Sanktionen, auch permanente und noch laufende, werden nie gelöscht. Nur abgelaufene oder aufgehobene Sanktionen einer eindeutig beendeten Season werden entfernt. `leaderboard` und `player_stats` bleiben unberührt.

### 5. Live-Aktualisierung nach dem Speichern

Mute, Bann und Aufheben speichern die Sanktion zuerst. Danach wird die Aktualisierung der offenen Chatverbindungen abgewartet. Schlägt sie fehl, bleibt die Sanktion gespeichert und wird beim nächsten Senden, Empfangen und bei der regelmäßigen Prüfung weiter durchgesetzt.

Die Antwort ist dann HTTP 200 mit `stored: true`, `ok: false`, `liveUpdated: false` und dem Code `live_update_failed`. Das ist kein vollständiger Erfolg und auch keine Meldung, die Sanktion sei nicht angelegt. Befehle, Testfenster und Website zeigen den Hinweis, dass die Sanktion gespeichert ist und die laufenden Verbindungen nicht aktualisiert wurden. Eine bereits aktive Sanktion löst diese Aktualisierung nicht erneut aus.

## Tatsächlich ausgeführte Tests

Lokal ausgeführt, beide erfolgreich:

- `node development/website/tests/chat-moderation.test.js`
- `gradlew test --tests net.felix.chat.ChatShareRegressionTest`

Die Node-Suite prüft jetzt zusätzlich:

- Historie ohne Token, mit ungültigem Token und mit Chatbann
- Mute darf die Historie weiter lesen, Bann nicht
- Staff-Endpunkte `/staff/messages` und `/moderation/events` bleiben bei einem gebannten Zielspieler erreichbar
- Gründe, Dauer, Ablauf, permanente Laufzeit und Originaltext bleiben nach späterer Änderung der Sanktion oder der Chatzeile im Ereignis
- Sanktion mit fremder Nachricht wird abgelehnt, ohne ID entsteht kein Beleg
- Share-Beleg und Originaltext werden mit der Sanktion gespeichert; schlägt der Beleg fehl, wird die Sanktion zurückgerollt
- Season-Bereinigung entfernt nur die eindeutig beendete Season, auch wenn ihre ID höher ist als die der künftigen Season
- aktive Sanktionen bleiben, abgelaufene und aufgehobene der beendeten Season nicht
- eine Season, die während der Löschung wieder aktuell wird, behält ihr Protokoll
- ein ablehnender Live-Callback nach Mute und nach Aufheben: die Sanktion bleibt gespeichert, Senden bleibt gesperrt beziehungsweise ist nach dem Aufheben wieder möglich, und die Antwort ist weder `ok: true` noch ein Speicherfehler. Eine zweite Anfrage auf eine schon aktive Sanktion ruft den Callback nicht erneut auf

Der Java-Test prüft, dass `chat.deleted` nur die Zeile mit der genannten ID ersetzt. Eine zweite CCLive-Zeile und eine Vanilla-Zeile bleiben unverändert. `compileJava` ist dabei mitgelaufen. Ein Mod-JAR wurde nicht erzeugt.

## Offene Live-Tests

Nicht gegen den laufenden Server und nicht im Spiel ausgeführt:

- `GET /chat/messages` am echten Server: ohne Token, mit ungültigem Token, als gemuteter Spieler, als gebannter Spieler
- WebSocket am echten Server: Bann empfängt weder Historie noch neue Nachrichten; Mute empfängt Nachrichten, kann aber nicht senden; nach Unban kommt der Chat wieder an
- Sichtbare Ersetzung genau einer Chatzeile im Minecraft-Client, ohne den restlichen Chat zu leeren. Dafür muss der aktuelle Mod selbst gebaut und Minecraft neu gestartet werden
- Nachrichten-IDs, `/cclive mod` und das Testfenster im selbst gebauten Client
- Optionale Nachrichten-ID aus dem Website-Formular
- Season-Bereinigung an der echten Datenbank, einschließlich einer vorbereiteten künftigen Season, die nicht gelöscht werden darf
- Bedienung der Staff-Seiten im Browser nach dem Upload

## Frühere Gesamtliste gegenüber Etappe 2

Diese Liste ist nicht die Uploadliste. Dafür gilt nur „Uploadliste nur dieser Runde“ im Abschnitt „Nachbesserung nach dem zweiten Live-Test“. Hier stehen die Dateien der ersten Etappe-3-Fassung gegenüber der deployed Etappe 2. Etappe-2-Dateien für Rollen, Control Center und `/cclive perms` bleiben liegen und stehen hier nicht noch einmal.

### Backend

- `backend_server/server.js` — Nachrichten-ID, Löschereignis, Chatabruf, Sanktionsprüfung, Live-Aktualisierung der Verbindungen, Season-Bereinigung
- `backend_server/website_backend/chat-moderation.js` — Sanktionen, Löschbelege, Ereignisse, öffentliche Historie, Season-Bereinigung
- `backend_server/website_backend/moderation-http.js` — Staff-Aktionen im Spiel und auf der Website, einschließlich der abgewarteten Live-Aktualisierung
- `backend_server/website_backend/schema.js` — legt die Moderations-Tabellen und die ergänzten Spalten beim Start an

### Website

- `backend_server/public/staff-moderation.js` — Chat, Sanktionen, Protokoll und der Hinweis, wenn die Sanktion gespeichert ist, die Verbindungen aber nicht aktualisiert wurden
- `backend_server/public/staff-home.html` — Moderator-Start mit den Etappe-3-Aktionen
- `backend_server/public/admin-workspace.js` — dieselben Aktionen im Admin-Control-Center
- `backend_server/public/admin-style.css` — Darstellung der Moderationszeilen und des Moderator-Starts
- `backend_server/admin_ui/admin.html` — bindet die Moderationsskripte ein

### Mod

Quelltext gegenüber dem Etappe-2-Stand. Kein JAR erzeugen und keines wurde erzeugt. Für Live-Tests von Nachrichten-IDs, der sichtbaren Zeilenersetzung und der neuen Moderationsbefehle baut der Benutzer den aktuellen Mod selbst und startet Minecraft neu.

- `src/main/java/net/felix/chat/ChatManager.java` — speichert die Server-ID in der Chatzeile und ersetzt bei `chat.deleted` nur diese Zeile
- `src/main/java/net/felix/chat/ChatWebSocket.java` — übernimmt `id` in Ack und Nachricht und reicht `chat.deleted` weiter
- `src/main/java/net/felix/chat/ChatDisplayDedup.java` — unterscheidet Zeilen anhand der Nachrichten-ID
- `src/main/java/net/felix/chat/ChatHudMessageIds.java` — sucht die passende Zeile und setzt den Platzhalter
- `src/main/java/net/felix/mixin/ChatHudMessagesAccess.java` — Zugriff auf die Chatzeilen und ihre Aktualisierung
- `src/main/resources/cclive-utilities.mixins.json` — registriert `ChatHudMessagesAccess`
- `src/main/java/net/felix/commands/CCLiveCommands.java` — registriert `/cclive mod` für Admin und Moderator
- `src/main/java/net/felix/staff/StaffClientAccess.java` — Sichtbarkeit der Staff-Befehle und der Hinweis nach einer unvollständigen Live-Aktualisierung
- `src/main/java/net/felix/staff/StaffModerationCommands.java` — Löschen, Mute, Bann, Entmuten und Entbannen
- `src/main/java/net/felix/staff/StaffActionScreen.java` — Testfenster, optional mit Nachrichten-ID

Nicht hochladen: `development/website/tests/chat-moderation.test.js` und `src/test/java/net/felix/chat/ChatShareRegressionTest.java`.

## Einstellungen, Schema, Neustarts

Keine neue Umgebungsvariable. `MODERATION_TOOLS_ENABLED` bleibt wie bisher: leer oder nicht `0` schaltet die Werkzeuge ein, `0` sperrt nur neue Aktionen.

Beim Backend-Start ergänzt das vorhandene Schema additive Spalten, falls die Tabellen schon existieren:

- `chat_sanctions.duration_key`
- `chat_sanctions.message_id`
- `player_file_evidence.sanction_id`
- `player_file_evidence.share_ids`
- `player_file_evidence.share_snapshots`

Bestehende Zeilen bleiben erhalten. Es ist kein manuelles SQL nötig. Aktive Sanktionen werden durch die neuen Spalten nicht beendet.

Neustart: den Backend-Prozess einmal neu starten, nachdem die vier Backend-Dateien liegen. Damit laufen Schema, Chatabruf, Sanktionen und die Live-Aktualisierung. Die Website-Dateien danach hart neu laden.

Der laufende Client ist noch Etappe 2. Nachrichten-IDs, die sichtbare Ersetzung einer einzelnen Zeile und `/cclive mod` gibt es dort nicht. Dafür den aktuellen Mod selbst bauen und Minecraft neu starten. Es wird kein JAR mitgeliefert.

## Live-Testcheckliste

### Admin

1. Mit Adminfreigabe das Control Center öffnen. Rollen und `/cclive perms` verhalten sich wie nach Etappe 2.
2. Eine Chatzeile eines normalen Spielers auswählen. Das optionale ID-Feld darf leer sein: Mute mit Grund und `10m` muss ohne Beleg funktionieren.
3. Dieselbe oder eine andere eigene Zeile dieses Spielers in das ID-Feld setzen und bannen. Im Protokoll stehen Grund, Dauer oder Ablauf und die Nachrichten-ID. Der Originaltext bleibt im Ereignis, auch nachdem die Zeile ersetzt wurde.
4. Eine Nachrichten-ID eines anderen Spielers ablehnen lassen. Es darf keine neue Sanktion entstehen.
5. Dauer `permanent` nur mit Bestätigung. Im Protokoll steht Permanent, ohne Enddatum.
6. Aufheben mit eigenem Grund. Der Bann endet, ein gleichzeitig aktiver Mute bleibt, bis er selbst aufgehoben oder abgelaufen ist.
7. Protokollzeile prüfen, nachdem die Sanktion aufgehoben wurde: Grund und Laufzeit sind noch lesbar.

### Moderator

1. `/cclive mod` und die Moderator-Startseite öffnen. Keine Admin-Rechteverwaltung.
2. Nachricht ersetzen. Im öffentlichen Chat steht nur der Platzhalter, im Protokoll Grund und Originaltext.
3. Mute und Bann über die Befehle ohne Nachrichten-ID. Eine zweite Mute-Anfrage bleibt „bereits aktiv“ und verlängert nicht.
4. Optional im Formular eine passende Nachrichten-ID mitgeben. Share-Links aus dieser Nachricht müssen im Beleg erhalten bleiben.
5. Entmuten lässt einen Bann bestehen. Der gebannte Spieler sieht die Staff-Seite des Moderators nicht; der Moderator sieht seine Staff-Seite weiter.

### Normaler Spieler

1. Ohne Token oder mit ungültigem Token liefert `GET /chat/messages` keine Historie.
2. Ohne Sanktion senden und empfangen, inklusive einer gelöschten Zeile nur als Platzhalter. Andere eigene Zeilen bleiben sichtbar.
3. Während eines Mutes weiterlesen, aber nicht senden. Die Ablehnung nennt Grund und Restdauer.
4. Während eines Banns weder senden noch Historie oder neue WebSocket-Nachrichten erhalten.
5. Nach Ablauf oder Aufheben wieder senden und empfangen, ohne Relog-Sonderfall.
6. Ein normaler Spieler erreicht keine Staff-Route und sieht keine Originaltexte gelöschter Nachrichten.
