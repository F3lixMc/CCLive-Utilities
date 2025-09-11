# CCLive-Utilities Leaderboard-System

## Übersicht

Das Leaderboard-System sammelt automatisch verschiedene Spieler-Daten und sendet diese an deinen Node.js-Server.

## Funktionen

### Automatische Datensammlung

1. **Statistiken** (`StatsCollector`)
   - Alltime Kills
   - Aktuelle Coins
   - Rare Mob Kills (integriert mit MobTimerUtility)

2. **Collections** (`CollectionCollector`)
   - Alle Materialien aus deinem Mapping
   - Erkennt Chat-Nachrichten wie "Oak Collection +123"

3. **Floor Progress** (`FloorProgressCollector`)
   - Beste Zeiten pro Floor (floor_1 bis floor_100)
   - Automatische Floor-Erkennung basierend auf Dimension

### Server-Kommunikation

- Automatische Spieler-Registrierung
- Asynchrone Score-Updates
- Fehlerbehandlung und Retry-Logik

## Setup

### 1. Server-Konfiguration

Standardmäßig verbindet sich die Mod mit `http://localhost:2062`. 

Um die Server-URL zu ändern:
```
-Dleaderboard.server.url=http://deine-domain.com:2062
```

### 2. Debug-Modus aktivieren

```
-Dleaderboard.debug=true
```

## Commands

### `/leaderboard status`
Zeigt den Status des Leaderboard-Systems

### `/leaderboard top <board>`
Zeigt die Top 10 eines Leaderboards
Beispiel: `/leaderboard top alltime_kills`

### `/leaderboard refresh`
Erneuert die Server-Registrierung

### `/leaderboard toggle`
Aktiviert/Deaktiviert das System

### `/leaderboard test <board> <score>`
Sendet einen Test-Score
Beispiel: `/leaderboard test current_coins 1000`

## Verfügbare Leaderboards

### Statistiken
- `alltime_kills` - Gesamte Kills
- `current_coins` - Aktuelle Coins
- `alltime_rare_mob_kills` - Rare Mob Kills

### Collections
- `oak_collection` - Eichenholz
- `coal_collection` - Kohle
- `raw_copper_collection` - Kupfer
- `jungle_collection` - Dschungelholz
- `spruce_collection` - Fichtenholz
- `bamboo_collection` - Bambus
- `raw_iron_collection` - Eisen
- `mushroom_collection` - Pilzholz
- `dark_oak_collection` - Dunkeleichenholz
- `raw_gold_collection` - Gold
- `mangrove_collection` - Mangrovenholz
- `diamond_collection` - Diamant
- `sulfur_collection` - Schwefel
- `quartz_collection` - Quartz
- `obsidian_collection` - Obsidian
- `crimson_collection` - Karmesinholz
- `warped_collection` - Wirrwarrholz
- `ancient_debris_collection` - Antiker Schutt
- `echo_collection` - Echokristall

### Floor Progress
- `floor_1` bis `floor_100` - Beste Zeiten pro Ebene (in Sekunden)

## Architektur

```
LeaderboardManager (Singleton)
├── Config (LeaderboardConfig)
├── HTTP Client (HttpClient)
└── Data Collectors
    ├── StatsCollector (Scoreboard-basiert)
    ├── CollectionCollector (Chat-basiert)
    ├── FloorProgressCollector (Dimension-basiert)
    └── RareMobCollector (Integration mit MobTimerUtility)
```

## Erweiterung

### Neuen Datensammler hinzufügen

1. Erstelle neue Klasse die `DataCollector` implementiert
2. Registriere sie im `LeaderboardManager.initializeCollectors()`
3. Füge entsprechende Leaderboard-Namen zu deinem Server-Mapping hinzu

### Neue Collection hinzufügen

Erweitere das `MATERIAL_MAPPING` in `CollectionCollector.java`:

```java
MATERIAL_MAPPING.put("neues_material", "neues_material_collection");
```

## Troubleshooting

### "Leaderboard-System ist nicht registriert"
- Prüfe Server-Verbindung
- Verwende `/leaderboard refresh`
- Aktiviere Debug-Modus für detaillierte Logs

### Scores werden nicht gesendet
- Prüfe `/leaderboard status`
- Stelle sicher, dass das System aktiviert ist
- Prüfe Server-Logs für Fehler

### Chat-Nachrichten werden nicht erkannt
- Prüfe die Pattern in `CollectionCollector`
- Aktiviere Debug-Modus
- Teste mit `/leaderboard test`
