package net.felix.utilities.Aincraft.information.mklevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to store Combined Wave information
 */
public class CombinedWaveInfo {
	public final String level; // e.g., "1-13", "25+30"
	public final int wave;
	public final List<String> without; // Optional list of essences to exclude
	
	public CombinedWaveInfo(String level, int wave, List<String> without) {
		this.level = level;
		this.wave = wave;
		this.without = without != null ? without : new ArrayList<>();
	}
}
