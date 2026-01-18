package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.ZeichenUtility;
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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
    
    // Clipboard settings overlay
    private boolean clipboardSettingsOpen = false;
    
    public OverlayEditorScreen() {
        super(Text.literal("Overlay Editor"));
        // Store the current screen before opening the overlay editor
        this.previousScreen = MinecraftClient.getInstance().currentScreen;
        initializeOverlays();
    }
    
    public void refreshOverlays() {
        // Entferne alle TabInfo-Overlays aus der Liste
        overlays.removeIf(overlay -> overlay instanceof TabInfoMainDraggableOverlay || overlay instanceof TabInfoSeparateDraggableOverlay);
        
        // Prüfe ob wir in einem Inventar oder im Chat sind
        boolean isInAnyInventoryRefresh = isInAnyInventoryScreen();
        boolean isInChatScreenRefresh = isInChatScreen();
        
        // Füge TabInfo-Overlays wieder hinzu (basierend auf aktuellen Config-Werten)
        // Nur außerhalb von Inventaren und außerhalb des Chats anzeigen
        if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled && !isInAnyInventoryRefresh && !isInChatScreenRefresh) {
            // Haupt-Overlay - immer hinzufügen wenn Tab Info Utility aktiviert ist (wird nur gerendert wenn enabled)
            overlays.add(new TabInfoMainDraggableOverlay());
            
            // Separate Overlays - immer hinzufügen (werden nur gerendert wenn enabled)
            overlays.add(new TabInfoSeparateDraggableOverlay("forschung", "forschung", "Forschung"));
            overlays.add(new TabInfoSeparateDraggableOverlay("amboss", "amboss", "Amboss"));
            overlays.add(new TabInfoSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("jaeger", "jaeger", "Jäger"));
            overlays.add(new TabInfoSeparateDraggableOverlay("seelen", "seelen", "Seelen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle"));
            overlays.add(new TabInfoSeparateDraggableOverlay("recycler", "recycler", "Recycler"));
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3"));
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3"));
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
            // In blueprint inventory screens, show overlays that are relevant
            // AspectOverlay is only shown in blueprint inventories
            overlays.add(new AspectOverlayDraggableOverlay());
            
            // Hide Uncraftable and Hide Wrong Class buttons are only in blueprint inventories
            overlays.add(new HideUncraftableButtonDraggableOverlay());
            overlays.add(new HideWrongClassButtonDraggableOverlay());
            
            // Check if we're in a Kit Filter relevant inventory
            if (isInKitFilterInventory()) {
                overlays.add(new KitFilterButton1DraggableOverlay());
                overlays.add(new KitFilterButton2DraggableOverlay());
                overlays.add(new KitFilterButton3DraggableOverlay());
            }
        }
        
        // Star Aspect Overlay - available ONLY in inventories EXCEPT blueprint inventories and "Machtkristalle Verbessern" inventory
        // This is for items with "⭐" in tooltip, different from the blueprint Aspect Overlay
        if (isInAnyInventory && !isInBlueprintInventory && !isInMKLevelInventory()) {
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
                
                // Collection overlay - not available in player name dimension or in any inventory
                // The actual overlay will only show in-game when not in player name dimension or floor dimension
                // Only show if biome is detected in scoreboard
                if (!isInPlayerNameDimension() && !isInventoryOpen() && InformationenUtility.isBiomDetected()) {
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
        
        // Tab Info Overlays - nur außerhalb von Inventaren und außerhalb des Chats anzeigen
        if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled && !isInAnyInventory && !isInChatScreen) {
            // Haupt-Overlay - immer hinzufügen wenn Tab Info Utility aktiviert ist (wird nur gerendert wenn enabled)
            TabInfoMainDraggableOverlay mainOverlay = new TabInfoMainDraggableOverlay();
            overlays.add(mainOverlay);
            
            // Separate Overlays - immer hinzufügen (werden nur gerendert wenn enabled)
            overlays.add(new TabInfoSeparateDraggableOverlay("forschung", "forschung", "Forschung"));
            overlays.add(new TabInfoSeparateDraggableOverlay("amboss", "amboss", "Amboss"));
            overlays.add(new TabInfoSeparateDraggableOverlay("schmelzofen", "schmelzofen", "Schmelzofen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("jaeger", "jaeger", "Jäger"));
            overlays.add(new TabInfoSeparateDraggableOverlay("seelen", "seelen", "Seelen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("essenzen", "essenzen", "Essenzen"));
            overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalle", "machtkristalle", "Machtkristalle"));
            overlays.add(new TabInfoSeparateDraggableOverlay("recycler", "recycler", "Recycler"));
            
            // Einzelne MK-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot1", "machtkristalleSlot1", "MK Slot 1"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot2", "machtkristalleSlot2", "MK Slot 2"));
                }
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3 && 
                    CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate) {
                    overlays.add(new TabInfoSeparateDraggableOverlay("machtkristalleSlot3", "machtkristalleSlot3", "MK Slot 3"));
                }
            }
            
            // Einzelne Recycler-Slot Overlays (nur wenn "Separates Overlay", "Einzeln" aktiviert sind UND Slot aktiviert ist)
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot1", "recyclerSlot1", "Recycler Slot 1"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot2", "recyclerSlot2", "Recycler Slot 2"));
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay && 
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate) {
                overlays.add(new TabInfoSeparateDraggableOverlay("recyclerSlot3", "recyclerSlot3", "Recycler Slot 3"));
            }
        }
        
        // Clipboard is available everywhere
        overlays.add(new ClipboardDraggableOverlay());
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
        
        // Check if the title contains "Machtkristalle Verbessern" or the Glyph "㮌" (Essence Harvester UI)
        return title.contains("Machtkristalle Verbessern") || 
               ZeichenUtility.containsEssenceHarvesterUi(title);
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
        
        // Buttons horizontal zentrieren
        // 4 Buttons à 80 Pixel = 320 Pixel, 3 Abstände à 10 Pixel = 30 Pixel, Gesamt = 350 Pixel
        int buttonWidth = 80;
        int buttonSpacing = 10;
        int totalButtonsWidth = 4 * buttonWidth + 3 * buttonSpacing; // 350 Pixel
        int startX = width / 2 - totalButtonsWidth / 2; // width / 2 - 175
        
        // Done Button
        doneButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> close()
        ).dimensions(startX, height - 30, buttonWidth, 20).build();
        addDrawableChild(doneButton);
        
        // Tab Info Button
        ButtonWidget tabInfoButton = ButtonWidget.builder(
            Text.literal("Tab Info"),
            button -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.setScreen(new net.felix.utilities.Overall.TabInfo.TabInfoSettingsScreen(this));
                }
            }
        ).dimensions(startX + buttonWidth + buttonSpacing, height - 30, buttonWidth, 20).build();
        addDrawableChild(tabInfoButton);
        
        // Overlay Button
        overlayButton = ButtonWidget.builder(
            Text.literal("Overlay"),
            button -> overlaySettingsOpen = !overlaySettingsOpen
        ).dimensions(startX + 2 * (buttonWidth + buttonSpacing), height - 30, buttonWidth, 20).build();
        addDrawableChild(overlayButton);
        
        // Reset Button
        resetButton = ButtonWidget.builder(
            Text.literal("Reset All"),
            button -> resetAllOverlays()
        ).dimensions(startX + 3 * (buttonWidth + buttonSpacing), height - 30, buttonWidth, 20).build();
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
        
        // Render all overlays in edit mode
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
        
        // Render clipboard settings overlay if open
        if (clipboardSettingsOpen) {
            renderClipboardSettings(context, mouseX, mouseY);
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
        // Handle clipboard settings click
        if (handleClipboardSettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle overlay settings click
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
                    
                    // Prüfe, ob es das MKLevel-Overlay ist (sollte immer von oben links vergrößern)
                    boolean isMKLevelOverlay = resizingOverlay instanceof MKLevelDraggableOverlay;
                    
                    int newX;
                    int newY = currentY;
                    
                    if (isMKLevelOverlay) {
                        // Für MKLevel: Obere linke Ecke immer fixiert
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
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                overlay.resetToDefault();
                clampOverlayToScreen(overlay);
                overlay.savePosition();
            }
        }
    }

    private void clampOverlayToScreen(DraggableOverlay overlay) {
        int maxX = Math.max(0, width - overlay.getWidth());
        int maxY = Math.max(0, height - overlay.getHeight());
        int clampedX = Math.max(0, Math.min(overlay.getX(), maxX));
        int clampedY = Math.max(0, Math.min(overlay.getY(), maxY));
        overlay.setPosition(clampedX, clampedY);
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
        int checkboxX = boxX + 10;
        
        for (OverlayEntry entry : displayOverlays) {
            boolean isEnabled = entry.isEnabled;
            
            // Checkbox-Hintergrund
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
            int textX = checkboxX + checkboxSize + 5;
            int textWidth = textRenderer.getWidth(overlayName);
            int textHeight = textRenderer.fontHeight;
            context.drawText(textRenderer, overlayName, textX, checkboxY + 1, isEnabled ? 0xFFFFFFFF : 0xFF808080, false);
            
            // Settings-Button für Clipboard (rechts in der Zeile, wie TabInfo)
            if (entry.overlay instanceof ClipboardDraggableOverlay) {
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
                context.drawBorder(gearX - 1, gearY - 1, gearSize + 2, gearSize + 2, 0xFFFFFFFF);
                
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
        
        // Prüfe ob Klick auf eine Checkbox, den Namen oder Settings-Button ist
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        int textX = checkboxX + checkboxSize + 5;
        
        for (OverlayEntry entry : displayOverlays) {
            int checkboxY = y;
            int textY = checkboxY;
            int textHeight = textRenderer.fontHeight;
            int textWidth = textRenderer.getWidth(entry.displayName);
            
            // Prüfe ob Klick auf Checkbox
            boolean clickedOnCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                         mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize);
            
            // Prüfe ob Klick auf Text (Overlay-Name)
            boolean clickedOnText = (mouseX >= textX && mouseX <= textX + textWidth &&
                                     mouseY >= textY && mouseY <= textY + textHeight);
            
            // Prüfe ob Klick auf Settings-Button für Clipboard
            boolean clickedOnSettings = false;
            if (entry.overlay instanceof ClipboardDraggableOverlay) {
                int gearSize = 12;
                int gearX = boxX + boxWidth - gearSize - 10;
                int gearY = checkboxY - 1;
                clickedOnSettings = (mouseX >= gearX - 1 && mouseX <= gearX + gearSize + 1 &&
                                     mouseY >= gearY - 1 && mouseY <= gearY + gearSize + 1);
            }
            
            if (clickedOnSettings) {
                // Öffne Clipboard-Settings
                clipboardSettingsOpen = !clipboardSettingsOpen;
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
            boolean newValue = !config.showBossHP;
            config.showBossHP = newValue;
            config.bossHPEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof CardsDraggableOverlay) {
            boolean newValue = !config.showCard;
            config.showCard = newValue;
            config.cardEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof StatuesDraggableOverlay) {
            boolean newValue = !config.showStatue;
            config.showStatue = newValue;
            config.statueEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof BlueprintViewerDraggableOverlay) {
            // Nur showBlueprintViewer togglen (gleichgestellt mit "Blueprint Tracker anzeigen" in Config)
            // blueprintViewerEnabled bleibt unabhängig (wird nicht synchronisiert)
            config.showBlueprintViewer = !config.showBlueprintViewer;
        } else if (overlay instanceof MaterialTrackerDraggableOverlay) {
            boolean newValue = !config.materialTrackerEnabled;
            config.materialTrackerEnabled = newValue;
            config.showMaterialTracker = newValue; // Synchronisiere mit *Enabled Option
        } else if (overlay instanceof KillsUtilityDraggableOverlay) {
            boolean newValue = !config.killsUtilityEnabled;
            config.killsUtilityEnabled = newValue;
            config.showKillsUtility = newValue; // Synchronisiere mit *Enabled Option
        } else if (overlay instanceof EquipmentDisplayDraggableOverlay) {
            boolean newValue = !config.showEquipmentDisplay;
            config.showEquipmentDisplay = newValue;
            config.equipmentDisplayEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof MiningLumberjackDraggableOverlay) {
            boolean newValue = !config.miningLumberjackOverlayEnabled;
            config.miningLumberjackOverlayEnabled = newValue;
            // Mining/Lumberjack hat separate show* Optionen, aber keine gemeinsame show* Option
            // Beide show* Optionen synchronisieren
            config.showMiningOverlay = newValue;
            config.showLumberjackOverlay = newValue;
        } else if (overlay instanceof CollectionDraggableOverlay) {
            boolean newValue = !config.showCollectionOverlay;
            config.showCollectionOverlay = newValue;
            // Collection hat keine separate *Enabled Option, nur show* Option
        } else if (overlay instanceof AspectOverlayDraggableOverlay) {
            boolean newValue = !config.showAspectOverlay;
            config.showAspectOverlay = newValue;
            config.aspectOverlayEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof StarAspectOverlayDraggableOverlay) {
            boolean newValue = !config.showAspectOverlay;
            config.showAspectOverlay = newValue;
            config.aspectOverlayEnabled = newValue; // Synchronisiere mit show* Option
        } else if (overlay instanceof ChatAspectOverlayDraggableOverlay) {
            boolean newValue = !config.chatAspectOverlayEnabled;
            config.chatAspectOverlayEnabled = newValue;
            // ChatAspectOverlay hat keine separate show* Option, nur *Enabled Option
        } else if (overlay instanceof HideUncraftableButtonDraggableOverlay) {
            boolean newValue = !config.hideUncraftableEnabled;
            config.hideUncraftableEnabled = newValue;
            // HideUncraftableButton hat keine separate show* Option, nur *Enabled Option
        } else if (overlay instanceof HideWrongClassButtonDraggableOverlay) {
            boolean newValue = !config.hideWrongClassEnabled;
            config.hideWrongClassEnabled = newValue;
            config.showHideWrongClassButton = newValue; // Synchronisiere mit *Enabled Option
        } else if (overlay instanceof KitFilterButton1DraggableOverlay || 
                   overlay instanceof KitFilterButton2DraggableOverlay || 
                   overlay instanceof KitFilterButton3DraggableOverlay) {
            boolean newValue = !config.kitFilterButtonsEnabled;
            config.kitFilterButtonsEnabled = newValue;
            // Kit Filter Buttons haben keine separate show* Option, nur *Enabled Option
        } else if (overlay instanceof MKLevelDraggableOverlay) {
            boolean newValue = !config.mkLevelEnabled;
            config.mkLevelEnabled = newValue;
            // MKLevel hat keine separate show* Option, nur *Enabled Option
        } else if (overlay instanceof ClipboardDraggableOverlay) {
            boolean newValue = !config.clipboardEnabled;
            config.clipboardEnabled = newValue;
            config.showClipboard = newValue; // Synchronisiere mit *Enabled Option
        }
    }
    
    /**
     * Zeichnet das Zahnrad-Icon (Settings-Icon) wie in TabInfoSettingsScreen
     */
    private void drawGearIcon(DrawContext context, int x, int y, int size) {
        try {
            // Zeichne das Settings-Icon aus der Textur
            Identifier settingsIcon = Identifier.of("cclive-utilities", "textures/alert_icons/alert_icons_settings.png");
            context.drawTexture(
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
            context.drawBorder(x, y, size, size, 0xFFFFFFFF);
            
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
    
    /**
     * Rendert das Clipboard-Settings-Overlay
     */
    private void renderClipboardSettings(DrawContext context, int mouseX, int mouseY) {
        int boxWidth = 300;
        int boxHeight = 340; // Angepasst für neue Buttons (Fertige Kosten)
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, "Clipboard Settings", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Buttons
        int buttonY = boxY + 40;
        int buttonHeight = 20;
        int buttonSpacing = 30;
        int buttonWidth = boxWidth - 20;
        
        // Button 1: Blueprint Shop Kosten anzeigen/ausblenden
        boolean showBPCosts = CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts;
        int button1Y = buttonY;
        boolean button1Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button1Y && mouseY <= button1Y + buttonHeight;
        int button1Color = button1Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button1Y, boxX + 10 + buttonWidth, button1Y + buttonHeight, button1Color);
        context.drawBorder(boxX + 10, button1Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button1Text = "Blueprint Shop Kosten: " + (showBPCosts ? "An" : "Aus");
        context.drawText(textRenderer, button1Text, boxX + 15, button1Y + 6, 0xFFFFFFFF, false);
        
        // Überschrift: Material Sortierung
        int headerY = buttonY + buttonSpacing + 5;
        context.drawText(textRenderer, "Material Sortierung", boxX + 10, headerY, 0xFFFFFF00, false);
        
        // Button 2: Material Sortierung An/Aus
        int button2Y = headerY + 20;
        boolean materialSortEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
        boolean button2Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button2Y && mouseY <= button2Y + buttonHeight;
        int button2Color = button2Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button2Y, boxX + 10 + buttonWidth, button2Y + buttonHeight, button2Color);
        context.drawBorder(boxX + 10, button2Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button2Text = "Material Sortierung: " + (materialSortEnabled ? "An" : "Aus");
        context.drawText(textRenderer, button2Text, boxX + 15, button2Y + 6, 0xFFFFFFFF, false);
        
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
        context.drawBorder(boxX + 10, button3Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button3Text = "Sortierung: " + (materialSortAscending ? "Aufsteigend (1-100)" : "Absteigend (100-1)");
        int button3TextColor = materialSortEnabled ? 0xFFFFFFFF : 0xFF808080; // Grau wenn deaktiviert
        context.drawText(textRenderer, button3Text, boxX + 15, button3Y + 6, button3TextColor, false);
        
        // Überschrift: Fertige Kosten
        int costHeaderY = button3Y + buttonSpacing + 5;
        context.drawText(textRenderer, "Fertige Kosten", boxX + 10, costHeaderY, 0xFFFFFF00, false);
        
        // Button 4: Fertige Kosten Sortierung An/Aus
        int button4Y = costHeaderY + 20;
        boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
        boolean button4Hovered = mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
                                 mouseY >= button4Y && mouseY <= button4Y + buttonHeight;
        int button4Color = button4Hovered ? 0xFF404040 : 0xFF202020;
        context.fill(boxX + 10, button4Y, boxX + 10 + buttonWidth, button4Y + buttonHeight, button4Color);
        context.drawBorder(boxX + 10, button4Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button4Text = "Fertige Kosten Sortierung: " + (costDisplayEnabled ? "An" : "Aus");
        context.drawText(textRenderer, button4Text, boxX + 15, button4Y + 6, 0xFFFFFFFF, false);
        
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
        context.drawBorder(boxX + 10, button5Y, buttonWidth, buttonHeight, 0xFFFFFFFF);
        String button5Text = costDisplayMode == 1 ? "Ausblenden" : "Ans Ende setzen";
        int button5TextColor = costDisplayEnabled ? 0xFFFFFFFF : 0xFF808080; // Grau wenn deaktiviert
        context.drawText(textRenderer, button5Text, boxX + 15, button5Y + 6, button5TextColor, false);
    }
    
    /**
     * Behandelt Klicks im Clipboard-Settings-Overlay
     */
    private boolean handleClipboardSettingsClick(double mouseX, double mouseY, int button) {
        if (!clipboardSettingsOpen || button != 0) {
            return false;
        }
        
        int boxWidth = 300;
        int boxHeight = 340; // Angepasst für neue Buttons (Fertige Kosten)
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            // Klick außerhalb - schließe Overlay
            clipboardSettingsOpen = false;
            return false;
        }
        
        // Button-Koordinaten
        int buttonY = boxY + 40;
        int buttonHeight = 20;
        int buttonSpacing = 30;
        int buttonWidth = boxWidth - 20;
        
        // Button 1: Blueprint Shop Kosten
        int button1Y = buttonY;
        if (mouseX >= boxX + 10 && mouseX <= boxX + 10 + buttonWidth &&
            mouseY >= button1Y && mouseY <= button1Y + buttonHeight) {
            // Toggle Blueprint Shop Kosten
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts = 
                !CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts;
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Button 2: Material Sortierung An/Aus
        int headerY = buttonY + buttonSpacing + 5;
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
}


