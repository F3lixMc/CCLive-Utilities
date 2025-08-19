package net.felix.mixin;

import net.felix.utilities.SearchBarUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class SearchBarMixin {

    @Shadow protected int x;
    @Shadow protected int y;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderSearchBar(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SearchBarUtility.renderInScreen(context, (HandledScreen<?>) (Object) this, x, y);
        SearchBarUtility.renderSearchFrames(context, (HandledScreen<?>) (Object) this, x, y);
    }
    
    @Inject(method = "getTooltipFromItem", at = @At("HEAD"), cancellable = true)
    private void blockTooltipsFromItem(net.minecraft.item.ItemStack stack, CallbackInfoReturnable<java.util.List<net.minecraft.text.Text>> cir) {
        // Blockiere Tooltips wenn der Hilfe-Screen offen ist
        if (SearchBarUtility.isHelpScreenOpen()) {
            cir.setReturnValue(java.util.Collections.emptyList());
        }
    }
    

} 