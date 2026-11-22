package net.felix.utilities.DragOverlay.editor;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.language.ModTexts;
import net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerDisplayMode;
import net.felix.utilities.Other.keybinding.NumericFieldInput;
import net.felix.Community.buildshare.BuildShareDraggableOverlay;
import net.felix.Community.leaderboards.panel.LeaderboardDraggableOverlay;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.blueprints.viewer.BlueprintViewerDraggableOverlay;
import net.felix.utilities.Aincraft.cardsstatues.overlay.CardsDraggableOverlay;
import net.felix.utilities.Aincraft.cardsstatues.overlay.StatuesDraggableOverlay;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerDraggableOverlay;
import net.felix.utilities.Aincraft.kills.KillTrackerColors;
import net.felix.utilities.Aincraft.kills.KillsUtilityDraggableOverlay;
import net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerDraggableOverlay;
import net.felix.utilities.Factory.bosshp.BossHPDraggableOverlay;
import net.felix.utilities.Factory.essencefarm.EssenceFarmDraggableOverlay;
import net.felix.utilities.Factory.factorylevel.FactoryLevelDraggableOverlay;
import net.felix.utilities.Factory.factorylevel.FactoryLevelTracker;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelDraggableOverlay;
import net.felix.utilities.Farmworld.collections.overlay.CollectionDraggableOverlay;
import net.felix.utilities.Farmworld.collections.tracker.CollectionTracker;
import net.felix.utilities.Farmworld.farmzone.FarmzoneScoreboard;
import net.felix.utilities.Farmworld.miningxp.overlay.MiningLumberjackDraggableOverlay;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.DragOverlay.OverlayType;
import net.felix.utilities.DragOverlay.detail.DetailSettingsFrame;
import net.felix.utilities.DragOverlay.detail.TrackerDetailSettings;
import net.felix.utilities.DragOverlay.dock.OverlayDockLayout;
import net.felix.utilities.DragOverlay.keybind.DetailKeybindActions;
import net.felix.utilities.DragOverlay.keybind.DetailKeybindBindings;
import net.felix.utilities.DragOverlay.keybind.DetailKeybindCapture;
import net.felix.utilities.DragOverlay.keybind.DetailKeybindCatalog;
import net.felix.utilities.DragOverlay.dock.model.OverlayDockLink;
import net.felix.utilities.DragOverlay.dock.model.OverlayDockRect;
import net.felix.utilities.DragOverlay.dock.resolve.OverlayDockSnap;
import net.felix.utilities.DragOverlay.dock.OverlayDockStore;
import net.felix.utilities.Other.hud.RateDisplayUnit;
import net.felix.utilities.Overall.aspekte.overlay.ChatAspectOverlayDraggableOverlay;
import net.felix.utilities.Overall.clipboard.ClipboardTargetTime;
import net.felix.utilities.Overall.clipboard.overlay.ClipboardDraggableOverlay;
import net.felix.utilities.Overall.npcalerts.overlay.draggable.AspectDraggableOverlay;
import net.felix.utilities.Overall.npcalerts.overlay.draggable.NpcAlertsMainDraggableOverlay;
import net.felix.utilities.Overall.npcalerts.overlay.draggable.NpcAlertsSeparateDraggableOverlay;
import net.felix.utilities.Overall.npcalerts.overlay.draggable.StarAspectDraggableOverlay;
import net.felix.utilities.Town.kits.filter.KitFilterButtonDraggableOverlay;
import net.felix.utilities.Town.schmied.HideUncraftableButtonDraggableOverlay;
import net.felix.utilities.Town.schmied.HideWrongClassButtonDraggableOverlay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Screen für das Drag-and-Drop-Positionieren von Overlays
 */
public class OverlayEditorScreen extends Screen {
    
    private static final Identifier DOCK_MAGNET_ICON = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/magnet.png");
    private static final int MIN_OVERLAY_RESIZE_WIDTH = 20;
    private static final int MIN_OVERLAY_RESIZE_HEIGHT = 6;
    
    private final List<DraggableOverlay> overlays = new ArrayList<>();
    private DraggableOverlay draggingOverlay = null;
    private DraggableOverlay resizingOverlay = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int resizeStartX = 0;
    private int resizeStartY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private int resizeStartOverlayX = 0; // Speichere die ursprüngliche X-Position beim Resize-Start
    private int dragStartX = 0;
    private int dragStartY = 0;
    private boolean dragDetached = false;
    private OverlayDockSnap.Candidate snapOffer;
    
    private Button doneButton;
    private Button resetButton;
    private Button overlayButton;
    private StringWidget titleWidget;
    private F6TransparencySlider transparencySlider;
    private boolean editorButtonsVisible = true;
    
    // Store the previous screen so we can restore it
    private Screen previousScreen;
    
    // Overlay settings overlay
    private boolean overlaySettingsOpen = false;
    
    // Clipboard settings overlay
    private static final int CLIPBOARD_SETTINGS_WIDTH = 300;
    private static final int CLIPBOARD_SETTINGS_TOP_MARGIN = 48;
    private static final int CLIPBOARD_SETTINGS_BOTTOM_MARGIN = 44;
    private static final int CLIPBOARD_BUTTON_HEIGHT = 20;
    private static final int CLIPBOARD_BUTTON_SPACING = 30;
    private static final int CLIPBOARD_FIELD_HEIGHT = 16;
    private static final int DETAIL_BORDER_COLOR = 0xFF55FFFF;
    private boolean clipboardSettingsOpen = false;
    private boolean materialTrackerSettingsOpen = false;
    private boolean killTrackerSettingsOpen = false;
    private DraggableOverlay openTrackerDetail = null;
    private int detailScroll;
    private boolean clipboardMaxLinesFocused = false;
    private final NumericFieldInput.State clipboardMaxLinesField = new NumericFieldInput.State();
    
    public OverlayEditorScreen() {
        super(ModTexts.text("cclive.f6.screen"));
        // Store the current screen before opening the overlay editor
        this.previousScreen = Minecraft.getInstance().gui.screen();
        initializeOverlays();
    }
    
    public void refreshOverlays() {
        // Entferne alle npcAlerts-Overlays aus der Liste
        overlays.removeIf(overlay -> overlay instanceof NpcAlertsMainDraggableOverlay || overlay instanceof NpcAlertsSeparateDraggableOverlay);
        
        // Prüfe ob wir in einem Inventar oder im Chat sind
        boolean isInAnyInventoryRefresh = isInAnyInventoryScreen();
        boolean isInChatScreenRefresh = isInChatScreen();
        
        // Füge npcAlerts-Overlays wieder hinzu (basierend auf aktuellen Config-Werten)
        // Nur außerhalb von Inventaren und außerhalb des Chats anzeigen
        if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsUtilityEnabled && !isInAnyInventoryRefresh && !isInChatScreenRefresh) {
            // Haupt-Overlay - immer hinzufügen wenn NPC Alerts Utility aktiviert ist (wird nur gerendert wenn enabled)
            overlays.add(new NpcAlertsMainDraggableOverlay());
            
            // Separate Overlays - immer hinzufügen (werden nur gerendert wenn enabled)
            overlays.add(new NpcAlertsSeparateDraggableOverlay("forschung", "forschung", "Forschung"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("amboss", "amboss", "Amboss"));
            if (net.felix.utilities.Overall.npcalerts.overlay.AmbossSlots.slot2SeparateOverlay()) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("ambossSlot2", "ambossSlot2", "Amboss Slot 2"));
            }
            overlays.add(new NpcAlertsSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("jaeger", "jaeger", "Jäger"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("seelen", "seelen", "Seelen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("komboKiste", "komboKiste", "Kombo Kiste"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("recycler", "recycler", "Recycler"));
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3"));
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3"));
            }
        }
    }
    
    private void initializeOverlays() {
        // Biom-Erkennung aktualisieren, damit Collection/Mining-Overlays im F6-Menü erscheinen
        FarmzoneScoreboard.refreshFarmzoneScoreboardCache(Minecraft.getInstance());

        // Check if we're in a chat screen
        boolean isInChatScreen = isInChatScreen();
        
        // Check if we're in a blueprint inventory screen (where Hide Uncraftable button and Aspect Overlay work)
        boolean isInBlueprintInventory = isInInventoryScreen();
        
        // Check if we're in any inventory screen
        boolean isInAnyInventory = isInAnyInventoryScreen();
        
        // BossHP overlay is only available in dimensions that match the player name
        // And only when NOT in any inventory
        if (isInPlayerNameDimension() && !isInAnyInventory) {
            overlays.add(new BossHPDraggableOverlay());
        }

        if (FactoryLevelTracker.isInFactory()) {
            overlays.add(new FactoryLevelDraggableOverlay());
            overlays.add(new EssenceFarmDraggableOverlay());
        }
        
        if (isInChatScreen) {
            // In chat screen, show chat-specific overlays
            overlays.add(new ChatAspectOverlayDraggableOverlay());
        } else if (isInBlueprintInventory) {
            // In blueprint inventory screens, show overlays that are relevant
            // AspectOverlay is only shown in blueprint inventories
            overlays.add(new AspectDraggableOverlay());
            
            // Hide Uncraftable and Hide Wrong Class buttons are only in blueprint inventories
            overlays.add(new HideUncraftableButtonDraggableOverlay());
            overlays.add(new HideWrongClassButtonDraggableOverlay());
            
            // Check if we're in a Kit Filter relevant inventory
            if (isInKitFilterInventory()) {
                overlays.add(new KitFilterButtonDraggableOverlay(1));
                overlays.add(new KitFilterButtonDraggableOverlay(2));
                overlays.add(new KitFilterButtonDraggableOverlay(3));
            }
        }
        
        // Star Aspect Overlay - available ONLY in inventories EXCEPT blueprint inventories and "Machtkristalle Verbessern" inventory
        // This is for items with "⭐" in tooltip, different from the blueprint Aspect Overlay
        if (isInAnyInventory && !isInBlueprintInventory && !isInMkLevelInventory()) {
            overlays.add(new StarAspectDraggableOverlay());
        }
        
        if (!isInBlueprintInventory) {
            // Check if player is in a floor dimension
            boolean isInFloorDimension = isInFloorDimension();
            
            if (isInFloorDimension) {
                // In floor dimensions, only show floor-specific overlays
                overlays.add(new CardsDraggableOverlay());
                overlays.add(new StatuesDraggableOverlay());
                overlays.add(new BlueprintViewerDraggableOverlay());
                overlays.add(new MaterialTrackerDraggableOverlay());
                overlays.add(new KillsUtilityDraggableOverlay());
                overlays.add(new CoinTrackerDraggableOverlay());
            } else {
                // Mining/Lumberjack overlay - not available in player name dimension or in any inventory
                // The actual overlay will only show in-game when not in player name dimension or floor dimension
                // Only show if biome is detected in scoreboard
                if (!isInPlayerNameDimension() && !isInventoryOpen() && CollectionTracker.isBiomDetected()) {
                    overlays.add(new MiningLumberjackDraggableOverlay());
                }
                
                // Collection overlay - not available in player name dimension or in any inventory
                // The actual overlay will only show in-game when not in player name dimension or floor dimension
                // Only show if biome is detected in scoreboard
                if (!isInPlayerNameDimension() && !isInventoryOpen() && CollectionTracker.isBiomDetected()) {
                    overlays.add(new CollectionDraggableOverlay());
                }
                
                // MkLevel overlay - only available in "Machtkristalle Verbessern" inventory
                if (isInMkLevelInventory()) {
                    overlays.add(new MkLevelDraggableOverlay());
                }
                
                // Note: Aspect Overlay and Hide Uncraftable Button are only available in blueprint inventories
                // Note: Star Aspect Overlay is available in all inventories EXCEPT blueprint inventories
                // Note: Equipment Display is only available in equipment chest inventories (with Unicode characters 㬃, 㬄, 㬅, 㬆)
                // Note: Cards, Statues, BlueprintViewer, Material Tracker, and Kills are only available in floor dimensions
                // Note: MkLevel Overlay is only available in "Machtkristalle Verbessern" inventory
            }
        }
        
        // NPC Alerts Overlays - nur außerhalb von Inventaren und außerhalb des Chats anzeigen
        if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsUtilityEnabled && !isInAnyInventory && !isInChatScreen) {
            // Haupt-Overlay - immer hinzufügen wenn NPC Alerts Utility aktiviert ist (wird nur gerendert wenn enabled)
            NpcAlertsMainDraggableOverlay mainOverlay = new NpcAlertsMainDraggableOverlay();
            overlays.add(mainOverlay);
            
            // Separate Overlays - immer hinzufügen (werden nur gerendert wenn enabled)
            overlays.add(new NpcAlertsSeparateDraggableOverlay("forschung", "forschung", "Forschung"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("amboss", "amboss", "Amboss"));
            if (net.felix.utilities.Overall.npcalerts.overlay.AmbossSlots.slot2SeparateOverlay()) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("ambossSlot2", "ambossSlot2", "Amboss Slot 2"));
            }
            overlays.add(new NpcAlertsSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("jaeger", "jaeger", "Jäger"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("seelen", "seelen", "Seelen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("komboKiste", "komboKiste", "Kombo Kiste"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle"));
            overlays.add(new NpcAlertsSeparateDraggableOverlay("recycler", "recycler", "Recycler"));
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate) {
                    overlays.add(new NpcAlertsSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3"));
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate) {
                overlays.add(new NpcAlertsSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3"));
            }
        }
        
        // Rangliste nur im eigenen Spielerinventar. Die Position bleibt an der Inventarkante.
        if (previousScreen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            overlays.add(new LeaderboardDraggableOverlay());
        }

        // Clipboard is available everywhere
        overlays.add(new ClipboardDraggableOverlay());
        if (isBuildShareEditorContext()) {
            overlays.add(new BuildShareDraggableOverlay());
        }
    }

