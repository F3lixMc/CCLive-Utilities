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

public class CCLiveUtilitiesConfig {
    public static final ConfigClassHandler<CCLiveUtilitiesConfig> HANDLER = ConfigClassHandler.createBuilder(CCLiveUtilitiesConfig.class)
            .id(Identifier.of(CCLiveUtilities.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CCLiveUtilities.getConfigDir().resolve("cclive-utilities.json"))
                    .build())
            .build();

    // General Settings
    @SerialEntry
    public boolean enableMod = true;
    
    @SerialEntry
    public boolean showDebugInfo = false;

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
    
    @SerialEntry
    public boolean equipmentDisplayShowBackground = true; // Schwarzer Hintergrund für Ausrüstungsanzeige

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


    // Schmied Tracker Settings
    @SerialEntry
    public boolean schmiedTrackerEnabled = true;
    
    @SerialEntry
    public boolean showSchmiedTracker = true;
    
    @SerialEntry
    public boolean showMaterialTooltips = true; // Material tooltips in hover events
    
    // Informationen Utility Settings
    @SerialEntry
    public boolean informationenUtilityEnabled = true; // Informationen Utility aktivieren
    
    @SerialEntry
    public boolean showEbenenInSpecialInventory = true; // Ebenen in speziellem Inventar "㬪" anzeigen
    
    @SerialEntry
    public boolean showEbenenInNormalInventories = true; // Ebenen in normalen Inventaren anzeigen
    
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
    public boolean materialTrackerShowBackground = true; // Schwarzer Hintergrund für Material Tracker

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
    public int cardX = 143; // X-Position der Karten Anzeige (optimiert)
    
    @SerialEntry
    public int cardY = 125; // Y-Position der Karten Anzeige (optimiert)
    
    @SerialEntry
    public boolean cardShowBackground = true; // Schwarzer Hintergrund für Card Overlay
    
    // Statue Settings
    @SerialEntry
    public boolean statueEnabled = true;
    
    @SerialEntry
    public boolean showStatue = true;
    
    @SerialEntry
    public int statueX = 143; // X-Position der Statuen Anzeige (optimiert)
    
    @SerialEntry
    public int statueY = 60; // Y-Position der Statuen Anzeige (optimiert)
    
    @SerialEntry
    public boolean statueShowBackground = true; // Schwarzer Hintergrund für Statue Overlay



    // Animation Blocker Settings
    @SerialEntry
    public boolean animationBlockerEnabled = false;
    

    
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
    public boolean blueprintViewerShowBackground = true; // Schwarzer Hintergrund für Blueprint Viewer

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




