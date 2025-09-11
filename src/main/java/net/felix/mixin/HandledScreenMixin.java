package net.felix.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
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
    
    /**
     * Injects at the very end of the render method to ensure our overlays are drawn last
     * This preserves all normal rendering while ensuring our overlays appear above tooltips
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Render colored frames around smithing states ONLY in smithing-related screens
        if (isSmithingInventory()) {
            renderSmithingFrames(context);
        }
        
        // Render the Hide Uncraftable button ONLY in blueprint inventories
        if (isBlueprintInventory()) {
            renderHideUncraftableButton(context);
        }
        
        // Only show aspect overlay in specific blueprint inventories and if enabled in config
        if (isBlueprintInventory() && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
            // Update aspect overlay with current hovered item
            updateAspectOverlay(mouseX, mouseY);
            
            // Render our aspect overlay AFTER everything else (including tooltips)
            renderAspectOverlay(context);
        }
    }
    
    /**
     * Injects into mouseClicked to handle clicks on the Hide Uncraftable button
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Handle clicks on the Hide Uncraftable button ONLY in blueprint inventories
        if (isBlueprintInventory()) {
            if (handleHideUncraftableButtonClick(mouseX, mouseY, button)) {
                cir.setReturnValue(true); // Indicate that we handled the click
            }
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
            net.felix.utilities.SchmiedTrackerUtility.renderColoredFrames(context, screen, x, y);
            
        } catch (Exception e) {
            System.err.println("Error rendering smithing frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Renders the Hide Uncraftable button
     */
    private void renderHideUncraftableButton(DrawContext context) {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Call the SchmiedTrackerUtility to render the Hide Uncraftable button
            net.felix.utilities.SchmiedTrackerUtility.renderHideUncraftableButton(context, screen);
            
        } catch (Exception e) {
            System.err.println("Error rendering Hide Uncraftable button: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles clicks on the Hide Uncraftable button
     */
    private boolean handleHideUncraftableButtonClick(double mouseX, double mouseY, int button) {
        try {
            // Call the SchmiedTrackerUtility to handle the button click
            return net.felix.utilities.SchmiedTrackerUtility.handleButtonClick(mouseX, mouseY, button);
            
        } catch (Exception e) {
            System.err.println("Error handling Hide Uncraftable button click: " + e.getMessage());
            e.printStackTrace();
            return false;
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
            
            if (hoveredSlot != null && hoveredSlot.hasStack()) {
                ItemStack itemStack = hoveredSlot.getStack();
                if (itemStack != null && !itemStack.isEmpty()) {
                    // Update the aspect overlay with this item
                    net.felix.utilities.AspectOverlay.updateAspectInfo(itemStack);
                    return;
                }
            }
            
            // If no valid item is hovered, hide the overlay
            net.felix.utilities.AspectOverlay.onHoverStopped();
            
        } catch (Exception e) {
            System.err.println("Error updating aspect overlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Renders the aspect overlay
     */
    private void renderAspectOverlay(DrawContext context) {
        try {
            net.felix.utilities.AspectOverlay.renderForeground(context);
        } catch (Exception e) {
            System.err.println("Error rendering aspect overlay: " + e.getMessage());
            e.printStackTrace();
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
            return cleanTitle.contains("Baupläne [Waffen]") ||
                   cleanTitle.contains("Baupläne [Rüstung]") ||
                   cleanTitle.contains("Baupläne [Werkzeuge]") ||
                   cleanTitle.contains("Bauplan [Shop]") ||
                   cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
                   cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
                   cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
                   
        } catch (Exception e) {
            System.err.println("Error checking blueprint inventory: " + e.getMessage());
            e.printStackTrace();
            return false; // Default to false if there's an error
        }
    }

    /**
     * Checks if the current screen is a smithing-related inventory that should show colored frames
     */
    private boolean isSmithingInventory() {
        try {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            String title = screen.getTitle().getString();
            
            // Check if the title contains any smithing-related keywords
            return title.contains("Zerlegen") || 
                   title.contains("Umschmieden") || 
                   title.contains("Ausrüstung [Auswählen]") || 
                   title.contains("Aufwerten") || 
                   title.contains("Rüstungs Sammlung") || 
                   title.contains("Waffen Sammlung") || 
                   title.contains("Werkzeug Sammlung") || 
                   title.contains("CACTUS_CLICKER.CACTUS_CLICKER") || 
                   title.contains("Geschützte Items");
                   
        } catch (Exception e) {
            System.err.println("Error checking smithing inventory: " + e.getMessage());
            e.printStackTrace();
            return false; // Default to false if there's an error
        }
    }
} 