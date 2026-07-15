package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.InformationenUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest Schatz-Funde aus Server-Chatnachrichten und addiert Materialien
 * zur persistenten Clipboard-Materialien-Liste.
 *
 * <p>Beispiel:
 * {@code [Legend] Du hast einen Schatz gefunden: 45 Prismarine-Splitter}
 *
 * <p>Chat-Varianten wie {@code Prismarine-*} werden auf die kanonischen
 * {@code Prismarin-*}-Namen gemappt.
 */
public final class TreasureFoundChatCollector {

    private static final String TREASURE_MARKER = "Du hast einen Schatz gefunden:";
    private static final Pattern TREASURE_PATTERN = Pattern.compile(
            "Du hast einen Schatz gefunden:\\s*(\\d+(?:\\.\\d{3})*(?:,\\d+)?)\\s+(.+)",
            Pattern.CASE_INSENSITIVE);

    private TreasureFoundChatCollector() {
    }

    public static void initialize() {
        CollectedMaterialsResourcesStorage.initialize();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (message != null) {
                processChatMessage(message.getString());
            }
        });
    }

    public static void processChatMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return;
        }

        String clean = stripFormatting(messageText);
        if (clean.isEmpty() || !clean.contains(TREASURE_MARKER)) {
            return;
        }

        Matcher matcher = TREASURE_PATTERN.matcher(clean);
        if (!matcher.find()) {
            return;
        }

        long amount = parseAmount(matcher.group(1));
        String rawName = cleanMaterialName(matcher.group(2));
        if (amount <= 0 || rawName.isEmpty()) {
            return;
        }

        String materialName = resolveMaterialName(rawName);
        if (materialName == null || !isClipboardMaterial(materialName)) {
            return;
        }

        Map<String, Long> materialDeltas = new HashMap<>();
        materialDeltas.put(materialName, amount);
        CollectedMaterialsResourcesStorage.addMaterials(materialDeltas);
    }

    /**
     * Prismarin-Varianten auf kanonischen Namen mappen; sonst bereinigter Rohname.
     */
    private static String resolveMaterialName(String rawName) {
        String prismarin = InformationenUtility.resolvePrismarinMaterialName(rawName);
        if (prismarin != null) {
            return prismarin;
        }
        return rawName;
    }

    private static boolean isClipboardMaterial(String name) {
        return InformationenUtility.isPrismarinMaterial(name)
                || InformationenUtility.getMaterialFloorInfo(name) != null
                || ItemInfoUtility.isFishingRarityMaterial(name);
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                .trim();
    }

    private static String cleanMaterialName(String name) {
        if (name == null) {
            return "";
        }
        return stripFormatting(name).replaceAll("[.!]+$", "").trim();
    }

    private static long parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return 0L;
        }
        try {
            String normalized = amountStr.replace(".", "").replace(",", "").trim();
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
