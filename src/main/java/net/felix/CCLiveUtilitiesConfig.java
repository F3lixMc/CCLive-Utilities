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
import net.felix.OverlayType;

public class CCLiveUtilitiesConfig {
    public static final ConfigClassHandler<CCLiveUtilitiesConfig> HANDLER = ConfigClassHandler.createBuilder(CCLiveUtilitiesConfig.class)
            .id(Identifier.of(CCLiveUtilities.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CCLiveUtilities.getConfigDir().resolve("cclive-utilities.json"))
                    .build())
            .build();
    
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
        
        // Migration für Blueprint Viewer Overlay-Typ
        if (config.blueprintViewerShowBackground) {
            config.blueprintViewerOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
        } else {
            config.blueprintViewerOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
        }
        // blueprintViewerShowBackground wird für Abwärtskompatibilität beibehalten
        
        // Migration für Karten Overlay-Typ
        if (config.cardShowBackground) {
            config.cardOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
        } else {
            config.cardOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
        }
        // cardShowBackground wird für Abwärtskompatibilität beibehalten
        
        // Migration für Statuen Overlay-Typ
        if (config.statueShowBackground) {
            config.statueOverlayType = OverlayType.CUSTOM; // true = CUSTOM (Bild-Overlay)
        } else {
            config.statueOverlayType = OverlayType.NONE; // false = NONE (Kein Hintergrund)
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
    
    // Debug Settings
    @SerialEntry
    public boolean updateCheckerEnabled = true;
    
    @SerialEntry
    public boolean blueprintDebugging = false;
    
    @SerialEntry
    public boolean leaderboardDebugging = false;
    
    // Player Stats Debug Settings
    @SerialEntry
    public boolean playerStatsDebugging = false;
    
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
    public int schmiedTrackerX = 5; // X-Position des Schmied Trackers (optimiert)
    
    @SerialEntry
    public int schmiedTrackerY = 100; // Y-Position des Schmied Trackers (optimiert)
    
    @SerialEntry
    public boolean schmiedTrackerShowBackground = true; // Schwarzer Hintergrund für Schmied Tracker
    
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
    
    // Kit Filter Button Positions
    @SerialEntry
    public int kitFilterButton1X = -100; // X-Position des Kit Filter Button 1 (Offset)
    
    @SerialEntry
    public int kitFilterButton1Y = 50; // Y-Position des Kit Filter Button 1 (Offset)
    
    @SerialEntry
    public int kitFilterButton2X = -100; // X-Position des Kit Filter Button 2 (Offset)
    
    @SerialEntry
    public int kitFilterButton2Y = 75; // Y-Position des Kit Filter Button 2 (Offset)
    
    @SerialEntry
    public int kitFilterButton3X = -100; // X-Position des Kit Filter Button 3 (Offset)
    
    @SerialEntry
    public int kitFilterButton3Y = 100; // Y-Position des Kit Filter Button 3 (Offset)
    
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
    public float cardOverlayScale = 1.0f; // Skalierung der Karten-Overlays (0.5f bis 2.0f)
    
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
    public float statueOverlayScale = 1.0f; // Skalierung der Statuen-Overlays (0.5f bis 2.0f)



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

    // Overlay Editor Settings
    @SerialEntry
    public boolean overlayEditorEnabled = true; // Overlay Editor aktivieren
    
    @SerialEntry
    public boolean showOverlayEditor = true; // Overlay Editor anzeigen




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
                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Overlay-Größe"))
                                        .description(OptionDescription.of(Text.literal("Größe der Material Tracker-Anzeige und des Textes anpassen")))
                                        .binding(1.0f, () -> HANDLER.instance().materialTrackerScale, newVal -> HANDLER.instance().materialTrackerScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.1f))
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
                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Overlay-Größe"))
                                        .description(OptionDescription.of(Text.literal("Größe der Kills-Anzeige und des Textes anpassen")))
                                        .binding(1.0f, () -> HANDLER.instance().killsUtilityScale, newVal -> HANDLER.instance().killsUtilityScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.1f))
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
                                        .name(Text.literal("Karten Overlay-Größe"))
                                        .description(OptionDescription.of(Text.literal("Größe der Karten-Overlays und des Textes anpassen")))
                                        .binding(1.0f, () -> HANDLER.instance().cardOverlayScale, newVal -> HANDLER.instance().cardOverlayScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.1f))
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
                                        .name(Text.literal("Statuen Overlay-Größe"))
                                        .description(OptionDescription.of(Text.literal("Größe der Statuen-Overlays und des Textes anpassen")))
                                        .binding(1.0f, () -> HANDLER.instance().statueOverlayScale, newVal -> HANDLER.instance().statueOverlayScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.1f))
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
                                        .binding(OverlayType.CUSTOM, () -> HANDLER.instance().blueprintViewerOverlayType, newVal -> HANDLER.instance().blueprintViewerOverlayType = newVal)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(OverlayType.class)
                                            .valueFormatter(v -> switch (v) {
                                                case CUSTOM -> Text.literal("Bild-Overlay");
                                                case BLACK -> Text.literal("Schwarzes Overlay");
                                                case NONE -> Text.literal("Kein Hintergrund");
                                            }))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Overlay-Größe"))
                                        .description(OptionDescription.of(Text.literal("Größe der Blueprint-Anzeige und des Textes anpassen")))
                                        .binding(1.0f, () -> HANDLER.instance().blueprintViewerScale, newVal -> HANDLER.instance().blueprintViewerScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.1f))
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
                        .build())
                .save(() -> {
                    HANDLER.save();
                })
                .build()
                .generateScreen(parent);
    }
} 