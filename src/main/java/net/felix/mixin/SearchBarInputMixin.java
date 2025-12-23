package net.felix.mixin;

import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.felix.utilities.Overall.SearchBarUtility;
import net.felix.utilities.Town.SchmiedTrackerUtility;
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
        
        if (SearchBarUtility.isVisible() && SearchBarUtility.handleKeyPress(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
        
        // Handle key press for MKLevel search bar
        if (net.felix.utilities.Overall.InformationenUtility.handleMKLevelKeyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }


} 