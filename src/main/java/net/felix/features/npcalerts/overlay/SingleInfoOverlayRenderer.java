package net.felix.features.npcalerts.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.features.npcalerts.overlay.AlertIcons.AMBOSS_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.ESSENZEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.FORSCHUNG_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.JAEGER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.KOMBO_KISTE_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.MACHTKRISTALL_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.RECYCLER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SCHMELZOFEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SEELEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertSettings.getPercentColorForConfigKey;
import static net.felix.features.npcalerts.overlay.AlertSettings.getSeparateOverlayScale;
import static net.felix.features.npcalerts.overlay.AlertSettings.getTextColorForConfigKey;

/**
 * Zeichnet ein einzelnes Info-Overlay (Icon, Text, Prozent).
 */
public final class SingleInfoOverlayRenderer {

	private SingleInfoOverlayRenderer() {
	}

	/**
	 * Rendert ein einzelnes Overlay für eine Information (mit Icon-Support und Hintergrund-Option)
	 */
	static void renderSingleInfoOverlay(GuiGraphicsExtractor context, Minecraft client, 
		String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
		boolean showIcon, int xPosition, int yPosition, boolean showBackground) {
		final int PADDING = 5;
		final int LINE_HEIGHT = client.font.lineHeight + 2;
		// Hole konfigurierte Farben
		int textColor = getTextColorForConfigKey(configKey);
		int percentColor = getPercentColorForConfigKey(configKey);
		
		// Berechne Breite
		int width;
		if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
		                                        "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
		                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || "komboKiste".equals(configKey) || 
		                                        "machtkristalle".equals(configKey) ||
		                                        "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
		                                        "machtkristalleSlot3".equals(configKey) ||
		                                        "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
		                                        "recyclerSlot3".equals(configKey)))) {
			int iconSize = (int)(client.font.lineHeight * 1.5);
			width = iconSize + 2; // Icon + Abstand
			width += client.font.width(": "); // Doppelpunkt nach Icon
			width += client.font.width(text); // Werte nach dem Doppelpunkt
		} else {
			width = client.font.width(text);
		}
		if (showPercent && percentText != null) {
			width += client.font.width(" " + percentText);
		}
		// Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
		int actualLineHeight = LINE_HEIGHT;
		if (showIcon && configKey != null) {
			int iconSize = (int)(client.font.lineHeight * 1.5);
			// Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
			actualLineHeight = Math.max(actualLineHeight, iconSize);
		}
		
		// Berechne unskalierte Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		int unscaledWidth = width + (PADDING * 2);
		int unscaledHeight = actualLineHeight + (PADDING * 2);
		
		// Get scale
		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = xPosition;
		
		// Determine if overlay is on left or right side of screen
		int screenWidth = client.getWindow().getGuiScaledWidth();
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = yPosition;
		
		// Zeichne Hintergrund (wenn aktiviert) - skaliert
		if (showBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne Text - vertikal zentriert (actualLineHeight wurde bereits berechnet)
		int currentX = PADDING; // Relativ zu (posX, posY) nach Matrix-Transformation
		// Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der fontHeight (da Text-Baseline unten ist)
		// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
		int overlayCenterY = unscaledHeight / 2;
		int currentY = overlayCenterY - client.font.lineHeight / 2;
		
		// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
		int currentTextColor = textColor;
		if (showWarning) {
			// Blink-Animation: alle 300ms wechseln
			boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
			if (isVisible) {
				currentTextColor = 0xFFFF0000; // Rot
			} else {
				currentTextColor = textColor;
			}
		}
		
		try {
			// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
			if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
			                                        "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
			                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || "komboKiste".equals(configKey) || 
			                                        "machtkristalle".equals(configKey) ||
			                                        "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
			                                        "machtkristalleSlot3".equals(configKey) ||
			                                        "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
			                                        "recyclerSlot3".equals(configKey)))) {
				int iconSize = (int)(client.font.lineHeight * 1.5);
				// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
				int iconY = overlayCenterY - iconSize / 2;
				Identifier iconToUse = null;
				String fallbackText = null;
				
				if ("forschung".equals(configKey)) {
					iconToUse = FORSCHUNG_ICON;
					fallbackText = "Forschung: ";
				} else if ("amboss".equals(configKey)) {
					iconToUse = AMBOSS_ICON;
					fallbackText = "Amboss: ";
				} else if ("schmelzofen".equals(configKey)) {
					iconToUse = SCHMELZOFEN_ICON;
					fallbackText = "Schmelzofen: ";
				} else if ("seelen".equals(configKey)) {
					iconToUse = SEELEN_ICON;
					fallbackText = "Seelen: ";
				} else if ("essenzen".equals(configKey)) {
					iconToUse = ESSENZEN_ICON;
					fallbackText = "Essenzen: ";
				} else if ("jaeger".equals(configKey)) {
					iconToUse = JAEGER_ICON;
					fallbackText = "Jäger: ";
				} else if ("komboKiste".equals(configKey)) {
					iconToUse = KOMBO_KISTE_ICON;
					fallbackText = "Kombo Kiste: ";
				} else if ("recyclerSlot1".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 1: ";
				} else if ("recyclerSlot2".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 2: ";
				} else if ("recyclerSlot3".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 3: ";
				} else if ("machtkristalle".equals(configKey) || "machtkristalleSlot1".equals(configKey) || 
				           "machtkristalleSlot2".equals(configKey) || "machtkristalleSlot3".equals(configKey)) {
					iconToUse = MACHTKRISTALL_ICON;
					fallbackText = "MK: ";
				}
				
				if (iconToUse != null) {
					int textYForIcon = currentY; // Standard Y-Position für Text
					try {
						context.blit(
							RenderPipelines.GUI_TEXTURED,
							iconToUse,
							currentX, iconY,
							0.0f, 0.0f,
							iconSize, iconSize,
							iconSize, iconSize
						);
						currentX += iconSize + 2; // Abstand nach Icon
						// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
						textYForIcon = overlayCenterY - client.font.lineHeight / 2;
					} catch (Exception e) {
						// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
						if (fallbackText != null) {
							context.text(client.font, Component.literal(fallbackText), currentX, currentY, currentTextColor, true);
							currentX += client.font.width(fallbackText);
						}
					}
					// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
					context.text(client.font, Component.literal(": "), currentX, textYForIcon, currentTextColor, true);
					currentX += client.font.width(": ");
					// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
					context.text(client.font, Component.literal(text), currentX, textYForIcon, currentTextColor, true);
					currentX += client.font.width(text);
				} else {
					// Fallback: Zeichne normalen Text
					context.text(client.font, Component.literal(text), currentX, currentY, currentTextColor, true);
					currentX += client.font.width(text);
				}
			} else {
				context.text(client.font, Component.literal(text), currentX, currentY, currentTextColor, true);
				currentX += client.font.width(text);
			}
			
			// Prozente
			// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
			if (showPercent && percentText != null) {
				Component percentComponent = Component.literal(" " + percentText);
				int currentPercentColor = showWarning ? currentTextColor : percentColor;
				// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
				int percentY = (showIcon && configKey != null) ? 
					(overlayCenterY - client.font.lineHeight / 2) : currentY;
				context.text(client.font, percentComponent, currentX, percentY, currentPercentColor, true);
				currentX += client.font.width(" " + percentText);
			}
		} catch (Exception e) {
			// Fallback
			try {
				if (showIcon && configKey != null) {
					int iconSize = (int)(client.font.lineHeight * 1.5);
					// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
					// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
					int fallbackOverlayCenterY = unscaledHeight / 2;
					int iconY = fallbackOverlayCenterY - iconSize / 2;
					Identifier iconToUse = null;
					String fallbackText = null;
					
					if ("forschung".equals(configKey)) {
						iconToUse = FORSCHUNG_ICON;
						fallbackText = "Forschung: ";
					} else if ("amboss".equals(configKey)) {
						iconToUse = AMBOSS_ICON;
						fallbackText = "Amboss: ";
					} else if ("schmelzofen".equals(configKey)) {
						iconToUse = SCHMELZOFEN_ICON;
						fallbackText = "Schmelzofen: ";
					} else if ("seelen".equals(configKey)) {
						iconToUse = SEELEN_ICON;
						fallbackText = "Seelen: ";
					} else if ("essenzen".equals(configKey)) {
						iconToUse = ESSENZEN_ICON;
						fallbackText = "Essenzen: ";
					} else if ("jaeger".equals(configKey)) {
						iconToUse = JAEGER_ICON;
						fallbackText = "Jäger: ";
					} else if ("komboKiste".equals(configKey)) {
						iconToUse = KOMBO_KISTE_ICON;
						fallbackText = "Kombo Kiste: ";
					} else if ("recyclerSlot1".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 1: ";
					} else if ("recyclerSlot2".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 2: ";
					} else if ("recyclerSlot3".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 3: ";
					}
					
					if (iconToUse != null) {
						int fallbackTextYForIcon = currentY; // Standard Y-Position für Text
						try {
							context.blit(
								RenderPipelines.GUI_TEXTURED,
								iconToUse,
								currentX, iconY,
								0.0f, 0.0f,
								iconSize, iconSize,
								iconSize, iconSize
							);
							currentX += iconSize + 2;
							// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
							fallbackTextYForIcon = fallbackOverlayCenterY - client.font.lineHeight / 2;
						} catch (Exception e3) {
							// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
							if (fallbackText != null) {
								context.text(client.font, fallbackText, currentX, currentY, currentTextColor, true);
								currentX += client.font.width(fallbackText);
							}
						}
						// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
						context.text(client.font, ": ", currentX, fallbackTextYForIcon, currentTextColor, true);
						currentX += client.font.width(": ");
						// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
						context.text(client.font, text, currentX, fallbackTextYForIcon, currentTextColor, true);
						currentX += client.font.width(text);
					} else {
						// Fallback: Zeichne normalen Text
						context.text(client.font, text, currentX, currentY, currentTextColor, true);
						currentX += client.font.width(text);
					}
				} else {
					context.text(client.font, text, currentX, currentY, currentTextColor, true);
					currentX += client.font.width(text);
				}
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				if (showPercent && percentText != null) {
					int currentPercentColor = showWarning ? currentTextColor : percentColor;
					// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
					int percentY = (showIcon && configKey != null) ? 
						(unscaledHeight / 2 - client.font.lineHeight / 2) : currentY;
					context.text(client.font, " " + percentText, currentX, percentY, currentPercentColor, true);
					currentX += client.font.width(" " + percentText);
				}
			} catch (Exception e2) {
				// Ignoriere Fehler
			}
		}
		
		matrices.popMatrix();
	}
}
