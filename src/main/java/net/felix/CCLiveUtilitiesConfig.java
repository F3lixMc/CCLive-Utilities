package net.felix;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.felix.utilities.Town.ItemDisplayMode;
import net.felix.utilities.Town.OverlayType;
import net.felix.utilities.Town.SchmiedItemDisplayMode;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CCLiveUtilitiesConfig {
    public static final ConfigClassHandler<CCLiveUtilitiesConfig> HANDLER = ConfigClassHandler.createBuilder(CCLiveUtilitiesConfig.class)
            .id(Identifier.of(CCLiveUtilities.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("cclive-utilities.json"))
                    .build())
            .build();
    
    // Migration: Verschiebe alte Config-Datei von config/ nach config/cclive-utilities/
    public static void migrateConfigLocation() {
        try {
            java.nio.file.Path oldConfigPath = CCLiveUtilities.getConfigDir().resolve("cclive-utilities.json");
            java.nio.file.Path newConfigDir = CCLiveUtilities.getConfigDir().resolve("cclive-utilities");
            java.nio.file.Path newConfigPath = newConfigDir.resolve("cclive-utilities.json");
            
            // Erstelle neuen Ordner falls nicht vorhanden
            if (!java.nio.file.Files.exists(newConfigDir)) {
                java.nio.file.Files.createDirectories(newConfigDir);
            }
            
            // Verschiebe alte Datei falls vorhanden und neue noch nicht existiert
            if (java.nio.file.Files.exists(oldConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldConfigPath, newConfigPath);
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    // Migration von String zu Enum
    public static void migrateOverlayType() {
        CCLiveUtilitiesConfig config = HANDLER.instance();
        if (config.equipmentDisplayOverlayTypeString != null && !config.equipmentDisplayOverlayTypeString.isEmpty()) {
            // Migriere von String zu Enum
            if ("Bild-Overlays".equals(config.equipmentDisplayOverlayTypeString)) {
                config.equipmentDisplayOverlayType = OverlayType.CUSTOM;
            } else if ("Schwarze Overlays".equals(config.equipmentDisplayOverlayTypeString)) {
                config.equipmentDisplayOverlayType = OverlayType.BLACK;
            } else if ("Kein Hintergrund".equals(config.equipmentDisplayOverlayTypeString)) {
                config.equipmentDisplayOverlayType = OverlayType.NONE;
            } else {
                config.equipmentDisplayOverlayType = OverlayType.CUSTOM; // Standardwert
            }
            // Lösche den alten String-Wert
            config.equipmentDisplayOverlayTypeString = null;
            // Speichere die migrierte Konfiguration
            HANDLER.save();
        }
        
        // Migration für Blueprint Viewer Overlay-Typ (nur wenn noch auf Standardwert)
        if (config.blueprintViewerOverlayType == OverlayType.CUSTOM) {
            if (config.blueprintViewerShowBackground) {
                config.blueprintViewerOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
            } else {
                config.blueprintViewerOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
            }
        }
        // blueprintViewerShowBackground wird für Abwärtskompatibilität beibehalten
        
        // Migration für Karten Overlay-Typ (nur wenn noch auf Standardwert)
        if (config.cardOverlayType == OverlayType.CUSTOM) {
            if (config.cardShowBackground) {
                config.cardOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
            } else {
                config.cardOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
            }
        }
        // cardShowBackground wird für Abwärtskompatibilität beibehalten
        
        // Migration für Statuen Overlay-Typ (nur wenn noch auf Standardwert)
        if (config.statueOverlayType == OverlayType.CUSTOM) {
            if (config.statueShowBackground) {
                config.statueOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
            } else {
                config.statueOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
            }
        }
        // statueShowBackground wird für Abwärtskompatibilität beibehalten
        
        // Migration für hoverStatsChosenStat von String zu Enum
        if (config.hoverStatsChosenStatString != null && !config.hoverStatsChosenStatString.isEmpty()) {
            config.hoverStatsChosenStat = net.felix.profile.PlayerHoverStatsUtility.HoverStatsType.fromString(config.hoverStatsChosenStatString);
            config.hoverStatsChosenStatString = null;
            HANDLER.save();
        }
    }
    
    /**
     * Migriert Config-Felder: Fügt fehlende Felder mit Default-Werten hinzu
     * Dies stellt sicher, dass bei Updates neue Felder automatisch zur Config hinzugefügt werden
     */
    /**
     * Benennt gespeicherte Config-Keys von tabInfo/showTabInfo zu npcAlerts/showNpcAlerts um.
     */
    public static void migrateTabInfoToNpcAlerts() {
        try {
            JsonObject raw = readRawConfigJson();
            if (raw == null) {
                return;
            }
            boolean changed = false;
            java.util.List<String> keys = new java.util.ArrayList<>(raw.keySet());
            for (String key : keys) {
                String newKey = null;
                if (key.startsWith("showTabInfo")) {
                    newKey = "showNpcAlerts" + key.substring("showTabInfo".length());
                } else if (key.startsWith("tabInfo")) {
                    newKey = "npcAlerts" + key.substring("tabInfo".length());
                }
                if (newKey != null) {
                    if (!raw.has(newKey)) {
                        raw.add(newKey, raw.get(key));
                    }
                    raw.remove(key);
                    changed = true;
                }
            }
            if (changed) {
                java.nio.file.Path configPath = CCLiveUtilities.getConfigDir()
                        .resolve("cclive-utilities")
                        .resolve("cclive-utilities.json");
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                java.nio.file.Files.writeString(configPath, gson.toJson(raw));
                HANDLER.load();
            }
        } catch (Exception ignored) {
        }
    }

    public static void migrateConfigFields() {
        migrateTabInfoToNpcAlerts();
        try {
            CCLiveUtilitiesConfig loadedConfig = HANDLER.instance();
            CCLiveUtilitiesConfig defaultConfig = new CCLiveUtilitiesConfig(); // Neue Instanz = alle Default-Werte
            JsonObject rawConfig = readRawConfigJson();
            if (rawConfig == null) {
                return;
            }
            boolean configChanged = false;
            
            // Durchlaufe alle Felder der Config-Klasse
            java.lang.reflect.Field[] fields = CCLiveUtilitiesConfig.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // Prüfe ob das Feld mit @SerialEntry annotiert ist
                if (field.isAnnotationPresent(SerialEntry.class)) {
                    field.setAccessible(true);
                    
                    try {
                        Object defaultValue = field.get(defaultConfig);
                        String fieldName = field.getName();
                        
                        // Nur migrieren, wenn das Feld in der Datei fehlt oder null ist
                        boolean missingInFile = !rawConfig.has(fieldName) || rawConfig.get(fieldName).isJsonNull();
                        if (missingInFile && defaultValue != null) {
                            field.set(loadedConfig, defaultValue);
                            configChanged = true;
                        }
                    } catch (IllegalAccessException e) {
                        // Feld konnte nicht gelesen/geschrieben werden, überspringe
                    }
                }
            }
            
            // Speichere Config nur wenn Änderungen vorgenommen wurden
            if (configChanged) {
                HANDLER.save();
            }
        } catch (Exception e) {
            // Silent error handling - Migration schlägt fehl, aber Mod funktioniert weiter
        }
    }

    /**
     * Liest die aktuelle Config-Datei als JsonObject, um fehlende Felder zu erkennen
     */
    private static JsonObject readRawConfigJson() {
        try {
            java.nio.file.Path configPath = CCLiveUtilities.getConfigDir()
                    .resolve("cclive-utilities")
                    .resolve("cclive-utilities.json");
            if (!java.nio.file.Files.exists(configPath)) {
                return null;
            }
            String content = java.nio.file.Files.readString(configPath);
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
    
    // General Settings
    @SerialEntry
    public boolean enableMod = true;
    
    @SerialEntry
    public boolean showDebugInfo = false;
    
    @SerialEntry
    public boolean showPlayerNametagIcon = true; // Mod-Icon über Spielernamen anzeigen
    
    // Chat Settings
    @SerialEntry
    public boolean chatIconEnabled = true; // Chat-Icon hinter Spielernamen im Chat anzeigen
    
    // Leaderboard Settings
    @SerialEntry
    public boolean trackerActivityEnabled = true; // Tracker-Aktivität aktivieren (Updates senden und Trackings anzeigen)
    
    // Debug Settings
    @SerialEntry
    public boolean updateCheckerEnabled = true;
    
    @SerialEntry
    public int zeichenConfigVersion = 0; // Version der geladenen zeichen.json vom Server
    
    @SerialEntry
    public int itemsConfigVersion = 0; // Version der geladenen items.json vom Server
    
    @SerialEntry
    public int aincraftConfigVersion = 0; // Version der geladenen Aincraft.json vom Server
    
    @SerialEntry
    public int aspekteConfigVersion = 0; // Version der geladenen Aspekte.json vom Server
    
    @SerialEntry
    public int blueprintsConfigVersion = 0; // Version der geladenen blueprints.json vom Server
    
    @SerialEntry
    public int cardsstatuesConfigVersion = 0; // Version der geladenen CardsStatues.json vom Server
    
    @SerialEntry
    public int collectionsConfigVersion = 0; // Version der geladenen Collections.json vom Server
    
    @SerialEntry
    public int essenzConfigVersion = 0; // Version der geladenen Essenz.json vom Server
    
    @SerialEntry
    public int farmworldConfigVersion = 0; // Version der geladenen Farmworld.json vom Server
    
    @SerialEntry
    public int kitsConfigVersion = 0; // Version der geladenen Kits.json vom Server
    
    @SerialEntry
    public int mklevelConfigVersion = 0; // Version der geladenen MKLevel.json vom Server
    
    @SerialEntry
    public boolean blueprintDebugging = false;
    
    @SerialEntry
    public boolean leaderboardDebugging = false;
    
    // Player Stats Debug Settings
    @SerialEntry
    public boolean playerStatsDebugging = false;
    
    @SerialEntry
    public boolean debugFunctionsEnabled = false; // Debug Funktionen aktivieren/deaktivieren
    
    // Player Hover Stats Settings
    @SerialEntry
    public net.felix.profile.PlayerHoverStatsUtility.HoverStatsType hoverStatsChosenStat = net.felix.profile.PlayerHoverStatsUtility.HoverStatsType.PLAYTIME; // Stat die in Chat-Hover-Events angezeigt wird
    
    // Migration für hoverStatsChosenStat von String zu Enum
    @SerialEntry
    public String hoverStatsChosenStatString = null; // Für Migration (veraltet)

    // Equipment Display Settings
    @SerialEntry
    public boolean equipmentDisplayEnabled = true;
    
    @SerialEntry
    public boolean showEquipmentDisplay = true;
    
    @SerialEntry
    public Color equipmentDisplayTextColor = new Color(0xE6FFFFFF); // Textfarbe (etwas transparenter)
    
    @SerialEntry
    public Color equipmentDisplayHeaderColor = new Color(0xFFFFFF00); // Überschriftenfarbe (Gelb)
    
    @SerialEntry
    public int equipmentDisplayArmorX = -134; // X-Position der Rüstungsanzeige (optimiert)
    
    @SerialEntry
    public int equipmentDisplayArmorY = 300; // Y-Position der Rüstungsanzeige (Pixel vom unteren Rand, optimiert)
    
    // equipmentDisplayShowBackground wurde durch equipmentDisplayOverlayType ersetzt
    // true = CUSTOM/BLACK, false = NONE
    
    @SerialEntry
    public OverlayType equipmentDisplayOverlayType = OverlayType.CUSTOM; // CUSTOM = Bild-Overlays, BLACK = Schwarze Overlays
    
    // Migration von String zu Enum
    @SerialEntry
    public String equipmentDisplayOverlayTypeString = "Bild-Overlays"; // Für Migration von alten Konfigurationen

    // Karten und Statuen Overlay Type Settings
    @SerialEntry
    public OverlayType cardOverlayType = OverlayType.CUSTOM; // CUSTOM = Bild-Overlays, BLACK = Schwarze Overlays, NONE = Kein Hintergrund
    
    @SerialEntry
    public OverlayType statueOverlayType = OverlayType.CUSTOM; // CUSTOM = Bild-Overlays, BLACK = Schwarze Overlays, NONE = Kein Hintergrund
    
    // Migration von Boolean zu Enum für Karten und Statuen
    @SerialEntry
    public boolean cardShowBackground = true; // Schwarzer Hintergrund für Card Overlay (veraltet, wird durch cardOverlayType ersetzt)
    
    @SerialEntry
    public boolean statueShowBackground = true; // Schwarzer Hintergrund für Statue Overlay (veraltet, wird durch statueOverlayType ersetzt)

    // Kills Utility Settings
    @SerialEntry
    public boolean killsUtilityEnabled = true;
    
    @SerialEntry
    public boolean showKillsUtility = true;
    
    @SerialEntry
    public int killsUtilityX = 570; // X-Position der Kills-Anzeige (Pixel vom rechten Rand, optimiert)
    
    @SerialEntry
    public int killsUtilityY = 100; // Y-Position der Kills-Anzeige (Pixel vom oberen Rand, optimiert)
    
    @SerialEntry
    public Color killsUtilityHeaderColor = new Color(0xFFFFFF00); // Überschriftenfarbe (Gelb)
    
    @SerialEntry
    public Color killsUtilityTextColor = new Color(0xE6FFFFFF); // Textfarbe (Weiß mit Transparenz)
    
    @SerialEntry
    public boolean killsUtilityShowBackground = true; // Schwarzer Hintergrund für Kills Utility
    
    @SerialEntry
    public float killsUtilityScale = 1.0f; // Skalierung der Kills-Anzeige (0.5f bis 2.0f)
    
    @SerialEntry
    public boolean killsUtilityShowRequiredKills = true; // "Benötigte Kills" Zeile anzeigen
    
    @SerialEntry
    public boolean killsUtilityShowNextLevel = true; // "Nächste Ebene" Zeile anzeigen

    // Coin Tracker Settings
    @SerialEntry
    public boolean coinTrackerEnabled = true;

    @SerialEntry
    public boolean showCoinTracker = true;

    @SerialEntry
    public int coinTrackerX = 570;

    @SerialEntry
    public int coinTrackerY = 180;

    @SerialEntry
    public Color coinTrackerHeaderColor = new Color(0xFFFFFF00);

    @SerialEntry
    public Color coinTrackerTextColor = new Color(0xE6FFFFFF);

    @SerialEntry
    public boolean coinTrackerShowBackground = true;

    @SerialEntry
    public float coinTrackerScale = 1.0f;

    @SerialEntry
    public CoinTrackerDisplayMode coinTrackerDisplayMode = CoinTrackerDisplayMode.OVERLAY;


    // Schmied Tracker Settings
    @SerialEntry
    public boolean schmiedTrackerEnabled = true;
    
    @SerialEntry
    public boolean showSchmiedTracker = true;
    
    @SerialEntry
    public boolean showMaterialTooltips = true; // Material tooltips in hover events
    
    @SerialEntry
    public boolean showSchmiedezustaendeInAusrüstungsMenü = true; // Schmiedezustände in Ausrüstungs-Menüs anzeigen
    
    // Informationen Utility Settings
    @SerialEntry
    public boolean informationenUtilityEnabled = true; // Informationen Utility aktivieren
    
    @SerialEntry
    public boolean showEbenenInSpecialInventory = true; // Ebenen in speziellem Inventar "㬉" anzeigen
    
    @SerialEntry
    public boolean showEbenenInNormalInventories = true; // Ebenen in normalen Inventaren anzeigen
    
    @SerialEntry
    public boolean showWaveDisplay = true; // Wellen-Anzeige bei Essenzen aktivieren
    
    @SerialEntry
    public boolean showBlueprintFloorNumber = true; // Ebenen-Nummer bei Bauplänen anzeigen
    
    @SerialEntry
    public boolean showItemViewer = true; // Item Viewer anzeigen

    @SerialEntry
    public String itemViewerMaxSlots = "360"; // Maximale Slot-Anzahl im Item Viewer (Textfeld)

    /** Standard und Fallback für {@link #itemViewerMaxSlots}. */
    public static final int DEFAULT_ITEM_VIEWER_MAX_SLOTS = 360;

    public static int getItemViewerMaxSlots() {
        try {
            int value = Integer.parseInt(HANDLER.instance().itemViewerMaxSlots.trim());
            return Math.max(1, value);
        } catch (NumberFormatException ignored) {
            return DEFAULT_ITEM_VIEWER_MAX_SLOTS;
        }
    }
    
    // Farmwelt Settings
    @SerialEntry
    public boolean showModuleInformation = true; // Informationen für Module anzeigen
    
    @SerialEntry
    public boolean showLicenseInformation = true; // Informationen für Lizenzen anzeigen
    
    @SerialEntry
    public boolean miningLumberjackOverlayEnabled = true; // Holzfäller / Bergbau Overlay ein/aus

    @SerialEntry
    public ResourceTrackerDisplayMode resourceTrackerDisplayMode = ResourceTrackerDisplayMode.OVERLAY;

    @SerialEntry
    public boolean resourceTrackerRateEnabled = false;

    // Mining & Lumberjack Overlay Settings
    @SerialEntry
    public boolean miningOverlayEnabled = true; // Bergbau Overlay aktivieren
    
    @SerialEntry
    public boolean showMiningOverlay = true; // Bergbau Overlay anzeigen
    
    @SerialEntry
    public int miningOverlayX = 5; // X-Position des Bergbau Overlays (am linken Bildschirmrand)
    
    @SerialEntry
    public int miningOverlayY = 200; // Y-Position des Bergbau Overlays (mittig am linken Rand)
    
    @SerialEntry
    public boolean miningOverlayShowBackground = true; // Schwarzer Hintergrund für Bergbau Overlay
    
    @SerialEntry
    public boolean lumberjackOverlayEnabled = true; // Holzfäller Overlay aktivieren
    
    @SerialEntry
    public boolean showLumberjackOverlay = true; // Holzfäller Overlay anzeigen
    
    @SerialEntry
    public int lumberjackOverlayX = 10; // X-Position des Holzfäller Overlays
    
    @SerialEntry
    public int lumberjackOverlayY = 100; // Y-Position des Holzfäller Overlays
    
    @SerialEntry
    public boolean lumberjackOverlayShowBackground = true; // Schwarzer Hintergrund für Holzfäller Overlay
    
    @SerialEntry
    public float miningLumberjackOverlayScale = 1.0f; // Skalierung der Mining/Holzfäller Overlays

    

    
    @SerialEntry
    public int schmiedTrackerX = 5; // X-Position des Schmied Trackers (optimiert)
    
    @SerialEntry
    public int schmiedTrackerY = 100; // Y-Position des Schmied Trackers (optimiert)
    
    @SerialEntry
    public boolean schmiedTrackerShowBackground = true; // Schwarzer Hintergrund für Schmied Tracker
    
    @SerialEntry
    public net.felix.utilities.Town.SchmiedItemDisplayMode schmiedTrackerItemDisplayMode = net.felix.utilities.Town.SchmiedItemDisplayMode.BORDER; // Anzeigemodus für Schmied-Items (Rahmen oder Hintergrund)
    
    @SerialEntry
    public boolean hideUncraftableEnabled = true; // Hide Uncraftable Button aktiviert
    
    @SerialEntry
    public boolean hideUncraftableUseUnicodeDetection = true; // Unicode-Zeichen Erkennung für Crafting-Status
    

    
    @SerialEntry
    public boolean hideUncraftableUseImprovedDetection = true; // Verbesserte Unicode-Erkennung
    
    @SerialEntry
    public boolean hideUncraftableUseAlternativeAnalysis = true; // Alternative Analyse-Methode
    
    @SerialEntry
    public int hideUncraftableButtonX = -80; // X-Position des Hide Uncraftable Buttons (Offset)
    
    @SerialEntry
    public int hideUncraftableButtonY = 54; // Y-Position des Hide Uncraftable Buttons (Offset)
    
    @SerialEntry
    public float hideUncraftableButtonScale = 1.0f; // Skalierung des Hide Uncraftable Buttons
    
    @SerialEntry
    public boolean hideWrongClassEnabled = true; // Hide Wrong Class Button aktiviert
    
    @SerialEntry
    public boolean showHideWrongClassButton = true; // Hide Wrong Class Button anzeigen/ausblenden
    
    @SerialEntry
    public int hideWrongClassButtonX = -80; // X-Position des Hide Wrong Class Buttons (Offset)
    
    @SerialEntry
    public int hideWrongClassButtonY = 80; // Y-Position des Hide Wrong Class Buttons (Offset)
    
    @SerialEntry
    public float hideWrongClassButtonScale = 1.0f; // Skalierung des Hide Wrong Class Buttons
    
    // Kit Filter Button Positions
    @SerialEntry
    public int kitFilterButton1X = -100; // X-Position des Kit Filter Button 1 (Offset)
    
    @SerialEntry
    public int kitFilterButton1Y = 50; // Y-Position des Kit Filter Button 1 (Offset)
    
    @SerialEntry
    public float kitFilterButton1Scale = 1.0f; // Skalierung des Kit Filter Button 1
    
    @SerialEntry
    public int kitFilterButton2X = -100; // X-Position des Kit Filter Button 2 (Offset)
    
    @SerialEntry
    public int kitFilterButton2Y = 75; // Y-Position des Kit Filter Button 2 (Offset)
    
    @SerialEntry
    public float kitFilterButton2Scale = 1.0f; // Skalierung des Kit Filter Button 2
    
    @SerialEntry
    public int kitFilterButton3X = -100; // X-Position des Kit Filter Button 3 (Offset)
    
    @SerialEntry
    public int kitFilterButton3Y = 100; // Y-Position des Kit Filter Button 3 (Offset)
    
    @SerialEntry
    public float kitFilterButton3Scale = 1.0f; // Skalierung des Kit Filter Button 3
    
    // Kit Filter Button Selections (persistent)
    @SerialEntry
    public String kitFilterButton1KitType = ""; // Kit-Typ für Button 1 (z.B. "MÜNZ_KIT")
    
    @SerialEntry
    public int kitFilterButton1Level = 1; // Level für Button 1 (1-7)
    
    @SerialEntry
    public String kitFilterButton1CustomKitId = ""; // Eigene Kit-ID für Button 1
    
    @SerialEntry
    public boolean kitFilterButton1NeuKit = false; // Neu-Tab Kit für Button 1
    
    @SerialEntry
    public String kitFilterButton2KitType = ""; // Kit-Typ für Button 2
    
    @SerialEntry
    public int kitFilterButton2Level = 1; // Level für Button 2 (1-7)
    
    @SerialEntry
    public String kitFilterButton2CustomKitId = ""; // Eigene Kit-ID für Button 2
    
    @SerialEntry
    public boolean kitFilterButton2NeuKit = false; // Neu-Tab Kit für Button 2
    
    @SerialEntry
    public String kitFilterButton3KitType = ""; // Kit-Typ für Button 3
    
    @SerialEntry
    public int kitFilterButton3Level = 1; // Level für Button 3 (1-7)
    
    @SerialEntry
    public String kitFilterButton3CustomKitId = ""; // Eigene Kit-ID für Button 3
    
    @SerialEntry
    public boolean kitFilterButton3NeuKit = false; // Neu-Tab Kit für Button 3
    
    @SerialEntry
    public boolean kitFilterButtonsEnabled = true; // Kit Filter Buttons ein-/ausblenden
    
    // Schmied Tracker - Individual Settings
    @SerialEntry
    public boolean frostgeschmiedetEnabled = true;
    
    @SerialEntry
    public Color frostgeschmiedetColor = new Color(0x0066FF); // Blau
    
    @SerialEntry
    public boolean lavageschmiedetEnabled = true;
    
    @SerialEntry
    public Color lavageschmiedetColor = new Color(0xcb0e0e); // Rot
    
    @SerialEntry
    public boolean titangeschmiedetEnabled = true;
    
    @SerialEntry
    public Color titangeschmiedetColor = new Color(0x0FD456); // Grün
    
    @SerialEntry
    public boolean drachengeschmiedetEnabled = true;
    
    @SerialEntry
    public Color drachengeschmiedetColor = new Color(0xFF6600); // Orange
    
    @SerialEntry
    public boolean daemonengeschmiedetEnabled = true;
    
    @SerialEntry
    public Color daemonengeschmiedetColor = new Color(0xcf22c9); // Lila
    
    @SerialEntry
    public boolean blitzgeschmiedetEnabled = true;
    
    @SerialEntry
    public Color blitzgeschmiedetColor = new Color(0xFFD700); // Gold
    
    @SerialEntry
    public boolean sternengeschmiedetEnabled = true;
    
    @SerialEntry
    public boolean sternengeschmiedetRainbow = true; // Regenbogen-Animation
    
    @SerialEntry
    public Color sternengeschmiedetColor = new Color(0xFF00FF); // Normale Farbe (wenn Regenbogen aus)

    // Material Tracker Settings
    @SerialEntry
    public boolean materialTrackerEnabled = true;
    
    @SerialEntry
    public boolean showMaterialTracker = true;
    
    @SerialEntry
    public int materialTrackerX = 1; // Pixel vom rechten Rand (optimiert)
    
    @SerialEntry
    public int materialTrackerY = 35; // Pixel vom oberen Rand (optimiert)
    
    @SerialEntry
    public boolean materialTrackerShowBackground = true; // Schwarzer Hintergrund für Material Tracker (veraltet, wird durch materialTrackerOverlayType ersetzt)
    
    @SerialEntry
    public boolean materialTrackerUseTexture = true; // Verwende Textur-Hintergrund statt farbigen Hintergrund
    
    @SerialEntry
    public float materialTrackerScale = 1.0f; // Skalierung der Material Tracker-Anzeige (0.5f bis 2.0f)
    
    @SerialEntry
    public OverlayType materialTrackerOverlayType = OverlayType.CUSTOM; // Overlay-Typ für Material Tracker

    @SerialEntry
    public MaterialTrackerDisplayMode materialTrackerDisplayMode = MaterialTrackerDisplayMode.OVERLAY;

    @SerialEntry
    public boolean materialTrackerRateEnabled = false;

    // Collection Overlay Settings
    @SerialEntry
    public boolean showCollectionOverlay = true;
    
    @SerialEntry
    public int collectionOverlayX = 10; // X-Position des Collection Overlays
    
    @SerialEntry
    public int collectionOverlayY = 10; // Y-Position des Collection Overlays
    
    @SerialEntry
    public float collectionOverlayScale = 1.0f; // Skalierung des Collection Overlays (0.1f bis 5.0f)
    
    @SerialEntry
    public Color collectionOverlayHeaderColor = new Color(0xFFFFFF00); // Überschriftenfarbe (Gelb)
    
    @SerialEntry
    public Color collectionOverlayTextColor = new Color(0xFFFFFFFF); // Textfarbe (Weiß)

    // Clipboard Settings
    @SerialEntry
    public boolean clipboardEnabled = true;
    
    @SerialEntry
    public boolean showClipboard = true;
    
    @SerialEntry
    public int clipboardX = 10; // Pixel vom linken Rand (oben links)
    
    @SerialEntry
    public int clipboardY = 10; // Pixel vom oberen Rand (oben links)
    
    @SerialEntry
    public int clipboardWidth = 200; // Breite des Clipboard-Overlays
    
    @SerialEntry
    public int clipboardHeight = 300; // Höhe des Clipboard-Overlays
    
    @SerialEntry
    public float clipboardScale = 1.0f; // Skalierungsfaktor für das gesamte Clipboard-Overlay
    
    @SerialEntry
    public boolean clipboardShowBlueprintShopCosts = false; // Zeige Blueprint Shop Kosten an
    
    @SerialEntry
    public int clipboardCurrentPage = 1; // Aktuelle Seite im Clipboard (1-basiert)
    
    @SerialEntry
    public int clipboardTotalPages = 1; // Gesamtanzahl der Seiten im Clipboard
    
    @SerialEntry
    public boolean clipboardMaterialSortEnabled = false; // Material-Sortierung nach Ebenen aktiviert
    
    @SerialEntry
    public boolean clipboardMaterialSortAscending = true; // Material-Sortierung aufsteigend (true) oder absteigend (false)
    
    @SerialEntry
    public boolean clipboardCostDisplayEnabled = false; // Kostenanzeige-Filterung aktiviert
    
    @SerialEntry
    public int clipboardCostDisplayMode = 1; // Kostenanzeige-Modus: 1 = ausblenden, 2 = ans Ende setzen
    
    @SerialEntry
    public List<ClipboardEntryData> clipboardEntries = new ArrayList<>(); // Gespeicherte Clipboard-Einträge
    
    /**
     * Datenklasse für persistente Clipboard-Einträge
     */
    public static class ClipboardEntryData {
        @SerialEntry
        public String blueprintName;
        
        @SerialEntry
        public int quantity;
        
        @SerialEntry
        public Integer clipboardId; // Optional: Interne ID für Baupläne mit doppelten Namen
        
        public ClipboardEntryData() {
            // Für Deserialisierung benötigt
        }
        
        public ClipboardEntryData(String blueprintName, int quantity) {
            this.blueprintName = blueprintName;
            this.quantity = quantity;
            this.clipboardId = null;
        }
        
        public ClipboardEntryData(String blueprintName, int quantity, Integer clipboardId) {
            this.blueprintName = blueprintName;
            this.quantity = quantity;
            this.clipboardId = clipboardId;
        }
    }

    // Search Bar Settings
    @SerialEntry
    public boolean searchBarEnabled = true;
    
    @SerialEntry
    public boolean showSearchBar = true;
    
    @SerialEntry
    public Color searchBarFrameColor = new Color(0xFFFF0000); // Rahmenfarbe (Rot)
    
    @SerialEntry
    public boolean searchBarShowBackground = true; // Schwarzer Hintergrund für Search Bar
    
    @SerialEntry
    public ItemDisplayMode searchBarItemDisplayMode = ItemDisplayMode.BORDER; // Anzeigemodus für gefilterte Items (Rahmen oder Hintergrund)
    
    // Boss HP Settings
    @SerialEntry
    public boolean bossHPEnabled = true;
    
    @SerialEntry
    public boolean showBossHP = true;
    
    @SerialEntry
    public int bossHPX = 562; // X-Position der Boss-HP Anzeige (optimiert)
    
    @SerialEntry
    public int bossHPY = 201; // Y-Position der Boss-HP Anzeige (optimiert)
    
    @SerialEntry
    public Color bossHPTextColor = new Color(0xFFFFFFFF); // Textfarbe (Weiß)
    
    @SerialEntry
    public boolean bossHPShowBackground = true; // Schwarzer Hintergrund für Boss HP
    
    @SerialEntry
    public float bossHPScale = 1.0f; // Scale für Boss HP Overlay
    
    @SerialEntry
    public boolean bossHPShowDPM = true; // DPM (Damage Per Minute) Anzeige im Boss HP Overlay
    
    @SerialEntry
    public boolean bossHPShowLastDmg = true; // Last-Dmg-Zeile im Boss-HP-Overlay
    
    @SerialEntry
    public boolean bossHPShowOverallDmg = true; // Gesamtschaden-Zeile (Overall DMG) im Boss-HP-Overlay
    
    // MKLevel Settings
    @SerialEntry
    public boolean mkLevelEnabled = true; // MKLevel Overlay aktiviert
    
    @SerialEntry
    public float mkLevelScale = 1.0f; // Skalierung des MKLevel Overlays
    
    @SerialEntry
    public int mkLevelX = -1; // X-Position des MKLevel Overlays (-1 = automatisch rechts, >= 0 = absolute X-Position)
    
    @SerialEntry
    public int mkLevelY = -1; // Y-Position des MKLevel Overlays (-1 = am Inventar ausrichten, >= 0 = absolute Position)
    
    // Cards/Statues Settings
    @SerialEntry
    public boolean cardsStatuesEnabled = true;
    
    // Card Settings
    @SerialEntry
    public boolean cardEnabled = true;
    
    @SerialEntry
    public boolean showCard = true;
    
    @SerialEntry
    public int cardX = 151; // X-Position der Karten Anzeige (optimiert)
    
    @SerialEntry
    public int cardY = 125; // Y-Position der Karten Anzeige (optimiert)
    
    @SerialEntry
    public float cardOverlayScale = 1.2f; // Skalierung der Karten-Overlays (0.5f bis 2.0f)
    
    @SerialEntry
    public float cardTextScale = 1.0f; // Zusätzliche Text-Skalierung für Karten-Overlay (nur Text, nicht Hintergrund) - Bereich: 1.0 bis 1.5
    
    // Statue Settings
    @SerialEntry
    public boolean statueEnabled = true;
    
    @SerialEntry
    public boolean showStatue = true;
    
    @SerialEntry
    public int statueX = 151; // X-Position der Statuen Anzeige (optimiert)
    
    @SerialEntry
    public int statueY = 60; // Y-Position der Statuen Anzeige (optimiert)
    
    @SerialEntry
    public float statueOverlayScale = 1.2f; // Skalierung der Statuen-Overlays (0.5f bis 2.0f)
    
    @SerialEntry
    public float statueTextScale = 1.0f; // Zusätzliche Text-Skalierung für Statuen-Overlay (nur Text, nicht Hintergrund) - Bereich: 1.0 bis 1.5



    // Animation Blocker Settings
    @SerialEntry
    public boolean animationBlockerEnabled = true;
    
    // Kill Animation Utility Settings
    @SerialEntry
    public boolean killAnimationUtilityEnabled = false;
    

    
    // Individual Animation Blocker Settings
    @SerialEntry
    public boolean epicDropsBlockingEnabled = false;
    
    @SerialEntry
    public boolean legendaryDropsBlockingEnabled = false;
    
    @SerialEntry
    public boolean loggingLevelUpBlockingEnabled = false;
        
    @SerialEntry
    public boolean miningLevelUpBlockingEnabled = false;

    @SerialEntry
    public boolean fishingLevelUpBlockingEnabled = false;
    
    @SerialEntry
    public boolean moblexiconBlockingEnabled = false;
    
    @SerialEntry
    public boolean airshipBlockingEnabled = false;
    

    // Blueprint Viewer Settings
    @SerialEntry
    public boolean blueprintViewerEnabled = true;
    
    @SerialEntry
    public boolean showBlueprintViewer = true;
    
    @SerialEntry
    public int blueprintViewerX = 1; // X-Position des Blueprint Viewers (Pixel vom rechten Rand)
    
    @SerialEntry
    public int blueprintViewerY = 2; // Y-Position des Blueprint Viewers (Prozent vom oberen Rand)
    
    @SerialEntry
    public OverlayType blueprintViewerOverlayType = OverlayType.CUSTOM; // Overlay-Typ für Blueprint Viewer
    
    @SerialEntry
    public boolean blueprintViewerShowBackground = true; // Textur-Hintergrund für Blueprint Viewer (veraltet, wird durch blueprintViewerOverlayType ersetzt)
    
    @SerialEntry
    public float blueprintViewerScale = 1.0f; // Skalierung der Blueprint-Anzeige (0.5f bis 2.0f)
    
    @SerialEntry
    public boolean blueprintViewerMissingMode = false; // Missing Mode: Zeige nur fehlende Baupläne (gefundene werden ausgeblendet)

    /** Nur Moblexicon: grüner Haken / rotes Kreuz bei gefundenen bzw. fehlenden Bauplänen in den Tooltips. */
    @SerialEntry
    public boolean moblexiconBlueprintCheckmarksEnabled = false;

    // Aspect Overlay Settings
    @SerialEntry
    public boolean aspectOverlayEnabled = true;
    
    @SerialEntry
    public boolean showAspectOverlay = true;
    
    @SerialEntry
    public int aspectOverlayX = 10; // X-Position des Aspect Overlays (Pixel vom rechten Rand)
    
    @SerialEntry
    public int aspectOverlayY = 100; // Y-Position des Aspect Overlays (Pixel vom oberen Rand)
    
    @SerialEntry
    public boolean aspectOverlayShowBackground = true; // Schwarzer Hintergrund für Aspect Overlay
    
    @SerialEntry
    public int chatAspectOverlayX = 15; // X-Position des Chat Aspect Overlays (Pixel vom linken Rand)
    
    @SerialEntry
    public int chatAspectOverlayY = 15; // Y-Position des Chat Aspect Overlays (Pixel vom oberen Rand)
    
    @SerialEntry
    public boolean chatAspectOverlayEnabled = true; // Chat Aspect Overlay aktivieren

    @SerialEntry
    public int starAspectOverlayX = 7; // X-Position des Star Aspect Overlays (für Items mit ⭐) (Pixel vom linken Rand)
    
    @SerialEntry
    public int starAspectOverlayY = 15; // Y-Position des Star Aspect Overlays (für Items mit ⭐) (Pixel vom oberen Rand)

    // Overlay Editor Settings
    @SerialEntry
    public boolean overlayEditorEnabled = true; // Overlay Editor aktivieren
    
    @SerialEntry
    public boolean showOverlayEditor = true; // Overlay Editor anzeigen

    // NPC Alerts Prozent-Einstellungen
    @SerialEntry
    public boolean showNpcAlertsForschungPercent = true; // Prozente für Forschung anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsAmbossPercent = true; // Prozente für Amboss anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsSchmelzofenPercent = true; // Prozente für Schmelzofen anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsJaegerPercent = true; // Prozente für Jäger anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsSeelenPercent = true; // Prozente für Seelen anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsEssenzenPercent = true; // Prozente für Essenzen anzeigen
    
    @SerialEntry
    public boolean showNpcAlertsRecyclerPercent = true; // Prozente für Recycler anzeigen (gilt für alle 3 Slots)
    
    @SerialEntry
    public boolean showNpcAlertsMachtkristallePercent = true; // Prozente für Machtkristalle anzeigen (gilt für alle 3 Slots)
    
    // NPC Alerts Separate Overlay Settings
    @SerialEntry
    public boolean npcAlertsForschungSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsAmbossSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsSchmelzofenSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsJaegerSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsSeelenSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsEssenzenSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsKomboKisteSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleSeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleSlot1Separate = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleSlot2Separate = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleSlot3Separate = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleOption = false; // Machtkristall-spezifische Option
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot1SeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot1Separate = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot2SeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot2Separate = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot3SeparateOverlay = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot3Separate = false;
    
    // NPC Alerts Warn-Schwellen
    /** Forschung: Warnung wenn verbleibender Wert &lt;= Schwelle (0–23); -1 = deaktiviert */
    @SerialEntry
    public int npcAlertsForschungWarnValue = -1;
    
    @SerialEntry
    public double npcAlertsAmbossWarnPercent = -1.0;
    
    @SerialEntry
    public double npcAlertsSchmelzofenWarnPercent = -1.0;
    
    @SerialEntry
    public double npcAlertsJaegerWarnPercent = -1.0;
    
    @SerialEntry
    public double npcAlertsSeelenWarnPercent = -1.0;
    
    @SerialEntry
    public double npcAlertsEssenzenWarnPercent = -1.0;
    
    /** Rechter Wert in „X / Y“; Warnung wenn Kombowert (links) diesen Wert erreicht oder überschreitet */
    @SerialEntry
    public int npcAlertsKomboKisteZielwert = 1000;
    
    @SerialEntry
    public double npcAlertsRecyclerWarnPercent = -1.0;
    
    @SerialEntry
    public double npcAlertsMachtkristalleWarnPercent = -1.0;
    
    // NPC Alerts Show Icon Settings
    @SerialEntry
    public boolean npcAlertsForschungShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsAmbossShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsSchmelzofenShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsSeelenShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsEssenzenShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsKomboKisteShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsJaegerShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot1ShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot2ShowIcon = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerSlot3ShowIcon = false;
    
    // NPC Alerts Show Background Settings
    @SerialEntry
    public boolean npcAlertsForschungShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsAmbossShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsSchmelzofenShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsJaegerShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsSeelenShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsEssenzenShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsKomboKisteShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsRecyclerShowBackground = true;
    
    @SerialEntry
    public boolean npcAlertsForschungScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsAmbossScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsSchmelzofenScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsJaegerScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsSeelenScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsEssenzenScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsKomboKisteScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsMachtkristalleScreenMessage = false;
    
    @SerialEntry
    public boolean npcAlertsRecyclerScreenMessage = false;
    
    // NPC Alerts Text Colors
    @SerialEntry
    public Color npcAlertsForschungTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsAmbossTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsSchmelzofenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsJaegerTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsSeelenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsEssenzenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsKomboKisteTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsMachtkristalleTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot1TextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot2TextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot3TextColor = new Color(0xFFFFFFFF);
    
    // NPC Alerts Percent Colors
    @SerialEntry
    public Color npcAlertsForschungPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsAmbossPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsSchmelzofenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsJaegerPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsSeelenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsEssenzenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsMachtkristallePercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot1PercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot2PercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color npcAlertsRecyclerSlot3PercentColor = new Color(0xFFFFFF00);
    
    // NPC Alerts Main Overlay Settings
    /** Alle NPC Alerts-Overlays sichtbar; nur Sichtbarkeit, ändert keine einzelnen showNpcAlerts-Schalter. */
    @SerialEntry
    public boolean npcAlertsOverlaysVisible = true;
    
    @SerialEntry
    public int npcAlertsMainOverlayX = 5;
    
    @SerialEntry
    public int npcAlertsMainOverlayY = 5;
    
    @SerialEntry
    public float npcAlertsMainOverlayScale = 1.0f;
    
    @SerialEntry
    public boolean npcAlertsMainOverlayShowBackground = true;
    
    // NPC Alerts Utility Enabled
    @SerialEntry
    public boolean npcAlertsUtilityEnabled = true;
    
    // NPC Alerts Scales
    @SerialEntry
    public float npcAlertsForschungScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsAmbossScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsSchmelzofenScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsJaegerScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsSeelenScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsEssenzenScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsKomboKisteScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsMachtkristalleScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsMachtkristalleSlot1Scale = 1.0f;
    
    @SerialEntry
    public float npcAlertsMachtkristalleSlot2Scale = 1.0f;
    
    @SerialEntry
    public float npcAlertsMachtkristalleSlot3Scale = 1.0f;
    
    @SerialEntry
    public float npcAlertsRecyclerScale = 1.0f;
    
    @SerialEntry
    public float npcAlertsRecyclerSlot1Scale = 1.0f;
    
    @SerialEntry
    public float npcAlertsRecyclerSlot2Scale = 1.0f;
    
    @SerialEntry
    public float npcAlertsRecyclerSlot3Scale = 1.0f;
    
    // NPC Alerts Show Settings
    @SerialEntry
    public boolean showNpcAlertsRecyclerSlot1 = true;
    
    @SerialEntry
    public boolean showNpcAlertsRecyclerSlot2 = true;
    
    @SerialEntry
    public boolean showNpcAlertsRecyclerSlot3 = true;
    
    // NPC Alerts Show Settings
    @SerialEntry
    public boolean showNpcAlertsForschung = true;
    
    @SerialEntry
    public boolean showNpcAlertsAmboss = true;
    
    @SerialEntry
    public boolean showNpcAlertsSchmelzofen = true;
    
    @SerialEntry
    public boolean showNpcAlertsJaeger = true;
    
    @SerialEntry
    public boolean showNpcAlertsSeelen = true;
    
    @SerialEntry
    public boolean showNpcAlertsEssenzen = true;
    
    @SerialEntry
    public boolean showNpcAlertsKomboKiste = true;
    
    @SerialEntry
    public boolean showNpcAlertsMachtkristalle = true;
    
    @SerialEntry
    public boolean showNpcAlertsMachtkristalleSlot1 = true;
    
    @SerialEntry
    public boolean showNpcAlertsMachtkristalleSlot2 = true;
    
    @SerialEntry
    public boolean showNpcAlertsMachtkristalleSlot3 = true;
    
    // NPC Alerts Position Settings
    @SerialEntry
    public int npcAlertsForschungX = 10;
    
    @SerialEntry
    public int npcAlertsForschungY = 10;
    
    @SerialEntry
    public int npcAlertsAmbossX = 10;
    
    @SerialEntry
    public int npcAlertsAmbossY = 50;
    
    @SerialEntry
    public int npcAlertsSchmelzofenX = 10;
    
    @SerialEntry
    public int npcAlertsSchmelzofenY = 90;
    
    @SerialEntry
    public int npcAlertsJaegerX = 10;
    
    @SerialEntry
    public int npcAlertsJaegerY = 130;
    
    @SerialEntry
    public int npcAlertsSeelenX = 10;
    
    @SerialEntry
    public int npcAlertsSeelenY = 170;
    
    @SerialEntry
    public int npcAlertsEssenzenX = 10;
    
    @SerialEntry
    public int npcAlertsEssenzenY = 210;
    
    @SerialEntry
    public int npcAlertsKomboKisteX = 10;
    
    @SerialEntry
    public int npcAlertsKomboKisteY = 230;
    
    @SerialEntry
    public int npcAlertsMachtkristalleX = 10;
    
    @SerialEntry
    public int npcAlertsMachtkristalleY = 250;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot1X = 10;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot1Y = 250;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot2X = 10;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot2Y = 280;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot3X = 10;
    
    @SerialEntry
    public int npcAlertsMachtkristalleSlot3Y = 310;
    
    @SerialEntry
    public int npcAlertsRecyclerX = 10;
    
    @SerialEntry
    public int npcAlertsRecyclerY = 290;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot1X = 10;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot1Y = 290;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot2X = 10;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot2Y = 330;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot3X = 10;
    
    @SerialEntry
    public int npcAlertsRecyclerSlot3Y = 370;
    
    // Boss HP Settings (fehlende Variablen)
    @SerialEntry
    public Color bossHPDPMColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color bossHPPercentageColor = new Color(0xFFFF5555);
    
    @SerialEntry
    public boolean bossHPShowPercentage = true;
    
    // Mining Lumberjack Overlay Colors
    @SerialEntry
    public Color miningLumberjackOverlayHeaderColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color miningLumberjackOverlayTextColor = new Color(0xFFFFFFFF);

    public static Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("CCLive Utilities Configuration"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Overall"))
                        .tooltip(Text.literal("Allgemeine Einstellungen für verschiedene Utilities"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Informationen Anzeige"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ebenen Nummer im Moblexicon "))
                                        .description(OptionDescription.of(Text.literal("Ebenen Nummer im Moblexicon anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showEbenenInSpecialInventory, newVal -> HANDLER.instance().showEbenenInSpecialInventory = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Materialien Ebenen"))
                                        .description(OptionDescription.of(Text.literal("Materialien Ebenen anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showEbenenInNormalInventories, newVal -> HANDLER.instance().showEbenenInNormalInventories = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Bauplan Ebene"))
                                        .description(OptionDescription.of(Text.literal("Ebenen-Nummer bei Bauplänen anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showBlueprintFloorNumber, newVal -> HANDLER.instance().showBlueprintFloorNumber = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("ItemViewer"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Item Viewer anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Item Viewer in Inventaren anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showItemViewer, newVal -> HANDLER.instance().showItemViewer = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Maximale Slot-Anzahl"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Maximale Anzahl an Slots pro Seite (Standard: 360). Spalten und Zeilen passen sich an verfügbaren Platz an.")))
                                        .binding("360",
                                                () -> HANDLER.instance().itemViewerMaxSlots,
                                                newVal -> {
                                                    HANDLER.instance().itemViewerMaxSlots = newVal;
                                                    net.felix.utilities.ItemViewer.ItemViewerUtility.onMaxSlotsConfigChanged();
                                                })
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Wellen Anzeige"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Wellen Anzeige bei Essenzen"))
                                        .description(OptionDescription.of(Text.literal("Zeigt die Wellen-Nummer bei Essenz-Items in Tooltips an")))
                                        .binding(true, () -> HANDLER.instance().showWaveDisplay, newVal -> HANDLER.instance().showWaveDisplay = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Aspekt Anzeige"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aspekt Anzeige aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Aspekt Anzeige aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().aspectOverlayEnabled, newVal -> HANDLER.instance().aspectOverlayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aspekt Overlay im Chat"))
                                        .description(OptionDescription.of(Text.literal("Aspekt Overlay in Chat-Nachrichten ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().chatAspectOverlayEnabled, newVal -> HANDLER.instance().chatAspectOverlayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Suchleiste"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Suchleiste Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Suchleiste aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().searchBarEnabled, newVal -> HANDLER.instance().searchBarEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Rahmenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe der Rahmen um gefilterte Items in der Suchleiste")))
                                        .binding(new Color(0xFFFF0000), () -> HANDLER.instance().searchBarFrameColor, newVal -> HANDLER.instance().searchBarFrameColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<ItemDisplayMode>createBuilder()
                                        .name(Text.literal("Item-Markierungs Art"))
                                        .description(OptionDescription.of(Text.literal("Rahmen, Hintergrund oder nicht passende Items ausblenden")))
                                        .binding(ItemDisplayMode.BORDER, () -> HANDLER.instance().searchBarItemDisplayMode, newVal -> HANDLER.instance().searchBarItemDisplayMode = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(ItemDisplayMode.class)
                                                .formatValue(mode -> {
                                                    if (mode == ItemDisplayMode.BORDER) {
                                                        return Text.literal("Rahmen");
                                                    }
                                                    if (mode == ItemDisplayMode.BACKGROUND) {
                                                        return Text.literal("Hintergrund");
                                                    }
                                                    return Text.literal("Falsche Ausblenden");
                                                }))
                                        .build())

                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Animation Blocker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Epic Drops blockieren"))
                                        .description(OptionDescription.of(Text.literal("Epic Drops Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().epicDropsBlockingEnabled, newVal -> HANDLER.instance().epicDropsBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Legendary Drops blockieren"))
                                        .description(OptionDescription.of(Text.literal("Legendary Drops Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().legendaryDropsBlockingEnabled, newVal -> HANDLER.instance().legendaryDropsBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Holzfäller Level Up blockieren"))
                                        .description(OptionDescription.of(Text.literal("Holzfäller Level Up Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().loggingLevelUpBlockingEnabled, newVal -> HANDLER.instance().loggingLevelUpBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())   
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Bergbau Level Up blockieren"))
                                        .description(OptionDescription.of(Text.literal("Bergbau Level Up Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().miningLevelUpBlockingEnabled, newVal -> HANDLER.instance().miningLevelUpBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Angeln Level Up blockieren"))
                                        .description(OptionDescription.of(Text.literal("Angeln Level Up Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().fishingLevelUpBlockingEnabled, newVal -> HANDLER.instance().fishingLevelUpBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Moblexicon blockieren"))
                                        .description(OptionDescription.of(Text.literal("Moblexicon Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().moblexiconBlockingEnabled, newVal -> HANDLER.instance().moblexiconBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Luftschiff blockieren"))
                                        .description(OptionDescription.of(Text.literal("Luftschiff Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().airshipBlockingEnabled, newVal -> HANDLER.instance().airshipBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())  
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Monster Todesanimationen Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Monster Todesanimationen aktivieren oder deaktivieren")))
                                        .binding(false, () -> HANDLER.instance().killAnimationUtilityEnabled, newVal -> HANDLER.instance().killAnimationUtilityEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Fabrik"))
                        .tooltip(Text.literal("Einstellungen für Fabrik-bezogene Utilities"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Boss HP"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Boss HP aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Boss-HP Anzeige aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().bossHPEnabled, newVal -> HANDLER.instance().bossHPEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den Text in der Boss-HP Anzeige")))
                                        .binding(new Color(0xFFFFFFFF), () -> HANDLER.instance().bossHPTextColor, newVal -> HANDLER.instance().bossHPTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter der Boss HP Anzeige anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().bossHPShowBackground, newVal -> HANDLER.instance().bossHPShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("DPM anzeigen"))
                                        .description(OptionDescription.of(Text.literal("DPM (Damage Per Minute) Anzeige im Boss HP Overlay ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().bossHPShowDPM, newVal -> HANDLER.instance().bossHPShowDPM = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Last Dmg anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Zeile „Last Dmg“ (Schaden im letzten Messfenster) im Boss-HP-Overlay ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().bossHPShowLastDmg, newVal -> HANDLER.instance().bossHPShowLastDmg = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Gesamtschaden anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Zeile „Overall DMG“ (kumulativer Schaden und Prozent) im Boss-HP-Overlay ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().bossHPShowOverallDmg, newVal -> HANDLER.instance().bossHPShowOverallDmg = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Stadt"))
                        .tooltip(Text.literal("Einstellungen für Stadt-bezogene Utilities"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Ausrüstungsanzeige"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ausrüstungsanzeige Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Ausrüstungsanzeige aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().equipmentDisplayEnabled, newVal -> HANDLER.instance().equipmentDisplayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den normalen Text in der Ausrüstungsanzeige")))
                                        .binding(new Color(0xE6FFFFFF), () -> HANDLER.instance().equipmentDisplayTextColor, newVal -> HANDLER.instance().equipmentDisplayTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Überschriftenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für die Überschriften (Prozentwerte/Flatwerte) in der Ausrüstungsanzeige")))
                                        .binding(new Color(0xFFFFFF00), () -> HANDLER.instance().equipmentDisplayHeaderColor, newVal -> HANDLER.instance().equipmentDisplayHeaderColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<OverlayType>createBuilder()
                                        .name(Text.literal("Overlay-Typ"))
                                        .description(OptionDescription.of(Text.literal("Wähle den Hintergrund-Typ für die Ausrüstungsanzeige:\n• Bild-Overlay \n• Schwarzes Overlay \n• Kein Hintergrund ")))
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().equipmentDisplayOverlayType, newVal -> HANDLER.instance().equipmentDisplayOverlayType = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .formatValue(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("KitFilter"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Kit Filter Buttons Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Kit Filter Buttons in Baupläne Inventaren ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().kitFilterButtonsEnabled, newVal -> HANDLER.instance().kitFilterButtonsEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Schmied Tracker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Schmied Tracker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Schmied Tracker aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().schmiedTrackerEnabled, newVal -> HANDLER.instance().schmiedTrackerEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<SchmiedItemDisplayMode>createBuilder()
                                        .name(Text.literal("Schmiedezustand Markierungs Art"))
                                        .description(OptionDescription.of(Text.literal("Rahmen oder Hintergrund für Schmiedezustand Items")))
                                        .binding(SchmiedItemDisplayMode.BORDER, () -> HANDLER.instance().schmiedTrackerItemDisplayMode, newVal -> HANDLER.instance().schmiedTrackerItemDisplayMode = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(SchmiedItemDisplayMode.class)
                                                .formatValue(mode -> {
                                                    if (mode == SchmiedItemDisplayMode.BORDER) {
                                                        return Text.literal("Rahmen");
                                                    }
                                                    return Text.literal("Hintergrund");
                                                }))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hide Uncraftable Button"))
                                        .description(OptionDescription.of(Text.literal("Hide Uncraftable Button in Baupläne Inventaren aktivieren")))
                                        .binding(true, () -> HANDLER.instance().hideUncraftableEnabled, newVal -> HANDLER.instance().hideUncraftableEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hide Wrong Class Button"))
                                        .description(OptionDescription.of(Text.literal("Hide Wrong Class Button in Baupläne Inventaren ein- oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showHideWrongClassButton, newVal -> HANDLER.instance().showHideWrongClassButton = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Frostgeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Frostgeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().frostgeschmiedetEnabled, newVal -> HANDLER.instance().frostgeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Frostgeschmiedete Items")))
                                        .binding(new Color(0x0066FF), () -> HANDLER.instance().frostgeschmiedetColor, newVal -> HANDLER.instance().frostgeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Lavageschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Lavageschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().lavageschmiedetEnabled, newVal -> HANDLER.instance().lavageschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Lavageschmiedete Items")))
                                        .binding(new Color(0xcb0e0e), () -> HANDLER.instance().lavageschmiedetColor, newVal -> HANDLER.instance().lavageschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Titangeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Titangeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().titangeschmiedetEnabled, newVal -> HANDLER.instance().titangeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Titangeschmiedete Items")))
                                        .binding(new Color(0x0FD456), () -> HANDLER.instance().titangeschmiedetColor, newVal -> HANDLER.instance().titangeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Drachengeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Drachengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().drachengeschmiedetEnabled, newVal -> HANDLER.instance().drachengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Drachengeschmiedete Items")))
                                        .binding(new Color(0xFF6600), () -> HANDLER.instance().drachengeschmiedetColor, newVal -> HANDLER.instance().drachengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Dämonengeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Dämonengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().daemonengeschmiedetEnabled, newVal -> HANDLER.instance().daemonengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Dämonengeschmiedete Items")))
                                        .binding(new Color(0xcf22c9), () -> HANDLER.instance().daemonengeschmiedetColor, newVal -> HANDLER.instance().daemonengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Blitzgeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Blitzgeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().blitzgeschmiedetEnabled, newVal -> HANDLER.instance().blitzgeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Blitzgeschmiedete Items")))
                                        .binding(new Color(0xFFD700), () -> HANDLER.instance().blitzgeschmiedetColor, newVal -> HANDLER.instance().blitzgeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Sternengeschmiedet"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Sternengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().sternengeschmiedetEnabled, newVal -> HANDLER.instance().sternengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Sternengeschmiedete Items")))
                                        .binding(new Color(0xFF00FF), () -> HANDLER.instance().sternengeschmiedetColor, newVal -> HANDLER.instance().sternengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Regenbogen"))
                                        .description(OptionDescription.of(Text.literal("Regenbogen-Animation für Sternengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().sternengeschmiedetRainbow, newVal -> HANDLER.instance().sternengeschmiedetRainbow = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Aincraft"))
                        .tooltip(Text.literal("Einstellungen für Aincraft-bezogene Utilities"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Material Tracker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Material Tracker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Material Tracker aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().materialTrackerEnabled, newVal -> HANDLER.instance().materialTrackerEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<OverlayType>createBuilder()
                                        .name(Text.literal("Overlay-Typ"))
                                        .description(OptionDescription.of(Text.literal("Wähle den Hintergrund-Typ für den Material Tracker:\n• Bild-Overlay \n• Schwarzes Overlay \n• Kein Hintergrund ")))
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().materialTrackerOverlayType, newVal -> HANDLER.instance().materialTrackerOverlayType = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .formatValue(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Materialien/min aktivieren"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Materialien pro Minute berechnen und anzeigen (Overlay oder Scoreboard)")))
                                        .binding(false,
                                                () -> HANDLER.instance().materialTrackerRateEnabled,
                                                newVal -> HANDLER.instance().materialTrackerRateEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<MaterialTrackerDisplayMode>createBuilder()
                                        .name(Text.literal("Materialien/min-Anzeige Position"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Wo Materialien pro Minute angezeigt werden:\n"
                                                        + "• Overlay – hinter der Anzahl im Material Tracker\n"
                                                        + "• Scoreboard – hinter der Anzahl im Scoreboard")))
                                        .binding(MaterialTrackerDisplayMode.OVERLAY,
                                                () -> HANDLER.instance().materialTrackerDisplayMode,
                                                newVal -> HANDLER.instance().materialTrackerDisplayMode = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(MaterialTrackerDisplayMode.class)
                                                .formatValue(mode -> switch (mode) {
                                                    case OVERLAY -> Text.literal("Overlay");
                                                    case SCOREBOARD -> Text.literal("Scoreboard");
                                                }))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Kill Tracker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Kill Tracker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Kill Tracker aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().killsUtilityEnabled, newVal -> HANDLER.instance().killsUtilityEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Überschriftenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für die Überschrift in der Kills-Anzeige")))
                                        .binding(new Color(0xFFFFFF00), () -> HANDLER.instance().killsUtilityHeaderColor, newVal -> HANDLER.instance().killsUtilityHeaderColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den Text in der Kills-Anzeige")))
                                        .binding(new Color(0xE6FFFFFF), () -> HANDLER.instance().killsUtilityTextColor, newVal -> HANDLER.instance().killsUtilityTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Kill Tracker anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().killsUtilityShowBackground, newVal -> HANDLER.instance().killsUtilityShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Benötigte Kills anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Zeile 'Benötigte Kills' im Kill Tracker anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().killsUtilityShowRequiredKills, newVal -> HANDLER.instance().killsUtilityShowRequiredKills = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Nächste Ebene anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Zeile 'Nächste Ebene' im Kill Tracker anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().killsUtilityShowNextLevel, newVal -> HANDLER.instance().killsUtilityShowNextLevel = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Coin Tracker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Coin Tracker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Liest Coins aus der HUD-Bossbar in Aincraft-Ebenen (floor_X) und zeigt Session-Statistiken an")))
                                        .binding(true, () -> HANDLER.instance().coinTrackerEnabled, newVal -> HANDLER.instance().coinTrackerEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<CoinTrackerDisplayMode>createBuilder()
                                        .name(Text.literal("CPM-Anzeige Position"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Wo Coins pro Minute angezeigt werden:\n"
                                                        + "• Overlay – CPM im Coin Tracker Overlay\n"
                                                        + "• Scoreboard – CPM im Scoreboard")))
                                        .binding(CoinTrackerDisplayMode.OVERLAY,
                                                () -> HANDLER.instance().coinTrackerDisplayMode,
                                                newVal -> HANDLER.instance().coinTrackerDisplayMode = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(CoinTrackerDisplayMode.class)
                                                .formatValue(mode -> switch (mode) {
                                                    case OVERLAY -> Text.literal("Overlay");
                                                    case SCOREBOARD -> Text.literal("Scoreboard");
                                                }))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Überschriftenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für die Überschrift im Coin Tracker")))
                                        .binding(new Color(0xFFFFFF00), () -> HANDLER.instance().coinTrackerHeaderColor, newVal -> HANDLER.instance().coinTrackerHeaderColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den Text im Coin Tracker")))
                                        .binding(new Color(0xE6FFFFFF), () -> HANDLER.instance().coinTrackerTextColor, newVal -> HANDLER.instance().coinTrackerTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Coin Tracker anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().coinTrackerShowBackground, newVal -> HANDLER.instance().coinTrackerShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Karten"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Karten anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Karten Overlay anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showCard, newVal -> HANDLER.instance().showCard = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<OverlayType>createBuilder()
                                        .name(Text.literal("Overlay-Typ"))
                                        .description(OptionDescription.of(Text.literal("Wähle den Hintergrund-Typ für die Karten:\n• Bild-Overlay \n• Schwarzes Overlay \n• Kein Hintergrund ")))
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().cardOverlayType, newVal -> {
                                            HANDLER.instance().cardOverlayType = newVal;
                                            HANDLER.save();
                                        })
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .formatValue(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Text-Größe"))
                                        .description(OptionDescription.of(Text.literal("Text-Größe für Karten-Overlay (1.0 = normal, 1.5 = 50% größer)")))
                                        .binding(1.0f, () -> HANDLER.instance().cardTextScale, newVal -> HANDLER.instance().cardTextScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(1.0f, 1.5f)
                                                .step(0.1f))
                                        .build())
                                .build())
                                .group(OptionGroup.createBuilder()
                                .name(Text.literal("Statuen"))        
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Statuen anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Statuen Overlay anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showStatue, newVal -> HANDLER.instance().showStatue = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<OverlayType>createBuilder()
                                        .name(Text.literal("Overlay-Typ"))
                                        .description(OptionDescription.of(Text.literal("Wähle den Hintergrund-Typ für die Statuen:\n• Bild-Overlay \n• Schwarzes Overlay \n• Kein Hintergrund ")))
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().statueOverlayType, newVal -> {
                                            HANDLER.instance().statueOverlayType = newVal;
                                            HANDLER.save();
                                        })
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .formatValue(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Text-Größe"))
                                        .description(OptionDescription.of(Text.literal("Text-Größe für Statuen-Overlay (1.0 = normal, 1.5 = 50% größer)")))
                                        .binding(1.0f, () -> HANDLER.instance().statueTextScale, newVal -> HANDLER.instance().statueTextScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(1.0f, 1.5f)
                                                .step(0.1f))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Blueprint Tracker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Blueprint Tracker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Blueprint Tracker aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().blueprintViewerEnabled, newVal -> HANDLER.instance().blueprintViewerEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Blueprint Tracker Overlay anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Blueprint Tracker Overlay anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showBlueprintViewer, newVal -> HANDLER.instance().showBlueprintViewer = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<OverlayType>createBuilder()
                                        .name(Text.literal("Overlay-Typ"))
                                        .description(OptionDescription.of(Text.literal("Wähle den Hintergrund-Typ für den Blueprint Tracker:\n• Bild-Overlay \n• Schwarzes Overlay \n• Kein Hintergrund ")))
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().blueprintViewerOverlayType, newVal -> {
                                            HANDLER.instance().blueprintViewerOverlayType = newVal;
                                            HANDLER.save();
                                        })
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .formatValue(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Missing Mode"))
                                        .description(OptionDescription.of(Text.literal("Im Missing Mode werden nur fehlende Baupläne angezeigt. Gefundene Baupläne werden ausgeblendet.")))
                                        .binding(false, () -> HANDLER.instance().blueprintViewerMissingMode, newVal -> HANDLER.instance().blueprintViewerMissingMode = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Haken im Moblexicon"))
                                        .description(OptionDescription.of(Text.literal("Zeigt im Moblexicon grüne Haken bzw. rote Kreuze bei Bauplan-Tooltips (nur dort, keine anderen Menüs).")))
                                        .binding(false, () -> HANDLER.instance().moblexiconBlueprintCheckmarksEnabled, newVal -> HANDLER.instance().moblexiconBlueprintCheckmarksEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Farmwelt"))
                        .tooltip(Text.literal("Einstellungen für Farmwelt-bezogene Utilities"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Informations Anzeige"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Informationen für Module"))
                                        .description(OptionDescription.of(Text.literal("Informationen für Module in Inventaren anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showModuleInformation, newVal -> HANDLER.instance().showModuleInformation = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Informationen für Lizenzen"))
                                        .description(OptionDescription.of(Text.literal("Informationen für Lizenzen in Inventaren anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showLicenseInformation, newVal -> HANDLER.instance().showLicenseInformation = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Holzfäller/Bergbau"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Holzfäller/Bergbau Overlay ein/aus"))
                                        .description(OptionDescription.of(Text.literal("Aktiviert oder deaktiviert sowohl das Holzfäller- als auch das Bergbau-Overlay")))
                                        .binding(true, () -> HANDLER.instance().miningLumberjackOverlayEnabled, newVal -> HANDLER.instance().miningLumberjackOverlayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Überschriftenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für die Überschriften (Bergbau/Holzfäller)")))
                                        .binding(new Color(0xFFFFFF00), () -> HANDLER.instance().miningLumberjackOverlayHeaderColor, newVal -> HANDLER.instance().miningLumberjackOverlayHeaderColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den normalen Text im Holzfäller/Bergbau Overlay")))
                                        .binding(new Color(0xFFFFFFFF), () -> HANDLER.instance().miningLumberjackOverlayTextColor, newVal -> HANDLER.instance().miningLumberjackOverlayTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ressourcen/min aktivieren"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Ressourcen pro Minute berechnen und anzeigen (Overlay oder Scoreboard)")))
                                        .binding(false,
                                                () -> HANDLER.instance().resourceTrackerRateEnabled,
                                                newVal -> HANDLER.instance().resourceTrackerRateEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<ResourceTrackerDisplayMode>createBuilder()
                                        .name(Text.literal("Ressourcen/min-Anzeige Position"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Wo Ressourcen pro Minute angezeigt werden:\n"
                                                        + "• Overlay – im Holzfäller/Bergbau Overlay\n"
                                                        + "• Scoreboard – hinter dem Ressourcennamen im Scoreboard")))
                                        .binding(ResourceTrackerDisplayMode.OVERLAY,
                                                () -> HANDLER.instance().resourceTrackerDisplayMode,
                                                newVal -> HANDLER.instance().resourceTrackerDisplayMode = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(ResourceTrackerDisplayMode.class)
                                                .formatValue(mode -> switch (mode) {
                                                    case OVERLAY -> Text.literal("Overlay");
                                                    case SCOREBOARD -> Text.literal("Scoreboard");
                                                }))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Collection"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Collection Overlay ein/aus"))
                                        .description(OptionDescription.of(Text.literal("Collection Overlay aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().showCollectionOverlay, newVal -> HANDLER.instance().showCollectionOverlay = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Überschriftenfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für die Überschrift (Collection:)")))
                                        .binding(new Color(0xFFFFFF00), () -> HANDLER.instance().collectionOverlayHeaderColor, newVal -> HANDLER.instance().collectionOverlayHeaderColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Textfarbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für den normalen Text im Collection Overlay")))
                                        .binding(new Color(0xFFFFFFFF), () -> HANDLER.instance().collectionOverlayTextColor, newVal -> HANDLER.instance().collectionOverlayTextColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Verschiedenes"))
                        .tooltip(Text.literal("Verschiedene Einstellungen"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Leaderboards"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Tracker Aktivität"))
                                        .description(OptionDescription.of(Text.literal("Tracker-Updates an den Server senden und Trackings von anderen Spielern anzeigen")))
                                        .binding(true, () -> HANDLER.instance().trackerActivityEnabled, newVal -> HANDLER.instance().trackerActivityEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build()) 
                                .option(Option.<net.felix.profile.PlayerHoverStatsUtility.HoverStatsType>createBuilder()
                                        .name(Text.literal("Hover Stats: Chosen Stat"))
                                        .description(OptionDescription.of(Text.literal("Welcher Stat soll anderen Spielern in Chat-Hover-Events angezeigt werden?\n\n-Spielzeit\n-Max Coins\n-Gesendete Nachrichten\n-Gefundene Baupläne\n-Max Schaden")))
                                        .binding(net.felix.profile.PlayerHoverStatsUtility.HoverStatsType.PLAYTIME, () -> HANDLER.instance().hoverStatsChosenStat != null ? HANDLER.instance().hoverStatsChosenStat : net.felix.profile.PlayerHoverStatsUtility.HoverStatsType.PLAYTIME, newVal -> HANDLER.instance().hoverStatsChosenStat = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(net.felix.profile.PlayerHoverStatsUtility.HoverStatsType.class)
                                                .formatValue(value -> value != null ? Text.literal(value.getDisplayName()) : Text.literal("Spielzeit")))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Icon"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Mod-Icon im Chat"))
                                        .description(OptionDescription.of(Text.literal("Mod-Icon hinter Spielernamen im Chat anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().chatIconEnabled, newVal -> HANDLER.instance().chatIconEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Mod-Icon über Spielernamen"))
                                        .description(OptionDescription.of(Text.literal("Zeigt das Mod-Icon über dem Namen von Spielern an, die die Mod installiert haben")))
                                        .binding(true, () -> HANDLER.instance().showPlayerNametagIcon, newVal -> HANDLER.instance().showPlayerNametagIcon = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Update Checker"))
                                .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Update Checker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Automatische Update-Prüfung beim Server-Beitritt aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().updateCheckerEnabled, newVal -> HANDLER.instance().updateCheckerEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Debug"))
                        .tooltip(Text.literal("Debug-Einstellungen für Entwickler und fortgeschrittene Benutzer"))
                        
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Blueprint Debugging"))
                                .description(OptionDescription.of(Text.literal("Aktiviert Debug-Nachrichten und -Commands für das Blueprint-System")))
                                .binding(false, () -> HANDLER.instance().blueprintDebugging, newVal -> HANDLER.instance().blueprintDebugging = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Leaderboard Debugging"))
                                .description(OptionDescription.of(Text.literal("Aktiviert Debug-Nachrichten und -Commands für das Leaderboard-System")))
                                .binding(false, () -> HANDLER.instance().leaderboardDebugging, newVal -> HANDLER.instance().leaderboardDebugging = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Player Stats Debugging"))
                                .description(OptionDescription.of(Text.literal("Aktiviert Debug-Nachrichten für das Senden von Player-Stats")))
                                .binding(false, () -> HANDLER.instance().playerStatsDebugging, newVal -> HANDLER.instance().playerStatsDebugging = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Debug Funktionen"))
                                .description(OptionDescription.of(Text.literal("Debug Funktionen\n-ItemHoverLogger (F8)\n-InventoryNameLogger (F9)\n-ScoreboardLogger (F10)\n-BossBarLogger (F12)")))
                                .binding(false, () -> HANDLER.instance().debugFunctionsEnabled, newVal -> HANDLER.instance().debugFunctionsEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .save(() -> {
                    HANDLER.save();
                })
                .build()
                .generateScreen(parent);
    }
} 