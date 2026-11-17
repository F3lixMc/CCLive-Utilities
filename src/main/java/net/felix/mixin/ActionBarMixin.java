package net.felix.mixin;

import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.materials.tracker.ActionBarData;
import net.felix.utilities.Factory.bosshp.BossHPUtility;
import net.felix.utilities.Other.clipboard.collector.ClipboardFarmzoneActionBar;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(Hud.class)
public class ActionBarMixin {
    
    @Shadow
    private Component overlayMessageString;
    
    @Shadow
    private int overlayMessageTime;
    
    @Inject(at = @At("HEAD"), method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V")
    private void onSetOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        if (message != null) {
            String content = message.getString();
            
            // Farmzone: Ressourcen (+XXX NAME [GESAMT]) → collected_materials-ressources.json
            ClipboardFarmzoneActionBar.processActionBarMessage(message);

            // Aincrad-Floor: Material-Tracking aus der ActionBar
            if (ActionBarData.isOnFloor()) {
                ActionBarData.processActionBarMessage(message);
            }
            
            // Check for blueprint messages in action bar (combo chest rewards)
            // (Diese Prüfung läuft unabhängig von der Dimension)
            BPViewerUtility instance = BPViewerUtility.getInstance();
            if (instance != null) {
                instance.checkForBlueprint(message, content);
            }
            
            // Check for boss defeat
            // (Diese Prüfung läuft unabhängig von der Dimension)
            if (content.contains("Seelen" )) {
                BossHPUtility instance2 = BossHPUtility.getInstance();
                if (instance2 != null) {
                    instance2.handleBossDefeated();
                }
            }
        }
    }
    

} 
