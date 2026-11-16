package net.felix.features.information.essences;


/**
 * Data class to store essence information
 */
public class EssenceInfo {
	/** {@code null} = unknown wave, display {@code "?"}. */
	public final Integer wave;
	
	public EssenceInfo(Integer wave) {
		this.wave = wave;
	}
}