    public static Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("CCLive Utilities Configuration"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Ausrüstungsanzeige"))
                        .tooltip(Text.literal("Einstellungen für die Ausrüstungsanzeige"))
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
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Rüstungswertanzeige X-Position"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position des Rüstungswertes (0 = zentriert, negative = links, positive = rechts)")))
                                .binding(-134, () -> HANDLER.instance().equipmentDisplayArmorX, newVal -> HANDLER.instance().equipmentDisplayArmorX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Rüstungswertanzeige Y-Position"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position des Rüstungswertes (Pixel vom unteren Bildschirmrand)")))
                                .binding(300, () -> HANDLER.instance().equipmentDisplayArmorY, newVal -> HANDLER.instance().equipmentDisplayArmorY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hintergrund anzeigen"))
                                .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter der Ausrüstungsanzeige anzeigen oder ausblenden")))
                                .binding(true, () -> HANDLER.instance().equipmentDisplayShowBackground, newVal -> HANDLER.instance().equipmentDisplayShowBackground = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Schmied Tracker"))
                        .tooltip(Text.literal("Einstellungen für den Schmied Tracker"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Schmied Tracker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Schmied Tracker aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().schmiedTrackerEnabled, newVal -> HANDLER.instance().schmiedTrackerEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hintergrund anzeigen"))
                                .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Schmied Tracker anzeigen oder ausblenden")))
                                .binding(true, () -> HANDLER.instance().schmiedTrackerShowBackground, newVal -> HANDLER.instance().schmiedTrackerShowBackground = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Material Ebenen anzeigen"))
                                .description(OptionDescription.of(Text.literal("Ebenen-Informationen in Material-Tooltips anzeigen oder ausblenden")))
                                .binding(true, () -> HANDLER.instance().showMaterialTooltips, newVal -> HANDLER.instance().showMaterialTooltips = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hide Uncraftable Button"))
                                .description(OptionDescription.of(Text.literal("Hide Uncraftable Button in Baupläne Inventaren aktivieren")))
                                .binding(true, () -> HANDLER.instance().hideUncraftableEnabled, newVal -> HANDLER.instance().hideUncraftableEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Button X-Position"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position des Hide Uncraftable Buttons (Offset von der Standardposition)")))
                                .binding(-80, () -> HANDLER.instance().hideUncraftableButtonX, newVal -> HANDLER.instance().hideUncraftableButtonX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Button Y-Position"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position des Hide Uncraftable Buttons (Offset von der Standardposition)")))
                                .binding(54, () -> HANDLER.instance().hideUncraftableButtonY, newVal -> HANDLER.instance().hideUncraftableButtonY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Frostgeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Frostgeschmiedete Items")))
                                        .binding(new Color(0x0066FF), () -> HANDLER.instance().frostgeschmiedetColor, newVal -> HANDLER.instance().frostgeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Frostgeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().frostgeschmiedetEnabled, newVal -> HANDLER.instance().frostgeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Lavageschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Lavageschmiedete Items")))
                                        .binding(new Color(0xcb0e0e), () -> HANDLER.instance().lavageschmiedetColor, newVal -> HANDLER.instance().lavageschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Lavageschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().lavageschmiedetEnabled, newVal -> HANDLER.instance().lavageschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Titangeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Titangeschmiedete Items")))
                                        .binding(new Color(0x0FD456), () -> HANDLER.instance().titangeschmiedetColor, newVal -> HANDLER.instance().titangeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Titangeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().titangeschmiedetEnabled, newVal -> HANDLER.instance().titangeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Drachengeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Drachengeschmiedete Items")))
                                        .binding(new Color(0xFF6600), () -> HANDLER.instance().drachengeschmiedetColor, newVal -> HANDLER.instance().drachengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Drachengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().drachengeschmiedetEnabled, newVal -> HANDLER.instance().drachengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Dämonengeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Dämonengeschmiedete Items")))
                                        .binding(new Color(0xcf22c9), () -> HANDLER.instance().daemonengeschmiedetColor, newVal -> HANDLER.instance().daemonengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Dämonengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().daemonengeschmiedetEnabled, newVal -> HANDLER.instance().daemonengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Blitzgeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Blitzgeschmiedete Items")))
                                        .binding(new Color(0xFFD700), () -> HANDLER.instance().blitzgeschmiedetColor, newVal -> HANDLER.instance().blitzgeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus    "))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Blitzgeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().blitzgeschmiedetEnabled, newVal -> HANDLER.instance().blitzgeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Sternengeschmiedet"))
                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Farbe"))
                                        .description(OptionDescription.of(Text.literal("Farbe für Sternengeschmiedete Items")))
                                        .binding(new Color(0xFF00FF), () -> HANDLER.instance().sternengeschmiedetColor, newVal -> HANDLER.instance().sternengeschmiedetColor = newVal)
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Ein/Aus"))
                                        .description(OptionDescription.of(Text.literal("Rahmen für Sternengeschmiedete Items Ein/Aus")))
                                        .binding(true, () -> HANDLER.instance().sternengeschmiedetEnabled, newVal -> HANDLER.instance().sternengeschmiedetEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
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
                        .name(Text.literal("Material Tracker"))
                        .tooltip(Text.literal("Einstellungen für den Material Tracker"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Material Tracker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Material Tracker aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().materialTrackerEnabled, newVal -> HANDLER.instance().materialTrackerEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("X Position (vom rechten Rand)"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position des Material Trackers (Pixel vom rechten Bildschirmrand)")))
                                .binding(1, () -> HANDLER.instance().materialTrackerX, newVal -> HANDLER.instance().materialTrackerX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Y Position (vom oberen Rand)"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position des Material Trackers (Pixel vom oberen Bildschirmrand)")))
                                .binding(35, () -> HANDLER.instance().materialTrackerY, newVal -> HANDLER.instance().materialTrackerY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hintergrund anzeigen"))
                                .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Material Tracker anzeigen oder ausblenden")))
                                .binding(true, () -> HANDLER.instance().materialTrackerShowBackground, newVal -> HANDLER.instance().materialTrackerShowBackground = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Informationen Anzeige"))
                        .tooltip(Text.literal("Einstellungen für die Informationen Anzeige"))
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
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Aspekt Info"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Aspect Anzeige aktivieren"))
                                        .description(OptionDescription.of(Text.literal("Aspect Overlay aktivieren oder deaktivieren")))
                                        .binding(true, () -> HANDLER.instance().aspectOverlayEnabled, newVal -> HANDLER.instance().aspectOverlayEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("X Position (vom rechten Rand)"))
                                        .description(OptionDescription.of(Text.literal("Horizontale Position des Aspect Overlays (Pixel vom rechten Bildschirmrand)")))
                                        .binding(415, () -> HANDLER.instance().aspectOverlayX, newVal -> HANDLER.instance().aspectOverlayX = newVal)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Y Position (vom oberen Rand)"))
                                        .description(OptionDescription.of(Text.literal("Vertikale Position des Aspect Overlays (Pixel vom oberen Bildschirmrand)")))
                                        .binding(60, () -> HANDLER.instance().aspectOverlayY, newVal -> HANDLER.instance().aspectOverlayY = newVal)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Aspect Overlay anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().aspectOverlayShowBackground, newVal -> HANDLER.instance().aspectOverlayShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Suchleiste"))
                        .tooltip(Text.literal("Einstellungen für die Suchleiste"))
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
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Kill Tracker"))
                        .tooltip(Text.literal("Einstellungen für die Kills-Anzeige"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Kill Tracker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Kill Tracker aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().killsUtilityEnabled, newVal -> HANDLER.instance().killsUtilityEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("X Position (vom rechten Rand)"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position der Kills-Anzeige (Pixel vom rechten Bildschirmrand)")))
                                .binding(570, () -> HANDLER.instance().killsUtilityX, newVal -> HANDLER.instance().killsUtilityX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Y Position (vom oberen Rand)"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position der Kills-Anzeige (Pixel vom oberen Bildschirmrand)")))
                                .binding(100, () -> HANDLER.instance().killsUtilityY, newVal -> HANDLER.instance().killsUtilityY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
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

                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Boss HP"))
                        .tooltip(Text.literal("Einstellungen für die Boss-HP Anzeige"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Boss HP aktivieren"))
                                .description(OptionDescription.of(Text.literal("Boss-HP Anzeige aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().bossHPEnabled, newVal -> HANDLER.instance().bossHPEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("X Position"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position der Boss-HP Anzeige")))
                                .binding(562, () -> HANDLER.instance().bossHPX, newVal -> HANDLER.instance().bossHPX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Y Position"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position der Boss-HP Anzeige")))
                                .binding(201, () -> HANDLER.instance().bossHPY, newVal -> HANDLER.instance().bossHPY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
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
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Cards/Statues"))
                        .tooltip(Text.literal("Einstellungen für Cards/Statues"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Cards/Statues aktivieren"))
                                .description(OptionDescription.of(Text.literal("Cards/Statues aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().cardsStatuesEnabled, newVal -> HANDLER.instance().cardsStatuesEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Karten"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Karten anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Karten Overlay anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().showCard, newVal -> HANDLER.instance().showCard = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                                                        .name(Text.literal("X Position"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position der Karten Anzeige")))
                                .binding(143, () -> HANDLER.instance().cardX, newVal -> HANDLER.instance().cardX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                                                        .name(Text.literal("Y Position"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position der Karten Anzeige")))
                                .binding(125, () -> HANDLER.instance().cardY, newVal -> HANDLER.instance().cardY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter der Karten Anzeige anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().cardShowBackground, newVal -> HANDLER.instance().cardShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
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
                                .option(Option.<Integer>createBuilder()
                                                                        .name(Text.literal("X Position"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position der Statuen Anzeige")))
                                .binding(143, () -> HANDLER.instance().statueX, newVal -> HANDLER.instance().statueX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                                                        .name(Text.literal("Y Position"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position der Statuen Anzeige")))
                                .binding(60, () -> HANDLER.instance().statueY, newVal -> HANDLER.instance().statueY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Hintergrund anzeigen"))
                                        .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter der Statuen Anzeige anzeigen oder ausblenden")))
                                        .binding(true, () -> HANDLER.instance().statueShowBackground, newVal -> HANDLER.instance().statueShowBackground = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Animation Blocker"))
                        .tooltip(Text.literal("Einstellungen für den Animation Blocker"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Animation Blocker aktivieren"))
                                .description(OptionDescription.of(Text.literal("Animation Blocker aktivieren oder deaktivieren")))
                                .binding(true, () -> HANDLER.instance().animationBlockerEnabled, newVal -> HANDLER.instance().animationBlockerEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Epic Drops"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Epic Drops blockieren"))
                                        .description(OptionDescription.of(Text.literal("Epic Drops Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().epicDropsBlockingEnabled, newVal -> HANDLER.instance().epicDropsBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Legendary Drops"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Legendary Drops blockieren"))
                                        .description(OptionDescription.of(Text.literal("Legendary Drops Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().legendaryDropsBlockingEnabled, newVal -> HANDLER.instance().legendaryDropsBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Holzfäller Level Up"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Holzfäller Level Up blockieren"))
                                        .description(OptionDescription.of(Text.literal("Holzfäller Level Up Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().loggingLevelUpBlockingEnabled, newVal -> HANDLER.instance().loggingLevelUpBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())   
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Bergbau Level Up"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Bergbau Level Up blockieren"))
                                        .description(OptionDescription.of(Text.literal("Bergbau Level Up Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().miningLevelUpBlockingEnabled, newVal -> HANDLER.instance().miningLevelUpBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Moblexicon"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Moblexicon blockieren"))
                                        .description(OptionDescription.of(Text.literal("Moblexicon Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().moblexiconBlockingEnabled, newVal -> HANDLER.instance().moblexiconBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Luftschiff"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Luftschiff blockieren"))
                                        .description(OptionDescription.of(Text.literal("Luftschiff Animationen blockieren")))
                                        .binding(false, () -> HANDLER.instance().airshipBlockingEnabled, newVal -> HANDLER.instance().airshipBlockingEnabled = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
  
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Blueprint Tracker"))
                        .tooltip(Text.literal("Einstellungen für den Blueprint Tracker"))
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
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("X Position (vom rechten Rand)"))
                                .description(OptionDescription.of(Text.literal("Horizontale Position des Blueprint Trackers (Pixel vom rechten Bildschirmrand)")))
                                .binding(1, () -> HANDLER.instance().blueprintViewerX, newVal -> HANDLER.instance().blueprintViewerX = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Y Position (vom oberen Rand)"))
                                .description(OptionDescription.of(Text.literal("Vertikale Position des Blueprint Trackers (Prozent vom oberen Bildschirmrand)")))
                                .binding(2, () -> HANDLER.instance().blueprintViewerY, newVal -> HANDLER.instance().blueprintViewerY = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-2000, 2000).step(1))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hintergrund anzeigen"))
                                .description(OptionDescription.of(Text.literal("Schwarzen Hintergrund hinter dem Blueprint Tracker anzeigen oder ausblenden")))
                                .binding(true, () -> HANDLER.instance().blueprintViewerShowBackground, newVal -> HANDLER.instance().blueprintViewerShowBackground = newVal)
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