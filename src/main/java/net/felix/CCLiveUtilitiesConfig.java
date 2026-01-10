package net.felix;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.felix.OverlayType;

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
    public String hoverStatsChosenStat = "playtime"; // "playtime", "max_coins", "messages_sent", "blueprints_found", "max_damage"

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
    
    // Farmwelt Settings
    @SerialEntry
    public boolean showModuleInformation = true; // Informationen für Module anzeigen
    
    @SerialEntry
    public boolean showLicenseInformation = true; // Informationen für Lizenzen anzeigen
    
    @SerialEntry
    public boolean miningLumberjackOverlayEnabled = true; // Holzfäller / Bergbau Overlay ein/aus

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
    public net.felix.ItemDisplayMode schmiedTrackerItemDisplayMode = net.felix.ItemDisplayMode.BORDER; // Anzeigemodus für Schmied-Items (Rahmen oder Hintergrund)
    
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
    public String kitFilterButton2KitType = ""; // Kit-Typ für Button 2
    
    @SerialEntry
    public int kitFilterButton2Level = 1; // Level für Button 2 (1-7)
    
    @SerialEntry
    public String kitFilterButton3KitType = ""; // Kit-Typ für Button 3
    
    @SerialEntry
    public int kitFilterButton3Level = 1; // Level für Button 3 (1-7)
    
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
        
        public ClipboardEntryData() {
            // Für Deserialisierung benötigt
        }
        
        public ClipboardEntryData(String blueprintName, int quantity) {
            this.blueprintName = blueprintName;
            this.quantity = quantity;
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
    public boolean animationBlockerEnabled = false;
    
    // Kill Animation Utility Settings
    @SerialEntry
    public boolean killAnimationUtilityEnabled = false;
    

    
    // Individual Animation Blocker Settings
    @SerialEntry
    public boolean epicDropsBlockingEnabled = true;
    
    @SerialEntry
    public boolean legendaryDropsBlockingEnabled = true;
    
    @SerialEntry
    public boolean loggingLevelUpBlockingEnabled = true;
        
    @SerialEntry
    public boolean miningLevelUpBlockingEnabled = true;
    
    @SerialEntry
    public boolean moblexiconBlockingEnabled = true;
    
    @SerialEntry
    public boolean airshipBlockingEnabled = true;
    

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

    // Tab Info Prozent-Einstellungen
    @SerialEntry
    public boolean showTabInfoForschungPercent = true; // Prozente für Forschung anzeigen
    
    @SerialEntry
    public boolean showTabInfoAmbossPercent = true; // Prozente für Amboss anzeigen
    
    @SerialEntry
    public boolean showTabInfoSchmelzofenPercent = true; // Prozente für Schmelzofen anzeigen
    
    @SerialEntry
    public boolean showTabInfoJaegerPercent = true; // Prozente für Jäger anzeigen
    
    @SerialEntry
    public boolean showTabInfoSeelenPercent = true; // Prozente für Seelen anzeigen
    
    @SerialEntry
    public boolean showTabInfoEssenzenPercent = true; // Prozente für Essenzen anzeigen
    
    @SerialEntry
    public boolean showTabInfoRecyclerPercent = true; // Prozente für Recycler anzeigen (gilt für alle 3 Slots)
    
    // Tab Info Separate Overlay Settings
    @SerialEntry
    public boolean tabInfoForschungSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoAmbossSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoSchmelzofenSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoJaegerSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoSeelenSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoEssenzenSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleSeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleSlot1Separate = false;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleSlot2Separate = false;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleSlot3Separate = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot1SeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot1Separate = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot2SeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot2Separate = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot3SeparateOverlay = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot3Separate = false;
    
    // Tab Info Warn-Prozentwerte
    @SerialEntry
    public double tabInfoForschungWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoAmbossWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoSchmelzofenWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoJaegerWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoSeelenWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoEssenzenWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoRecyclerWarnPercent = -1.0;
    
    @SerialEntry
    public double tabInfoMachtkristalleWarnPercent = -1.0;
    
    // Tab Info Show Icon Settings
    @SerialEntry
    public boolean tabInfoForschungShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoAmbossShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoSchmelzofenShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoSeelenShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoEssenzenShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoJaegerShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot1ShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot2ShowIcon = false;
    
    @SerialEntry
    public boolean tabInfoRecyclerSlot3ShowIcon = false;
    
    // Tab Info Show Background Settings
    @SerialEntry
    public boolean tabInfoForschungShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoAmbossShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoSchmelzofenShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoJaegerShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoSeelenShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoEssenzenShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoMachtkristalleShowBackground = true;
    
    @SerialEntry
    public boolean tabInfoRecyclerShowBackground = true;
    
    // Tab Info Text Colors
    @SerialEntry
    public Color tabInfoForschungTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoAmbossTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoSchmelzofenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoJaegerTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoSeelenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoEssenzenTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoMachtkristalleTextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot1TextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot2TextColor = new Color(0xFFFFFFFF);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot3TextColor = new Color(0xFFFFFFFF);
    
    // Tab Info Percent Colors
    @SerialEntry
    public Color tabInfoForschungPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoAmbossPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoSchmelzofenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoJaegerPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoSeelenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoEssenzenPercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoMachtkristallePercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot1PercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot2PercentColor = new Color(0xFFFFFF00);
    
    @SerialEntry
    public Color tabInfoRecyclerSlot3PercentColor = new Color(0xFFFFFF00);
    
    // Tab Info Main Overlay Settings
    @SerialEntry
    public int tabInfoMainOverlayX = 5;
    
    @SerialEntry
    public int tabInfoMainOverlayY = 5;
    
    @SerialEntry
    public float tabInfoMainOverlayScale = 1.0f;
    
    @SerialEntry
    public boolean tabInfoMainOverlayShowBackground = true;
    
    // Tab Info Utility Enabled
    @SerialEntry
    public boolean tabInfoUtilityEnabled = true;
    
    // Tab Info Scales
    @SerialEntry
    public float tabInfoForschungScale = 1.0f;
    
    @SerialEntry
    public float tabInfoAmbossScale = 1.0f;
    
    @SerialEntry
    public float tabInfoSchmelzofenScale = 1.0f;
    
    @SerialEntry
    public float tabInfoJaegerScale = 1.0f;
    
    @SerialEntry
    public float tabInfoSeelenScale = 1.0f;
    
    @SerialEntry
    public float tabInfoEssenzenScale = 1.0f;
    
    @SerialEntry
    public float tabInfoMachtkristalleScale = 1.0f;
    
    @SerialEntry
    public float tabInfoMachtkristalleSlot1Scale = 1.0f;
    
    @SerialEntry
    public float tabInfoMachtkristalleSlot2Scale = 1.0f;
    
    @SerialEntry
    public float tabInfoMachtkristalleSlot3Scale = 1.0f;
    
    @SerialEntry
    public float tabInfoRecyclerScale = 1.0f;
    
    @SerialEntry
    public float tabInfoRecyclerSlot1Scale = 1.0f;
    
    @SerialEntry
    public float tabInfoRecyclerSlot2Scale = 1.0f;
    
    @SerialEntry
    public float tabInfoRecyclerSlot3Scale = 1.0f;
    
    // Tab Info Show Settings
    @SerialEntry
    public boolean showTabInfoRecyclerSlot1 = true;
    
    @SerialEntry
    public boolean showTabInfoRecyclerSlot2 = true;
    
    @SerialEntry
    public boolean showTabInfoRecyclerSlot3 = true;
    
    // Tab Info Show Settings
    @SerialEntry
    public boolean showTabInfoForschung = true;
    
    @SerialEntry
    public boolean showTabInfoAmboss = true;
    
    @SerialEntry
    public boolean showTabInfoSchmelzofen = true;
    
    @SerialEntry
    public boolean showTabInfoJaeger = true;
    
    @SerialEntry
    public boolean showTabInfoSeelen = true;
    
    @SerialEntry
    public boolean showTabInfoEssenzen = true;
    
    @SerialEntry
    public boolean showTabInfoMachtkristalle = true;
    
    // Tab Info Position Settings
    @SerialEntry
    public int tabInfoForschungX = 10;
    
    @SerialEntry
    public int tabInfoForschungY = 10;
    
    @SerialEntry
    public int tabInfoAmbossX = 10;
    
    @SerialEntry
    public int tabInfoAmbossY = 50;
    
    @SerialEntry
    public int tabInfoSchmelzofenX = 10;
    
    @SerialEntry
    public int tabInfoSchmelzofenY = 90;
    
    @SerialEntry
    public int tabInfoJaegerX = 10;
    
    @SerialEntry
    public int tabInfoJaegerY = 130;
    
    @SerialEntry
    public int tabInfoSeelenX = 10;
    
    @SerialEntry
    public int tabInfoSeelenY = 170;
    
    @SerialEntry
    public int tabInfoEssenzenX = 10;
    
    @SerialEntry
    public int tabInfoEssenzenY = 210;
    
    @SerialEntry
    public int tabInfoMachtkristalleX = 10;
    
    @SerialEntry
    public int tabInfoMachtkristalleY = 250;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot1X = 10;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot1Y = 250;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot2X = 10;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot2Y = 280;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot3X = 10;
    
    @SerialEntry
    public int tabInfoMachtkristalleSlot3Y = 310;
    
    @SerialEntry
    public int tabInfoRecyclerX = 10;
    
    @SerialEntry
    public int tabInfoRecyclerY = 290;
    
    @SerialEntry
    public int tabInfoRecyclerSlot1X = 10;
    
    @SerialEntry
    public int tabInfoRecyclerSlot1Y = 290;
    
    @SerialEntry
    public int tabInfoRecyclerSlot2X = 10;
    
    @SerialEntry
    public int tabInfoRecyclerSlot2Y = 330;
    
    @SerialEntry
    public int tabInfoRecyclerSlot3X = 10;
    
    @SerialEntry
    public int tabInfoRecyclerSlot3Y = 370;
    
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
                                        .name(Text.literal("Aspect Overlay im Chat"))
                                        .description(OptionDescription.of(Text.literal("Aspect Overlay in Chat-Nachrichten ein- oder ausblenden")))
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
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter der Suchleiste anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().searchBarShowBackground, newVal -> HANDLER.instance().searchBarShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Animation Blocker"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Animation Blocker aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Animation Blocker aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().animationBlockerEnabled, newVal -> HANDLER.instance().animationBlockerEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
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
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Monster Todesanimationen"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Monster Todesanimationen Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Monster Todesanimationen aktivieren oder deaktivieren")))
                                        .binding(false, () -> HANDLER.instance().killAnimationUtilityEnabled, newVal -> HANDLER.instance().killAnimationUtilityEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Spieler Icon"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Mod-Icon über Spielernamen"))
                                        .description(OptionDescription.of(Text.literal("Zeigt das Mod-Icon über dem Namen von Spielern an, die die Mod installiert haben")))
                                        .binding(true, () -> HANDLER.instance().showPlayerNametagIcon, newVal -> HANDLER.instance().showPlayerNametagIcon = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Chat"))
                        .tooltip(Text.literal("Einstellungen für Chat-Funktionen"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Chat Icon"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Chat Icon"))
                                        .description(OptionDescription.of(Text.literal("Chat-Icon hinter Spielernamen im Chat anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().chatIconEnabled, newVal -> HANDLER.instance().chatIconEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Leaderboards"))
                        .tooltip(Text.literal("Einstellungen für Leaderboards und Tracker"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Tracker Aktivität"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Tracker Aktivität"))
                                        .description(OptionDescription.of(Text.literal("Tracker-Updates an den Server senden und Trackings von anderen Spielern anzeigen")))
                                        .binding(true, () -> HANDLER.instance().trackerActivityEnabled, newVal -> HANDLER.instance().trackerActivityEnabled = newVal)
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
                                            .valueFormatter(v -> switch (v) {
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
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hide Uncraftable Button"))
                                        .description(OptionDescription.of(Text.literal("Hide Uncraftable Button in Baupläne Inventaren aktivieren")))
                                        .binding(true, () -> HANDLER.instance().hideUncraftableEnabled, newVal -> HANDLER.instance().hideUncraftableEnabled = newVal)
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
                                            .valueFormatter(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
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
                                            .valueFormatter(v -> switch (v) {
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
                                            .valueFormatter(v -> switch (v) {
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
                                        .name(Text.literal("Blueprint Tracker anzeigen"))
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
                                            .valueFormatter(v -> switch (v) {
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
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Holzfäller / Bergbau Overlay ein/aus"))
                                        .description(OptionDescription.of(Text.literal("Aktiviert oder deaktiviert sowohl das Holzfäller- als auch das Bergbau-Overlay")))
                                        .binding(true, () -> HANDLER.instance().miningLumberjackOverlayEnabled, newVal -> HANDLER.instance().miningLumberjackOverlayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Debug"))
                        .tooltip(Text.literal("Debug-Einstellungen für Entwickler und fortgeschrittene Benutzer"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Update Checker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Automatische Update-Prüfung beim Server-Beitritt aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().updateCheckerEnabled, newVal -> HANDLER.instance().updateCheckerEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
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
                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Hover Stats: Chosen Stat"))
                                .description(OptionDescription.of(Text.literal("Welche Stat soll anderen Spielern in Chat-Hover-Events angezeigt werden?\n• playtime: Spielzeit\n• max_coins: Max Coins\n• messages_sent: Gesendete Nachrichten\n• blueprints_found: Gefundene Baupläne\n• max_damage: Max Schaden")))
                                .binding("playtime", () -> HANDLER.instance().hoverStatsChosenStat, newVal -> {
                                    // Validiere den Wert
                                    String[] validValues = {"playtime", "max_coins", "messages_sent", "blueprints_found", "max_damage"};
                                    boolean isValid = false;
                                    for (String valid : validValues) {
                                        if (valid.equals(newVal)) {
                                            isValid = true;
                                            break;
                                        }
                                    }
                                    HANDLER.instance().hoverStatsChosenStat = isValid ? newVal : "playtime";
                                })
                                .controller(StringControllerBuilder::create)
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