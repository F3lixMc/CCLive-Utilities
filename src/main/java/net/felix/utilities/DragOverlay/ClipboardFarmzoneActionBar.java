package net.felix.utilities.DragOverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.InformationenUtility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest Farmzone-Ressourcen aus der Actionbar ({@code +XXX NAME [GESAMT]}) und speichert
 * {@code NAME} → {@code GESAMT} in {@code collected_materials-ressources.json} (Sektion {@code resources}).
 * Aktiv nur in der Farmzone (Scoreboard-Biom wie beim Collection-Overlay).
 */
public final class ClipboardFarmzoneActionBar {

    /**
     * Optionaler {@code +Zahl}-Prefix, dann Ressourcenname, dann Gesamtmenge in eckigen Klammern.
     * Beispiele: {@code +20 Eichenholz [50]}, {@code 3x Eichenholz [50]}, {@code Eichenholz [50]}
     */
    private static final Pattern FARMZONE_RESOURCE_PATTERN = Pattern.compile(
            "(?:\\+\\d+(?:[.,]\\d{3})*\\s+|\\d+x\\s+)?([^\\[]+?)\\s*\\[(\\d+(?:[.,]\\d{3})*)\\]",
            Pattern.CASE_INSENSITIVE);

    private ClipboardFarmzoneActionBar() {
    }

    public static void initialize() {
        CollectedMaterialsResourcesStorage.initialize();
    }

    /**
     * Verarbeitet eine Actionbar-Nachricht; aktualisiert bei Treffer die persistente JSON.
     *
     * @return true wenn eine Ressourcen-Zeile erkannt und gespeichert wurde
     */
    public static boolean processActionBarMessage(Text message) {
        if (message == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return false;
        }

        InformationenUtility.refreshFarmzoneScoreboardCache(client);
        if (!InformationenUtility.isInFarmzone(client)) {
            return false;
        }

        String raw = message.getString();
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }

        String clean = stripFormatting(raw);
        Matcher matcher = FARMZONE_RESOURCE_PATTERN.matcher(clean);
        if (!matcher.find()) {
            return false;
        }

        String resourceName = cleanResourceName(matcher.group(1));
        if (resourceName.isEmpty()) {
            return false;
        }

        long total = parseAmount(matcher.group(2));
        if (total < 0) {
            return false;
        }

        CollectedMaterialsResourcesStorage.setSyncedOwnedAmount(resourceName, total);
        return true;
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                .trim();
    }

    private static String cleanResourceName(String name) {
        if (name == null) {
            return "";
        }
        return stripFormatting(name).trim();
    }

    private static long parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return -1;
        }
        try {
            String normalized = amountStr.replace(".", "").replace(",", "").trim();
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
