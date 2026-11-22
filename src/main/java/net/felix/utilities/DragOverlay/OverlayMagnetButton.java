package net.felix.utilities.DragOverlay;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.dock.OverlayDockStore;
import net.felix.utilities.Overall.language.ModTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/** 20×20-Schalter direkt rechts neben „Alles zurücksetzen“. */
public final class OverlayMagnetButton extends AbstractWidget {
	private static final Identifier ICON = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/magnet.png");
	private static final int ACTIVE = 0xA036539E;
	private static final int INACTIVE = 0xA0000000;
	private static final int OUTLINE = 0xFFFFFFFF;

	public OverlayMagnetButton(int x, int y) {
		super(x, y, 20, 20, ModTexts.text("cclive.f6.magnet"));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !isMouseOver(event.x(), event.y())) {
			return false;
		}
		OverlayDockStore.setMagnetEnabled(!OverlayDockStore.isMagnetEnabled());
		CCLiveUtilitiesConfig.HANDLER.save();
		return true;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int fill = OverlayDockStore.isMagnetEnabled() ? ACTIVE : INACTIVE;
		graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
		graphics.outline(getX(), getY(), width, height, OUTLINE);
		Matrix3x2fStack matrices = graphics.pose();
		matrices.pushMatrix();
		matrices.translate(getX() + 2, getY() + 2);
		matrices.scale(16f / 24f, 16f / 24f);
		graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, 0, 0, 0.0f, 0.0f, 24, 24, 24, 24);
		matrices.popMatrix();
		if (isHovered()) {
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				graphics.setComponentTooltipForNextFrame(client.font, List.of(
						ModTexts.text("cclive.f6.magnet"),
						ModTexts.text("cclive.f6.magnet_keep")
				), mouseX, mouseY);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
