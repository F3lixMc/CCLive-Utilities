package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.InformationenUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest Fisch-Belohnungen aus Server-Chatnachrichten und addiert Materialien
 * zur persistenten Clipboard-Materialien-Liste.
 *
 * <p>Beispiel:
 * {@code [Legend] Du hast folgende Belohnungen für den Fisch erhalten:
 *  - x10.356 Münzen
 *  - x1 Gewöhnliches Quallenfleisch}
 */
public final class FishingRewardChatCollector {

    private static final String FISH_REWARD_MARKER = "Belohnungen für den Fisch";
    private static final Pattern REWARD_LINE_PATTERN = Pattern.compile(
        "(?:-\\s*)?x(\\d+(?:\\.\\d{3})*(?:,\\d+)?)\\s+(.+)",
        Pattern.CASE_INSENSITIVE);

    private FishingRewardChatCollector() {
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
        if (clean.isEmpty()) {
            return;
        }

        if (!clean.contains(FISH_REWARD_MARKER) && !REWARD_LINE_PATTERN.matcher(clean).find()) {
            return;
        }

        Map<String, Long> materialDeltas = new HashMap<>();
        Matcher matcher = REWARD_LINE_PATTERN.matcher(clean);
        while (matcher.find()) {
            long amount = parseAmount(matcher.group(1));
            String name = cleanMaterialName(matcher.group(2));
            if (amount <= 0 || name.isEmpty() || isCoinsName(name)) {
                continue;
            }
            if (isClipboardMaterial(name)) {
                materialDeltas.merge(name, amount, Long::sum);
            }
        }

        if (!materialDeltas.isEmpty()) {
            CollectedMaterialsResourcesStorage.addMaterials(materialDeltas);
        }
    }

    private static boolean isClipboardMaterial(String name) {
        return InformationenUtility.getMaterialFloorInfo(name) != null
            || ItemInfoUtility.isFishingRarityMaterial(name);
    }

    private static boolean isCoinsName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return "coins".equalsIgnoreCase(trimmed) || "münzen".equalsIgnoreCase(trimmed);
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
        return stripFormatting(name).trim();
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
