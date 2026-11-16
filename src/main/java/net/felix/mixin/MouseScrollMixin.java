package net.felix.mixin;

import net.felix.utilities.Factory.mklevel.overlay.MkLevelInputHandler;
import net.felix.utilities.Overall.itemviewer.ui.ViewerInputHandler;
import net.felix.utilities.Town.equipment.EquipmentDisplayUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(MouseHandler.class)
public class MouseScrollMixin {
    
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Prüfe zuerst ob ItemViewer das Scroll-Event behandelt
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null && client.getWindow() != null) {
            // Hole aktuelle Mausposition und skaliere auf Screen-Koordinaten
            int windowWidth = client.getWindow().getScreenWidth();
            int windowHeight = client.getWindow().getScreenHeight();
            double mouseX = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) windowWidth;
            double mouseY = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) windowHeight;
            
            if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                    && net.felix.utilities.Community.leaderboards.panel.LeaderboardInventoryOverlay.coversScroll(mouseX, mouseY)) {
                return;
            }
            if (ViewerInputHandler.handleMouseScroll(mouseX, mouseY, vertical)) {
                // ItemViewer hat das Event behandelt
                // Hinweis: onMouseScroll ist nicht cancellable, daher können wir das Event nicht verhindern
                // Das ist aber in Ordnung, da wir nur die Pagination ändern
                return;
            }
            if (net.felix.utilities.Community.chat.BuildShareViewScreen.isActive()) {
                net.felix.utilities.Community.chat.BuildShareViewScreen view = net.felix.utilities.Community.chat.BuildShareViewScreen.getActive();
                if (view != null && view.handleStatOverlayScroll(mouseX, mouseY, vertical)) {
                    return;
                }
            }
            if (net.felix.utilities.Community.chat.BuildShareManager.getInstance().handleOverlayScroll(mouseX, mouseY, vertical)) {
                return;
            }
        }
        
        // Übergebe das Scroll-Event an die EquipmentDisplayUtility
        EquipmentDisplayUtility.onMouseScroll(vertical);
        // Übergebe das Scroll-Event an den MkLevelInputHandler für das MkLevel Overlay
        MkLevelInputHandler.onMkLevelMouseScroll(vertical);
    }
} 
