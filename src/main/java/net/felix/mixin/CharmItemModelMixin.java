package net.felix.mixin;

import net.felix.utilities.Overall.customtextures.CustomItemModelUtility;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps the item model id of charm and charm fragment stacks for our own item definitions, before the
 * server pack's {@code minecraft:items/paper.json} is consulted. Covers GUI, hand, ground and frames.
 */
@Mixin(ItemModelResolver.class)
public class CharmItemModelMixin {

	@ModifyVariable(method = "appendItemLayers", at = @At("STORE"), ordinal = 0)
	private Identifier cclive$overrideCharmModel(Identifier modelId, ItemStackRenderState state, ItemStack stack,
			ItemDisplayContext context, Level level, ItemOwner owner, int seed) {
		if (modelId == null) {
			return null;
		}
		Identifier override = CustomItemModelUtility.modelOverride(stack);
		return override != null ? override : modelId;
	}
}
