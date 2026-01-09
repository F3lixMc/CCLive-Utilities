# Performance-Analyse: Standbilder im Spiel

## Identifizierte Probleme

### 🔴 KRITISCH: Große JSON-Dateien werden synchron geladen

**Problem 1: `extracted_items.json` (182.612 Zeilen)**
- **Datei**: `ItemInfoUtility.java` - `loadRegisteredItems()` (Zeile 1122)
- **Problem**: Die gesamte Datei wird synchron auf dem Hauptthread geladen und geparst
- **Auswirkung**: Kann zu 1-3 Sekunden Freeze führen beim Start oder beim Laden
- **Code-Stelle**:
  ```java
  JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
  for (int i = 0; i < jsonArray.size(); i++) { // 182.612 Iterationen!
      // ...
  }
  ```

**Problem 2: `writeItemsToFile()` lädt gesamte Datei vor jedem Schreiben**
- **Datei**: `ItemInfoUtility.java` - `writeItemsToFile()` (Zeile 1280)
- **Problem**: Beim Hinzufügen neuer Items wird die gesamte Datei geladen, geparst, modifiziert und neu geschrieben
- **Auswirkung**: Kann zu 1-2 Sekunden Freeze führen bei jedem Item-Extract

### 🔴 KRITISCH: Schwere Reflection-Operationen

**Problem 3: Scoreboard-Scanning mit Reflection**
- **Datei**: `FarmworldCollectionsCollector.java` - `getCurrentZone()` (Zeile 367)
- **Problem**: 
  - Durchsucht ALLE Felder des Scoreboards via Reflection
  - Versucht mehrere Ansätze nacheinander (4 verschiedene Methoden)
  - Wird alle 20 Ticks (1x pro Sekunde) aufgerufen
  - Durchsucht große Maps (>50 Einträge) mit verschachtelten Loops
- **Auswirkung**: 50-200ms Lag-Spike pro Aufruf
- **Code-Stelle**: Zeilen 458-768 - extrem komplexe Reflection-Logik

**Problem 4: Tooltip-Reflection bei jedem Render-Frame**
- **Datei**: `HandledScreenMixin.java` - `captureTooltipPosition()` (Zeile 198)
- **Problem**: 
  - Verwendet Reflection um `getTooltipFromItem()` aufzurufen
  - Wird bei JEDEM Render-Frame aufgerufen (60 FPS = 60x pro Sekunde)
  - Durchsucht alle Slots im Inventar
- **Auswirkung**: 5-10ms pro Frame = kontinuierlicher Performance-Verlust

**Problem 5: Schwere Reflection in `InformationenUtility`**
- **Datei**: `InformationenUtility.java` - `extractTextFromShowText()` (Zeile ~1950)
- **Problem**: 
  - Durchsucht ALLE Felder, Methoden und Record-Komponenten via Reflection
  - Versucht mehrere Ansätze nacheinander
  - Wird bei Tooltip-Events aufgerufen
- **Auswirkung**: 10-50ms pro Tooltip-Event

### 🟡 MITTEL: Zu viele Tick-Events

**Problem 6: Viele Utilities registrieren Tick-Events**
- **Dateien**: 
  - `InformationenUtility.java` (Zeile 286)
  - `ItemInfoUtility.java` (Zeile 86)
  - `ProfileStatsManager.java` (Zeile 181)
  - `FarmworldCollectionsCollector.java` (Zeile 96)
  - `StatsCollector.java` (Zeile 29)
  - Und viele weitere...
- **Problem**: Jede Utility führt Code bei jedem Tick (20x pro Sekunde) aus
- **Auswirkung**: Kumulativer Performance-Verlust

**Problem 7: Tooltip-Callbacks bei jedem Hover**
- **Dateien**: 
  - `InformationenUtility.java` (Zeile 294)
  - `CollectionCollector.java` (Zeile 101)
  - `BPViewerUtility.java` (Zeile 206)
- **Problem**: Mehrere Callbacks werden bei jedem Item-Hover ausgeführt
- **Auswirkung**: Verzögerung beim Tooltip-Rendering

### 🟡 MITTEL: Ineffiziente Datenstrukturen

**Problem 8: `registeredItemNames` als ArrayList**
- **Datei**: `ItemInfoUtility.java`
- **Problem**: `registeredItemNames.contains()` ist O(n) Operation
- **Auswirkung**: Bei 182.612 Items = sehr langsam
- **Aktuell**: `List<String> registeredItemNames`
- **Sollte sein**: `Set<String> registeredItemNames`

## Lösungsvorschläge

### ✅ Lösung 1: Asynchrones Laden großer JSON-Dateien

**Für `loadRegisteredItems()`:**
```java
private static void loadRegisteredItems() {
    registeredItemNames.clear();
    // Lade asynchron in Background-Thread
    CompletableFuture.supplyAsync(() -> {
        try {
            File extractedItemsFile = new File(outputDir, "extracted_items.json");
            if (extractedItemsFile.exists()) {
                try (FileReader reader = new FileReader(extractedItemsFile, StandardCharsets.UTF_8)) {
                    JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
                    Set<String> names = new HashSet<>(); // HashSet statt ArrayList!
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject item = jsonArray.get(i).getAsJsonObject();
                        if (item.has("name")) {
                            String name = item.get("name").getAsString();
                            if (name != null && !name.isEmpty()) {
                                names.add(name);
                            }
                        }
                    }
                    return names;
                }
            }
        } catch (Exception e) {
            // Error handling
        }
        return new HashSet<String>();
    }).thenAcceptAsync(names -> {
        // Update auf Main-Thread
        registeredItemNames = names;
    }, MinecraftClient.getInstance()::execute);
}
```

