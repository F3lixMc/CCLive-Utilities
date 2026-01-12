package net.felix.mixin;

import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.felix.utilities.Overall.SearchBarUtility;
import net.felix.utilities.Town.SchmiedTrackerUtility;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class SearchBarInputMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Blockiere Mausklicks wenn der Hilfe-Screen offen ist
        if (SearchBarUtility.isHelpScreenOpen()) {
            cir.setReturnValue(true);
            return;
        }
        
        if (SearchBarUtility.isVisible() && SearchBarUtility.handleMouseClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle Hide Uncraftable Button clicks
        if (SchmiedTrackerUtility.isInBlueprintInventory() && SchmiedTrackerUtility.handleButtonClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle Hide Wrong Class Button clicks
        if (SchmiedTrackerUtility.isInBlueprintInventory() && SchmiedTrackerUtility.handleWrongClassButtonClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle Kit Filter Button clicks
        if (net.felix.utilities.Town.KitFilterUtility.handleButtonClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle F6 button clicks
        if (net.felix.utilities.DragOverlay.OverlayEditorButtonUtility.handleButtonClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        
        // Handle MKLevel search bar clicks - need to get position from screen
        // We can't use @Shadow here, so we'll use reflection as fallback
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
            try {
                java.lang.reflect.Field xField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("x");
                java.lang.reflect.Field yField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("y");
                java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("backgroundHeight");
                xField.setAccessible(true);
                yField.setAccessible(true);
                bgHeightField.setAccessible(true);
                int inventoryX = xField.getInt(handledScreen);
                int inventoryY = yField.getInt(handledScreen);
                int inventoryHeight = bgHeightField.getInt(handledScreen);
                if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelSearchClick(mouseX, mouseY, button, inventoryX, inventoryY, inventoryHeight)) {
                    cir.setReturnValue(true);
                }
            } catch (Exception e) {
                // Reflection failed, skip
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Handle F6 key for overlay editor (works in inventories)
        if (OverlayEditorUtility.handleKeyPress(keyCode)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Handle F7 key for toggling smithing frames (works in inventories)
        if (SchmiedTrackerUtility.handleKeyPress(keyCode)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Handle ItemInfo extraction hotkey (works in inventories)
        if (ItemInfoUtility.handleKeyPress(keyCode)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Handle Item Logger hotkey (works in inventories)
        if (net.felix.utilities.DebugUtility.handleItemLoggerKeyPress(keyCode)) {
            cir.setReturnValue(true);
            return;
        }
        
        // Handle ItemInfo auto-click hotkey (works in inventories)
        // Get screen position using reflection (same pattern as MKLevel)
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
            try {
                // Try to get x and y fields (may be obfuscated)
                java.lang.reflect.Field xField = null;
                java.lang.reflect.Field yField = null;
                
                // Try common field names first
                for (String fieldName : new String[]{"x", "field_2776", "field_2777"}) {
                    try {
                        java.lang.reflect.Field field = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        if (field.getType() == int.class) {
                            if (xField == null) {
                                xField = field;
                            } else if (yField == null) {
                                yField = field;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Try next field name
                    }
                }
                
                // If we found both fields, use them
                if (xField != null && yField != null) {
                    int inventoryX = xField.getInt(handledScreen);
                    int inventoryY = yField.getInt(handledScreen);
                    if (ItemInfoUtility.handleAutoClickKeyPress(keyCode, inventoryX, inventoryY)) {
                        cir.setReturnValue(true);
                        return;
                    }
                } else {
                    // Fallback: search all int fields
                    java.lang.reflect.Field[] fields = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredFields();
                    int inventoryX = 0, inventoryY = 0;
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            int value = field.getInt(handledScreen);
                            // Heuristic: x and y are usually small positive values
                            if (value > 0 && value < 1000) {
                                if (inventoryX == 0) {
                                    inventoryX = value;
                                } else if (inventoryY == 0) {
                                    inventoryY = value;
                                    break;
                                }
                            }
                        }
                    }
                    if (inventoryX > 0 && inventoryY > 0) {
                        if (ItemInfoUtility.handleAutoClickKeyPress(keyCode, inventoryX, inventoryY)) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // Reflection failed, try fallback method
            }
        }
        
        // Fallback: try without position
        if (ItemInfoUtility.handleAutoClickKeyPress(keyCode)) {
            cir.setReturnValue(true);
            return;
        }
        
        if (SearchBarUtility.isVisible() && SearchBarUtility.handleKeyPress(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
        
        // Handle key press for MKLevel search bar
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelKeyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }


} 