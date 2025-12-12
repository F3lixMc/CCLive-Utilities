package net.felix.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.felix.utilities.Overall.ActionBarData;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.utilities.Factory.BossHPUtility;


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
                instance.checkForBlueprint(message, content);
            }
            
            // Check for boss defeat
            if (content.contains("Seelen" ) || content.contains("Souls")) {
                BossHPUtility instance2 = BossHPUtility.getInstance();
                if (instance2 != null) {
                    instance2.handleBossDefeated();
                }
            }
        }
    }
    

} 