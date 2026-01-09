package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.Aincraft.BlueprintViewerDraggableOverlay;
import net.felix.utilities.DragOverlay.Aincraft.CardsDraggableOverlay;
import net.felix.utilities.DragOverlay.Aincraft.ChatAspectOverlayDraggableOverlay;
import net.felix.utilities.DragOverlay.Aincraft.KillsUtilityDraggableOverlay;
import net.felix.utilities.DragOverlay.Aincraft.MaterialTrackerDraggableOverlay;
import net.felix.utilities.DragOverlay.Aincraft.StatuesDraggableOverlay;
import net.felix.utilities.DragOverlay.Factory.BossHPDraggableOverlay;
import net.felix.utilities.DragOverlay.Factory.MKLevelDraggableOverlay;
import net.felix.utilities.DragOverlay.Farmworld.CollectionDraggableOverlay;
import net.felix.utilities.DragOverlay.Farmworld.MiningLumberjackDraggableOverlay;
import net.felix.utilities.DragOverlay.Schmied.HideUncraftableButtonDraggableOverlay;
import net.felix.utilities.DragOverlay.Schmied.HideWrongClassButtonDraggableOverlay;
import net.felix.utilities.DragOverlay.Schmied.KitFilterButton1DraggableOverlay;
import net.felix.utilities.DragOverlay.Schmied.KitFilterButton2DraggableOverlay;
import net.felix.utilities.DragOverlay.Schmied.KitFilterButton3DraggableOverlay;
import net.felix.utilities.DragOverlay.TabInfo.AspectOverlayDraggableOverlay;
import net.felix.utilities.DragOverlay.TabInfo.StarAspectOverlayDraggableOverlay;
import net.felix.utilities.DragOverlay.TabInfo.TabInfoMainDraggableOverlay;
import net.felix.utilities.DragOverlay.TabInfo.TabInfoSeparateDraggableOverlay;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen für das Drag-and-Drop-Positionieren von Overlays
 */
public class OverlayEditorScreen extends Screen {
    
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
    
    private ButtonWidget doneButton;
    private ButtonWidget resetButton;
    private ButtonWidget overlayButton;
    private TextWidget titleWidget;
    
    // Store the previous screen so we can restore it
    private Screen previousScreen;
    
    // Overlay settings overlay
    private boolean overlaySettingsOpen = false;
    
    public OverlayEditorScreen() {
        super(Text.literal("Overlay Editor"));
        // Store the current screen before opening the overlay editor
        this.previousScreen = MinecraftClient.getInstance().currentScreen;
        initializeOverlays();
    }
    