    /** Build-Share nur im Kleiderschrank und im Equipment, nicht in jedem F6-Menü. */
    private boolean isBuildShareEditorContext() {
        if (!(previousScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return false;
        }
        String title = screen.getTitle().getString();
        return ZeichenUtility.containsEquipmentDisplay(title) || ZeichenUtility.isWardrobeFullTitle(title);
    }
    
    /**
     * Check if the player is currently in a chat screen
     */
    private boolean isInChatScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is a ChatScreen
        return client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen;
    }
    
    /**
     * Check if the player is currently in any inventory screen (AbstractContainerScreen)
     */
    private boolean isInAnyInventoryScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is a AbstractContainerScreen (inventory-like screen)
        return client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
    }
    
    /**
     * Check if the player is currently in a blueprint inventory screen where Hide Uncraftable button and Aspect Overlay work
     */
    private boolean isInInventoryScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is a AbstractContainerScreen (inventory-like screen)
        if (!(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a blueprint inventory
        net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen = 
            (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains any of the allowed blueprint inventory names
        // (same logic as in AbstractContainerScreenMixin.isBlueprintInventory())
        return cleanTitle.contains("Baupläne [Waffen]") ||
               cleanTitle.contains("Baupläne [Rüstung]") ||
               cleanTitle.contains("Baupläne [Werkzeuge]") ||
               cleanTitle.contains("Bauplan [Shop]") ||
               cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
               cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
               cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
               cleanTitle.contains("Favorisierte [Shop-Baupläne]");
    }
    
    /**
     * Check if the player is currently in a Kit Filter relevant inventory
     */
    private boolean isInKitFilterInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is a AbstractContainerScreen (inventory-like screen)
        if (!(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a Kit Filter relevant inventory
        net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen = 
            (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains Kit Filter relevant inventory names
        // (same logic as KitFilterUtility.isRelevantInventory())
        return cleanTitle.contains("Baupläne [Waffen]") || cleanTitle.contains("Blueprints [Weapons]") ||
               cleanTitle.contains("Baupläne [Rüstung]") || cleanTitle.contains("Blueprints [Armor]") ||
               cleanTitle.contains("Baupläne [Werkzeuge]") || cleanTitle.contains("Blueprints [Tools]") ||
               cleanTitle.contains("Bauplan [Shop]") || cleanTitle.contains("Blueprint Store") ||
               cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") || cleanTitle.contains("Favorited [Armor Blueprints]") ||
               cleanTitle.contains("Favorisierte [Waffenbaupläne]") || cleanTitle.contains("Favorited [Weapon Blueprints]") ||
               cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") || cleanTitle.contains("Favorited [Tools Blueprints]") ||
               cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
    }
    
    /**
     * Check if the player is currently in a floor dimension
     */
    private boolean isInFloorDimension() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        
        // Check if the player is in a floor dimension by checking the dimension identifier
        String dimensionId = client.level.dimension().identifier().toString();
        
        // Floor dimensions typically have "floor" in their identifier
        return dimensionId.contains("floor") || dimensionId.contains("dungeon");
    }
    
    /**
     * Check if the player is currently in the "Machtkristalle Verbessern" inventory
     */
    private boolean isInMkLevelInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is a AbstractContainerScreen (inventory-like screen)
        if (!(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's the MkLevel inventory
        net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen = 
            (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
        
        String title = handledScreen.getTitle().getString();
        
        // Check if the title contains "Machtkristalle Verbessern" or the Glyph "㮌" (Essence Harvester UI)
        return ZeichenUtility.isMkLevelInventoryTitle(
                net.felix.utilities.Aincraft.information.tooltip.TooltipTextCleaner.getPlainTextFromText(handledScreen.getTitle()),
                title);
    }
    
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor context) {
    }

    @Override
    protected void init() {
        clearWidgets();
        super.init();
        transparencySlider = null;
        
        // Title
        titleWidget = new StringWidget(
            ModTexts.text("cclive.f6.title"),
            font
        );
        int buttonHeight = 20;
        int buttonWidth = 80;
        int buttonSpacing = 10;
        int magnetSize = 20;
        int totalButtonsWidth = 4 * buttonWidth + 4 * buttonSpacing + magnetSize;
        int startX = Math.max(0, (width - totalButtonsWidth) / 2);
        boolean buttonsOnTop = CCLiveUtilitiesConfig.HANDLER.instance().f6ButtonPlace != F6ButtonPlace.BOTTOM;
        int buttonY = buttonsOnTop ? 5 : Math.max(5, height - 30);
        int titleY = 5;
        if (editorButtonsVisible) {
            int gap = 4;
            int sliderWidth = 118;
            int sliderHeight = 28;
            int sliderX = startX - gap - sliderWidth;
            if (sliderX < 2) {
                sliderX = 2;
                int needed = sliderX + sliderWidth + gap;
                if (startX < needed) {
                    startX = needed;
                }
                if (startX + totalButtonsWidth > width - 2) {
                    buttonSpacing = 4;
                    int available = Math.max(0, width - 2 - needed);
                    buttonWidth = Math.max(32, (available - 4 * buttonSpacing - magnetSize) / 4);
                    totalButtonsWidth = 4 * buttonWidth + 4 * buttonSpacing + magnetSize;
                    startX = needed;
                }
            }
            int sliderY = buttonsOnTop ? 5 : Math.max(5, buttonY - (sliderHeight - buttonHeight) / 2);
            transparencySlider = new F6TransparencySlider(sliderX, sliderY, sliderWidth, sliderHeight);
            addRenderableWidget(transparencySlider);
            if (buttonsOnTop) {
                titleY = Math.max(5 + buttonHeight, 5 + sliderHeight) + 5;
            }
        }
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, titleY);
        addRenderableWidget(titleWidget);

        if (!editorButtonsVisible) {
            return;
        }

        
        // Done Button
        doneButton = Button.builder(
            ModTexts.text("cclive.f6.done"),
            button -> onClose()
        ).bounds(startX, buttonY, buttonWidth, buttonHeight).build();
        addRenderableWidget(doneButton);
        
        // NPC Alerts Button
        Button npcAlertsButton = Button.builder(
            ModTexts.text("cclive.f6.npc_alerts"),
            button -> {
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.gui.setScreen(new net.felix.utilities.Overall.npcalerts.ui.NpcAlertsSettingsScreen(this));
                }
            }
        ).bounds(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight).build();
        addRenderableWidget(npcAlertsButton);
        
        // Overlay Button
        overlayButton = Button.builder(
            ModTexts.text("cclive.f6.overlay"),
            button -> overlaySettingsOpen = !overlaySettingsOpen
        ).bounds(startX + 2 * (buttonWidth + buttonSpacing), buttonY, buttonWidth, buttonHeight).build();
        addRenderableWidget(overlayButton);
        
        // Reset Button
        resetButton = Button.builder(
            ModTexts.text("cclive.f6.reset_all"),
            button -> resetAllOverlays()
        ).bounds(startX + 3 * (buttonWidth + buttonSpacing), buttonY, buttonWidth, buttonHeight).build();
        addRenderableWidget(resetButton);
        int magnetX = startX + 3 * (buttonWidth + buttonSpacing) + buttonWidth + buttonSpacing;
        addRenderableWidget(new OverlayMagnetButton(magnetX, buttonY));
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        F6MenuBackground.setSuppressUnderlyingDim(true);
        try {
            if (previousScreen != null) {
                previousScreen.extractRenderState(context, mouseX, mouseY, delta);
            }
        } finally {
            F6MenuBackground.setSuppressUnderlyingDim(false);
        }

        context.fill(0, 0, width, height, CCLiveUtilitiesConfig.f6MenuBackgroundColor());
        OverlayDockLayout.syncEditor(overlays, width, height);
        updateSnapOffer();
        
        // Render title
        titleWidget.extractRenderState(context, mouseX, mouseY, delta);
        
        // Render all overlays in edit mode
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isDrawnInEditor()) {
                overlay.renderInEditMode(context, mouseX, mouseY, delta);
                
                // Render resize handle and reset handle if hovering over the overlay
                if (overlay.isHovered(mouseX, mouseY)) {
                    renderResizeHandle(context, overlay);
                    renderResetHandle(context, overlay);
                }
            }
        }
        renderDockMarkers(context);
        
        // Render instructions
        renderInstructions(context);
        
        // Render overlay settings overlay if open
        if (overlaySettingsOpen) {
            renderOverlaySettings(context, mouseX, mouseY);
        }
        
        // Render clipboard settings overlay if open
        if (clipboardSettingsOpen) {
            renderClipboardSettings(context, mouseX, mouseY);
        }
        if (materialTrackerSettingsOpen) {
            renderMaterialTrackerSettings(context, mouseX, mouseY);
        }
        if (killTrackerSettingsOpen) {
            renderKillTrackerSettings(context, mouseX, mouseY);
        }
        if (openTrackerDetail != null) {
            renderTrackerDetail(context, mouseX, mouseY);
        }
        
        // Render buttons
        super.extractRenderState(context, mouseX, mouseY, delta);
        
        renderOverlayHandleTooltips(context, mouseX, mouseY);
    }
    
    private void renderOverlayHandleTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            DraggableOverlay overlay = overlays.get(i);
            if (!overlay.isDrawnInEditor()) {
                continue;
            }
            
            if (isOverlayResetHandle(mouseX, mouseY, overlay)) {
                context.setComponentTooltipForNextFrame(font, List.of(ModTexts.text("cclive.f6.tooltip.reset_size")), mouseX, mouseY);
                return;
            }
            if (isOverlayResizeHandle(mouseX, mouseY, overlay)) {
                context.setComponentTooltipForNextFrame(font, List.of(ModTexts.text("cclive.f6.tooltip.resize")), mouseX, mouseY);
                return;
            }
        }
    }
    
    private boolean isOverlayResizeHandle(int mouseX, int mouseY, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + OverlayDockLayout.shownWidth(overlay) - handleSize;
        int y = overlay.getY() + OverlayDockLayout.shownHeight(overlay) - handleSize;
        return mouseX >= x && mouseX <= x + handleSize && mouseY >= y && mouseY <= y + handleSize;
    }
    
    private boolean isOverlayResetHandle(int mouseX, int mouseY, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + OverlayDockLayout.shownWidth(overlay) - handleSize;
        int y = overlay.getY();
        return mouseX >= x && mouseX <= x + handleSize && mouseY >= y && mouseY <= y + handleSize;
    }
    
    private void renderResizeHandle(GuiGraphicsExtractor context, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + OverlayDockLayout.shownWidth(overlay) - handleSize;
        int y = overlay.getY() + OverlayDockLayout.shownHeight(overlay) - handleSize;
        
        // Draw resize handle background (small square with white border)
        context.fill(x, y, x + handleSize, y + handleSize, 0xFFFFFFFF);
        context.fill(x + 1, y + 1, x + handleSize - 1, y + handleSize - 1, 0xFF000000);
        
        // Draw diagonal arrow from top-left to bottom-right
        int arrowLength = handleSize - 4;
        
        // Draw diagonal line from top-left to bottom-right
        for (int i = 0; i < arrowLength; i++) {
            int px = x + 2 + i;
            int py = y + 2 + i;
            if (px < x + handleSize - 1 && py < y + handleSize - 1) {
                context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }
        }
        
        // Draw arrowhead at bottom-right (pointing down-right)
        // Bottom-right corner arrowhead
        context.fill(x + handleSize - 3, y + handleSize - 2, x + handleSize - 1, y + handleSize - 1, 0xFFFFFFFF);
        context.fill(x + handleSize - 2, y + handleSize - 3, x + handleSize - 1, y + handleSize - 2, 0xFFFFFFFF);
        
        // Draw arrowhead at top-left (pointing up-left)
        // Top-left corner arrowhead
        context.fill(x + 1, y + 1, x + 3, y + 2, 0xFFFFFFFF);
        context.fill(x + 1, y + 2, x + 2, y + 3, 0xFFFFFFFF);
    }
    
    private void renderResetHandle(GuiGraphicsExtractor context, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + OverlayDockLayout.shownWidth(overlay) - handleSize;
        int y = overlay.getY();
        
        // Draw reset handle background (small square with white border)
        context.fill(x, y, x + handleSize, y + handleSize, 0xFFFFFFFF);
        context.fill(x + 1, y + 1, x + handleSize - 1, y + handleSize - 1, 0xFF000000);
        
        // Draw "<-" text (scaled down to fit better, centered)
        float scale = 0.7f;
        String arrowText = "<-";
        int textWidth = font.width(arrowText);
        int textHeight = font.lineHeight;
        
        // Calculate centered position (accounting for scale)
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = textHeight * scale;
        float textX = x + (handleSize - scaledTextWidth) / 2.0f;
        float textY = y + (handleSize - scaledTextHeight) / 2.0f;
        
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(textX, textY);
        matrices.scale(scale, scale);
        context.text(font, arrowText, 0, 0, 0xFFFFFFFF, false);
        matrices.popMatrix();
    }
    
    private void renderInstructions(GuiGraphicsExtractor context) {
        String[] instructions = {
            ModTexts.get("cclive.f6.instruction.move"),
            ModTexts.get("cclive.f6.instruction.resize"),
            ModTexts.get("cclive.f6.instruction.nudge"),
            ModTexts.get("cclive.f6.instruction.close"),
            ModTexts.get("cclive.f6.instruction.buttons")
        };
        int y = height - 104;
        
        for (String instruction : instructions) {
            context.text(font, instruction, 10, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        // Handle clipboard settings click
        if (handleClipboardSettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        if (handleMaterialTrackerSettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        if (handleKillTrackerSettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        if (handleTrackerDetailClick(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle overlay settings click
        if (handleOverlaySettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        
        if (draggingOverlay != null || resizingOverlay != null) {
            return true;
        }

        if (super.mouseClicked(event, doubled)) {
            return true;
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            int clickX = (int) mouseX;
            int clickY = (int) mouseY;
            DraggableOverlay top = OverlayHitOrder.topmost(overlays, overlay -> overlay.isDrawnInEditor()
                    && (overlay.isResetArea(clickX, clickY) || overlay.isResizeArea(clickX, clickY) || overlay.isHovered(clickX, clickY)));
            if (top != null) {
                if (top.isResetArea(clickX, clickY)) {
                    top.resetSizeToDefault();
                    OverlayDockLayout.invalidatePositions();
                    top.savePosition();
                    CCLiveUtilitiesConfig.HANDLER.save();
                    return true;
                }
                if (top.isResizeArea(clickX, clickY) && canDragOverlay(top)) {
                    resizingOverlay = top;
                    resizeStartX = clickX;
                    resizeStartY = clickY;
                    resizeStartWidth = top.getWidth();
                    resizeStartHeight = top.getHeight();
                    resizeStartOverlayX = top.getX();
                    return true;
                }
                if (top.isHovered(clickX, clickY) && canDragOverlay(top) && top.allowsMove()) {
                    draggingOverlay = top;
                    dragOffsetX = clickX - top.getX();
                    dragOffsetY = clickY - top.getY();
                    dragStartX = top.getX();
                    dragStartY = top.getY();
                    dragDetached = top.dockId() == null || top.dockId().isBlank() || !OverlayDockStore.involves(top.dockId());
                    snapOffer = null;
                    if (top.dockId() != null && !top.dockId().isBlank()) {
                        OverlayDockLayout.setEditorDragSkip(top.dockId());
                    }
                    return true;
                }
                return true;
            }
        }

        return false;
    }
    
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        if (transparencySlider != null && transparencySlider.isDragging()) {
            transparencySlider.mouseDragged(event, deltaX, deltaY);
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                // Check if dragging is still allowed (might have changed dimension or opened inventory)
                if (!canDragOverlay(draggingOverlay)) {
                    OverlayDockLayout.clearEditorDragSkip();
                    draggingOverlay = null;
                    snapOffer = null;
                    return false;
                }
                
                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;
                
                // Keep overlay within screen bounds
                newX = Math.max(0, Math.min(newX, width - draggingOverlay.getWidth()));
                newY = Math.max(0, Math.min(newY, height - draggingOverlay.getHeight()));
                
                draggingOverlay.setPosition(newX, newY);
                detachIfPulledAway(draggingOverlay, newX, newY);
                OverlayDockLayout.invalidatePositions();
                return true;
            }
            
            if (resizingOverlay != null) {
                // Check if resizing is still allowed (might have changed dimension or opened inventory)
                if (!canDragOverlay(resizingOverlay)) {
                    resizingOverlay = null;
                    return false;
                }
                
                int deltaWidth = (int) mouseX - resizeStartX;
                int deltaHeight = (int) mouseY - resizeStartY;
                
                int newWidth = Math.max(MIN_OVERLAY_RESIZE_WIDTH, resizeStartWidth + deltaWidth);
                int newHeight = Math.max(MIN_OVERLAY_RESIZE_HEIGHT, resizeStartHeight + deltaHeight);
                
                // Setze die neue Größe
                resizingOverlay.setSize(newWidth, newHeight);
                
                // Berechne die neue Position basierend auf der Bildschirmseite
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getWindow() != null) {
                    int screenWidth = client.getWindow().getGuiScaledWidth();
                    int screenHeight = client.getWindow().getGuiScaledHeight();
                    int currentY = resizingOverlay.getY();
                    
                    // Prüfe, ob es das MkLevel-Overlay ist (sollte immer von oben links vergrößern)
                    boolean isMkLevelOverlay = resizingOverlay instanceof MkLevelDraggableOverlay;
                    
                    int newX;
                    int newY = currentY;
                    
                    if (isMkLevelOverlay) {
                        // Für MkLevel: Obere linke Ecke immer fixiert
                        newX = resizeStartOverlayX;
                        // Stelle sicher, dass das Overlay nicht aus dem Bildschirm ragt
                        if (newX + newWidth > screenWidth) {
                            newX = screenWidth - newWidth;
                        }
                    } else {
                        // Bestimme ob das Overlay auf der linken oder rechten Seite ist
                        // Verwende die ursprüngliche Position, nicht die aktuelle (die könnte durch getX() falsch berechnet sein)
                        boolean isOnRightSide = resizeStartOverlayX >= screenWidth / 2;
                        
                        if (isOnRightSide) {
                            // Auf der rechten Seite: Rechte Kante fix halten
                            // Berechne die Position der rechten Kante vor dem Resize
                            int rightEdgeBefore = resizeStartOverlayX + resizeStartWidth;
                            // Neue X-Position: Rechte Kante minus neue Breite
                            newX = rightEdgeBefore - newWidth;
                            // Stelle sicher, dass das Overlay nicht aus dem Bildschirm ragt
                            newX = Math.max(0, Math.min(newX, screenWidth - newWidth));
                        } else {
                            // Auf der linken Seite: Linke Kante fix halten (X bleibt gleich)
                            newX = resizeStartOverlayX;
                            // Stelle sicher, dass das Overlay nicht aus dem Bildschirm ragt
                            if (newX + newWidth > screenWidth) {
                                newX = screenWidth - newWidth;
                            }
                        }
                    }
                    
                    // Stelle sicher, dass Y innerhalb der Bildschirmgrenzen bleibt
                    newY = Math.max(0, Math.min(newY, screenHeight - newHeight));
                    
                    // Setze die neue Position
                    resizingOverlay.setPosition(newX, newY);
                }
                
                OverlayDockLayout.invalidatePositions();
                return true;
            }
        }
        
        return super.mouseDragged(event, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (transparencySlider != null && transparencySlider.isDragging()) {
            super.mouseReleased(event);
            transparencySlider.stopDragging();
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button = event.button();
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                commitDock(draggingOverlay);
                OverlayDockLayout.clearEditorDragSkip();
                draggingOverlay.savePosition();
                draggingOverlay = null;
                snapOffer = null;
                return true;
            }
            
            if (resizingOverlay != null) {
                resizingOverlay.savePosition();
                resizingOverlay = null;
                return true;
            }
        }
        
        return super.mouseReleased(event);
    }
    
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (DetailKeybindCapture.blocksModHotkeys()) {
            if (DetailKeybindCapture.isCapturing()) {
                DetailKeybindActions.onKey(keyCode);
            }
            return true;
        }
        if (clipboardSettingsOpen && clipboardMaxLinesFocused) {
            NumericFieldInput.Result result = NumericFieldInput.handle(
                    clipboardMaxLinesField, keyCode, event.modifiers(), 4);
            if (result == NumericFieldInput.Result.SUBMIT || result == NumericFieldInput.Result.CANCEL) {
                commitClipboardMaxLines();
            }
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (OverlayEditorUtility.matchesEditorBarKey(event)) {
            editorButtonsVisible = !editorButtonsVisible;
            init();
            return true;
        }

        if (draggingOverlay == null && transparencySlider != null && getFocused() == transparencySlider
                && transparencySlider.keyPressed(event)) {
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Also close with the configured overlay editor key (toggle behavior)
        // Use the same key binding as outside inventories
        if (OverlayEditorUtility.getOverlayEditorKeyMapping() != null) {
            if (OverlayEditorUtility.getOverlayEditorKeyMapping().matches(new net.minecraft.client.input.KeyEvent(keyCode, -1, 0))) {
                onClose();
                return true;
            }
        }
        
        // Arrow key movement when holding an overlay with left mouse button
        if (draggingOverlay != null) {
            // Check if dragging is still allowed (might have changed dimension or opened inventory)
            if (!canDragOverlay(draggingOverlay)) {
                draggingOverlay = null;
                return false;
            }
            
            boolean leftMousePressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
            
            if (leftMousePressed) {
                int currentX = draggingOverlay.getX();
                int currentY = draggingOverlay.getY();
                int newX = currentX;
                int newY = currentY;
                
                // Move overlay by 1 pixel in the direction of the arrow key
                if (keyCode == InputConstants.KEY_LEFT) {
                    newX = currentX - 1;
                } else if (keyCode == InputConstants.KEY_RIGHT) {
                    newX = currentX + 1;
                } else if (keyCode == InputConstants.KEY_UP) {
                    newY = currentY - 1;
                } else if (keyCode == InputConstants.KEY_DOWN) {
                    newY = currentY + 1;
                }
                
                // Keep overlay within screen bounds
                newX = Math.max(0, Math.min(newX, width - draggingOverlay.getWidth()));
                newY = Math.max(0, Math.min(newY, height - draggingOverlay.getHeight()));
                
                // Update position if it changed
                if (newX != currentX || newY != currentY) {
                    draggingOverlay.setPosition(newX, newY);
                    detachIfPulledAway(draggingOverlay, newX, newY);
                    OverlayDockLayout.invalidatePositions();
                    return true;
                }
            }
        }
        
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (detailPanelOpen() && vertical != 0 && detailScrollHit(mouseX, mouseY)) {
            detailScroll = DetailSettingsFrame.clampScroll(
                    detailScroll - (int) Math.signum(vertical) * 30,
                    detailContentHeight(),
                    detailVisibleHeight());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    
    private void updateSnapOffer() {
        OverlayDockSnap.Candidate previous = snapOffer;
        snapOffer = null;
        if (draggingOverlay == null || !dragDetached || !OverlayDockStore.isMagnetEnabled()) {
            return;
        }
        String id = draggingOverlay.dockId();
        if (id == null || id.isBlank()) {
            return;
        }
        snapOffer = OverlayDockSnap.findBest(
                id,
                new OverlayDockRect(draggingOverlay.getX(), draggingOverlay.getY(), draggingOverlay.getWidth(), draggingOverlay.getHeight()),
                snapParticipants(id),
                previous,
                true,
                OverlayDockStore.links(),
                width,
                height);
    }

    private List<OverlayDockSnap.Participant> snapParticipants(String draggedId) {
        List<OverlayDockSnap.Participant> participants = new java.util.ArrayList<>();
        for (DraggableOverlay overlay : overlays) {
            if (overlay == null || !overlay.isDrawnInEditor()) {
                continue;
            }
            String id = overlay.dockId();
            if (id == null || id.isBlank() || id.equals(draggedId)) {
                continue;
            }
            participants.add(new OverlayDockSnap.Participant(id,
                    new OverlayDockRect(overlay.getX(), overlay.getY(), overlay.getWidth(), overlay.getHeight())));
        }
        return participants;
    }

    private void detachIfPulledAway(DraggableOverlay overlay, int x, int y) {
        if (dragDetached || overlay.dockId() == null || overlay.dockId().isBlank()) {
            return;
        }
        if (!OverlayDockLayout.shouldDetach(x - dragStartX, y - dragStartY)) {
            return;
        }
        if (OverlayDockStore.removeLinksInvolving(overlay.dockId())) {
            CCLiveUtilitiesConfig.HANDLER.save();
        }
        dragDetached = true;
    }

    private void commitDock(DraggableOverlay overlay) {
        String id = overlay.dockId();
        if (!dragDetached || id == null || id.isBlank() || !OverlayDockStore.isMagnetEnabled()) {
            return;
        }
        OverlayDockLayout.syncEditor(overlays, width, height);
        OverlayDockSnap.Candidate fresh = OverlayDockSnap.revalidate(
                id,
                new OverlayDockRect(overlay.getX(), overlay.getY(), overlay.getWidth(), overlay.getHeight()),
                snapParticipants(id),
                snapOffer,
                true,
                OverlayDockStore.links(),
                width,
                height);
        if (fresh == null) {
            return;
        }
        overlay.setPosition(fresh.snappedX(), fresh.snappedY());
        OverlayDockStore.setAttachment(fresh.toLink());
    }

    private void renderDockMarkers(GuiGraphicsExtractor context) {
        if (snapOffer != null) {
            drawDockMark(context, snapOffer.dragIconX(), snapOffer.dragIconY(), 0xE036539E, 0xFFFFFFFF);
            drawDockMark(context, snapOffer.anchorIconX(), snapOffer.anchorIconY(), 0xE036539E, 0xFFFFFFFF);
        }
        if (!OverlayDockStore.isMagnetEnabled()) {
            return;
        }
        for (OverlayDockLink link : OverlayDockStore.links()) {
            DraggableOverlay attached = overlayByDockId(link.attachedId);
            DraggableOverlay anchor = overlayByDockId(link.anchorId);
            if (attached == null || anchor == null || !attached.isDrawnInEditor() || !anchor.isDrawnInEditor()) {
                continue;
            }
            int attachedX = attached.getX();
            int attachedY = attached.getY();
            int anchorX = anchor.getX();
            int anchorY = anchor.getY();
            int markX;
            int markY;
            switch (link.side) {
                case BOTTOM -> {
                    markX = (Math.max(attachedX, anchorX) + Math.min(attachedX + attached.getWidth(), anchorX + anchor.getWidth())) / 2;
                    markY = anchorY + anchor.getHeight();
                }
                case TOP -> {
                    markX = (Math.max(attachedX, anchorX) + Math.min(attachedX + attached.getWidth(), anchorX + anchor.getWidth())) / 2;
                    markY = anchorY;
                }
                case RIGHT -> {
                    markX = anchorX + anchor.getWidth();
                    markY = (Math.max(attachedY, anchorY) + Math.min(attachedY + attached.getHeight(), anchorY + anchor.getHeight())) / 2;
                }
                case LEFT -> {
                    markX = anchorX;
                    markY = (Math.max(attachedY, anchorY) + Math.min(attachedY + attached.getHeight(), anchorY + anchor.getHeight())) / 2;
                }
                default -> {
                    continue;
                }
            }
            drawDockMark(context, markX, markY, 0x50202848, 0x60FFFFFF);
        }
    }

    private DraggableOverlay overlayByDockId(String id) {
        for (DraggableOverlay overlay : overlays) {
            if (overlay != null && id.equals(overlay.dockId())) {
                return overlay;
            }
        }
        return null;
    }

    private void drawDockMark(GuiGraphicsExtractor context, int centerX, int centerY, int fill, int outline) {
        int size = 10;
        int x = centerX - size / 2;
        int y = centerY - size / 2;
        context.fill(x, y, x + size, y + size, fill);
        context.outline(x, y, size, size, outline);
        int icon = size - 2;
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x + 1, y + 1);
        matrices.scale(icon / 24f, icon / 24f);
        context.blit(RenderPipelines.GUI_TEXTURED, DOCK_MAGNET_ICON, 0, 0, 0.0f, 0.0f, 24, 24, 24, 24);
        matrices.popMatrix();
    }

    private void resetAllOverlays() {
        OverlayDockStore.clearAll();
        OverlayDockLayout.invalidate();
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isConfigEnabled()) {
                overlay.resetToDefault();
                clampOverlayToScreen(overlay);
                overlay.savePosition();
            }
        }
        OverlayDockLayout.syncEditor(overlays, width, height);
    }

    private void clampOverlayToScreen(DraggableOverlay overlay) {
        int maxX = Math.max(0, width - overlay.getWidth());
        int maxY = Math.max(0, height - overlay.getHeight());
        int x = overlay.undockedX();
        int y = overlay.undockedY();
        int clampedX = Math.max(0, Math.min(x, maxX));
        int clampedY = Math.max(0, Math.min(y, maxY));
        if (clampedX != x || clampedY != y) {
            overlay.setPosition(clampedX, clampedY);
        }
    }
    
    @Override
    public void onClose() {
        // Save all overlay positions
        for (DraggableOverlay overlay : overlays) {
            overlay.savePosition();
        }
        
        // Save configuration
        CCLiveUtilitiesConfig.HANDLER.save();
        
        // Reset the overlay editor open status
        OverlayEditorUtility.setOverlayEditorOpen(false);
        OverlayDockLayout.onEditorClosed();
        resetDetailScroll();
        
        // Restore the previous screen instead of closing
        Minecraft client = Minecraft.getInstance();
        if (client != null && previousScreen != null) {
            client.gui.setScreen(previousScreen);
        } else {
            super.onClose();
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Check if the player is currently in a dimension that matches their player name
     */
    private boolean isInPlayerNameDimension() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        
        String playerName = client.player.getName().getString().toLowerCase();
        String dimensionPath = client.level.dimension().identifier().getPath();
        
        // Check if dimension matches player name (player is in their own dimension)
        return dimensionPath.equals(playerName);
    }
    
    /**
     * Check if the player is currently in the Overworld dimension
     */
    private boolean isInOverworld() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        
        // Check if the dimension is the Overworld
        return client.level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }
    
    /**
     * Check if an inventory screen is currently open
     */
    private boolean isInventoryOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Check if the current screen is an inventory-like screen (AbstractContainerScreen)
        return client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
    }
    
    /**
     * Check if dragging is allowed for the given overlay
     */
    private boolean canDragOverlay(DraggableOverlay overlay) {
        // Mining/Lumberjack overlay can only be dragged in Overworld and not in inventories
        if (overlay instanceof MiningLumberjackDraggableOverlay) {
            return isInOverworld() && !isInventoryOpen();
        }
        // All other overlays can be dragged normally
        return true;
    }
    
    /**
     * Rendert das Overlay-Settings-Overlay mit Checkboxen für alle verfügbaren Overlays
     */
    private void renderOverlaySettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Erstelle gefilterte Liste der Overlays (Kit-Filter-Buttons werden gruppiert)
        List<OverlayEntry> displayOverlays = getDisplayOverlays();
        
        int boxWidth = 250;
        int boxHeight = Math.min(400, displayOverlays.size() * 25 + 40);
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.outline(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.text(font, "Overlay Settings", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Checkboxen für alle Overlays
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        
        for (OverlayEntry entry : displayOverlays) {
            boolean isEnabled = entry.isEnabled;
            
            // Checkbox-Hintergrund
            int checkboxY = y;
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.outline(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
            // Checkmark wenn aktiviert
            if (isEnabled) {
                // Zeichne Häkchen (✓)
                int checkX = checkboxX + 2;
                int checkY = checkboxY + 2;
                int checkSize = checkboxSize - 4;
                // Zeichne Häkchen als zwei Linien
                // Linke Linie (von oben-links nach mitte)
                for (int i = 0; i < checkSize / 2; i++) {
                    int px = checkX + i;
                    int py = checkY + checkSize / 2 + i;
                    if (px < checkboxX + checkboxSize - 2 && py < checkboxY + checkboxSize - 2) {
                        context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                    }
                }
                // Rechte Linie (von mitte nach unten-rechts)
                for (int i = 0; i < checkSize / 2; i++) {
                    int px = checkX + checkSize / 2 + i;
                    int py = checkY + checkSize - 2 - i;
                    if (px < checkboxX + checkboxSize - 2 && py >= checkboxY + 2) {
                        context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                    }
                }
            }
            
            // Overlay-Name
            String overlayName = entry.displayName;
            int textX = checkboxX + checkboxSize + 5;
            int textWidth = font.width(overlayName);
            int textHeight = font.lineHeight;
            context.text(font, overlayName, textX, checkboxY + 1, isEnabled ? 0xFFFFFFFF : 0xFF808080, false);
            
            // Settings-Button rechts in der Zeile, wie npcAlerts
            if (hasDetailSettings(entry.overlay)) {
                int gearSize = 12;
                int gearX = boxX + boxWidth - gearSize - 10;
                int gearY = checkboxY - 1;
                
                // Prüfe ob Maus über Checkbox, Text oder Icon ist
                boolean isHoveringCheckbox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                            mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize;
                boolean isHoveringText = mouseX >= textX && mouseX <= textX + textWidth &&
                                       mouseY >= checkboxY && mouseY <= checkboxY + textHeight;
                boolean isHoveringGear = mouseX >= gearX && mouseX <= gearX + gearSize &&
                                         mouseY >= gearY && mouseY <= gearY + gearSize;
                boolean isHoveringEntry = isHoveringCheckbox || isHoveringText;
                
                // Zeichne weißen Rahmen um das Zahnrad-Icon
                context.outline(gearX - 1, gearY - 1, gearSize + 2, gearSize + 2, 0xFFFFFFFF);
                
                drawGearIcon(context, gearX, gearY, gearSize);
                
                // Zeichne Hover-Hintergrund NACH allen Elementen, damit es darüber liegt
                if (isHoveringEntry) {
                    // Hover-Hintergrund für Checkbox und Text
                    int hoverStartX = checkboxX - 2;
                    int hoverEndX = textX + textWidth + 2;
                    context.fill(hoverStartX, checkboxY - 1, hoverEndX, checkboxY + checkboxSize + 1, 0x40FFFFFF);
                }
                
                // Zeichne Hover-Hintergrund für Zahnrad-Icon
                if (isHoveringGear) {
                    context.fill(gearX - 1, gearY - 1, gearX + gearSize + 1, gearY + gearSize + 1, 0x40FFFFFF);
                }
            }
            
            y += checkboxSpacing;
        }
    }
    
    /**
     * Hilfsklasse für Overlay-Einträge im Settings-Overlay
     */
    private static class OverlayEntry {
        String displayName;
        boolean isEnabled;
        DraggableOverlay overlay; // Kann null sein für gruppierte Einträge
        boolean isKitFilterGroup; // true wenn es die gruppierten Kit-Filter-Buttons sind
        
        OverlayEntry(String displayName, boolean isEnabled, DraggableOverlay overlay, boolean isKitFilterGroup) {
            this.displayName = displayName;
            this.isEnabled = isEnabled;
            this.overlay = overlay;
            this.isKitFilterGroup = isKitFilterGroup;
        }
    }
    
    /**
     * Erstellt eine gefilterte Liste der Overlays für die Anzeige
     * Kit-Filter-Buttons werden als ein Eintrag "Kit Filter" gruppiert
     */
    private List<OverlayEntry> getDisplayOverlays() {
        List<OverlayEntry> displayList = new ArrayList<>();
        boolean hasKitFilterButtons = false;
        boolean kitFilterEnabled = false;
        
        for (DraggableOverlay overlay : overlays) {
            // Überspringe NPC Alerts Overlays - diese sollen nicht im Overlay Picker erscheinen
            if (overlay instanceof NpcAlertsMainDraggableOverlay || overlay instanceof NpcAlertsSeparateDraggableOverlay) {
                continue;
            }
            
            // Prüfe ob es ein Kit-Filter-Button ist
            if (overlay instanceof KitFilterButtonDraggableOverlay) {
                hasKitFilterButtons = true;
                // Kit-Filter ist enabled wenn mindestens einer der Buttons enabled ist
                if (overlay.isConfigEnabled()) {
                    kitFilterEnabled = true;
                }
            } else {
                // Normales Overlay - direkt hinzufügen
                displayList.add(new OverlayEntry(overlay.getOverlayName(), overlay.isConfigEnabled(), overlay, false));
            }
        }
        
        // Füge gruppierten Kit-Filter-Eintrag hinzu, wenn Kit-Filter-Buttons vorhanden sind
        if (hasKitFilterButtons) {
            displayList.add(new OverlayEntry("Kit Filter", kitFilterEnabled, null, true));
        }
        
        return displayList;
    }
    
    /**
     * Behandelt Klicks im Overlay-Settings-Overlay
     */
    private boolean handleOverlaySettingsClick(double mouseX, double mouseY, int button) {
        if (!overlaySettingsOpen || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        
        // Erstelle gefilterte Liste der Overlays (Kit-Filter-Buttons werden gruppiert)
        List<OverlayEntry> displayOverlays = getDisplayOverlays();
        
        int boxWidth = 250;
        int boxHeight = Math.min(400, displayOverlays.size() * 25 + 40);
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            // Klick außerhalb - schließe Overlay
            overlaySettingsOpen = false;
            return false;
        }
        
        // Prüfe ob Klick auf eine Checkbox, den Namen oder Settings-Button ist
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        int textX = checkboxX + checkboxSize + 5;
        
        for (OverlayEntry entry : displayOverlays) {
            int checkboxY = y;
            int textY = checkboxY;
            int textHeight = font.lineHeight;
            int textWidth = font.width(entry.displayName);
            
            // Prüfe ob Klick auf Checkbox
            boolean clickedOnCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                         mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize);
            
            // Prüfe ob Klick auf Text (Overlay-Name)
            boolean clickedOnText = (mouseX >= textX && mouseX <= textX + textWidth &&
                                     mouseY >= textY && mouseY <= textY + textHeight);
            
            // Prüfe ob Klick auf Settings-Button für Clipboard
            boolean clickedOnSettings = false;
            if (hasDetailSettings(entry.overlay)) {
                int gearSize = 12;
                int gearX = boxX + boxWidth - gearSize - 10;
                int gearY = checkboxY - 1;
                clickedOnSettings = (mouseX >= gearX - 1 && mouseX <= gearX + gearSize + 1 &&
                                     mouseY >= gearY - 1 && mouseY <= gearY + gearSize + 1);
            }
            
            if (clickedOnSettings) {
                resetDetailScroll();
                if (entry.overlay instanceof MaterialTrackerDraggableOverlay) {
                    materialTrackerSettingsOpen = !materialTrackerSettingsOpen;
                    if (materialTrackerSettingsOpen) {
                        clipboardSettingsOpen = false;
                        killTrackerSettingsOpen = false;
                        openTrackerDetail = null;
                    }
                } else if (entry.overlay instanceof KillsUtilityDraggableOverlay) {
                    killTrackerSettingsOpen = !killTrackerSettingsOpen;
                    if (killTrackerSettingsOpen) {
                        clipboardSettingsOpen = false;
                        materialTrackerSettingsOpen = false;
                        openTrackerDetail = null;
                    }
                } else if (entry.overlay instanceof ClipboardDraggableOverlay) {
                    clipboardSettingsOpen = !clipboardSettingsOpen;
                    if (clipboardSettingsOpen) {
                        materialTrackerSettingsOpen = false;
                        killTrackerSettingsOpen = false;
                        openTrackerDetail = null;
                    }
                } else if (openTrackerDetail == entry.overlay) {
                    openTrackerDetail = null;
                } else {
                    openTrackerDetail = entry.overlay;
                    clipboardSettingsOpen = false;
                    materialTrackerSettingsOpen = false;
                    killTrackerSettingsOpen = false;
                }
                return true;
            }
            
            if (clickedOnCheckbox || clickedOnText) {
                // Toggle Overlay
                if (entry.isKitFilterGroup) {
                    // Toggle alle Kit-Filter-Buttons gemeinsam
                    CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled;
                } else if (entry.overlay != null) {
                    // Toggle einzelnes Overlay
                    toggleOverlayEnabled(entry.overlay);
                }
                CCLiveUtilitiesConfig.HANDLER.save();
                return true;
            }
            
            y += checkboxSpacing;
        }
        
        return false;
    }
    
    /**
     * Schaltet ein Overlay ein/aus
     * Synchronisiert sowohl die *Enabled als auch die show* Optionen
     */
    private void toggleOverlayEnabled(DraggableOverlay overlay) {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        
        if (overlay instanceof BossHPDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showBossHP = newValue;
            config.bossHPEnabled = newValue;
        } else if (overlay instanceof FactoryLevelDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showFactoryLevel = newValue;
            config.factoryLevelEnabled = newValue;
        } else if (overlay instanceof EssenceFarmDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showEssenceFarm = newValue;
            config.essenceFarmEnabled = newValue;
        } else if (overlay instanceof CardsDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showCard = newValue;
            config.cardEnabled = newValue;
            if (newValue) {
                config.cardsStatuesEnabled = true;
            }
        } else if (overlay instanceof StatuesDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showStatue = newValue;
            config.statueEnabled = newValue;
            if (newValue) {
                config.cardsStatuesEnabled = true;
            }
        } else if (overlay instanceof BlueprintViewerDraggableOverlay) {
            // Nur showBlueprintViewer togglen (gleichgestellt mit "Blueprint Tracker Overlay anzeigen" in Config)
            config.showBlueprintViewer = !config.showBlueprintViewer;
            BPViewerUtility.resetHotkeyVisibility();
        } else if (overlay instanceof MaterialTrackerDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.materialTrackerEnabled = newValue;
            config.showMaterialTracker = newValue;
        } else if (overlay instanceof KillsUtilityDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.killsUtilityEnabled = newValue;
            config.showKillsUtility = newValue;
        } else if (overlay instanceof CoinTrackerDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.coinTrackerEnabled = newValue;
            config.showCoinTracker = newValue;
            if (newValue) {
                // Overlay-Picker steuert das Overlay selbst → Anzeige auf Overlay setzen
                config.coinTrackerDisplayMode = net.felix.utilities.Aincraft.cointracker.CoinTrackerDisplayMode.OVERLAY;
            }
        } else if (overlay instanceof LeaderboardDraggableOverlay) {
            config.leaderboardVisible = !config.leaderboardVisible;
        } else if (overlay instanceof MiningLumberjackDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.miningLumberjackOverlayEnabled = newValue;
            config.miningOverlayEnabled = newValue;
            config.lumberjackOverlayEnabled = newValue;
            config.showMiningOverlay = newValue;
            config.showLumberjackOverlay = newValue;
        } else if (overlay instanceof CollectionDraggableOverlay) {
            config.showCollectionOverlay = !config.showCollectionOverlay;
        } else if (overlay instanceof AspectDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showAspectOverlay = newValue;
            config.aspectOverlayEnabled = newValue;
        } else if (overlay instanceof StarAspectDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.showAspectOverlay = newValue;
            config.aspectOverlayEnabled = newValue;
        } else if (overlay instanceof ChatAspectOverlayDraggableOverlay) {
            config.chatAspectOverlayEnabled = !config.chatAspectOverlayEnabled;
        } else if (overlay instanceof HideUncraftableButtonDraggableOverlay) {
            config.hideUncraftableEnabled = !config.hideUncraftableEnabled;
        } else if (overlay instanceof HideWrongClassButtonDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.hideWrongClassEnabled = newValue;
            config.showHideWrongClassButton = newValue;
        } else if (overlay instanceof KitFilterButtonDraggableOverlay) {
            config.kitFilterButtonsEnabled = !config.kitFilterButtonsEnabled;
        } else if (overlay instanceof MkLevelDraggableOverlay) {
            config.mkLevelEnabled = !config.mkLevelEnabled;
        } else if (overlay instanceof ClipboardDraggableOverlay) {
            boolean newValue = !overlay.isConfigEnabled();
            config.clipboardEnabled = newValue;
            config.showClipboard = newValue;
        } else if (overlay instanceof BuildShareDraggableOverlay) {
            config.buildShareEnabled = !config.buildShareEnabled;
        }
        if (overlay.dockId() != null && !overlay.dockId().isBlank() && !overlay.isConfigEnabled()) {
            OverlayDockStore.removeLinksInvolving(overlay.dockId());
        }
    }
    
    /**
     * Zeichnet das Zahnrad-Icon (Settings-Icon) wie in NpcAlertsSettingsScreen
     */
    private void drawGearIcon(GuiGraphicsExtractor context, int x, int y, int size) {
        try {
            // Zeichne das Settings-Icon aus der Textur
            Identifier settingsIcon = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/alert_icons/alert_icons_settings.png");
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                settingsIcon,
                x, y,
                0.0f, 0.0f,
                size, size,
                size, size
            );
        } catch (Exception e) {
            // Fallback: Zeichne ein einfaches Zahnrad-Icon als gefüllte Formen
            // Äußerer Rahmen
            context.outline(x, y, size, size, 0xFFFFFFFF);
            
            // Diagonale Linien (als gefüllte Rechtecke)
            int lineWidth = 1;
            // Hauptdiagonale
            for (int i = 0; i < size; i++) {
                int px = x + i;
                int py = y + i;
                if (px < x + size && py < y + size) {
                    context.fill(px, py, px + lineWidth, py + lineWidth, 0xFFFFFFFF);
                }
            }
            // Nebendiagonale
            for (int i = 0; i < size; i++) {
                int px = x + size - 1 - i;
                int py = y + i;
                if (px >= x && py < y + size) {
                    context.fill(px, py, px + lineWidth, py + lineWidth, 0xFFFFFFFF);
                }
            }
        }
    }
    
    private void syncClipboardMaxLinesDraft() {
        int stored = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaxCostLines;
        clipboardMaxLinesField.setText(stored > 0 ? Integer.toString(stored) : "");
    }

    private void commitClipboardMaxLines() {
        int[] parsed = new int[1];
        if (ClipboardTargetTime.tryParseMaxLines(clipboardMaxLinesField.text, parsed)) {
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaxCostLines = parsed[0];
            CCLiveUtilitiesConfig.HANDLER.save();
            ClipboardDraggableOverlay.invalidateRenderSnapshot();
            clipboardMaxLinesField.setText(parsed[0] > 0 ? Integer.toString(parsed[0]) : "");
        } else {
            syncClipboardMaxLinesDraft();
        }
        clipboardMaxLinesFocused = false;
    }

    private int clipboardMaxLinesY(int boxY) {
        return boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.CLIPBOARD);
    }

    private int clipboardFirstButtonY(int boxY) {
        return clipboardMaxLinesY(boxY) + CLIPBOARD_FIELD_HEIGHT + 4 + 12 + 10;
    }

    private int clipboardButton5Y(int boxY) {
        int button1 = clipboardFirstButtonY(boxY);
        int target = button1 + CLIPBOARD_BUTTON_SPACING;
        int hideEmpty = target + CLIPBOARD_BUTTON_SPACING;
        int hideCompleted = hideEmpty + CLIPBOARD_BUTTON_SPACING;
        int button2 = hideCompleted + CLIPBOARD_BUTTON_SPACING + 5 + CLIPBOARD_BUTTON_HEIGHT;
        int button3 = button2 + CLIPBOARD_BUTTON_SPACING;
        int button4 = button3 + CLIPBOARD_BUTTON_SPACING + 5 + CLIPBOARD_BUTTON_HEIGHT;
        return button4 + CLIPBOARD_BUTTON_SPACING;
    }

    private int clipboardSettingsBoxHeight() {
        return clipboardButton5Y(0) + CLIPBOARD_BUTTON_HEIGHT + 12;
    }

    private void drawSettingsCloseCross(GuiGraphicsExtractor context, int x, int y, int size, int color) {
        int x2 = x + size - 2;
        int y2 = y + size - 2;
        int steps = Math.max(Math.abs(x2 - (x + 2)), Math.abs(y2 - (y + 2)));
        for (int i = 0; i <= steps; i++) {
            int px = (x + 2) + (x2 - (x + 2)) * i / Math.max(1, steps);
            int py = (y + 2) + (y2 - (y + 2)) * i / Math.max(1, steps);
            context.fill(px, py, px + 2, py + 2, color);
            int qx = x2 - (x2 - (x + 2)) * i / Math.max(1, steps);
            context.fill(qx, py, qx + 2, py + 2, color);
        }
    }

    private float clipboardSettingsScale() {
        return DetailSettingsFrame.scale(width, height, CLIPBOARD_SETTINGS_WIDTH, clipboardSettingsBoxHeight());
    }

    private float clipboardSettingsCenterY(float scale) {
        int visible = DetailSettingsFrame.visibleHeight(height, scale, clipboardSettingsBoxHeight());
        float scaledHeight = visible * scale;
        float top = height - CLIPBOARD_SETTINGS_BOTTOM_MARGIN - scaledHeight;
        if (top < CLIPBOARD_SETTINGS_TOP_MARGIN) {
            top = CLIPBOARD_SETTINGS_TOP_MARGIN;
        }
        return top + scaledHeight / 2.0f;
    }

    private int clipboardSettingsMouseX(double mouseX, float scale) {
        return (int) Math.floor((mouseX - width / 2.0) / scale + width / 2.0);
    }

    private int clipboardSettingsMouseY(double mouseY, float scale) {
        return (int) Math.floor((mouseY - clipboardSettingsCenterY(scale)) / scale + height / 2.0);
    }

    /**
     * Rendert das Clipboard-Settings-Overlay
     */
    private void renderClipboardSettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        float scale = clipboardSettingsScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = clipboardSettingsCenterY(scale) - height / 2.0f * scale;
        int localMouseX = clipboardSettingsMouseX(mouseX, scale);
        int localMouseY = clipboardSettingsMouseY(mouseY, scale);
        context.pose().pushMatrix();
        try {
            context.pose().translate(offsetX, offsetY);
            context.pose().scale(scale, scale);
            renderClipboardSettingsBox(context, localMouseX, localMouseY);
        } finally {
            context.pose().popMatrix();
        }
    }

    private void renderClipboardSettingsBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(clipboardSettingsBoxHeight(), clipboardSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.outline(boxX, boxY, boxWidth, boxHeight, DETAIL_BORDER_COLOR);
        
        // Titel
        context.text(font, "Clipboard Settings", boxX + 10, boxY + 10, 0xFFFFFF00, false);

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        drawSettingsCloseCross(context, closeX, closeY, closeSize, closeHovered ? 0xFFFF6666 : 0xFFFF0000);
        beginDetailContent(context, mouseX, mouseY, boxX, boxY, boxWidth, boxHeight, DetailKeybindCatalog.Panel.CLIPBOARD);
        mouseY += detailScroll;
        
        // Buttons
        int buttonHeight = CLIPBOARD_BUTTON_HEIGHT;
        int buttonSpacing = CLIPBOARD_BUTTON_SPACING;
        int buttonWidth = boxWidth - 20;
        
        if (!clipboardMaxLinesFocused && clipboardMaxLinesField.text.isEmpty()) {
            int stored = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaxCostLines;
            if (stored > 0) {
                clipboardMaxLinesField.setText(Integer.toString(stored));
            }
        }

        int maxLinesY = clipboardMaxLinesY(boxY);
        String maxLinesLabel = "Maximale Zeilen:";
        context.text(font, maxLinesLabel, boxX + 10, maxLinesY + 4, 0xFFFFFFFF, false);
        int fieldX = boxX + 10 + font.width(maxLinesLabel) + 8;
        int fieldWidth = 80;
        boolean fieldHovered = mouseX >= fieldX && mouseX <= fieldX + fieldWidth
                && mouseY >= maxLinesY && mouseY <= maxLinesY + CLIPBOARD_FIELD_HEIGHT;
        context.fill(fieldX, maxLinesY, fieldX + fieldWidth, maxLinesY + CLIPBOARD_FIELD_HEIGHT,
                clipboardMaxLinesFocused || fieldHovered ? 0xFF404040 : 0xFF202020);
        context.outline(fieldX, maxLinesY, fieldWidth, CLIPBOARD_FIELD_HEIGHT,
                clipboardMaxLinesFocused ? 0xFFFFFF00 : 0xFF808080);
        NumericFieldInput.draw(context, font, clipboardMaxLinesField, fieldX + 3, maxLinesY + 4,
                0xFFFFFFFF, clipboardMaxLinesFocused);
        context.text(font, "Leer = alle Zeilen", boxX + 10, maxLinesY + CLIPBOARD_FIELD_HEIGHT + 4, 0xFF808080, false);

        // Button 1: Blueprint Shop Kosten anzeigen/ausblenden
        boolean showBPCosts = CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts;
        int button1Y = clipboardFirstButtonY(boxY);
        boolean button1Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button1Y && mouseY <= button1Y + buttonHeight;
        int button1Color = button1Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button1Y, boxX + 10 + buttonWidth, button1Y + buttonHeight, button1Color);
        context.outline(boxX + 10, button1Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button1Text = "Blueprint Shop Kosten: " + (showBPCosts ? "An" : "Aus");
        context.text(font, button1Text, boxX + 15, button1Y + 6, 0xFFFFFFFF, false);

        boolean approxTimeEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardApproxTimeEnabled;
        int targetY = button1Y + buttonSpacing;
        boolean targetHovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                mouseY >= targetY && mouseY <= targetY + buttonHeight;
        int targetColor = targetHovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, targetY, boxX + 10 + buttonWidth, targetY + buttonHeight, targetColor);
        context.outline(boxX + 10, targetY, buttonWidth, buttonHeight, 0xFFFFFFFF);
        context.text(font, "Ungefähre Zeit: " + (approxTimeEnabled ? "An" : "Aus"), boxX + 15, targetY + 6, 0xFFFFFFFF, false);

        boolean hideEmpty = CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty;
        int hideEmptyY = targetY + buttonSpacing;
        boolean hideEmptyHovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                mouseY >= hideEmptyY && mouseY <= hideEmptyY + buttonHeight;
        context.fill(boxX + 10, hideEmptyY, boxX + 10 + buttonWidth, hideEmptyY + buttonHeight,
                hideEmptyHovered ? 0xFF404040 : 0xFF202020);
        context.outline(boxX + 10, hideEmptyY, buttonWidth, buttonHeight, 0xFFFFFFFF);
        context.text(font, "Leeres Clipboard ausblenden: " + (hideEmpty ? "An" : "Aus"),
                boxX + 15, hideEmptyY + 6, 0xFFFFFFFF, false);

        boolean hideCompleted = CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideCompletedBlueprints;
        int hideCompletedY = hideEmptyY + buttonSpacing;
        boolean hideCompletedHovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                mouseY >= hideCompletedY && mouseY <= hideCompletedY + buttonHeight;
        context.fill(boxX + 10, hideCompletedY, boxX + 10 + buttonWidth, hideCompletedY + buttonHeight,
                hideCompletedHovered ? 0xFF404040 : 0xFF202020);
        context.outline(boxX + 10, hideCompletedY, buttonWidth, buttonHeight, 0xFFFFFFFF);
        context.text(font, "Vervollständigte Baupläne ausblenden: " + (hideCompleted ? "An" : "Aus"),
                boxX + 15, hideCompletedY + 6, 0xFFFFFFFF, false);
        
        // Überschrift: Material Sortierung
        int headerY = hideCompletedY + buttonSpacing + 5;
        context.text(font, "Material Sortierung", boxX + 10, headerY, 0xFFFFFF00, false);
        
        // Button 2: Material Sortierung An/Aus
        int button2Y = headerY + 20;
        boolean materialSortEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
        boolean button2Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button2Y && mouseY <= button2Y + buttonHeight;
        int button2Color = button2Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button2Y, boxX + 10 + buttonWidth, button2Y + buttonHeight, button2Color);
        context.outline(boxX + 10, button2Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button2Text = "Material Sortierung: " + (materialSortEnabled ? "An" : "Aus");
        context.text(font, button2Text, boxX + 15, button2Y + 6, 0xFFFFFFFF, false);
        
        // Button 3: Aufsteigend/Absteigend Sortierung
        int button3Y = button2Y + buttonSpacing;
        boolean materialSortAscending = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortAscending;
        boolean button3Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button3Y && mouseY <= button3Y + buttonHeight;
        int button3Color = button3Hovered ? 0xFF404040 : 0xFF202020;
        // Button nur aktiv, wenn Sortierung aktiviert ist
        if (!materialSortEnabled) {
            button3Color = 0xFF101010; // Dunkler wenn deaktiviert
        }
        context.fill(boxX + 10, button3Y, boxX + 10 + buttonWidth, button3Y + buttonHeight, button3Color);
        context.outline(boxX + 10, button3Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button3Text = "Sortierung: " + (materialSortAscending ? "Aufsteigend (1-100)" : "Absteigend (100-1)");
        int button3TextColor = materialSortEnabled ? 0xFFFFFFFF : 0xFF808080; // Grau wenn deaktiviert
        context.text(font, button3Text, boxX + 15, button3Y + 6, button3TextColor, false);
        
        // Überschrift: Fertige Kosten
        int costHeaderY = button3Y + buttonSpacing + 5;
        context.text(font, "Fertige Kosten", boxX + 10, costHeaderY, 0xFFFFFF00, false);
        
        // Button 4: Fertige Kosten Sortierung An/Aus
        int button4Y = costHeaderY + 20;
        boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
        boolean button4Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button4Y && mouseY <= button4Y + buttonHeight;
        int button4Color = button4Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button4Y, boxX + 10 + buttonWidth, button4Y + buttonHeight, button4Color);
        context.outline(boxX + 10, button4Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button4Text = "Fertige Kosten Sortierung: " + (costDisplayEnabled ? "An" : "Aus");
        context.text(font, button4Text, boxX + 15, button4Y + 6, 0xFFFFFFFF, false);
        
        // Button 5: Option 1/2 - Ausblenden/Ans Ende setzen (wechselt zwischen beiden)
        int button5Y = button4Y + buttonSpacing;
        int costDisplayMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
        boolean button5Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button5Y && mouseY <= button5Y + buttonHeight;
        int button5Color = button5Hovered ? 0xFF404040 : 0xFF202020;
        // Button nur aktiv, wenn Sortierung aktiviert ist
        if (!costDisplayEnabled) {
            button5Color = 0xFF101010; // Dunkler wenn deaktiviert
        }
        context.fill(boxX + 10, button5Y, boxX + 10 + buttonWidth, button5Y + buttonHeight, button5Color);
        context.outline(boxX + 10, button5Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button5Text = costDisplayMode == 1 ? "Ausblenden" : "Ans Ende setzen";
        int button5TextColor = costDisplayEnabled ? 0xFFFFFFFF : 0xFF808080; // Grau wenn deaktiviert
        context.text(font, button5Text, boxX + 15, button5Y + 6, button5TextColor, false);
        endDetailContent(context);
    }
    
    /**
     * Behandelt Klicks im Clipboard-Settings-Overlay
     */
    private boolean handleClipboardSettingsClick(double mouseX, double mouseY, int button) {
        if (!clipboardSettingsOpen || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        
        float scale = clipboardSettingsScale();
        mouseX = clipboardSettingsMouseX(mouseX, scale);
        mouseY = clipboardSettingsMouseY(mouseY, scale);
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(clipboardSettingsBoxHeight(), clipboardSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            // Klick außerhalb - schließe Overlay
            if (clipboardMaxLinesFocused) {
                commitClipboardMaxLines();
            }
            clipboardSettingsOpen = false;
            resetDetailScroll();
            return false;
        }
        
        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        if (mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize) {
            if (clipboardMaxLinesFocused) {
                commitClipboardMaxLines();
            }
            clipboardSettingsOpen = false;
            resetDetailScroll();
            return true;
        }
        if (mouseY < boxY + 36) {
            return true;
        }
        mouseY += detailScroll;

        // Button-Koordinaten
        int buttonHeight = CLIPBOARD_BUTTON_HEIGHT;
        int buttonSpacing = CLIPBOARD_BUTTON_SPACING;
        int buttonWidth = boxWidth - 20;
        if (handleKeybindClick(mouseX, mouseY, boxX, boxY, buttonWidth, DetailKeybindCatalog.Panel.CLIPBOARD)) {
            return true;
        }

        int maxLinesY = clipboardMaxLinesY(boxY);
        int fieldX = boxX + 10 + font.width("Maximale Zeilen:") + 8;
        int fieldWidth = 80;
        if (mouseX >= fieldX && mouseX <= fieldX + fieldWidth
                && mouseY >= maxLinesY && mouseY <= maxLinesY + CLIPBOARD_FIELD_HEIGHT) {
            if (!clipboardMaxLinesFocused) {
                syncClipboardMaxLinesDraft();
                clipboardMaxLinesField.focusAtEnd();
            }
            clipboardMaxLinesFocused = true;
            return true;
        }
        if (clipboardMaxLinesFocused) {
            commitClipboardMaxLines();
        }

        // Button 1: Blueprint Shop Kosten
        int button1Y = clipboardFirstButtonY(boxY);
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button1Y && mouseY <= button1Y + buttonHeight) {
            // Toggle Blueprint Shop Kosten
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts = 
                !CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts;
            CCLiveUtilitiesConfig.HANDLER.save();
            ClipboardDraggableOverlay.invalidateRenderSnapshot();
            return true;
        }

        int targetY = button1Y + buttonSpacing;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= targetY && mouseY <= targetY + buttonHeight) {
            boolean showTime = !CCLiveUtilitiesConfig.HANDLER.instance().clipboardApproxTimeEnabled;
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardApproxTimeEnabled = showTime;
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowTargetTime = showTime;
            CCLiveUtilitiesConfig.HANDLER.save();
            ClipboardDraggableOverlay.invalidateRenderSnapshot();
            return true;
        }

        int hideEmptyY = targetY + buttonSpacing;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= hideEmptyY && mouseY <= hideEmptyY + buttonHeight) {
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty =
                    !CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty;
            CCLiveUtilitiesConfig.HANDLER.save();
            ClipboardDraggableOverlay.invalidateRenderSnapshot();
            return true;
        }

        int hideCompletedY = hideEmptyY + buttonSpacing;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= hideCompletedY && mouseY <= hideCompletedY + buttonHeight) {
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideCompletedBlueprints =
                    !CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideCompletedBlueprints;
            CCLiveUtilitiesConfig.HANDLER.save();
            ClipboardDraggableOverlay.invalidateRenderSnapshot();
            return true;
        }
        
        // Button 2: Material Sortierung An/Aus
        int headerY = hideCompletedY + buttonSpacing + 5;
        int button2Y = headerY + 20;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button2Y && mouseY <= button2Y + buttonHeight) {
            // Toggle Material Sortierung
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled = 
                !CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Button 3: Aufsteigend/Absteigend Sortierung
        int button3Y = button2Y + buttonSpacing;
        boolean materialSortEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
        if (materialSortEnabled && // Nur klickbar wenn Sortierung aktiviert ist
            mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button3Y && mouseY <= button3Y + buttonHeight) {
            // Toggle Sortierungsrichtung
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortAscending = 
                !CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortAscending;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Button 4: Fertige Kosten Sortierung An/Aus
        int costHeaderY = button3Y + buttonSpacing + 5;
        int button4Y = costHeaderY + 20;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button4Y && mouseY <= button4Y + buttonHeight) {
            // Toggle Fertige Kosten Sortierung
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled = 
                !CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Button 5: Option 1/2 - Ausblenden/Ans Ende setzen (wechselt zwischen beiden)
        int button5Y = button4Y + buttonSpacing;
        boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
        if (costDisplayEnabled && // Nur klickbar wenn Sortierung aktiviert ist
            mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button5Y && mouseY <= button5Y + buttonHeight) {
            // Toggle zwischen Option 1 (Ausblenden) und Option 2 (Ans Ende setzen)
            int currentMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode = (currentMode == 1) ? 2 : 1;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        return false;
    }

    private int materialTrackerSettingsBoxHeight() {
        return 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.MATERIAL)
                + CLIPBOARD_BUTTON_SPACING * 3 + CLIPBOARD_BUTTON_HEIGHT + 12;
    }

    private float materialTrackerSettingsScale() {
        return DetailSettingsFrame.scale(width, height, CLIPBOARD_SETTINGS_WIDTH, materialTrackerSettingsBoxHeight());
    }

    private float materialTrackerSettingsCenterY(float scale) {
        return height / 2.0f;
    }

    private int materialTrackerSettingsMouseX(double mouseX, float scale) {
        return (int) Math.floor((mouseX - width / 2.0) / scale + width / 2.0);
    }

    private int materialTrackerSettingsMouseY(double mouseY, float scale) {
        return (int) Math.floor((mouseY - materialTrackerSettingsCenterY(scale)) / scale + height / 2.0);
    }

    private void renderMaterialTrackerSettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        float scale = materialTrackerSettingsScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = materialTrackerSettingsCenterY(scale) - height / 2.0f * scale;
        int localMouseX = materialTrackerSettingsMouseX(mouseX, scale);
        int localMouseY = materialTrackerSettingsMouseY(mouseY, scale);
        context.pose().pushMatrix();
        try {
            context.pose().translate(offsetX, offsetY);
            context.pose().scale(scale, scale);
            renderMaterialTrackerSettingsBox(context, localMouseX, localMouseY);
        } finally {
            context.pose().popMatrix();
        }
    }

    private void renderMaterialTrackerSettingsBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(materialTrackerSettingsBoxHeight(), materialTrackerSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        context.outline(boxX, boxY, boxWidth, boxHeight, DETAIL_BORDER_COLOR);
        context.text(font, "Material Tracker", boxX + 10, boxY + 10, 0xFFFFFF00, false);

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        drawSettingsCloseCross(context, closeX, closeY, closeSize, closeHovered ? 0xFFFF6666 : 0xFFFF0000);
        beginDetailContent(context, mouseX, mouseY, boxX, boxY, boxWidth, boxHeight, DetailKeybindCatalog.Panel.MATERIAL);
        mouseY += detailScroll;

        int buttonWidth = boxWidth - 20;
        int button1Y = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.MATERIAL);
        RateDisplayUnit materialUnit = RateDisplayUnit.orDefault(config.materialTrackerRateUnit);
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button1Y, buttonWidth,
                materialUnit.settingsSwitcherLabel(), true);
        int button2Y = button1Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button2Y, buttonWidth,
                "Overlay-Typ: " + materialTrackerOverlayTypeLabel(config.materialTrackerOverlayType), true);
        int button3Y = button2Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button3Y, buttonWidth,
                materialUnit.withBase("Materialien") + ": "
                        + (config.materialTrackerRateEnabled ? "An" : "Aus"), true);
        int button4Y = button3Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button4Y, buttonWidth,
                "Anzeige: " + (config.materialTrackerDisplayMode == MaterialTrackerDisplayMode.SCOREBOARD
                        ? "Scoreboard" : "Overlay"),
                config.materialTrackerRateEnabled);
        endDetailContent(context);
    }

    private void drawDetailButton(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y, int buttonWidth,
            String text, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX <= x + buttonWidth
                && mouseY >= y && mouseY <= y + CLIPBOARD_BUTTON_HEIGHT;
        int color = !enabled ? 0xFF101010 : hovered ? 0xFF404040 : 0xFF202020;
        context.fill(x, y, x + buttonWidth, y + CLIPBOARD_BUTTON_HEIGHT, color);
        context.outline(x, y, buttonWidth, CLIPBOARD_BUTTON_HEIGHT, 0xFFFFFFFF);
        context.text(font, text, x + 5, y + 6, enabled ? 0xFFFFFFFF : 0xFF808080, false);
    }

    private static String materialTrackerOverlayTypeLabel(OverlayType type) {
        return switch (type) {
            case CUSTOM -> "Bild-Overlay";
            case BLACK -> "Schwarzes Overlay";
            case NONE -> "Kein Hintergrund";
        };
    }

    private boolean handleMaterialTrackerSettingsClick(double mouseX, double mouseY, int button) {
        if (!materialTrackerSettingsOpen || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        float scale = materialTrackerSettingsScale();
        mouseX = materialTrackerSettingsMouseX(mouseX, scale);
        mouseY = materialTrackerSettingsMouseY(mouseY, scale);
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(materialTrackerSettingsBoxHeight(), materialTrackerSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            materialTrackerSettingsOpen = false;
            resetDetailScroll();
            return false;
        }

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        if (mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize) {
            materialTrackerSettingsOpen = false;
            resetDetailScroll();
            return true;
        }
        if (mouseY < boxY + 36) {
            return true;
        }
        mouseY += detailScroll;

        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        int buttonWidth = boxWidth - 20;
        int button1Y = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.MATERIAL);
        if (handleKeybindClick(mouseX, mouseY, boxX, boxY, buttonWidth, DetailKeybindCatalog.Panel.MATERIAL)) {
            return true;
        }
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button1Y && mouseY <= button1Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.materialTrackerRateUnit =
                    RateDisplayUnit.orDefault(config.materialTrackerRateUnit).toggled();
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button2Y = button1Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button2Y && mouseY <= button2Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.materialTrackerOverlayType = switch (config.materialTrackerOverlayType) {
                case CUSTOM -> OverlayType.BLACK;
                case BLACK -> OverlayType.NONE;
                case NONE -> OverlayType.CUSTOM;
            };
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button3Y = button2Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button3Y && mouseY <= button3Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.materialTrackerRateEnabled = !config.materialTrackerRateEnabled;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button4Y = button3Y + CLIPBOARD_BUTTON_SPACING;
        if (config.materialTrackerRateEnabled
                && mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button4Y && mouseY <= button4Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.materialTrackerDisplayMode = config.materialTrackerDisplayMode == MaterialTrackerDisplayMode.SCOREBOARD
                    ? MaterialTrackerDisplayMode.OVERLAY
                    : MaterialTrackerDisplayMode.SCOREBOARD;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        return true;
    }

    private static boolean hasDetailSettings(DraggableOverlay overlay) {
        return overlay instanceof ClipboardDraggableOverlay
                || overlay instanceof MaterialTrackerDraggableOverlay
                || overlay instanceof KillsUtilityDraggableOverlay
                || TrackerDetailSettings.supports(overlay);
    }

    private int killTrackerSettingsBoxHeight() {
        return 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.KILLS)
                + CLIPBOARD_BUTTON_SPACING * 4 + CLIPBOARD_BUTTON_HEIGHT + 12;
    }

    private float killTrackerSettingsScale() {
        return DetailSettingsFrame.scale(width, height, CLIPBOARD_SETTINGS_WIDTH, killTrackerSettingsBoxHeight());
    }

    private int killTrackerSettingsMouseX(double mouseX, float scale) {
        return (int) Math.floor((mouseX - width / 2.0) / scale + width / 2.0);
    }

    private int killTrackerSettingsMouseY(double mouseY, float scale) {
        return (int) Math.floor((mouseY - height / 2.0) / scale + height / 2.0);
    }

    private void renderKillTrackerSettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        float scale = killTrackerSettingsScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = height / 2.0f * (1.0f - scale);
        int localMouseX = killTrackerSettingsMouseX(mouseX, scale);
        int localMouseY = killTrackerSettingsMouseY(mouseY, scale);
        context.pose().pushMatrix();
        try {
            context.pose().translate(offsetX, offsetY);
            context.pose().scale(scale, scale);
            renderKillTrackerSettingsBox(context, localMouseX, localMouseY);
        } finally {
            context.pose().popMatrix();
        }
    }

    private void renderKillTrackerSettingsBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(killTrackerSettingsBoxHeight(), killTrackerSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        context.outline(boxX, boxY, boxWidth, boxHeight, DETAIL_BORDER_COLOR);
        context.text(font, "Kill Tracker", boxX + 10, boxY + 10, 0xFFFFFF00, false);

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        drawSettingsCloseCross(context, closeX, closeY, closeSize, closeHovered ? 0xFFFF6666 : 0xFFFF0000);
        beginDetailContent(context, mouseX, mouseY, boxX, boxY, boxWidth, boxHeight, DetailKeybindCatalog.Panel.KILLS);
        mouseY += detailScroll;

        int buttonWidth = boxWidth - 20;
        int button1Y = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.KILLS);
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button1Y, buttonWidth,
                RateDisplayUnit.orDefault(config.killsUtilityRateUnit).settingsSwitcherLabel(), true);
        int button2Y = button1Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button2Y, buttonWidth,
                "Hintergrund: " + (config.killsUtilityShowBackground ? "An" : "Aus"), true);
        int button3Y = button2Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button3Y, buttonWidth,
                "Benötigte Kills: " + (config.killsUtilityShowRequiredKills ? "An" : "Aus"), true);
        int button4Y = button3Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button4Y, buttonWidth,
                "Nächste Ebene: " + (config.killsUtilityShowNextLevel ? "An" : "Aus"), true);
        int button5Y = button4Y + CLIPBOARD_BUTTON_SPACING;
        drawDetailButton(context, mouseX, mouseY, boxX + 10, button5Y, buttonWidth, "Farben", true);
        endDetailContent(context);
    }

    private boolean handleKillTrackerSettingsClick(double mouseX, double mouseY, int button) {
        if (!killTrackerSettingsOpen || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        float scale = killTrackerSettingsScale();
        mouseX = killTrackerSettingsMouseX(mouseX, scale);
        mouseY = killTrackerSettingsMouseY(mouseY, scale);
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(killTrackerSettingsBoxHeight(), killTrackerSettingsScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            killTrackerSettingsOpen = false;
            resetDetailScroll();
            return false;
        }

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        if (mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize) {
            killTrackerSettingsOpen = false;
            resetDetailScroll();
            return true;
        }
        if (mouseY < boxY + 36) {
            return true;
        }
        mouseY += detailScroll;

        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        int buttonWidth = boxWidth - 20;
        int button1Y = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.KILLS);
        if (handleKeybindClick(mouseX, mouseY, boxX, boxY, buttonWidth, DetailKeybindCatalog.Panel.KILLS)) {
            return true;
        }
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button1Y && mouseY <= button1Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.killsUtilityRateUnit =
                    RateDisplayUnit.orDefault(config.killsUtilityRateUnit).toggled();
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button2Y = button1Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button2Y && mouseY <= button2Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.killsUtilityShowBackground = !config.killsUtilityShowBackground;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button3Y = button2Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button3Y && mouseY <= button3Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.killsUtilityShowRequiredKills = !config.killsUtilityShowRequiredKills;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button4Y = button3Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button4Y && mouseY <= button4Y + CLIPBOARD_BUTTON_HEIGHT) {
            config.killsUtilityShowNextLevel = !config.killsUtilityShowNextLevel;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        int button5Y = button4Y + CLIPBOARD_BUTTON_SPACING;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                && mouseY >= button5Y && mouseY <= button5Y + CLIPBOARD_BUTTON_HEIGHT) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.gui.setScreen(KillTrackerColors.createScreen(this));
            }
            return true;
        }
        return true;
    }

    private int trackerDetailBoxHeight() {
        int count = Math.max(1, TrackerDetailSettings.buttons(openTrackerDetail, this).size());
        DetailKeybindCatalog.Panel panel = DetailKeybindBindings.panelFor(openTrackerDetail);
        return 36 + DetailKeybindCatalog.introducedHeight(panel)
                + CLIPBOARD_BUTTON_SPACING * (count - 1) + CLIPBOARD_BUTTON_HEIGHT + 12;
    }

    private float trackerDetailScale() {
        return DetailSettingsFrame.scale(width, height, CLIPBOARD_SETTINGS_WIDTH, trackerDetailBoxHeight());
    }

    private int trackerDetailMouseX(double mouseX, float scale) {
        return (int) Math.floor((mouseX - width / 2.0) / scale + width / 2.0);
    }

    private int trackerDetailMouseY(double mouseY, float scale) {
        return (int) Math.floor((mouseY - height / 2.0) / scale + height / 2.0);
    }

    private void renderTrackerDetail(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        float scale = trackerDetailScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = height / 2.0f * (1.0f - scale);
        int localMouseX = trackerDetailMouseX(mouseX, scale);
        int localMouseY = trackerDetailMouseY(mouseY, scale);
        context.pose().pushMatrix();
        try {
            context.pose().translate(offsetX, offsetY);
            context.pose().scale(scale, scale);
            renderTrackerDetailBox(context, localMouseX, localMouseY);
        } finally {
            context.pose().popMatrix();
        }
    }

    private void renderTrackerDetailBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<TrackerDetailSettings.Button> buttons = TrackerDetailSettings.buttons(openTrackerDetail, this);
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(trackerDetailBoxHeight(), trackerDetailScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        context.outline(boxX, boxY, boxWidth, boxHeight, DETAIL_BORDER_COLOR);
        context.text(font, TrackerDetailSettings.title(openTrackerDetail), boxX + 10, boxY + 10, 0xFFFFFF00, false);

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        drawSettingsCloseCross(context, closeX, closeY, closeSize, closeHovered ? 0xFFFF6666 : 0xFFFF0000);
        beginDetailContent(context, mouseX, mouseY, boxX, boxY, boxWidth, boxHeight,
                DetailKeybindBindings.panelFor(openTrackerDetail));
        mouseY += detailScroll;

        int buttonWidth = boxWidth - 20;
        int buttonY = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindBindings.panelFor(openTrackerDetail));
        for (TrackerDetailSettings.Button detailButton : buttons) {
            drawDetailButton(context, mouseX, mouseY, boxX + 10, buttonY, buttonWidth,
                    detailButton.label(), detailButton.enabled());
            buttonY += CLIPBOARD_BUTTON_SPACING;
        }
        endDetailContent(context);
    }

    private boolean handleTrackerDetailClick(double mouseX, double mouseY, int button) {
        boolean leftClick = button == InputConstants.MOUSE_BUTTON_LEFT;
        boolean rightClick = button == InputConstants.MOUSE_BUTTON_RIGHT;
        if (openTrackerDetail == null || (!leftClick && !rightClick)) {
            return false;
        }
        float scale = trackerDetailScale();
        mouseX = trackerDetailMouseX(mouseX, scale);
        mouseY = trackerDetailMouseY(mouseY, scale);
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = fittedDetailHeight(trackerDetailBoxHeight(), trackerDetailScale());
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            if (leftClick) {
                openTrackerDetail = null;
                resetDetailScroll();
            }
            return false;
        }

        int closeSize = 12;
        int closeX = boxX + boxWidth - closeSize - 5;
        int closeY = boxY + 5;
        if (leftClick && mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize) {
            openTrackerDetail = null;
            resetDetailScroll();
            return true;
        }
        if (mouseY < boxY + 36) {
            return true;
        }
        mouseY += detailScroll;

        List<TrackerDetailSettings.Button> buttons = TrackerDetailSettings.buttons(openTrackerDetail, this);
        int buttonWidth = boxWidth - 20;
        int buttonY = boxY + 36 + DetailKeybindCatalog.introducedHeight(DetailKeybindBindings.panelFor(openTrackerDetail));
        if (handleKeybindClick(mouseX, mouseY, boxX, boxY, buttonWidth, DetailKeybindBindings.panelFor(openTrackerDetail))) {
            return true;
        }
        for (TrackerDetailSettings.Button detailButton : buttons) {
            if (detailButton.enabled()
                    && mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                    && mouseY >= buttonY && mouseY <= buttonY + CLIPBOARD_BUTTON_HEIGHT) {
                if (leftClick) {
                    detailButton.onClick().run();
                } else if (detailButton.onRightClick() != null) {
                    detailButton.onRightClick().run();
                }
                return true;
            }
            buttonY += CLIPBOARD_BUTTON_SPACING;
        }
        return true;
    }

    private void resetDetailScroll() {
        detailScroll = 0;
        DetailKeybindActions.abort();
    }

    private boolean detailPanelOpen() {
        return clipboardSettingsOpen || materialTrackerSettingsOpen || killTrackerSettingsOpen || openTrackerDetail != null;
    }

    private int detailContentHeight() {
        if (clipboardSettingsOpen) {
            return clipboardSettingsBoxHeight();
        }
        if (materialTrackerSettingsOpen) {
            return materialTrackerSettingsBoxHeight();
        }
        if (killTrackerSettingsOpen) {
            return killTrackerSettingsBoxHeight();
        }
        if (openTrackerDetail != null) {
            return trackerDetailBoxHeight();
        }
        return 0;
    }

    private float detailScale() {
        return DetailSettingsFrame.scale(width, height, CLIPBOARD_SETTINGS_WIDTH, Math.max(1, detailContentHeight()));
    }

    private int detailVisibleHeight() {
        return DetailSettingsFrame.visibleHeight(height, detailScale(), Math.max(1, detailContentHeight()));
    }

    private int fittedDetailHeight(int contentHeight, float scale) {
        int visible = DetailSettingsFrame.visibleHeight(height, scale, contentHeight);
        detailScroll = DetailSettingsFrame.clampScroll(detailScroll, contentHeight, visible);
        return visible;
    }

    private boolean detailScrollHit(double mouseX, double mouseY) {
        float scale = detailScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = height / 2.0f * (1.0f - scale);
        double localX = (mouseX - offsetX) / scale;
        double localY = (mouseY - offsetY) / scale;
        int boxWidth = CLIPBOARD_SETTINGS_WIDTH;
        int boxHeight = detailVisibleHeight();
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        return localX >= boxX && localX <= boxX + boxWidth && localY >= boxY && localY <= boxY + boxHeight;
    }

    private void beginDetailContent(GuiGraphicsExtractor context, int mouseX, int mouseY, int boxX, int boxY,
            int boxWidth, int boxHeight, DetailKeybindCatalog.Panel panel) {
        int top = boxY + 30;
        int bottom = boxY + boxHeight - 4;
        context.enableScissor(boxX + 1, top, boxX + boxWidth - 1, Math.max(top + 1, bottom));
        context.pose().pushMatrix();
        context.pose().translate(0.0f, -detailScroll);
        drawSectionHeadersAndKeybinds(context, mouseX, mouseY + detailScroll, boxX, boxY, panel, boxWidth - 20);
    }

    private void endDetailContent(GuiGraphicsExtractor context) {
        context.pose().popMatrix();
        context.disableScissor();
    }

    private void drawSectionHeadersAndKeybinds(GuiGraphicsExtractor context, int mouseX, int mouseY, int boxX, int boxY,
            DetailKeybindCatalog.Panel panel, int buttonWidth) {
        int y = boxY + 36;
        if (DetailKeybindCatalog.hasKeybinds(panel)) {
            context.text(font, "Keybinds", boxX + 10, y, 0xFFFFFF00, false);
            y += 16;
            for (KeyMapping mapping : DetailKeybindBindings.mappings(panel)) {
                drawDetailButton(context, mouseX, mouseY, boxX + 10, y, buttonWidth,
                        DetailKeybindActions.rowLabel(mapping), true);
                y += CLIPBOARD_BUTTON_SPACING;
            }
        }
        context.text(font, "Haupt Einstellungen", boxX + 10, y, 0xFFFFFF00, false);
    }

    private boolean handleKeybindClick(double mouseX, double mouseY, int boxX, int boxY, int buttonWidth,
            DetailKeybindCatalog.Panel panel) {
        if (!DetailKeybindCatalog.hasKeybinds(panel)) {
            return false;
        }
        int y = boxY + 36 + 16;
        for (KeyMapping mapping : DetailKeybindBindings.mappings(panel)) {
            if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth
                    && mouseY >= y && mouseY <= y + CLIPBOARD_BUTTON_HEIGHT) {
                DetailKeybindActions.begin(mapping);
                return true;
            }
            y += CLIPBOARD_BUTTON_SPACING;
        }
        return false;
    }
}


