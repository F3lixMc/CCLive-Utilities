package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseScrollMixin {
    
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Prüfe zuerst ob ItemViewer das Scroll-Event behandelt
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null && client.getWindow() != null) {
            // Hole aktuelle Mausposition und skaliere auf Screen-Koordinaten
            int windowWidth = client.getWindow().getWidth();
            int windowHeight = client.getWindow().getHeight();
            double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) windowWidth;
            double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) windowHeight;
            
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