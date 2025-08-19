package net.felix.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.felix.utilities.BossHPUtility;
import net.felix.utilities.ActionBarData;
import net.felix.utilities.BPViewerUtility;


@Mixin(InGameHud.class)
public class ActionBarMixin {
    
    @Shadow
    private Text overlayMessage;
    
    @Shadow
    private int overlayRemaining;
    
    @Inject(at = @At("HEAD"), method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V")
    private void onSetOverlayMessage(Text message, boolean animate, CallbackInfo ci) {
        if (message != null) {
            String content = message.getString();
            
            // Process material tracking with original text (including color codes)
            ActionBarData.processActionBarMessage(message);
            
            // Check for blueprint messages in action bar (combo chest rewards)
            BPViewerUtility instance = BPViewerUtility.getInstance();
            if (instance != null) {
                instance.checkForBlueprint(content);
            }
            
            // Check for boss defeat
            if (content.contains("Seelen")) {
                BossHPUtility instance2 = BossHPUtility.getInstance();
                if (instance2 != null) {
                    instance2.handleBossDefeated();
                }
            }
        }
    }
    
    @Inject(at = @At("HEAD"), method = "tick()V")
    private void onTick(CallbackInfo ci) {
        // Check current overlay message every tick
        if (overlayMessage != null && overlayRemaining > 0) {
            String content = overlayMessage.getString();
            
            // Process material tracking on tick as well with original text
            ActionBarData.processActionBarMessage(overlayMessage);
            
            // Check for blueprint messages in action bar (combo chest rewards)
            BPViewerUtility instance = BPViewerUtility.getInstance();
            if (instance != null) {
                instance.checkForBlueprint(content);
            }
            
            if (content.contains("Seelen")) {
                // Boss defeat detected via tick
            }
        }
    }
} 