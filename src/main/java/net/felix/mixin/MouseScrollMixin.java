package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.felix.utilities.Overall.InformationenUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseScrollMixin {
    
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Prüfe zuerst ob ItemViewer das Scroll-Event behandelt
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null && client.getWindow() != null) {
            // Hole aktuelle Mausposition und skaliere auf Screen-Koordinaten
            int windowWidth = client.getWindow().getScreenWidth();
            int windowHeight = client.getWindow().getScreenHeight();
            double mouseX = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) windowWidth;
            double mouseY = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) windowHeight;
            
            if (ItemViewerUtility.handleMouseScroll(mouseX, mouseY, vertical)) {
                // ItemViewer hat das Event behandelt
                // Hinweis: onMouseScroll ist nicht cancellable, daher können wir das Event nicht verhindern
                // Das ist aber in Ordnung, da wir nur die Pagination ändern
                return;
            }
        }
        
        // Übergebe das Scroll-Event an die EquipmentDisplayUtility
        EquipmentDisplayUtility.onMouseScroll(vertical);
        // Übergebe das Scroll-Event an die InformationenUtility für MKLevel Overlay
        InformationenUtility.onMKLevelMouseScroll(vertical);
    }
} 