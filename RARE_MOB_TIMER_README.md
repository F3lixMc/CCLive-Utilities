# Seltener Mob Timer - Neue Funktion

## Übersicht
Die neue Funktion "Seltener Mob Timer" überwacht den Chat und startet automatisch einen Timer, wenn ein seltener Mob erscheint. Der Timer läuft, bis der seltene Mob verschwindet oder besiegt wird.

## Funktionalität
- **Automatische Erkennung**: Überwacht Chat-Nachrichten auf "Ein seltener Mob ist erschienen"
- **Timer-Anzeige**: Zeigt die verstrichene Zeit seit dem Erscheinen des seltenen Mobs an
- **Dauerhafte Anzeige**: Der Timer bleibt auf dem Bildschirm sichtbar, auch nachdem der seltene Mob verschwunden ist
- **Manuelles Verstecken**: Der Timer verschwindet nur, wenn die Leertaste gedrückt wird
- **Automatisches Stoppen**: Der Timer stoppt bei Nachrichten wie:
  - "Der seltene Mob ist verschwunden"
  - "Der seltene Mob wurde besiegt"
  - "Der seltene Mob ist geflohen"

## Konfiguration
Die Funktion kann über das Mod-Menü konfiguriert werden:

### Hauptoptionen
- **Seltener Mob Timer aktivieren**: Aktiviert/deaktiviert die gesamte Funktion
- **Timer anzeigen**: Zeigt den Timer auf dem Bildschirm an oder versteckt ihn

### Position
- **X Position**: Horizontale Position vom linken Bildschirmrand (Standard: 5 Pixel)
- **Y Position**: Vertikale Position vom oberen Bildschirmrand (Standard: 200 Pixel)

### Darstellung
- **Überschriftenfarbe**: Farbe für den Titel "Seltener Mob Timer" (Standard: Gelb)
- **Textfarbe**: Farbe für die Zeit-Anzeige (Standard: Weiß mit Transparenz)
- **Hintergrund anzeigen**: Schwarzer Hintergrund hinter dem Timer (Standard: Aktiviert)

## Technische Details
- **Chat-Überwachung**: Verwendet `ClientReceiveMessageEvents.GAME` für Server-Chat-Nachrichten
- **HUD-Rendering**: Zeichnet den Timer über `HudRenderCallback.EVENT`
- **Tab-Taste**: Timer wird ausgeblendet, wenn die Spielerliste geöffnet wird
- **Performance**: Minimaler Performance-Impact durch effiziente Event-Behandlung
- **Singleplayer-Tests**: R-Taste startet den Timer manuell für Testzwecke

## Verwendung
1. Aktiviere die Funktion in den Mod-Einstellungen
2. Der Timer startet automatisch, wenn ein seltener Mob erscheint
3. Die verstrichene Zeit wird im Format MM:SS angezeigt
4. Der Timer läuft weiter, auch nachdem der seltene Mob verschwunden ist
5. Drücke die **Leertaste**, um den Timer zu verstecken

### Für Tests (Singleplayer)
- Drücke die **R-Taste**, um den Timer manuell zu starten (nur wenn kein Timer läuft)
- Nützlich für Tests und Demonstrationen im Singleplayer

## Integration
Die Funktion ist in die bestehende `InformationenUtility` integriert und folgt dem gleichen Design-Pattern wie andere Utilities im Mod:
- Konfigurierbare Einstellungen
- Tab-Taste für Overlay-Sichtbarkeit
- Konsistente Farbgebung und Positionierung
- Deutsche Lokalisierung
