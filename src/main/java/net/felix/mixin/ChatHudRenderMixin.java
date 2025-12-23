package net.felix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for ChatHud rendering functionality.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudRenderMixin {
    
    @Shadow(remap = true)
    private List<ChatHudLine.Visible> visibleMessages;
    
    @Shadow(remap = true)
    private List<ChatHudLine> messages;
    
    @Inject(
        method = "render",
        at = @At("TAIL"),
        cancellable = false
    )
    private void renderIconsInChat(
        DrawContext context,
        int currentTick,
        int mouseX,
        int mouseY,
        boolean bl,
        CallbackInfo ci
    ) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null || client.getWindow() == null) {
                return;
            }
            
            // Don't render if world is not loaded (prevents issues during startup)
            if (client.world == null) {
                return;
            }
            
        } catch (Exception e) {
            // Silently fail if there's any error
        }
    }
    
}

