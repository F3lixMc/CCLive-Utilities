# Ideen für CCLive Utilities

Unverbindliche Vorschläge. Ob sie umgesetzt werden, ist offen.

- **Leaderboard für sterngeschmiedete Ausrüstung.** Die Rangliste zählt, wie viele sterngeschmiedete Ausrüstungsteile jeder Spieler hergestellt hat.

# Zu den Teams
- eigenen Command erstellen wie /cclive Team oder /team oder /cc team.
- wenn command ausgeführt silten /cc team info ausführen, info aus chat auslesen und nachricht blockieren/nicht rendern
- Custom Screen Öffnen und Infos aus Nachricht Optisch ansprechender Darstellen.
- Teammitglieder Onlinestatus anzeigen und bei hover stats wie ebene/wellen hoverstats halt
- Button für Chat einfügen oder chat im fenster neben diesem "Screen"
- Ingame chat eine option direkt in team chat zu schreiben mit @team oder sowas, team chat im Chatmenü von PlayLegend mit einfügen Lokal -> L+ -> CCLive -> Team

## Bauplan-Drop-Nachrichten lokal ausblenden

Wenn PlayLegend auf „Keinen Anzeigen“ steht, sendet der Server keine Bauplan-Drop-Nachrichten mehr. Die Mod kann diese Drops dann nicht aus dem Chat erfassen. Idee: Bei „Keinen Anzeigen“ die Nachrichten serverseitig über „Alle Anzeigen“ weiter empfangen, zuerst für die Bauplan-Erfassung auswerten und danach für den Spieler im Chat ausblenden. Im Menü soll der Pfeil weiterhin auf „Keinen Anzeigen“ stehen.

### Weitere Drop-Kategorien

- Dasselbe Prinzip für Materialien, Ressourcen, Karten und Statuen umsetzen. Die Mod soll die jeweiligen Drop-Nachrichten weiterhin empfangen und auswerten können, auch wenn der Spieler sie lokal im Chat ausblendet. Die lokale Auswahl je Kategorie und Minecraft-Konto in der Config speichern und die passende Auswahl im Tooltip anzeigen. Pro Kategorie die tatsächlichen Menüoptionen und Klickfolgen prüfen.

### Menü und Tooltip erkennen

- Menü-Glyph über den vorhandenen Eintrag in `zeichen.json` erkennen: `ui_settings_notifications_background`, Gruppe `cactus_clicker_settings`, Name `ui_notifications`. Aktueller Glyph: `㷌`.
- Item mit Tooltip-Überschrift `[Baupläne]` erkennen. Reihenfolge: `[Baupläne] Alle Anzeigen` → `[Baupläne] Ab episch anzeigen` → `[Baupläne] Ab legendär anzeigen` → `[Baupläne] Nur ersten anzeigen` → `[Baupläne] Keinen Anzeigen` → zurück zu „Alle Anzeigen“.
- Der aktive Eintrag hat ein einzelnes Pfeilzeichen davor. Das tatsächliche Zeichen am Originaltooltip prüfen. Bei lokalem „Keinen Anzeigen“ den Pfeil im Tooltip clientseitig von der Serveroption auf „Keinen Anzeigen“ verschieben.

### Einstellung dauerhaft speichern

- Lokales „Keinen Anzeigen“ pro Minecraft-Konto in der Mod-Config speichern. Beim Aktivieren und bei der Rückkehr zu einem anderen Modus die Config aktualisieren.
- Nach Neustart oder Reconnect den gespeicherten Zustand wieder laden und sowohl den Tooltip als auch den Chatfilter daraus ableiten. Wenn PlayLegend inzwischen auf einem anderen Modus steht, den Abgleich erst im erkannten Menü am bestätigten Bauplan-Item durchführen.

### Klickfolgen

1. **„Nur ersten anzeigen“ + Linksklick:** Den normalen Klick auf „Keinen Anzeigen“ erlauben. Sobald das Item den Wechsel bestätigt, einen zusätzlichen Linksklick ausführen, damit der Server auf „Alle Anzeigen“ steht. Lokales Ausblenden aktivieren und den Tooltip auf „Keinen Anzeigen“ halten.
2. **Lokal „Keinen Anzeigen“ + Linksklick:** Den Klick abfangen. Lokales Ausblenden und Tooltip-Anpassung deaktivieren. Der Server steht bereits auf „Alle Anzeigen“.
3. **Lokal „Keinen Anzeigen“ + Rechtsklick:** Zwei Rechtsklicks an den Server schicken. Der erste wechselt serverseitig von „Alle Anzeigen“ auf „Keinen Anzeigen“, der zweite auf „Nur ersten anzeigen“. Anschließend lokales Ausblenden und Tooltip-Anpassung deaktivieren.
4. **Andere Klicks:** Normal an PlayLegend weiterleiten. Insbesondere löst ein Rechtsklick bei „Nur ersten anzeigen“ keine lokale Ausblendung aus.

Zusatzklicks nur im erkannten Menü am bestätigten Bauplan-Item ausführen. Vor dem zweiten Klick die erwartete Aktualisierung des Tooltips abwarten, damit bei Lag oder geschlossenem Menü kein falscher Modus gesetzt wird. Beim Testen auch prüfen, was bei einem Konto passiert, das schon vor Installation der Mod serverseitig auf „Keinen Anzeigen“ stand.

