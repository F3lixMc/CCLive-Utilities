package net.felix.utilities.Overall.itemviewer.tooltip;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Zeichnet einen bereits vermessenen, verkleinerten Item-Viewer-Tooltip
 * am Ende des verzögerten Tooltip-Durchlaufs.
 * Hintergrund und Rahmen liegen in der Endgröße, damit die Neunteilung jede Kante behält.
 */
public final class ItemTooltipPresenter {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("tooltip/background");
	private static final Identifier FRAME = Identifier.withDefaultNamespace("tooltip/frame");

	private static List<ClientTooltipComponent> pending;
	private static ItemTooltipFit.Placement placement;

	private ItemTooltipPresenter() {
	}

	public static void schedule(List<ClientTooltipComponent> components, ItemTooltipFit.Placement fitted) {
		pending = components;
		placement = fitted;
	}

	public static void render(GuiGraphicsExtractor context) {
		List<ClientTooltipComponent> components = pending;
		ItemTooltipFit.Placement fitted = placement;
		pending = null;
		placement = null;
		if (components == null || fitted == null || context == null || fitted.vanilla()) {
			return;
		}
		if (net.felix.utilities.Overall.itemviewer.ui.FilterOverlayControls.isOverlayOpen()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return;
		}
		context.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, fitted.outerX(), fitted.outerY(), fitted.outerWidth(), fitted.outerHeight());
		context.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, fitted.outerX(), fitted.outerY(), fitted.outerWidth(), fitted.outerHeight());
		int guiScale = client.getWindow() == null || client.getWindow().getGuiScale() <= 0
				? 1
				: client.getWindow().getGuiScale();
		drawText(context, client.font, components, fitted, guiScale);
	}

	private static void drawText(GuiGraphicsExtractor context, Font font, List<ClientTooltipComponent> components,
			ItemTooltipFit.Placement fitted, int guiScale) {
		int contentWidth = 0;
		int textHeight = 0;
		for (ClientTooltipComponent component : components) {
			contentWidth = Math.max(contentWidth, component.getWidth(font));
			textHeight += component.getHeight(font);
		}
		int pad = ItemTooltipFit.BACKGROUND_OUTSET;
		int innerWidth = fitted.outerWidth() - pad * 2;
		int innerHeight = fitted.outerHeight() - pad * 2;
		if (contentWidth < 1 || textHeight < 1 || innerWidth < 1 || innerHeight < 1) {
			return;
		}
		boolean fills = contentWidth * fitted.scale() <= innerWidth + 1f
				&& textHeight * fitted.scale() <= innerHeight + 1f;
		float textScale = fills
				? fitted.scale()
				: ItemTooltipFit.screenPixelScale(
						Math.min(innerWidth / (float) contentWidth, innerHeight / (float) textHeight), guiScale);
		if (textScale <= 0f) {
			return;
		}
		float rawX = fitted.outerX() + (fitted.outerWidth() - contentWidth * textScale) / 2f;
		float rawY = fitted.outerY() + (fitted.outerHeight() - textHeight * textScale) / 2f;
		int screenX = Math.round(rawX * guiScale);
		int screenY = Math.round(rawY * guiScale);
		var matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(screenX / (float) guiScale, screenY / (float) guiScale);
		matrices.scale(textScale, textScale);
		int y = 0;
		for (ClientTooltipComponent component : components) {
			component.extractText(context, font, 0, y);
			component.extractImage(font, 0, y, contentWidth, textHeight, context);
			y += component.getHeight(font);
		}
		matrices.popMatrix();
	}
}
