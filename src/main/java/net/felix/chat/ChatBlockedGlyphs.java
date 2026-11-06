package net.felix.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.felix.CCLiveUtilities;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sperrt Grafikglyphen im vom Spieler geschriebenen CCLive-Text.
 * Kernliste (Mod-Icons) immer aktiv; Pack-Liste nur nach erfolgreichem Font-Reload.
 */
public final class ChatBlockedGlyphs {
    public static final String ERROR_MESSAGE =
        "Nachricht nicht gesendet: Sie enthält ein gesperrtes Sonderzeichen.";
    public static final String ERROR_CODE = "blocked_glyph";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Integer> CORE = coreCodePoints();
    private static final AtomicReference<Set<Integer>> PACK = new AtomicReference<>(Set.of());
    private static final AtomicReference<Set<Integer>> SERVER = new AtomicReference<>(Set.of());

    private ChatBlockedGlyphs() {
    }

    private static Set<Integer> coreCodePoints() {
        Set<Integer> core = new LinkedHashSet<>();
        core.add(Character.codePointAt(String.valueOf(PlayerIconUtility.CHAT_ICON_MARKER), 0));
        core.add(Character.codePointAt(String.valueOf(PlayerIconUtility.ADMIN_CHAT_ICON_MARKER), 0));
        return Collections.unmodifiableSet(core);
    }

    public static boolean containsBlockedGlyph(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        Set<Integer> pack = PACK.get();
        Set<Integer> server = SERVER.get();
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (CORE.contains(codePoint) || pack.contains(codePoint) || server.contains(codePoint)) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }

    public static boolean isBlockedError(String error) {
        return ERROR_MESSAGE.equals(error) || ERROR_CODE.equals(error);
    }

    public static String clientErrorMessage(String serverError) {
        if (isBlockedError(serverError)) {
            return ERROR_MESSAGE;
        }
        return serverError;
    }

    /**
     * Ersetzt die Pack-Liste nur bei erfolgreichem Auslesen.
     * Fehlendes Pack leert die bisherige Liste nicht.
     */
    public static void reloadFromResourceManager(ResourceManager manager) {
        ZeichenUtility.applyBlockedGlyphPackUpdate(manager).ifPresent(codePoints -> {
            PACK.set(Set.copyOf(codePoints));
            writeExportFile();
        });
    }

    public static void replaceServerCodePoints(Set<Integer> codePoints) {
        SERVER.set(codePoints == null ? Set.of() : Set.copyOf(codePoints));
    }

    public static void refreshFromServerAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                var http = LeaderboardManager.getInstance().getHttpClient();
                if (http == null) {
                    return;
                }
                var json = http.get("/chat/blocked-glyphs");
                if (json == null) {
                    return;
                }
                Set<Integer> parsed = parseCodepointsJson(json.toString());
                if (!parsed.isEmpty()) {
                    replaceServerCodePoints(parsed);
                }
            } catch (Exception ignored) {
            }
        });
    }

    /** Tests: Pack-Liste setzen, ohne ResourceManager. */
    public static void replacePackCodePointsForTest(Set<Integer> codePoints) {
        PACK.set(codePoints == null ? Set.of() : Set.copyOf(codePoints));
    }

    public static Set<Integer> snapshotAllBlocked() {
        LinkedHashSet<Integer> all = new LinkedHashSet<>(CORE);
        all.addAll(PACK.get());
        all.addAll(SERVER.get());
        return Collections.unmodifiableSet(all);
    }

    public static Path exportPath() {
        return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("blocked-glyphs-export.json");
    }

    public static Path writeExportFile() {
        try {
            Path path = exportPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, toExportJson(), StandardCharsets.UTF_8);
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    public static String toExportJson() {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("error_code", ERROR_CODE);
        JsonArray sources = new JsonArray();
        sources.add("mod-core (CCLive-Chat-Icons ѳ/Ѳ)");
        sources.add("minecraft:font/default.json bitmap/space mit group, plus Nicht-Vanilla-Referenzen");
        sources.add("cclive-utilities:font/default.json");
        root.add("sources", sources);
        JsonArray codes = new JsonArray();
        List<Integer> sorted = new ArrayList<>(snapshotAllBlocked());
        Collections.sort(sorted);
        for (int codePoint : sorted) {
            codes.add(toUPlus(codePoint));
        }
        root.add("codepoints", codes);
        return GSON.toJson(root);
    }

    public static Set<Integer> parseCodepointsJson(String json) {
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("codepoints") || !root.get("codepoints").isJsonArray()) {
            return out;
        }
        for (var el : root.getAsJsonArray("codepoints")) {
            Integer parsed = parseUPlus(el.getAsString());
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    static String toUPlus(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    static Integer parseUPlus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("U+") || s.startsWith("u+")) {
            s = s.substring(2);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        try {
            int value = Integer.parseInt(s, 16);
            return Character.isValidCodePoint(value) ? value : null;
        } catch (Exception e) {
            return null;
        }
    }
}