    /**
     * Initialisiert die Overlays neu (z.B. wenn sich Config-Werte ändern)
     */
    public void refreshOverlays() {
        // Entferne alle TabInfo-Overlays aus der Liste
        overlays.removeIf(overlay -> overlay instanceof TabInfoMainDraggableOverlay || overlay instanceof TabInfoSeparateDraggableOverlay);
        
        // Füge TabInfo-Overlays wieder hinzu (basierend auf aktuellen Config-Werten)
        // Nicht in general_lobby und nicht in Inventaren anzeigen
        boolean isInAnyInventoryRefresh = isInAnyInventoryScreen();
        if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled && !isInGeneralLobby() && !isInAnyInventoryRefresh) {
            // Haupt-Overlay - nur hinzufügen wenn Tab Info Utility aktiviert ist
            TabInfoMainDraggableOverlay mainOverlay = new TabInfoMainDraggableOverlay();
            if (mainOverlay.isEnabled()) {
                overlays.add(mainOverlay);
            }
            
            // Separate Overlays (nur wenn aktiviert)
            TabInfoSeparateDraggableOverlay forschungOverlay = new TabInfoSeparateDraggableOverlay("forschung", "forschung", "Forschung");
            if (forschungOverlay.isEnabled()) {
                overlays.add(forschungOverlay);
            }
            
            TabInfoSeparateDraggableOverlay ambossOverlay = new TabInfoSeparateDraggableOverlay("amboss", "amboss", "Amboss");
            if (ambossOverlay.isEnabled()) {
                overlays.add(ambossOverlay);
            }
            
            TabInfoSeparateDraggableOverlay schmelzofenOverlay = new TabInfoSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen");
            if (schmelzofenOverlay.isEnabled()) {
                overlays.add(schmelzofenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay jaegerOverlay = new TabInfoSeparateDraggableOverlay("jaeger", "jaeger", "Jäger");
            if (jaegerOverlay.isEnabled()) {
                overlays.add(jaegerOverlay);
            }
            
            TabInfoSeparateDraggableOverlay seelenOverlay = new TabInfoSeparateDraggableOverlay("seelen", "seelen", "Seelen");
            if (seelenOverlay.isEnabled()) {
                overlays.add(seelenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay essenzenOverlay = new TabInfoSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen");
            if (essenzenOverlay.isEnabled()) {
                overlays.add(essenzenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay machtkristalleOverlay = new TabInfoSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle");
            if (machtkristalleOverlay.isEnabled()) {
                overlays.add(machtkristalleOverlay);
            }
            
            TabInfoSeparateDraggableOverlay recyclerOverlay = new TabInfoSeparateDraggableOverlay("recycler", "recycler", "Recycler");
            if (recyclerOverlay.isEnabled()) {
                overlays.add(recyclerOverlay);
            }
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay" und "Einzeln" aktiviert sind)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot1Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1");
                    if (mkSlot1Overlay.isEnabled()) {
                        overlays.add(mkSlot1Overlay);
                    }
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot2Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2");
                    if (mkSlot2Overlay.isEnabled()) {
                        overlays.add(mkSlot2Overlay);
                    }
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot3Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3");
                    if (mkSlot3Overlay.isEnabled()) {
                        overlays.add(mkSlot3Overlay);
                    }
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay" und "Einzeln" aktiviert sind)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot1Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1");
                if (recyclerSlot1Overlay.isEnabled()) {
                    overlays.add(recyclerSlot1Overlay);
                }
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot2Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2");
                if (recyclerSlot2Overlay.isEnabled()) {
                    overlays.add(recyclerSlot2Overlay);
                }
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot3Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3");
                if (recyclerSlot3Overlay.isEnabled()) {
                    overlays.add(recyclerSlot3Overlay);
                }
            }
        }
    }
    
    private void initializeOverlays() {
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
        
        if (isInChatScreen) {
            // In chat screen, show chat-specific overlays
            overlays.add(new ChatAspectOverlayDraggableOverlay());
        } else if (isInBlueprintInventory) {
            // In blueprint inventory screens, show overlays that are relevant for blueprint inventories
            overlays.add(new AspectOverlayDraggableOverlay());
            overlays.add(new HideUncraftableButtonDraggableOverlay());
            overlays.add(new HideWrongClassButtonDraggableOverlay());
            
            // Check if we're in a Kit Filter relevant inventory
            if (isInKitFilterInventory()) {
                overlays.add(new KitFilterButton1DraggableOverlay());
                overlays.add(new KitFilterButton2DraggableOverlay());
                overlays.add(new KitFilterButton3DraggableOverlay());
            }
        }
        
        // Star Aspect Overlay - available ONLY in inventories EXCEPT blueprint inventories
        // This is for items with "⭐" in tooltip, different from the blueprint Aspect Overlay
        if (isInAnyInventory && !isInBlueprintInventory) {
            overlays.add(new StarAspectOverlayDraggableOverlay());
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
            } else {
                // Check if we're in an equipment chest inventory (where Equipment Display works)
                boolean isInEquipmentChest = isInEquipmentChestInventory();
                
                if (isInEquipmentChest) {
                    // In equipment chest inventories, show equipment display overlay
                    overlays.add(new EquipmentDisplayDraggableOverlay());
                }
                
                // Mining/Lumberjack overlay - not available in player name dimension or in any inventory
                // The actual overlay will only show in-game when not in player name dimension or floor dimension
                // Only show if biome is detected in scoreboard
                if (!isInPlayerNameDimension() && !isInventoryOpen() && InformationenUtility.isBiomDetected()) {
                    overlays.add(new MiningLumberjackDraggableOverlay());
                }
                
                // Collection overlay - only available in Overworld (Farmworld) when not in inventory
                // Only show if biome is detected in scoreboard
                if (isInOverworld() && !isInventoryOpen() && InformationenUtility.isBiomDetected()) {
                    overlays.add(new CollectionDraggableOverlay());
                }
                
                // MKLevel overlay - only available in "Machtkristalle Verbessern" inventory
                if (isInMKLevelInventory()) {
                    overlays.add(new MKLevelDraggableOverlay());
                }
                
                // Note: Aspect Overlay and Hide Uncraftable Button are only available in blueprint inventories
                // Note: Star Aspect Overlay is available in all inventories EXCEPT blueprint inventories
                // Note: Equipment Display is only available in equipment chest inventories (with Unicode characters 㬃, 㬄, 㬅, 㬆)
                // Note: Cards, Statues, BlueprintViewer, Material Tracker, and Kills are only available in floor dimensions
                // Note: MKLevel Overlay is only available in "Machtkristalle Verbessern" inventory
            }
        }
        
        // Tab Info Overlays - immer verfügbar wenn aktiviert (außer in general_lobby und in Inventaren)
        if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled && !isInGeneralLobby() && !isInAnyInventory) {
            // Haupt-Overlay - nur hinzufügen wenn Tab Info Utility aktiviert ist
            TabInfoMainDraggableOverlay mainOverlay = new TabInfoMainDraggableOverlay();
            if (mainOverlay.isEnabled()) {
                overlays.add(mainOverlay);
            }
            
            // Separate Overlays (nur wenn aktiviert)
            TabInfoSeparateDraggableOverlay forschungOverlay = new TabInfoSeparateDraggableOverlay("forschung", "forschung", "Forschung");
            if (forschungOverlay.isEnabled()) {
                overlays.add(forschungOverlay);
            }
            
            TabInfoSeparateDraggableOverlay ambossOverlay = new TabInfoSeparateDraggableOverlay("amboss", "amboss", "Amboss");
            if (ambossOverlay.isEnabled()) {
                overlays.add(ambossOverlay);
            }
            
            TabInfoSeparateDraggableOverlay schmelzofenOverlay = new TabInfoSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen");
            if (schmelzofenOverlay.isEnabled()) {
                overlays.add(schmelzofenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay jaegerOverlay = new TabInfoSeparateDraggableOverlay("jaeger", "jaeger", "Jäger");
            if (jaegerOverlay.isEnabled()) {
                overlays.add(jaegerOverlay);
            }
            
            TabInfoSeparateDraggableOverlay seelenOverlay = new TabInfoSeparateDraggableOverlay("seelen", "seelen", "Seelen");
            if (seelenOverlay.isEnabled()) {
                overlays.add(seelenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay essenzenOverlay = new TabInfoSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen");
            if (essenzenOverlay.isEnabled()) {
                overlays.add(essenzenOverlay);
            }
            
            TabInfoSeparateDraggableOverlay machtkristalleOverlay = new TabInfoSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle");
            if (machtkristalleOverlay.isEnabled()) {
                overlays.add(machtkristalleOverlay);
            }
            
            TabInfoSeparateDraggableOverlay recyclerOverlay = new TabInfoSeparateDraggableOverlay("recycler", "recycler", "Recycler");
            if (recyclerOverlay.isEnabled()) {
                overlays.add(recyclerOverlay);
            }
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay" und "Einzeln" aktiviert sind)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot1Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1");
                    if (mkSlot1Overlay.isEnabled()) {
                        overlays.add(mkSlot1Overlay);
                    }
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot2Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2");
                    if (mkSlot2Overlay.isEnabled()) {
                        overlays.add(mkSlot2Overlay);
                    }
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate) {
                    TabInfoSeparateDraggableOverlay mkSlot3Overlay = new TabInfoSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3");
                    if (mkSlot3Overlay.isEnabled()) {
                        overlays.add(mkSlot3Overlay);
                    }
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay" und "Einzeln" aktiviert sind)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot1Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1");
                if (recyclerSlot1Overlay.isEnabled()) {
                    overlays.add(recyclerSlot1Overlay);
                }
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot2Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2");
                if (recyclerSlot2Overlay.isEnabled()) {
                    overlays.add(recyclerSlot2Overlay);
                }
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate) {
                TabInfoSeparateDraggableOverlay recyclerSlot3Overlay = new TabInfoSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3");
                if (recyclerSlot3Overlay.isEnabled()) {
                    overlays.add(recyclerSlot3Overlay);
                }
            }
        }
    }
    
    /**
     * Check if the player is currently in a chat screen
     */
    private boolean isInChatScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a ChatScreen
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
    }
    
