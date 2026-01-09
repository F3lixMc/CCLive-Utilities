package net.felix.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.felix.utilities.Overall.ZeichenUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    
    /**
     * Injects at the very end of the render method to ensure our overlays are drawn last
     * This preserves all normal rendering while ensuring our overlays appear above tooltips
     */
    // Debug: Letzter erkannter Screen (um Logs zu reduzieren)
    private static String lastDetectedScreen = "";
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        
        // Überspringe InventoryScreen - wird im ScreenMixin behandelt, um Doppel-Rendering zu vermeiden
        if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
            return;
        }
        
        // DEBUG: Logge Inventar-Erkennung (Kiste) nur wenn sich der Screen ändert
        String currentScreen = screen.getClass().getSimpleName();
        if (!currentScreen.equals(lastDetectedScreen)) {
            System.out.println("[ItemViewer] Inventar erkannt: HandledScreen (Kiste) - " + currentScreen);
            lastDetectedScreen = currentScreen;
        }
        
        // Store mouse position for use in tooltip callbacks
        net.felix.utilities.Overall.InformationenUtility.setLastMousePosition(mouseX, mouseY);
        
        // Update mouse position for Item Viewer
        net.felix.utilities.ItemViewer.ItemViewerUtility.updateMousePosition(mouseX, mouseY);
        
        // Update mouse position for DebugUtility (Item Logger)
        net.felix.utilities.DebugUtility.updateMousePosition(mouseX, mouseY);
        
        // Capture tooltip position for collision detection
        captureTooltipPosition(mouseX, mouseY);
        
        // Render colored frames around smithing states ONLY in smithing-related screens
        if (isSmithingInventory()) {
            renderSmithingFrames(context);
        }
        
        // Render the Hide Uncraftable button ONLY in blueprint inventories
        if (isBlueprintInventory()) {
            renderHideUncraftableButton(context);
            renderHideWrongClassButton(context);
        }
        
        // Render Kit Filter buttons in relevant inventories
        renderKitFilterButtons(context, mouseX, mouseY);
        
        // Show aspect overlay in blueprint inventories OR when hovering over items with "⭐" in tooltip
        // Render AFTER all buttons to ensure it appears on top
        if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
            // Update aspect overlay with current hovered item (works for both blueprint items and items with "⭐")
            updateAspectOverlay(mouseX, mouseY);
            
            // Render aspect overlay if:
            // 1. We're in a blueprint inventory (for blueprint items), OR
            // 2. We're hovering over an item with "⭐" (set up by addAspectNameToTooltip)
            // This allows aspect overlay to work in ALL inventories for items with "⭐"
            boolean shouldRender = isBlueprintInventory() || net.felix.utilities.Overall.Aspekte.AspectOverlay.isCurrentlyHovering();
            
            if (shouldRender) {
                // Render our aspect overlay AFTER everything else (including buttons and tooltips)
                renderAspectOverlay(context);
            }
        }
        
        // Clear tooltip bounds after rendering
        net.felix.utilities.Overall.Aspekte.AspectOverlay.clearTooltipBounds();
        
        // Render MKLevel overlay in "Machtkristalle Verbessern" inventory
        renderMKLevelOverlay(context);
        
        // Render Item Viewer overlay (nach allen anderen Overlays, damit es über dem dunklen Hintergrund liegt)
        renderItemViewer(context, mouseX, mouseY);
        
        // Rendere AspectOverlay NACH dem ItemViewer, damit es über allen Items liegt
        // (wird nur gerendert wenn Shift gedrückt und ItemViewer aktiv ist)
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isVisible()) {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.renderForeground(context);
        }
        
        // Rendere Help-Overlay ganz am Ende, damit es über allem liegt (wie im Blueprint Shop)
        // Wird sowohl hier als auch in InGameHudHelpOverlayMixin gerendert, um sicherzustellen, dass es überall funktioniert
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            net.felix.utilities.ItemViewer.ItemViewerUtility.renderHelpOverlay(context);
        }
    }
    
    /**
     * Rendert den Item Viewer Overlay
     */
    private void renderItemViewer(DrawContext context, int mouseX, int mouseY) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            
            // Rendere Item-Viewer (wird nur gerendert wenn sichtbar)
            net.felix.utilities.ItemViewer.ItemViewerUtility.renderItemViewerInScreen(context, client, screen, mouseX, mouseY);
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders MKLevel overlay if we're in the "Machtkristalle Verbessern" inventory or Essence Harvester UI (Glyph "㮌")
     */
    private void renderMKLevelOverlay(DrawContext context) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            net.minecraft.text.Text titleText = screen.getTitle();
            
            // Prüfe direkt auf dem Text-Objekt (bevor getPlainTextFromText Unicode-Zeichen entfernt)
            String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen
            String titlePlain = net.felix.utilities.Overall.InformationenUtility.getPlainTextFromText(titleText);
            
            // Prüfe sowohl "Machtkristalle Verbessern" als auch das Glyph "㮌" (Essence Harvester UI)
            // Verwende titleWithUnicode für die Glyph-Prüfung, da getPlainTextFromText Unicode entfernt
            if (titlePlain.contains("Machtkristalle Verbessern") || 
                ZeichenUtility.containsEssenceHarvesterUi(titleWithUnicode)) {
                // Get actual inventory dimensions from the screen using shadow fields
                net.felix.utilities.Overall.InformationenUtility.renderMKLevelOverlay(context, net.minecraft.client.MinecraftClient.getInstance(), x, y, backgroundWidth, backgroundHeight);
            }
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Blockiert Tooltips wenn das Hilfe-Overlay offen ist (wie im Bauplan-Shop)
     */
    @Inject(method = "getTooltipFromItem", at = @At("HEAD"), cancellable = true)
    private void blockTooltipsFromItem(net.minecraft.item.ItemStack stack, CallbackInfoReturnable<java.util.List<net.minecraft.text.Text>> cir) {
        // Blockiere Tooltips wenn das Hilfe-Overlay offen ist
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            cir.setReturnValue(java.util.Collections.emptyList());
        }
    }
    
    /**
     * Blockiert das Rendern der Items in InventoryScreen, wenn das Hilfe-Overlay offen ist
     * Injiziert in die drawSlot-Methode, um das Rendern der Items zu blockieren
     */
    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void blockSlotRenderingInInventoryScreen(DrawContext context, Slot slot, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        // Blockiere das Rendern der Items nur für InventoryScreen, wenn das Hilfe-Overlay offen ist
        if (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen && 
            net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            ci.cancel();
        }
    }
    
    
    /**
     * Injects into mouseClicked to handle clicks on the Hide Uncraftable button
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Handle clicks on Help Overlay FIRST (höchste Priorität wenn geöffnet)
        // Dies muss vor allen anderen Klicks geprüft werden
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleHelpOverlayClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
        
        // Handle clicks on Item Viewer buttons
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleMouseClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle clicks on the Hide Uncraftable button ONLY in blueprint inventories
        if (isBlueprintInventory()) {
            if (handleHideUncraftableButtonClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true); // Indicate that we handled the click
            }
            if (handleHideWrongClassButtonClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true); // Indicate that we handled the click
            }
        }
        
        // Handle clicks on MKLevel search bar and scrollbar - pass screen position directly from mixin (@Shadow fields)
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelSearchClick(mouseX, mouseY, button, x, y, backgroundHeight)) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Handles mouse dragging for MKLevel scrollbar
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelScrollbarDrag(mouseX, mouseY, button, x, y, backgroundHeight)) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Handles mouse release to stop scrollbar dragging
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelScrollbarRelease(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
    
    
    /**
     * Renders colored frames around smithing states
     */
    private void renderSmithingFrames(DrawContext context) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Call the SchmiedTrackerUtility to render colored frames
            // This will handle all the logic for determining which slots need frames and what colors to use
            net.felix.utilities.Town.SchmiedTrackerUtility.renderColoredFrames(context, screen, x, y);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders the Hide Uncraftable button
     */
    private void renderHideUncraftableButton(DrawContext context) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Call the SchmiedTrackerUtility to render the Hide Uncraftable button
            net.felix.utilities.Town.SchmiedTrackerUtility.renderHideUncraftableButton(context, screen);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Handles clicks on the Hide Uncraftable button
     */
    private boolean handleHideUncraftableButtonClick(double mouseX, double mouseY, int button) {
        try {
            // Call the SchmiedTrackerUtility to handle the button click
            return net.felix.utilities.Town.SchmiedTrackerUtility.handleButtonClick(mouseX, mouseY, button);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Renders the Hide Wrong Class button
     */
    private void renderHideWrongClassButton(DrawContext context) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Call the SchmiedTrackerUtility to render the Hide Wrong Class button
            net.felix.utilities.Town.SchmiedTrackerUtility.renderHideWrongClassButton(context, screen);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Handles clicks on the Hide Wrong Class button
     */
    private boolean handleHideWrongClassButtonClick(double mouseX, double mouseY, int button) {
        try {
            // Call the SchmiedTrackerUtility to handle the button click
            return net.felix.utilities.Town.SchmiedTrackerUtility.handleWrongClassButtonClick(mouseX, mouseY, button);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Captures tooltip position for collision detection with aspect overlay
     */
    private void captureTooltipPosition(int mouseX, int mouseY) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Find the hovered slot
            Slot hoveredSlot = null;
            for (Slot slot : screen.getScreenHandler().slots) {
                if (slot.x + x <= mouseX && mouseX < slot.x + x + 16 &&
                    slot.y + y <= mouseY && mouseY < slot.y + y + 16) {
                    hoveredSlot = slot;
                    break;
                }
            }
            
            if (hoveredSlot != null && hoveredSlot.hasStack()) {
                ItemStack stack = hoveredSlot.getStack();
                if (stack != null && !stack.isEmpty()) {
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client != null && client.textRenderer != null) {
                        // Get tooltip lines using reflection to access protected method
                        try {
                            java.lang.reflect.Method getTooltipMethod = HandledScreen.class.getDeclaredMethod("getTooltipFromItem", net.minecraft.client.MinecraftClient.class, ItemStack.class);
                            getTooltipMethod.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.text.Text> tooltip = (java.util.List<net.minecraft.text.Text>) getTooltipMethod.invoke(screen, client, stack);
                            
                            if (tooltip != null && !tooltip.isEmpty()) {
                                // Calculate tooltip dimensions
                                int tooltipWidth = 0;
                                for (net.minecraft.text.Text line : tooltip) {
                                    int lineWidth = client.textRenderer.getWidth(line);
                                    tooltipWidth = Math.max(tooltipWidth, lineWidth);
                                }
                                
                                // Add padding (Minecraft uses 3 pixels on each side)
                                tooltipWidth += 6;
                                int tooltipHeight = tooltip.size() * (client.textRenderer.fontHeight + 2) + 4;
                                
                                // Calculate tooltip position (Minecraft positions it near the mouse)
                                // Tooltip is typically positioned to the right and above the mouse cursor
                                int tooltipX = mouseX + 12;
                                int tooltipY = mouseY - 12;
                                
                                // Adjust if tooltip would go off screen (Minecraft does this automatically)
                                int screenWidth = client.getWindow().getScaledWidth();
                                int screenHeight = client.getWindow().getScaledHeight();
                                
                                if (tooltipX + tooltipWidth > screenWidth) {
                                    tooltipX = mouseX - tooltipWidth - 12;
                                }
                                if (tooltipY + tooltipHeight > screenHeight) {
                                    tooltipY = screenHeight - tooltipHeight - 3;
                                }
                                if (tooltipY < 3) {
                                    tooltipY = 3;
                                }
                                
                                // Store tooltip information in AspectOverlay
                                net.felix.utilities.Overall.Aspekte.AspectOverlay.setTooltipBounds(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
                            }
                        } catch (Exception e) {
                            // If reflection fails, try alternative approach
                            // Calculate approximate tooltip size based on item name
                            net.minecraft.text.Text itemName = stack.getName();
                            if (itemName != null) {
                                int tooltipWidth = client.textRenderer.getWidth(itemName) + 6;
                                int tooltipHeight = client.textRenderer.fontHeight + 4;
                                
                                int tooltipX = mouseX + 12;
                                int tooltipY = mouseY - 12;
                                
                                net.felix.utilities.Overall.Aspekte.AspectOverlay.setTooltipBounds(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Updates the aspect overlay with the currently hovered item
     */
    private void updateAspectOverlay(int mouseX, int mouseY) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Get the hovered slot by checking all slots manually
            Slot hoveredSlot = null;
            for (Slot slot : screen.getScreenHandler().slots) {
                if (slot.x + x <= mouseX && mouseX < slot.x + x + 16 &&
                    slot.y + y <= mouseY && mouseY < slot.y + y + 16) {
                    hoveredSlot = slot;
                    break;
                }
            }
            
            // Check if we're hovering over an item with "⭐" (set up by addAspectNameToTooltip)
            // The tooltip callback manages the lifecycle of "⭐" items
            boolean isHoveringStarItem = net.felix.utilities.Overall.Aspekte.AspectOverlay.isCurrentlyHovering();
            
            // If we're hovering over a "⭐" item, don't override it with blueprint item detection
            // But we still need to check if we're still hovering over a slot
            if (isHoveringStarItem) {
                // If we're not hovering over any slot, clear the overlay immediately
                // This ensures the overlay disappears as soon as we move away from the item
                if (hoveredSlot == null || !hoveredSlot.hasStack()) {
                    net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
                }
                return;
            }
            
            // Handle blueprint items
            if (hoveredSlot != null && hoveredSlot.hasStack()) {
                ItemStack itemStack = hoveredSlot.getStack();
                if (itemStack != null && !itemStack.isEmpty()) {
                    // Check if the item name contains Epic colors - if so, don't show overlay
                    net.minecraft.text.Text itemNameText = itemStack.getName();
                    if (itemNameText != null && net.felix.utilities.Overall.InformationenUtility.hasEpicColor(itemNameText)) {
                        net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
                        return;
                    }
                    
                    // Update the aspect overlay with this item (for blueprint items only)
                    // Items with "⭐" are handled by addAspectNameToTooltip
                    if (isBlueprintInventory()) {
                        net.felix.utilities.Overall.Aspekte.AspectOverlay.updateAspectInfo(itemStack);
                    }
                    return;
                }
            }
            
            // If no valid item is hovered, hide the overlay immediately
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Renders the aspect overlay
     */
    private void renderAspectOverlay(DrawContext context) {
        try {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.renderForeground(context);
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Checks if the current screen is a blueprint inventory that should show the aspect overlay
     */
    private boolean isBlueprintInventory() {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            String title = screen.getTitle().getString();
            
            // Remove Minecraft formatting codes and Unicode characters for comparison
            String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                                   .replaceAll("[\\u3400-\\u4DBF]", "");
            
            // Check if the clean title contains any of the allowed blueprint inventory names
            return cleanTitle.contains("Baupläne [Waffen]")  || cleanTitle.contains("Blueprints [Weapons]") ||
                   cleanTitle.contains("Baupläne [Rüstung]") || cleanTitle.contains("Blueprints [Armor]") ||
                   cleanTitle.contains("Baupläne [Werkzeuge]") || cleanTitle.contains("Blueprints [Tools]") ||
                   cleanTitle.contains("Bauplan [Shop]")         || cleanTitle.contains("Blueprint Store")  ||
                   cleanTitle.contains("Favorisierte [Rüstungsbaupläne]")   || cleanTitle.contains("Favorited [Armor Blueprints]") ||
                   cleanTitle.contains("Favorisierte [Waffenbaupläne]")     || cleanTitle.contains("Favorited [Weapon Blueprints]") ||
                   cleanTitle.contains("Favorisierte [Werkzeugbaupläne]")   || cleanTitle.contains("Favorited [Tools Blueprints]") ||
                   cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
                   
        } catch (Exception e) {
            return false; // Default to false if there's an error
        }
    }

    /**
     * Renders the Kit Filter buttons
     */
    private void renderKitFilterButtons(DrawContext context, int mouseX, int mouseY) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Call the KitFilterUtility to render the buttons
            net.felix.utilities.Town.KitFilterUtility.renderKitFilterButtons(context, screen, mouseX, mouseY);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Checks if the current screen is a smithing-related inventory that should show colored frames
     */
    private boolean isSmithingInventory() {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            String title = screen.getTitle().getString();
            
            // IMPORTANT: Check for Equipment Display BEFORE cleaning, as cleaning removes the characters
            boolean isEquipmentDisplay = ZeichenUtility.containsEquipmentDisplay(title);
            
            // Remove Minecraft formatting codes and Unicode characters for comparison (same as in SchmiedTrackerUtility)
            String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                                     .replaceAll("[\\u3400-\\u4DBF]", "");
            
            // Check if the clean title contains any smithing-related keywords
            return cleanTitle.contains("Zerlegen")  ||
                   cleanTitle.contains("Umschmieden")  ||
                   (cleanTitle.contains("Ausrüstung") && cleanTitle.contains("Auswählen")) || 
                   cleanTitle.contains("Aufwerten")  ||
                   cleanTitle.contains("Rüstungs Sammlung")  ||
                   cleanTitle.contains("Waffen Sammlung")  ||
                   cleanTitle.contains("Werkzeug Sammlung")  ||
                   cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER") || 
                   cleanTitle.contains("Geschützte Items")  ||
                   isEquipmentDisplay; //Equipment Display - checked BEFORE cleaning
                   
        } catch (Exception e) {
            return false; // Default to false if there's an error
        }
    }
} 