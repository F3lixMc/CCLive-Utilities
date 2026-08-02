package net.felix.mixin;

import dev.isxander.yacl3.gui.TextScaledButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * YACL 3.9.x für 26.1 zeichnet den Options-Reset-Button ohne Text-Skalierung
 * (textScale wird ignoriert). Stellt die 2x-Skalierung des ↺-Symbols wieder her.
 */
@Mixin(value = TextScaledButtonWidget.class, remap = false)
public abstract class YaclTextScaledButtonWidgetMixin {

	@Shadow(remap = false)
	public float textScale;

	@Inject(method = "extractDefaultLabel", at = @At("HEAD"), cancellable = true, remap = true)
	private void cclive$drawScaledResetLabel(ActiveTextCollector output, CallbackInfo ci) {
		float scale = this.textScale;
		if (scale <= 0.0f || Math.abs(scale - 1.0f) < 0.01f) {
			return;
		}

		TextScaledButtonWidget self = (TextScaledButtonWidget) (Object) this;
		Component message = self.getMessage();
		Font font = Minecraft.getInstance().font;
		float textWidth = font.width(message);
		float textHeight = font.lineHeight;

		float drawX = self.getX() + (self.getWidth() - textWidth * scale) / 2.0f;
		float drawY = self.getY() + (self.getHeight() - textHeight * scale) / 2.0f;

		ActiveTextCollector.Parameters base = output.defaultParameters();
		Matrix3x2f pose = new Matrix3x2f(base.pose());
		pose.translate(drawX, drawY);
		pose.scale(scale, scale);

		output.accept(TextAlignment.LEFT, 0, 0, base.withPose(pose), message);
		ci.cancel();
	}
}
