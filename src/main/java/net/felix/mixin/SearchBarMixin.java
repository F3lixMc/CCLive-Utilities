package net.felix.mixin;

import net.felix.utilities.Overall.SearchBarUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SearchBarMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderSearchBar(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Blockiere das Rendern der Suchleiste, wenn das Hilfe-Overlay vom ItemViewer offen ist
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isOverlayOpen()) {
            return;
        }
        SearchBarUtility.renderInScreen(context, (AbstractContainerScreen<?>) (Object) this, leftPos, topPos);
        SearchBarUtility.renderSearchFrames(context, (AbstractContainerScreen<?>) (Object) this, leftPos, topPos);
    }
    
    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void blockTooltipsFromItem(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<java.util.List<net.minecraft.network.chat.Component>> cir) {
        // Blockiere Tooltips wenn der Hilfe-Screen offen ist
        if (SearchBarUtility.isHelpScreenOpen()) {
            cir.setReturnValue(java.util.Collections.emptyList());
        }
    }
    

} 