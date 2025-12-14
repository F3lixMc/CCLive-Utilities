package net.felix.mixin;

import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add the CCLive-Utilities icon next to player names above their heads (nametags).
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    
    @Inject(
        method = "renderLabelIfPresent",
        at = @At("TAIL"),
        cancellable = false
    )
    private void renderIconAfterName(
        T entity,
        Text text,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        try {
            // Only render for player entities
            if (!(entity instanceof PlayerEntity)) {
                return;
            }
            
            PlayerEntity player = (PlayerEntity) entity;
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.textRenderer == null) {
                return;
            }
            
            // Don't render if world is not loaded
            if (client.world == null) {
                return;
            }
            
            // For testing: show icon for all players
            // In production, use: boolean shouldShowIcon = PlayerIconUtility.hasMod(player.getUuid());
            boolean shouldShowIcon = true; // Show for all for testing
            
            if (!shouldShowIcon) {
                return;
            }
            
            // Calculate icon position (to the left of the name)
            int nameWidth = client.textRenderer.getWidth(text);
            float iconX = -nameWidth / 2.0f - PlayerIconUtility.getDefaultIconWidth() - 2.0f;
            float iconY = -1.0f;
            
            // Render icon as a simple text character or symbol
            // For now, we'll render a simple marker
            // Note: Full 3D texture rendering requires more complex setup
            matrices.push();
            matrices.translate(iconX, iconY, 0);
            matrices.scale(0.5f, 0.5f, 0.5f);
            
            // Render a simple marker (you can replace this with actual icon rendering)
            // For now, we'll use a text-based approach
            net.minecraft.text.Text iconText = net.minecraft.text.Text.literal("●");
            client.textRenderer.draw(
                iconText,
                0, 0,
                0xFF00FFFF, // Cyan color
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
            
            matrices.pop();
        } catch (Exception e) {
            // Silently fail if there's any error
        }
    }
}
