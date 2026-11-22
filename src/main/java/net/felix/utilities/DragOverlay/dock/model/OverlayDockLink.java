package net.felix.utilities.DragOverlay.dock.model;

import dev.isxander.yacl3.config.v2.api.SerialEntry;

/**
 * Eine gespeicherte Andockkante. Gson liest die öffentlichen Felder.
 * {@code alignFromRight} gilt für TOP/BOTTOM und hält die rechte Kante, wenn das Overlay
 * beim Loslassen rechts der Bildschirmmitte lag.
 */
public class OverlayDockLink {
	@SerialEntry
	public String attachedId;
	@SerialEntry
	public String anchorId;
	@SerialEntry
	public OverlayDockSide side;
	@SerialEntry
	public int alignOffset;
	@SerialEntry
	public boolean alignFromRight;

	public OverlayDockLink() {
	}

	public OverlayDockLink(String attachedId, String anchorId, OverlayDockSide side, int alignOffset, boolean alignFromRight) {
		this.attachedId = attachedId;
		this.anchorId = anchorId;
		this.side = side;
		this.alignOffset = alignOffset;
		this.alignFromRight = alignFromRight;
	}

	public boolean isStructural() {
		return attachedId != null && !attachedId.isBlank()
				&& anchorId != null && !anchorId.isBlank()
				&& !attachedId.equals(anchorId)
				&& side != null;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof OverlayDockLink link)) {
			return false;
		}
		return java.util.Objects.equals(attachedId, link.attachedId)
				&& java.util.Objects.equals(anchorId, link.anchorId)
				&& side == link.side
				&& alignOffset == link.alignOffset
				&& alignFromRight == link.alignFromRight;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(attachedId, anchorId, side, alignOffset, alignFromRight);
	}
}
