package net.felix.mixin;

import net.felix.utilities.Overall.AnimationBlockerUtility;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InGameHud.class)
public class TitleMixin {

    
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(Text title, CallbackInfo ci) {
        if (AnimationBlockerUtility.isAnimationBlockingEnabled()) {
            if (title != null) {
                String titleString = title.getString();
                
                if (shouldBlockTitle(titleString)) {
                    ci.cancel(); // Block the title
                    return;
                }
            }
        }
    }
    
    @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
        if (AnimationBlockerUtility.isAnimationBlockingEnabled()) {
            if (subtitle != null) {
                String subtitleString = subtitle.getString();
                
                if (shouldBlockTitle(subtitleString)) {
                    ci.cancel(); // Block the subtitle
                    return;
                }
            }
        }
    }
    
    private boolean shouldBlockTitle(String titleString) {
        if (titleString == null || titleString.trim().isEmpty()) {
            return false;
        }
        
        // Check if title contains blocked characters
        for (String blockedChar : AnimationBlockerUtility.getBlockedCharacters()) {
            if (titleString.contains(blockedChar)) {
                return true;
            }
        }
        
        return false;
    }
} 