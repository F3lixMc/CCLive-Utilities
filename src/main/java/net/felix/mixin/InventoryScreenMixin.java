package net.felix.mixin;

/**
 * Mixin für InventoryScreen
 * (Der Crafting-Text wird jetzt über TextMixin dynamisch ausgeblendet)
 */
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.screen.ingame.InventoryScreen.class)
public abstract class InventoryScreenMixin {
    // Der Crafting-Text wird jetzt über TextMixin dynamisch ausgeblendet,
    // daher ist hier kein Code mehr nötig
}