    /**
     * Check if the player is currently in any inventory screen (HandledScreen)
     */
    private boolean isInAnyInventoryScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
    }
    
    /**
     * Check if the player is currently in a blueprint inventory screen where Hide Uncraftable button and Aspect Overlay work
     */
    private boolean isInInventoryScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a blueprint inventory
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains any of the allowed blueprint inventory names
        // (same logic as in HandledScreenMixin.isBlueprintInventory())
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a Kit Filter relevant inventory
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains Kit Filter relevant inventory names
        return cleanTitle.contains("Baupläne [Rüstung]") ||
               cleanTitle.contains("Bauplan [Shop]");
    }
    
    /**
     * Check if the player is currently in an equipment chest inventory where Equipment Display works
     */
    private boolean isInEquipmentChestInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it contains the equipment chest Unicode characters
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Check if the title contains any of the equipment chest Unicode characters
        // (same logic as in EquipmentDisplayUtility)
        return title.contains("㬃") || title.contains("㬄") || title.contains("㬅") || title.contains("㬆");
    }
    
    /**
     * Check if the player is currently in a floor dimension
     */
    private boolean isInFloorDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        // Check if the player is in a floor dimension by checking the dimension identifier
        String dimensionId = client.world.getRegistryKey().getValue().toString();
        
        // Floor dimensions typically have "floor" in their identifier
        return dimensionId.contains("floor") || dimensionId.contains("dungeon");
    }
    
    /**
     * Check if the player is currently in the "Machtkristalle Verbessern" inventory
     */
    private boolean isInMKLevelInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's the MKLevel inventory
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Check if the title contains "Machtkristalle Verbessern"
        return title.contains("Machtkristalle Verbessern");
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Title
        titleWidget = new TextWidget(
            Text.literal("Overlay Editor - Drag & Drop to reposition overlays"),
            textRenderer
        );
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, 20);
        addDrawableChild(titleWidget);
        
        // Done Button
        doneButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> close()
        ).dimensions(width / 2 - 180, height - 30, 80, 20).build();
        addDrawableChild(doneButton);
        
        // Overlay Button
        overlayButton = ButtonWidget.builder(
            Text.literal("Overlay"),
            button -> overlaySettingsOpen = !overlaySettingsOpen
        ).dimensions(width / 2 - 90, height - 30, 80, 20).build();
        addDrawableChild(overlayButton);
        
        // Tab Info Button
        ButtonWidget tabInfoButton = ButtonWidget.builder(
            Text.literal("Tab Info"),
            button -> {
                if (client != null) {
                    client.setScreen(new net.felix.utilities.Overall.TabInfo.TabInfoSettingsScreen(this));
                }
            }
        ).dimensions(width / 2, height - 30, 80, 20).build();
        addDrawableChild(tabInfoButton);
        
        // Reset Button
        resetButton = ButtonWidget.builder(
            Text.literal("Reset All"),
            button -> resetAllOverlays()
        ).dimensions(width / 2 + 90, height - 30, 80, 20).build();
        addDrawableChild(resetButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the previous screen in the background if it exists
        if (previousScreen != null) {
            previousScreen.render(context, mouseX, mouseY, delta);
        }
        
        // Render very transparent background overlay
        context.fill(0, 0, width, height, 0x20000000);
        
        // Render title
        titleWidget.render(context, mouseX, mouseY, delta);
        
        // Render all overlays in edit mode (nur wenn enabled)
        // Entferne disabled TabInfo-Overlays aus der Liste bei jedem Render, um sicherzustellen, dass sie nicht angezeigt werden
        // Dies stellt sicher, dass Overlays sofort verschwinden, wenn sie deaktiviert werden
        overlays.removeIf(overlay -> {
            if (overlay instanceof TabInfoMainDraggableOverlay || overlay instanceof TabInfoSeparateDraggableOverlay) {
                return !overlay.isEnabled();
            }
            return false;
        });
        
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                overlay.renderInEditMode(context, mouseX, mouseY, delta);
                
                // Render resize handle and reset handle if hovering over the overlay
                if (overlay.isHovered(mouseX, mouseY)) {
                    renderResizeHandle(context, overlay);
                    renderResetHandle(context, overlay);
                }
            }
        }
        
        // Render instructions
        renderInstructions(context);
        
        // Render overlay settings overlay if open
        if (overlaySettingsOpen) {
            renderOverlaySettings(context, mouseX, mouseY);
        }
        
        // Render buttons
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderResizeHandle(DrawContext context, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + overlay.getWidth() - handleSize;
        int y = overlay.getY() + overlay.getHeight() - handleSize;
        
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
    
    private void renderResetHandle(DrawContext context, DraggableOverlay overlay) {
        int handleSize = 10;
        int x = overlay.getX() + overlay.getWidth() - handleSize;
        int y = overlay.getY();
        
        // Draw reset handle background (small square with white border)
        context.fill(x, y, x + handleSize, y + handleSize, 0xFFFFFFFF);
        context.fill(x + 1, y + 1, x + handleSize - 1, y + handleSize - 1, 0xFF000000);
        
        // Draw "<-" text (scaled down to fit better, centered)
        float scale = 0.7f;
        String arrowText = "<-";
        int textWidth = textRenderer.getWidth(arrowText);
        int textHeight = textRenderer.fontHeight;
        
        // Calculate centered position (accounting for scale)
        float scaledTextWidth = textWidth * scale;
        float scaledTextHeight = textHeight * scale;
        float textX = x + (handleSize - scaledTextWidth) / 2.0f;
        float textY = y + (handleSize - scaledTextHeight) / 2.0f;
        
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(textX, textY);
        matrices.scale(scale, scale);
        context.drawText(textRenderer, arrowText, 0, 0, 0xFFFFFFFF, false);
        matrices.popMatrix();
    }
    
    private void renderInstructions(DrawContext context) {
        int y = height - 92;
        String[] instructions = {
            "Left Click + Drag: Move overlay",
            "Left Click + Drag (corner): Resize overlay",
            "Left Click + Arrow Keys: Move overlay 1 pixel",
            "ESC: Close editor"
        };
        
        for (String instruction : instructions) {
            context.drawText(textRenderer, instruction, 10, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle overlay settings click first
        if (handleOverlaySettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Check for reset area first (highest priority)
            for (DraggableOverlay overlay : overlays) {
                if (overlay.isEnabled() && overlay.isResetArea((int) mouseX, (int) mouseY)) {
                    overlay.resetSizeToDefault();
                    overlay.savePosition();
                    CCLiveUtilitiesConfig.HANDLER.save();
                    return true;
                }
            }
            
            // Check for resize area
            for (DraggableOverlay overlay : overlays) {
                if (overlay.isEnabled() && overlay.isResizeArea((int) mouseX, (int) mouseY)) {
                    // Check if dragging is allowed for this overlay
                    if (!canDragOverlay(overlay)) {
                        continue;
                    }
                    resizingOverlay = overlay;
                    resizeStartX = (int) mouseX;
                    resizeStartY = (int) mouseY;
                    resizeStartWidth = overlay.getWidth();
                    resizeStartHeight = overlay.getHeight();
                    resizeStartOverlayX = overlay.getX(); // Speichere die ursprüngliche X-Position
                    return true;
                }
            }
            
            // Check for drag area
            for (DraggableOverlay overlay : overlays) {
                if (overlay.isEnabled() && overlay.isHovered((int) mouseX, (int) mouseY)) {
                    // Check if dragging is allowed for this overlay
                    if (!canDragOverlay(overlay)) {
                        continue;
                    }
                    draggingOverlay = overlay;
                    dragOffsetX = (int) mouseX - overlay.getX();
                    dragOffsetY = (int) mouseY - overlay.getY();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                // Check if dragging is still allowed (might have changed dimension or opened inventory)
                if (!canDragOverlay(draggingOverlay)) {
                    draggingOverlay = null;
                    return false;
                }
                
                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;
                
                // Keep overlay within screen bounds
                newX = Math.max(0, Math.min(newX, width - draggingOverlay.getWidth()));
                newY = Math.max(0, Math.min(newY, height - draggingOverlay.getHeight()));
                
                draggingOverlay.setPosition(newX, newY);
                // Auto-save position during dragging to ensure it's always saved
                draggingOverlay.savePosition();
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
                
                int newWidth = Math.max(50, resizeStartWidth + deltaWidth);
                int newHeight = Math.max(20, resizeStartHeight + deltaHeight);
                
                // Setze die neue Größe
                resizingOverlay.setSize(newWidth, newHeight);
                
                // Berechne die neue Position basierend auf der Bildschirmseite
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    int screenWidth = client.getWindow().getScaledWidth();
                    int screenHeight = client.getWindow().getScaledHeight();
                    int currentY = resizingOverlay.getY();
                    
                    // Prüfe, ob es das MKLevel-Overlay oder ein Button-Overlay ist (sollte immer von oben links vergrößern)
                    boolean isMKLevelOverlay = resizingOverlay instanceof MKLevelDraggableOverlay;
                    boolean isButtonOverlay = resizingOverlay instanceof KitFilterButton1DraggableOverlay ||
                                             resizingOverlay instanceof KitFilterButton2DraggableOverlay ||
                                             resizingOverlay instanceof KitFilterButton3DraggableOverlay ||
                                             resizingOverlay instanceof HideUncraftableButtonDraggableOverlay ||
                                             resizingOverlay instanceof HideWrongClassButtonDraggableOverlay;
                    
                    int newX;
                    int newY = currentY;
                    
                    if (isMKLevelOverlay || isButtonOverlay) {
                        // Für MKLevel und Buttons: Obere linke Ecke immer fixiert
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
                
                // Auto-save position during resizing to ensure it's always saved
                resizingOverlay.savePosition();
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                draggingOverlay.savePosition();
                draggingOverlay = null;
                return true;
            }
            
            if (resizingOverlay != null) {
                resizingOverlay.savePosition();
                resizingOverlay = null;
                return true;
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        
        // Also close with the configured overlay editor key (toggle behavior)
        // Use the same key binding as outside inventories
        if (OverlayEditorUtility.getOverlayEditorKeyBinding() != null) {
            if (OverlayEditorUtility.getOverlayEditorKeyBinding().matchesKey(keyCode, -1)) {
                close();
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
            
            // Check if left mouse button is still pressed
            long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
            boolean leftMousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            
            if (leftMousePressed) {
                int currentX = draggingOverlay.getX();
                int currentY = draggingOverlay.getY();
                int newX = currentX;
                int newY = currentY;
                
                // Move overlay by 1 pixel in the direction of the arrow key
                if (keyCode == GLFW.GLFW_KEY_LEFT) {
                    newX = currentX - 1;
                } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                    newX = currentX + 1;
                } else if (keyCode == GLFW.GLFW_KEY_UP) {
                    newY = currentY - 1;
                } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    newY = currentY + 1;
                }
                
                // Keep overlay within screen bounds
                newX = Math.max(0, Math.min(newX, width - draggingOverlay.getWidth()));
                newY = Math.max(0, Math.min(newY, height - draggingOverlay.getHeight()));
                
                // Update position if it changed
                if (newX != currentX || newY != currentY) {
                    draggingOverlay.setPosition(newX, newY);
                    draggingOverlay.savePosition();
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void resetAllOverlays() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        // Reset all overlays to default positions
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                overlay.resetToDefault();
            }
        }
        
        // After resetting, ensure all overlays are within screen bounds
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Adjust all overlay positions to keep them within bounds
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                adjustOverlayPosition(overlay, screenWidth, screenHeight);
            }
        }
    }
    
    /**
     * Adjusts a single overlay position to keep it within screen bounds
     */
    private void adjustOverlayPosition(DraggableOverlay overlay, int screenWidth, int screenHeight) {
        int x = overlay.getX();
        int y = overlay.getY();
        int width = overlay.getWidth();
        int height = overlay.getHeight();
        
        // Adjust X position if overlay is outside screen
        int newX = x;
        if (x < 0) {
            newX = 0;
        } else if (x + width > screenWidth) {
            newX = Math.max(0, screenWidth - width);
        }
        
        // Adjust Y position if overlay is outside screen
        int newY = y;
        if (y < 0) {
            newY = 0;
        } else if (y + height > screenHeight) {
            newY = Math.max(0, screenHeight - height);
        }
        
        // Update position if adjustment is needed
        if (newX != x || newY != y) {
            overlay.setPosition(newX, newY);
        }
    }
    
    @Override
    public void close() {
        // Save all overlay positions
        for (DraggableOverlay overlay : overlays) {
            overlay.savePosition();
        }
        
        // Save configuration
        CCLiveUtilitiesConfig.HANDLER.save();
        
        // Reset the overlay editor open status
        OverlayEditorUtility.setOverlayEditorOpen(false);
        
        // Restore the previous screen instead of closing
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && previousScreen != null) {
            client.setScreen(previousScreen);
        } else {
            super.close();
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    /**
     * Check if the player is currently in a dimension that matches their player name
     */
    private boolean isInPlayerNameDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        String playerName = client.player.getName().getString().toLowerCase();
        String dimensionPath = client.world.getRegistryKey().getValue().getPath();
        
        // Check if dimension matches player name (player is in their own dimension)
        return dimensionPath.equals(playerName);
    }
    
    /**
     * Check if the player is currently in the Overworld dimension
     */
    private boolean isInOverworld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        // Check if the dimension is the Overworld
        return client.world.getRegistryKey() == net.minecraft.world.World.OVERWORLD;
    }
    
    /**
     * Check if the player is currently in the "general_lobby" dimension
     */
    private boolean isInGeneralLobby() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return false;
        }
        
        String dimensionPath = client.world.getRegistryKey().getValue().getPath();
        return dimensionPath.equals("general_lobby");
    }
    
    /**
     * Check if an inventory screen is currently open
     */
    private boolean isInventoryOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is an inventory-like screen (HandledScreen)
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
    }
    
    /**
     * Check if dragging is allowed for the given overlay
     */
    private boolean canDragOverlay(DraggableOverlay overlay) {
        // Overlays cannot be dragged when in an inventory
        if (isInventoryOpen()) {
            return false;
        }
        
        // Mining/Lumberjack overlay can only be dragged in Overworld and not in inventories
        if (overlay instanceof MiningLumberjackDraggableOverlay) {
            return isInOverworld() && !isInventoryOpen();
        }
        // All other overlays can be dragged normally (when not in inventory)
        return true;
    }
    
    /**
     * Rendert das Overlay-Settings-Overlay mit Checkboxen für alle verfügbaren Overlays
     */
    private void renderOverlaySettings(DrawContext context, int mouseX, int mouseY) {
        // Erstelle gefilterte Liste der Overlays (Kit-Filter-Buttons werden gruppiert)
        List<OverlayEntry> displayOverlays = getDisplayOverlays();
        
        int boxWidth = 250;
        int boxHeight = Math.min(400, displayOverlays.size() * 25 + 40);
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, "Overlay Settings", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Checkboxen für alle Overlays
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        
        for (OverlayEntry entry : displayOverlays) {
            boolean isEnabled = entry.isEnabled;
            
            // Checkbox-Hintergrund
            int checkboxX = boxX + 10;
            int checkboxY = y;
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
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
            context.drawText(textRenderer, overlayName, checkboxX + checkboxSize + 5, checkboxY + 1, isEnabled ? 0xFFFFFFFF : 0xFF808080, false);
            
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
            // Überspringe Tab Info Overlays - diese sollen nicht im Overlay Picker erscheinen
            if (overlay instanceof TabInfoMainDraggableOverlay || overlay instanceof TabInfoSeparateDraggableOverlay) {
                continue;
            }
            
            // Prüfe ob es ein Kit-Filter-Button ist
            if (overlay instanceof KitFilterButton1DraggableOverlay ||
                overlay instanceof KitFilterButton2DraggableOverlay ||
                overlay instanceof KitFilterButton3DraggableOverlay) {
                hasKitFilterButtons = true;
                // Kit-Filter ist enabled wenn mindestens einer der Buttons enabled ist
                if (overlay.isEnabled()) {
                    kitFilterEnabled = true;
                }
            } else {
                // Normales Overlay - direkt hinzufügen
                displayList.add(new OverlayEntry(overlay.getOverlayName(), overlay.isEnabled(), overlay, false));
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
        if (!overlaySettingsOpen || button != 0) {
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
        
        // Prüfe ob Klick auf eine Checkbox oder den Namen ist
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        int textX = checkboxX + checkboxSize + 5;
        
        for (OverlayEntry entry : displayOverlays) {
            int checkboxY = y;
            int textY = checkboxY;
            // Verwende die tatsächliche Text-Breite für korrekte Click-Erkennung
            String overlayName = entry.displayName;
            int textWidth = textRenderer.getWidth(overlayName);
            int textHeight = textRenderer.fontHeight;
            
            // Prüfe ob Klick auf Checkbox
            boolean clickedOnCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                         mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize);
            
            // Prüfe ob Klick auf Text (Overlay-Name)
            boolean clickedOnText = (mouseX >= textX && mouseX <= textX + textWidth &&
                                     mouseY >= textY && mouseY <= textY + textHeight);
            
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
     */
    private void toggleOverlayEnabled(DraggableOverlay overlay) {
        if (overlay instanceof BossHPDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showBossHP = !CCLiveUtilitiesConfig.HANDLER.instance().showBossHP;
        } else if (overlay instanceof CardsDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showCard = !CCLiveUtilitiesConfig.HANDLER.instance().showCard;
        } else if (overlay instanceof StatuesDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showStatue = !CCLiveUtilitiesConfig.HANDLER.instance().showStatue;
        } else if (overlay instanceof BlueprintViewerDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled;
        } else if (overlay instanceof MaterialTrackerDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled;
        } else if (overlay instanceof KillsUtilityDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityEnabled;
        } else if (overlay instanceof EquipmentDisplayDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showEquipmentDisplay = !CCLiveUtilitiesConfig.HANDLER.instance().showEquipmentDisplay;
        } else if (overlay instanceof MiningLumberjackDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled;
        } else if (overlay instanceof AspectOverlayDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay = !CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
        } else if (overlay instanceof StarAspectOverlayDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay = !CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
        } else if (overlay instanceof ChatAspectOverlayDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled;
        } else if (overlay instanceof HideUncraftableButtonDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableEnabled;
        } else if (overlay instanceof HideWrongClassButtonDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled;
        } else if (overlay instanceof KitFilterButton1DraggableOverlay || 
                   overlay instanceof KitFilterButton2DraggableOverlay || 
                   overlay instanceof KitFilterButton3DraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled;
        } else if (overlay instanceof MKLevelDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled = !CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled;
        } else if (overlay instanceof CollectionDraggableOverlay) {
            CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay = !CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
        }
    }
}


