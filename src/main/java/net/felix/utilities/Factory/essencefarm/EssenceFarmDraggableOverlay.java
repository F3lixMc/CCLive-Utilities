package net.felix.utilities.Factory.essencefarm;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * F6 frame for the essence farm planner. Height follows the current line count.
 */
public class EssenceFarmDraggableOverlay implements DraggableOverlay {

	@Override
	public String getOverlayName() {
		return "Essenzen-Farmplan";
	}

	@Override
	public String dockId() {
		return net.felix.utilities.DragOverlay.dock.model.OverlayDockIds.ESSENCE;
	}

	@Override
	public int undockedX() {
		return EssenceFarmTracker.x();
	}

	@Override
	public int undockedY() {
		return EssenceFarmTracker.y();
	}

	@Override
	public int getX() {
		return net.felix.utilities.DragOverlay.dock.OverlayDockLayout.x(this);
	}

	@Override
	public int getY() {
		return net.felix.utilities.DragOverlay.dock.OverlayDockLayout.y(this);
	}

	@Override
	public int getWidth() {
		return (int) (EssenceFarmTracker.unscaledWidth() * EssenceFarmTracker.scale());
	}

	@Override
	public int getHeight() {
		return (int) (EssenceFarmTracker.unscaledHeight() * EssenceFarmTracker.scale());
	}

	@Override
	public void setPosition(int x, int y) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		config.essenceFarmX = x;
		config.essenceFarmY = y;
	}

	@Override
	public void setSize(int width, int height) {
		int unscaledWidth = EssenceFarmTracker.unscaledWidth();
		int unscaledHeight = EssenceFarmTracker.unscaledHeight();
		if (unscaledWidth <= 0 || unscaledHeight <= 0) {
			return;
		}
		float scale = ((float) width / unscaledWidth + (float) height / unscaledHeight) / 2.0f;
		CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmScale = Math.max(0.1f, Math.min(5.0f, scale));
	}

	@Override
	public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.outline(getX(), getY(),
				net.felix.utilities.DragOverlay.dock.OverlayDockLayout.shownWidth(this),
				net.felix.utilities.DragOverlay.dock.OverlayDockLayout.shownHeight(this),
				0xFFFF0000);
		EssenceFarmTracker.render(context);
	}

	@Override
	public void savePosition() {
		CCLiveUtilitiesConfig.HANDLER.save();
	}

	@Override
	public boolean isEnabled() {
		return CCLiveUtilitiesConfig.isModRuntimeEnabled() && isConfigEnabled() && EssenceFarmTracker.isInFactory();
	}

	@Override
	public boolean isConfigEnabled() {
		return EssenceFarmTracker.isConfigEnabled();
	}

	@Override
	public Component getTooltip() {
		return Component.literal("Essenzen-Farmplan: beste Wellen für die gewählten Machtkristalle");
	}

	@Override
	public void resetToDefault() {
		net.felix.utilities.DragOverlay.dock.OverlayDockStore.removeLinksInvolving(dockId());
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		config.essenceFarmX = 8;
		config.essenceFarmY = 120;
		config.essenceFarmScale = 1.0f;
	}

	@Override
	public void resetSizeToDefault() {
		CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmScale = 1.0f;
	}
}