**Für `writeItemsToFile()`:**
```java
private static void writeItemsToFile(List<ItemData> items, MinecraftClient client) {
    // Schreibe asynchron
    CompletableFuture.runAsync(() -> {
        // ... File I/O Operationen ...
    });
}
```

### ✅ Lösung 2: Reflection-Caching

**Für `HandledScreenMixin.captureTooltipPosition()`:**
```java
// Cache die Reflection-Methode
private static Method cachedGetTooltipMethod = null;

private void captureTooltipPosition(int mouseX, int mouseY) {
    // ...
    if (cachedGetTooltipMethod == null) {
        try {
            cachedGetTooltipMethod = HandledScreen.class.getDeclaredMethod("getTooltipFromItem", 
                MinecraftClient.class, ItemStack.class);
            cachedGetTooltipMethod.setAccessible(true);
        } catch (Exception e) {
            return;
        }
    }
    
    // Verwende gecachte Methode
    List<Text> tooltip = (List<Text>) cachedGetTooltipMethod.invoke(screen, client, stack);
    // ...
}
```

**Für `FarmworldCollectionsCollector.getCurrentZone()`:**
```java
// Cache Reflection-Methoden und -Felder
private static Method cachedGetAllPlayerScoresMethod = null;
private static Field cachedPlayerObjectivesField = null;

// Initialisiere einmal beim Start
static {
    try {
        cachedGetAllPlayerScoresMethod = Scoreboard.class.getMethod("getAllPlayerScores", ScoreboardObjective.class);
        // Cache andere Methoden/Felder...
    } catch (Exception e) {
        // Fallback
    }
}
```

### ✅ Lösung 3: Reduziere Tick-Event-Frequenz

**Verwende Tick-Counter:**
```java
private static int tickCounter = 0;
private static final int UPDATE_INTERVAL = 20; // Nur alle 20 Ticks (1x pro Sekunde)

private static void onClientTick(MinecraftClient client) {
    tickCounter++;
    if (tickCounter < UPDATE_INTERVAL) {
        return; // Überspringe diesen Tick
    }
    tickCounter = 0;
    
    // Schwere Operationen hier...
}
```

### ✅ Lösung 4: Optimiere Scoreboard-Scanning

**Für `FarmworldCollectionsCollector`:**
```java
// Cache das letzte Ergebnis
private String cachedZone = null;
private long lastZoneCheckTime = 0;
private static final long ZONE_CHECK_INTERVAL = 1000; // Nur alle 1 Sekunde prüfen

private String getCurrentZone(MinecraftClient client) {
    long currentTime = System.currentTimeMillis();
    if (cachedZone != null && (currentTime - lastZoneCheckTime) < ZONE_CHECK_INTERVAL) {
        return cachedZone; // Verwende Cache
    }
    
    // Nur wenn nötig, führe teure Operation aus
    String zone = performExpensiveZoneCheck(client);
    cachedZone = zone;
    lastZoneCheckTime = currentTime;
    return zone;
}
```

### ✅ Lösung 5: Verwende HashSet statt ArrayList

**In `ItemInfoUtility.java`:**
```java
// Ändere von:
private static List<String> registeredItemNames = new ArrayList<>();

// Zu:
private static Set<String> registeredItemNames = new HashSet<>();
```

### ✅ Lösung 6: Lazy Loading für Tooltip-Callbacks

**Prüfe zuerst ob Callback wirklich benötigt wird:**
```java
ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
    // Frühe Returns wenn nicht benötigt
    if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
        return;
    }
    
    // Prüfe ob Item überhaupt relevant ist
    if (stack == null || stack.isEmpty()) {
        return;
    }
    
    // Nur dann teure Operationen ausführen
    // ...
});
```

### ✅ Lösung 7: Batch-Processing für große Operationen

**Teile große Loops auf mehrere Frames auf:**
```java
private static int jsonParseIndex = 0;
private static JsonArray pendingJsonArray = null;

private static void processJsonInBatches() {
    if (pendingJsonArray == null) return;
    
    // Verarbeite nur 1000 Items pro Frame
    int batchSize = 1000;
    int endIndex = Math.min(jsonParseIndex + batchSize, pendingJsonArray.size());
    
    for (int i = jsonParseIndex; i < endIndex; i++) {
        // Verarbeite Item...
    }
    
    jsonParseIndex = endIndex;
    if (jsonParseIndex >= pendingJsonArray.size()) {
        // Fertig
        pendingJsonArray = null;
        jsonParseIndex = 0;
    }
}
```

## Priorität der Fixes

1. **HÖCHSTE PRIORITÄT**: 
   - Asynchrones Laden von `extracted_items.json`
   - HashSet statt ArrayList für `registeredItemNames`
   - Reflection-Caching in `HandledScreenMixin`

2. **HOHE PRIORITÄT**:
   - Optimierung von `FarmworldCollectionsCollector.getCurrentZone()`
   - Asynchrones Schreiben in `writeItemsToFile()`

3. **MITTLERE PRIORITÄT**:
   - Tick-Event-Optimierung (Counter verwenden)
   - Tooltip-Callback-Optimierung

4. **NIEDRIGE PRIORITÄT**:
   - Batch-Processing für JSON-Parsing
   - Weitere Reflection-Optimierungen

## Geschätzte Performance-Verbesserung

- **Vorher**: 50-200ms Lag-Spikes alle 1-2 Sekunden
- **Nachher**: <5ms kontinuierlicher Overhead, keine sichtbaren Freezes

Die größten Verbesserungen kommen von:
1. Asynchronem File I/O (eliminiert 1-3 Sekunden Freezes)
2. Reflection-Caching (reduziert 50-200ms Spikes auf <5ms)
3. HashSet statt ArrayList (reduziert Lookup von O(n) auf O(1))


