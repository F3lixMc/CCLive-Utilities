package net.felix.features.information.mklevel;


/**
 * Data class to represent a single line to render
 */
public class RenderLine {
	public final String text;
	public final int color;
	public final int xOffset; // Additional X offset for alignment
	public final boolean hasSecondPart; // If true, has a second part with different color
	public final String secondPartText;
	public final int secondPartColor;
	public final int secondPartXOffset; // X offset for second part
	public final boolean hasThirdPart; // If true, has a third part (for wave numbers in green)
	public final String thirdPartText;
	public final int thirdPartColor;
	public final int thirdPartXOffset; // X offset for third part
	
	public RenderLine(String text, int color) {
		this.text = text;
		this.color = color;
		this.xOffset = 0;
		this.hasSecondPart = false;
		this.secondPartText = null;
		this.secondPartColor = 0;
		this.secondPartXOffset = 0;
		this.hasThirdPart = false;
		this.thirdPartText = null;
		this.thirdPartColor = 0;
		this.thirdPartXOffset = 0;
	}
	
	public RenderLine(String text, int color, int xOffset, String secondPartText, int secondPartColor, int secondPartXOffset) {
		this.text = text;
		this.color = color;
		this.xOffset = xOffset;
		this.hasSecondPart = true;
		this.secondPartText = secondPartText;
		this.secondPartColor = secondPartColor;
		this.secondPartXOffset = secondPartXOffset;
		this.hasThirdPart = false;
		this.thirdPartText = null;
		this.thirdPartColor = 0;
		this.thirdPartXOffset = 0;
	}
	
	public RenderLine(String text, int color, int xOffset, String secondPartText, int secondPartColor, int secondPartXOffset, String thirdPartText, int thirdPartColor, int thirdPartXOffset) {
		this.text = text;
		this.color = color;
		this.xOffset = xOffset;
		this.hasSecondPart = true;
		this.secondPartText = secondPartText;
		this.secondPartColor = secondPartColor;
		this.secondPartXOffset = secondPartXOffset;
		this.hasThirdPart = true;
		this.thirdPartText = thirdPartText;
		this.thirdPartColor = thirdPartColor;
		this.thirdPartXOffset = thirdPartXOffset;
	}
}
