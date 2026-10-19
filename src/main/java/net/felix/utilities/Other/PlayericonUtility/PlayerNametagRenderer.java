package net.felix.utilities.Other.PlayericonUtility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.felix.CCLiveUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders the CCLive-Utilities icon next to player nametags.
 * Behavior matches vanilla nametags: lowers while sneaking, see-through behind blocks unless sneaking.
 */
public class PlayerNametagRenderer {
    
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        LevelRenderEvents.END_MAIN.register(context -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.level == null || client.player == null) {
                    return;
                }
                
                if (client.font == null || context == null || context.gameRenderer().mainCamera() == null) {
                    return;
                }

                if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showPlayerNametagIcon) {
                    return;
                }

                if (client.gui.hud.isHidden()) {
                    return;
                }
                
                for (var entity : client.level.entitiesForRendering()) {
                    try {
                        if (!(entity instanceof Player player)) {
                            continue;
                        }
                        
                        if (player.equals(client.player)) {
                            continue;
                        }
                        
                        // Invisible players: kein Icon
                        if (player.isInvisible()) {
                            continue;
                        }
                        
                        if (!PlayerIconUtility.hasMod(player.getUUID())) {
                            continue;
                        }
                        
                        renderNametagIcon(context, player);
                    } catch (Exception e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                // Silently fail
            }
        });
        
        initialized = true;
    }
    
    private static void renderNametagIcon(LevelRenderContext context, Player player) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font == null) {
                return;
            }
            
            if (context == null || context.gameRenderer().mainCamera() == null || context.poseStack() == null) {
                return;
            }

            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            Vec3 playerPos = player.getPosition(partialTick);

            // Vanilla: NAME_TAG-Attachment folgt der Pose (inkl. Sneaken → tiefer)
            Vec3 attachment = player.getAttachments().getNullable(
                EntityAttachment.NAME_TAG,
                0,
                player.getYRot(partialTick)
            );
            if (attachment == null) {
                attachment = new Vec3(0.0, player.getBbHeight(), 0.0);
            }

            double x = playerPos.x + attachment.x;
            double y = playerPos.y + attachment.y + 0.5;
            double z = playerPos.z + attachment.z;

            // Wie AvatarRenderer.getRenderOffset: beim Crouchen zusätzlich -2/16 * scale
            if (player.isCrouching()) {
                y += player.getScale() * (-2.0F / 16.0F);
            }

            // Score unter dem Namen → Name (und Icon) eine Zeile höher
            if (player.belowNameDisplay() != null) {
                double distanceSq = playerPos.distanceToSqr(cameraPos);
                if (distanceSq < 100.0) {
                    y += 9.0F * 1.15F * EntityRenderer.NAMETAG_SCALE;
                }
            }
            
            double dx = x - cameraPos.x;
            double dy = y - cameraPos.y;
            double dz = z - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            if (distance > 64.0 || distance < 0.1) {
                return;
            }
            
            PoseStack matrices = context.poseStack();
            matrices.pushPose();
            
            try {
                matrices.translate((float) dx, (float) dy, (float) dz);
                matrices.mulPose(new Matrix4f().rotate(context.gameRenderer().mainCamera().rotation()));
                float baseScale = EntityRenderer.NAMETAG_SCALE;
                matrices.scale(baseScale, -baseScale, baseScale);
                
                net.minecraft.network.chat.Component playerName = player.getDisplayName();
                int nameWidth = client.font.width(playerName);
                
                float iconSize = 10.0f;
                float halfSize = iconSize / 2.0f;
                float spacing = 2.0f;
                float iconOffsetX = nameWidth / 2.0f + spacing + halfSize;
                float iconOffsetY = 9.0f / 2.0f;
                
                matrices.translate(iconOffsetX, iconOffsetY, 0.0f);
                
                // Wie Vanilla-Nametag: see-through nur wenn nicht discrete (sneaken)
                boolean seeThrough = !player.isDiscrete();
                int lightLevel = 15728880;
                renderIconTexture(context.submitNodeCollector(), matrices, iconSize, lightLevel, seeThrough);
            } finally {
                matrices.popPose();
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    private static void renderIconTexture(
        SubmitNodeCollector collector,
        PoseStack matrices,
        float size,
        int light,
        boolean seeThrough
    ) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getTextureManager() == null) {
                return;
            }
            
            Identifier iconId = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/icon.png");
            Identifier iconIdAlt = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "icon");
            
            Identifier iconToUse = iconId;
            try {
                if (client.getResourceManager() != null) {
                    var resource = client.getResourceManager().getResource(iconId);
                    if (resource.isEmpty()) {
                        var resourceAlt = client.getResourceManager().getResource(iconIdAlt);
                        if (resourceAlt.isPresent()) {
                            iconToUse = iconIdAlt;
                        } else {
                            return;
                        }
                    }
                } else {
                    iconToUse = iconIdAlt;
                }
            } catch (Exception e) {
                iconToUse = iconIdAlt;
            }
            
            float halfSize = size / 2.0f;

            // Wie NameTagFeatureRenderer: bei Sichtbarkeit durch Wände zuerst see-through, dann normal
            if (seeThrough) {
                collector.submitCustomGeometry(matrices, RenderTypes.textSeeThrough(iconToUse), (pose, consumer) ->
                    emitIconQuad(consumer, pose, halfSize, light));
            }
            collector.submitCustomGeometry(matrices, RenderTypes.text(iconToUse), (pose, consumer) ->
                emitIconQuad(consumer, pose, halfSize, light));
        } catch (Exception e) {
            // Silently fail
        }
    }

    private static void emitIconQuad(VertexConsumer vertexConsumer, PoseStack.Pose pose, float halfSize, int light) {
        vertexConsumer.addVertex(pose, -halfSize, -halfSize, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(0.0f, 0.0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0f, 0.0f, 1.0f);
        
        vertexConsumer.addVertex(pose, -halfSize, halfSize, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(0.0f, 1.0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0f, 0.0f, 1.0f);
        
        vertexConsumer.addVertex(pose, halfSize, halfSize, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(1.0f, 1.0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0f, 0.0f, 1.0f);
        
        vertexConsumer.addVertex(pose, halfSize, -halfSize, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(1.0f, 0.0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0f, 0.0f, 1.0f);
    }
}
