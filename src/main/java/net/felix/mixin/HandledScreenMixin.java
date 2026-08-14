package net.felix.mixin;

import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    
    /**
     * Injects at the very end of the render method to ensure our overlays are drawn last
     * This preserves all normal rendering while ensuring our overlays appear above tooltips
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        
        // Überspringe InventoryScreen - wird im ScreenMixin behandelt, um Doppel-Rendering zu vermeiden
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            return;
        }
        
        // Update mouse position for DebugUtility (Item Logger)
        net.felix.utilities.Other.DebugUtility.updateMousePosition(mouseX, mouseY);
        
        // Update mouse position for Item Viewer
        net.felix.utilities.ItemViewer.ItemViewerUtility.updateMousePosition(mouseX, mouseY);
        
        // Update hovered slot for Item Viewer (für Clipboard-Pin-Funktion)
        updateHoveredSlotForItemViewer(screen, mouseX, mouseY);
        
        // Update mouse position for DebugUtility (Item Logger)
        net.felix.utilities.Other.DebugUtility.updateMousePosition(mouseX, mouseY);
        
        // Render Clipboard Overlay (wenn aktiviert)
        net.felix.utilities.DragOverlay.Overall.ClipboardDraggableOverlay.renderInGame(context, mouseX, mouseY, delta);
        
        // Render Clipboard Button Tooltips
        net.felix.utilities.DragOverlay.Overall.ClipboardDraggableOverlay.renderButtonTooltips(context, mouseX, mouseY);
        
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
        
        // Render F6 button (bottom left corner)
        renderF6Button(context, mouseX, mouseY);
        
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
        
        // Item-Info-Overlay (nicht registrierte Baupläne / Angel-Komponenten markieren)
        if (net.felix.utilities.Aincraft.ItemInfoUtility.isSupportedInventory(screen)) {
            net.felix.utilities.Aincraft.ItemInfoUtility.renderUnregisteredItemOverlays(context, screen, leftPos, topPos);
        }
        
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
        // Wird sowohl hier als auch in GuiHelpOverlayMixin gerendert, um sicherzustellen, dass es überall funktioniert
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            net.felix.utilities.ItemViewer.ItemViewerUtility.renderHelpOverlay(context);
        }
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isOverlayOpen()) {
            net.felix.utilities.ItemViewer.ItemViewerUtility.renderFilterOverlay(context);
        }
        
        // Rendere minimierten Button (rechts unten), wenn minimiert - nach allem anderen, damit er über dem dunklen Hintergrund liegt
        net.felix.utilities.ItemViewer.ItemViewerUtility.renderMinimizedButtonIfNeeded(context);
    }
    
    /**
     * Rendert den Item Viewer Overlay
     */
    private void renderItemViewer(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            
            // Rendere Item-Viewer (wird nur gerendert wenn sichtbar)
            net.felix.utilities.ItemViewer.ItemViewerUtility.renderItemViewerInScreen(context, client, screen, mouseX, mouseY);
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders MKLevel overlay if we're in the "Machtkristalle Verbessern" inventory or Essence Harvester UI (Glyph "㮌")
     */
    private void renderMKLevelOverlay(GuiGraphicsExtractor context) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            net.minecraft.network.chat.Component titleText = screen.getTitle();
            
            // Prüfe direkt auf dem Text-Objekt (bevor getPlainTextFromText Unicode-Zeichen entfernt)
            String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen
            String titlePlain = net.felix.utilities.Overall.InformationenUtility.getPlainTextFromText(titleText);
            
            // Prüfe sowohl "Machtkristalle Verbessern" als auch das Glyph "㮌" (Essence Harvester UI)
            // Verwende titleWithUnicode für die Glyph-Prüfung, da getPlainTextFromText Unicode entfernt
            if (ZeichenUtility.isMkLevelInventoryTitle(titlePlain, titleWithUnicode)) {
                // Get actual inventory dimensions from the screen using shadow fields
                net.felix.utilities.Overall.InformationenUtility.renderMKLevelOverlay(context, net.minecraft.client.Minecraft.getInstance(), leftPos, topPos, imageWidth, imageHeight);
            }
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Blockiert Tooltips wenn das Hilfe-Overlay offen ist (wie im Bauplan-Shop)
     */
    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void blockTooltipsFromItem(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<java.util.List<net.minecraft.network.chat.Component>> cir) {
        // Blockiere Tooltips wenn das Hilfe-Overlay offen ist
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isOverlayOpen()) {
            cir.setReturnValue(java.util.Collections.emptyList());
        }
    }
    
    /**
     * Blockiert das Rendern der Items in InventoryScreen, wenn das Hilfe-Overlay offen ist
     * Injiziert in die drawSlot-Methode, um das Rendern der Items zu blockieren
     */
    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void blockSlotRenderingInInventoryScreen(GuiGraphicsExtractor context, Slot slot, int x, int y, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        // Blockiere das Rendern der Items nur für InventoryScreen, wenn das Hilfe-Overlay offen ist
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen && 
            net.felix.utilities.ItemViewer.ItemViewerUtility.isOverlayOpen()) {
            ci.cancel();
        }
    }
    
    
    /**
     * Injects into mouseClicked to handle clicks on the Hide Uncraftable button
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        // Handle clicks on Help Overlay FIRST (höchste Priorität wenn geöffnet)
        // Dies muss vor allen anderen Klicks geprüft werden
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isHelpOverlayOpen()) {
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleHelpOverlayClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
        if (net.felix.utilities.ItemViewer.ItemViewerFilterMenu.isOpen()) {
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleFilterOverlayClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }

        net.felix.utilities.ItemViewer.ItemViewerUtility.blurSearchFieldFocusUnlessClickOnField(mouseX, mouseY, button);
        net.felix.utilities.Overall.SearchBarUtility.blurSearchBarFocusUnlessClickOnBar(mouseX, mouseY, button);
        
        // Handle clicks on Clipboard Overlay buttons (inkl. Delete-Button)
        if (net.felix.utilities.DragOverlay.Overall.ClipboardDraggableOverlay.handleButtonClick((int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Handle clicks on Clipboard Delete-Button (auch wenn Bestätigungs-Overlay offen ist)
        // Dies wird bereits in handleButtonClick behandelt, aber wir müssen sicherstellen, dass es auch außerhalb von Screens funktioniert
        
        // Handle clicks on Clipboard quantity text field
        if (net.felix.utilities.DragOverlay.Overall.ClipboardDraggableOverlay.handleQuantityTextFieldClick((int) mouseX, (int) mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Priorität basierend auf Item Viewer Status:
        // - Wenn ausgeklappt (nicht minimiert): Item Viewer hat Priorität
        // - Wenn eingeklappt (minimiert): Buttons haben Priorität
        boolean isItemViewerMinimized = net.felix.utilities.ItemViewer.ItemViewerUtility.isMinimized();
        
        if (isItemViewerMinimized) {
            // Item Viewer ist eingeklappt: Buttons haben Priorität
            if (net.felix.utilities.Town.KitFilterUtility.handleButtonClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
            // Handle clicks on Item Viewer buttons (nur Minimize-Button wenn minimiert)
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleMouseClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        } else {
            // Item Viewer ist ausgeklappt: Item Viewer hat Priorität
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleMouseClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
            // Handle clicks on Kit Filter buttons (nur wenn Item Viewer den Klick nicht verarbeitet hat)
            if (net.felix.utilities.Town.KitFilterUtility.handleButtonClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
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
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelSearchClick(mouseX, mouseY, button, leftPos, topPos, imageHeight)) {
            cir.setReturnValue(true);
        }
        
        // Handle clicks on F6 button
        if (net.felix.utilities.DragOverlay.OverlayEditorButtonUtility.handleButtonClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }

        // Legend+-Sammler: Ressourcen vor Linksklick auf [Sammler] merken
        AbstractContainerScreen<?> legendPlusScreen = (AbstractContainerScreen<?>) (Object) this;
        net.felix.utilities.Other.Clipboard.LegendPlusSammlerCollector.handleMouseClick(
                legendPlusScreen, mouseX, mouseY, button, leftPos, topPos);

        // Amboss/Schmelzofen: Ressourcen aus Lager-Tooltip bei Linksklick addieren
        net.felix.utilities.Other.Clipboard.AnvilFurnaceLagerCollector.handleMouseClick(
                legendPlusScreen, mouseX, mouseY, button, leftPos, topPos);

        // Kosten-Inventare: Kauf-Kosten merken und nach Klick ggf. abziehen
        net.felix.utilities.Other.Clipboard.ClipboardCostPurchaseTracker.handleMouseClick(
                legendPlusScreen, mouseX, mouseY, button, leftPos, topPos);
    }
    
    /**
     * Handles mouse dragging for MKLevel scrollbar
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (net.felix.utilities.ItemViewer.ItemViewerFilterMenu.isOpen()) {
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleFilterOverlayDrag(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelScrollbarDrag(mouseX, mouseY, button, leftPos, topPos, imageHeight)) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Handles mouse release to stop scrollbar dragging
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.handleFilterOverlayRelease(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelScrollbarRelease(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
    
    
    /**
     * Renders colored frames around smithing states
     */
    private void renderSmithingFrames(GuiGraphicsExtractor context) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
            // Call the SchmiedTrackerUtility to render colored frames
            // This will handle all the logic for determining which slots need frames and what colors to use
            net.felix.utilities.Town.SchmiedTrackerUtility.renderColoredFrames(context, screen, leftPos, topPos);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders the Hide Uncraftable button
     */
    private void renderHideUncraftableButton(GuiGraphicsExtractor context) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
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
    private void renderHideWrongClassButton(GuiGraphicsExtractor context) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
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
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
            // Find the hovered slot
            Slot hoveredSlot = null;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.x + leftPos <= mouseX && mouseX < slot.x + leftPos + 16 &&
                    slot.y + topPos <= mouseY && mouseY < slot.y + topPos + 16) {
                    hoveredSlot = slot;
                    break;
                }
            }
            
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                ItemStack stack = hoveredSlot.getItem();
                if (stack != null && !stack.isEmpty()) {
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    if (client != null && client.font != null) {
                        // Get tooltip lines using reflection to access protected method
                        try {
                            java.lang.reflect.Method getTooltipMethod = net.minecraft.client.gui.screens.Screen.class.getDeclaredMethod("getTooltipFromItem", net.minecraft.client.Minecraft.class, ItemStack.class);
                            getTooltipMethod.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> tooltip = (java.util.List<net.minecraft.network.chat.Component>) getTooltipMethod.invoke(screen, client, stack);
                            
                            if (tooltip != null && !tooltip.isEmpty()) {
                                // Calculate tooltip dimensions
                                int tooltipWidth = 0;
                                for (net.minecraft.network.chat.Component line : tooltip) {
                                    int lineWidth = client.font.width(line);
                                    tooltipWidth = Math.max(tooltipWidth, lineWidth);
                                }
                                
                                // Add padding (Minecraft uses 3 pixels on each side)
                                tooltipWidth += 6;
                                int tooltipHeight = tooltip.size() * (client.font.lineHeight + 2) + 4;
                                
                                // Calculate tooltip position (Minecraft positions it near the mouse)
                                // Tooltip is typically positioned to the right and above the mouse cursor
                                int tooltipX = mouseX + 12;
                                int tooltipY = mouseY - 12;
                                
                                // Adjust if tooltip would go off screen (Minecraft does this automatically)
                                int screenWidth = client.getWindow().getGuiScaledWidth();
                                int screenHeight = client.getWindow().getGuiScaledHeight();
                                
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
                            net.minecraft.network.chat.Component itemName = stack.getHoverName();
                            if (itemName != null) {
                                int tooltipWidth = client.font.width(itemName) + 6;
                                int tooltipHeight = client.font.lineHeight + 4;
                                
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
     * Updates the hovered slot for Item Viewer (für Clipboard-Pin-Funktion)
     */
    private void updateHoveredSlotForItemViewer(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        try {
            // Get the hovered slot by checking all slots manually (gleiche Logik wie updateAspectOverlay)
            Slot hoveredSlot = null;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.x + leftPos <= mouseX && mouseX < slot.x + leftPos + 16 &&
                    slot.y + topPos <= mouseY && mouseY < slot.y + topPos + 16) {
                    hoveredSlot = slot;
                    break;
                }
            }
            
            // Debug: Nur wenn in Bauplan-Inventar und Slot gefunden
            if (hoveredSlot != null && hoveredSlot.hasItem() && isBlueprintInventory()) {
                // System.out.println("[ClipboardPin-DEBUG] 📍 Slot-Tracking: Slot gefunden (Index: " + hoveredSlot.id + ", Maus: " + mouseX + "," + mouseY + ", Screen: " + x + "," + y + ")");
            }
            
            // Update hovered slot in ItemViewerUtility
            net.felix.utilities.ItemViewer.ItemViewerUtility.updateHoveredSlot(screen, hoveredSlot);
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Updates the aspect overlay with the currently hovered item
     */
    private void updateAspectOverlay(int mouseX, int mouseY) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
            // Get the hovered slot by checking all slots manually
            Slot hoveredSlot = null;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.x + leftPos <= mouseX && mouseX < slot.x + leftPos + 16 &&
                    slot.y + topPos <= mouseY && mouseY < slot.y + topPos + 16) {
                    hoveredSlot = slot;
                    break;
                }
            }
            
            // Star items (⭐) only — not Item Viewer / blueprint hover (lastTooltipUpdateTime == -1)
            if (net.felix.utilities.Overall.Aspekte.AspectOverlay.isStarItemHovering()) {
                if (hoveredSlot == null || !hoveredSlot.hasItem()) {
                    net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
                }
                return;
            }

            // Item Viewer owns aspect state while hovering a viewer item (refreshed later this frame).
            // Clearing here caused a one-frame flicker and then a dead overlay when tooltip caching
            // skipped re-applying the same hovered ItemData.
            if (net.felix.utilities.ItemViewer.ItemViewerUtility.hasItemViewerHover()) {
                return;
            }
            
            // Handle blueprint items
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                ItemStack itemStack = hoveredSlot.getItem();
                if (itemStack != null && !itemStack.isEmpty()) {
                    // Check if the item name contains Epic colors - if so, don't show overlay
                    net.minecraft.network.chat.Component itemNameText = itemStack.getHoverName();
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
    private void renderAspectOverlay(GuiGraphicsExtractor context) {
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
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
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
    private void renderKitFilterButtons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
            // Call the KitFilterUtility to render the buttons
            net.felix.utilities.Town.KitFilterUtility.renderKitFilterButtons(context, screen, mouseX, mouseY);
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Renders the F6 button (bottom left corner)
     */
    private void renderF6Button(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            
            // Prüfe ob Button angezeigt werden soll
            if (net.felix.utilities.DragOverlay.OverlayEditorButtonUtility.shouldShowButton(screen)) {
                // Call the OverlayEditorButtonUtility to render the button
                net.felix.utilities.DragOverlay.OverlayEditorButtonUtility.renderButton(context, screen, mouseX, mouseY);
            }
            
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }
    
    /**
     * Checks if the current screen is a smithing-related inventory that should show colored frames
     */
    private boolean isSmithingInventory() {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            return net.felix.utilities.Town.SchmiedTrackerUtility.isSmithingRelatedInventoryTitle(
                    screen.getTitle().getString());
        } catch (Exception e) {
            return false;
        }
    }
} 