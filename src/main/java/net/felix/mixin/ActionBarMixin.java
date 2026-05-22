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
import net.felix.utilities.DragOverlay.ClipboardFarmzoneActionBar;


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