package net.felix.utilities.Town;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class StarForgedSoundUtility {

	private static final String DEBUG_PREFIX = "[StarForgedSound-DEBUG]";
	private static final long DUPLICATE_COOLDOWN_MS = 500L;

	private static String lastTriggeredMessage = "";
	private static long lastTriggeredAtMs = 0L;

	public static void handleIncomingMessage(Text message, boolean overlay) {
		if (isDebugEnabled()) {
			String preview = message != null ? truncate(message.getString(), 160) : "null";
			debug("Paket empfangen | overlay=" + overlay + " | raw='" + preview + "'");
		}

		if (overlay) {
			debugSkip("Nachricht ist Overlay/Actionbar (overlay=true)");
			return;
		}

		if (!CCLiveUtilitiesConfig.HANDLER.instance().starForgedSoundEnabled) {
			debugSkip("Schmiedezustand-Sound in den Einstellungen deaktiviert");
			return;
		}

		if (message == null) {
			debugSkip("Nachrichtentext ist null");
			return;
		}

		String normalized = normalizeMessageText(message);
		if (isDebugEnabled()) {
			debug("Normalisierter Text: '" + truncate(normalized, 160) + "'");
			debug("Matcher | Schmiedezustand erhalten=" + normalized.contains("Schmiedezustand erhalten")
					+ " | Sternengeschmiedet=" + normalized.contains("Sternengeschmiedet")
					+ " | Starforged=" + normalized.contains("Starforged"));
		}

		if (!isStarForgedSmithingMessage(normalized)) {
			debugSkip("Nachricht entspricht nicht dem Sternengeschmiedet-Muster");
			return;
		}

		long now = System.currentTimeMillis();
		if (normalized.equals(lastTriggeredMessage) && now - lastTriggeredAtMs < DUPLICATE_COOLDOWN_MS) {
			debugSkip("Duplikat innerhalb von " + DUPLICATE_COOLDOWN_MS + "ms übersprungen");
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			debugSkip("MinecraftClient ist null");
			return;
		}
		if (client.player == null) {
			debugSkip("Spieler ist null (nicht in einer Welt?)");
			return;
		}

		lastTriggeredMessage = normalized;
		lastTriggeredAtMs = now;

		debug("Treffer erkannt – Sound wird auf den Client-Thread geplant");
		client.execute(StarForgedSoundUtility::playStarForgedSound);
	}

	private static void playStarForgedSound() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			debugSkip("playStarForgedSound: MinecraftClient ist null");
			return;
		}
		if (client.player == null) {
			debugSkip("playStarForgedSound: Spieler ist null");
			return;
		}

		boolean registryLoaded = Registries.SOUND_EVENT.containsId(ModSounds.STARFORGED_ID);
		WeightedSoundSet soundSet = client.getSoundManager().get(ModSounds.STARFORGED_ID);
		boolean soundDataLoaded = soundSet != null && soundSet.getWeight() > 0;

		if (isDebugEnabled()) {
			debug("Sound-Registry | id=" + ModSounds.STARFORGED_ID + " | registriert=" + registryLoaded);
			debug("SoundManager | soundSet=" + (soundSet != null ? "vorhanden" : "null")
					+ " | weight=" + (soundSet != null ? soundSet.getWeight() : 0)
					+ " | masterVolume=" + client.options.getSoundVolume(net.minecraft.sound.SoundCategory.MASTER));
		}

		if (!registryLoaded) {
			debugSkip("Sound-Event nicht in der Registry: " + ModSounds.STARFORGED_ID);
			return;
		}

		if (!soundDataLoaded) {
			debugSkip("Sound-Datei nicht geladen (sounds.json/ogg fehlt oder noch nicht geladen): " + ModSounds.STARFORGED_ID);
			return;
		}

		client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.STARFORGED, 1.0f, 1.0f));
		debug("Sound abgespielt: " + ModSounds.STARFORGED_ID);
	}

	private static String normalizeMessageText(Text message) {
		return message.getString()
				.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
				.trim();
	}

	private static boolean isStarForgedSmithingMessage(String messageText) {
		if (messageText == null || messageText.isEmpty()) {
			return false;
		}

		return messageText.contains("Schmiedezustand erhalten")
				&& (messageText.contains("Sternengeschmiedet") || messageText.contains("Starforged"));
	}

	private static boolean isDebugEnabled() {
		return CCLiveUtilitiesConfig.HANDLER.instance().starForgedSoundDebugging;
	}

	private static void debug(String message) {
		if (isDebugEnabled()) {
			System.out.println(DEBUG_PREFIX + " " + message);
		}
	}

	private static void debugSkip(String reason) {
		if (isDebugEnabled()) {
			System.out.println(DEBUG_PREFIX + " Übersprungen: " + reason);
		}
	}

	private static String truncate(String text, int maxLength) {
		if (text == null) {
			return "null";
		}
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}
}
