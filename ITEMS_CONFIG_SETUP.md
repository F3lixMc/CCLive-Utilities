# Items Config Setup - Anleitung

Diese Anleitung beschreibt, wie das Server-seitige Update-System für `items.json` eingerichtet wird.

## 1. SQL-Tabelle erstellen

Führe folgenden SQL-Befehl in deiner MySQL-Datenbank aus:

```sql
CREATE TABLE IF NOT EXISTS `items_config` (
  `id` INT NOT NULL DEFAULT 1,
  `data` LONGTEXT NOT NULL,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Erklärung:**
- `id`: Primärschlüssel (immer 1, da nur eine Version existiert)
- `data`: Die komplette `items.json` als JSON-String (LONGTEXT für große Dateien)
- `version`: Versionsnummer, die bei jedem Update automatisch erhöht wird

**Initiale Daten einfügen (optional):**
Wenn du bereits eine `items.json` hast, kannst du sie initial einfügen:

```sql
INSERT INTO `items_config` (`id`, `data`, `version`) 
VALUES (1, '{"items": []}', 1)
ON DUPLICATE KEY UPDATE `data` = VALUES(`data`);
```

**Hinweis:** Ersetze `'{"items": []}'` mit dem tatsächlichen JSON-Inhalt deiner `items.json` (als String escaped).

## 2. Server-Endpoints

Die Server-Endpoints sind bereits in `server.js` implementiert:

- **GET `/items/version`**: Gibt die aktuelle Version zurück
- **GET `/items/data`**: Gibt die JSON-Daten zurück
- **POST `/admin/items`**: Speichert neue Items-Config (erfordert Admin-Key)
- **GET `/admin/items`**: Gibt aktuelle Items-Config für Admin zurück

## 3. Website-Integration

Die Website-Erweiterung ist in `admin.html` und `script.js` implementiert:

- Dropdown "Config" → Option "Items Data"
- Textarea für JSON-Inhalt
- Upload-Button zum Speichern
- Versionsanzeige

## 4. Mod-seitige Implementierung

Die Mod-seitige Implementierung ist bereits vorhanden in:
- `ItemViewerUtility.java` → `checkAndUpdateItemsConfig()`
- Prüft beim Server-Join die Version
- Lädt automatisch neue Version, wenn Server-Version > lokale Version
- Speichert lokal in `config/cclive-utilities/items.json`

## 5. Testen

1. **Server testen:**
   ```bash
   # Version abrufen
   curl http://localhost:2062/items/version
   
   # Daten abrufen
   curl http://localhost:2062/items/data
   ```

2. **Admin-Upload testen:**
   - Öffne `http://localhost:2062/admin`
   - Logge dich mit Admin-Key ein
   - Wähle "Config" → "Items Data"
   - Füge JSON-Inhalt ein und klicke "Speichern"

3. **Mod testen:**
   - Starte Minecraft mit der Mod
   - Verbinde dich mit dem Server
   - Die Mod sollte automatisch die neue Version herunterladen (wenn verfügbar)

## 6. Troubleshooting

**Problem: "Table doesn't exist"**
- Führe den SQL-Befehl aus Schritt 1 aus

**Problem: "Version wird nicht erhöht"**
- Prüfe, ob die SQL-Tabelle korrekt erstellt wurde
- Prüfe Server-Logs auf Fehler

**Problem: "Mod lädt keine Updates"**
- Prüfe, ob `LeaderboardManager` aktiviert ist
- Prüfe, ob Server-URL korrekt konfiguriert ist
- Prüfe Mod-Logs auf Fehler
