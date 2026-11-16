package net.felix.utilities.Community.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.felix.CCLiveUtilities;

/**
 * Lokale Bestätigung der CCLive-Chatregeln, getrennt nach Minecraft-Konto und bestätigter Season.
 * Ein fehlender Season-Wert ist kein Wechsel und lässt eine vorhandene Bestätigung bestehen.
 */
public final class ChatRulesAcceptance {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ChatRulesAcceptance shared;

    private final Path file;
    private final Map<String, Integer> accounts = new LinkedHashMap<>();
    private String draft;

    private ChatRulesAcceptance(Path file) {
        this.file = file;
        load();
    }

    public static ChatRulesAcceptance shared() {
        ChatRulesAcceptance current = shared;
        if (current == null) {
            synchronized (ChatRulesAcceptance.class) {
                current = shared;
                if (current == null) {
                    Path path = CCLiveUtilities.getConfigDir()
                        .resolve("cclive-utilities")
                        .resolve("chat-rules-accepted.json");
                    current = new ChatRulesAcceptance(path);
                    shared = current;
                }
            }
        }
        return current;
    }

    public static ChatRulesAcceptance load(Path file) {
        return new ChatRulesAcceptance(file);
    }

    /** Nur CCLive-Schreibziel und nichtleeres {@code @cclive}. Andere Chats und Befehle bleiben frei. */
    public static boolean isRulesGatedSend(String trimmed, boolean ccliveWriteMode) {
        if (trimmed == null) {
            return false;
        }
        String text = trimmed.trim();
        if (text.isEmpty() || text.startsWith("/") || text.startsWith("@lp") || text.startsWith("@global")) {
            return false;
        }
        if (text.startsWith("@cclive")) {
            return !text.substring("@cclive".length()).trim().isEmpty();
        }
        return ccliveWriteMode;
    }

    /**
     * Gespeicherte Season gilt weiter, solange keine neue bestätigte Season vorliegt.
     * Ohne gespeicherte Bestätigung bleibt der Sendeversuch gesperrt.
     */
    public static boolean covers(Integer storedSeason, Integer currentSeason) {
        if (storedSeason == null) {
            return false;
        }
        return currentSeason == null || storedSeason.equals(currentSeason);
    }

    public static boolean canStore(boolean rulesLoaded, String accountId, Integer seasonId) {
        return rulesLoaded && accountId != null && !accountId.isBlank() && seasonId != null;
    }

    public boolean isAccepted(String accountId, Integer currentSeason) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        Integer stored;
        synchronized (accounts) {
            stored = accounts.get(accountId);
        }
        return covers(stored, currentSeason);
    }

    public boolean shouldHold(String accountId, Integer currentSeason) {
        return !isAccepted(accountId, currentSeason);
    }

    public boolean accept(String accountId, Integer seasonId, boolean rulesLoaded) {
        if (!canStore(rulesLoaded, accountId, seasonId)) {
            return false;
        }
        synchronized (accounts) {
            accounts.put(accountId, seasonId);
        }
        save();
        return true;
    }

    public void hold(String text) {
        draft = text;
    }

    public String draft() {
        return draft;
    }

    public void clearDraft() {
        draft = null;
    }

    private void load() {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("accounts") || !root.get("accounts").isJsonObject()) {
                return;
            }
            synchronized (accounts) {
                accounts.clear();
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("accounts").entrySet()) {
                    Integer season = readSeason(entry.getValue());
                    if (entry.getKey() != null && !entry.getKey().isBlank() && season != null) {
                        accounts.put(entry.getKey(), season);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        JsonObject accountsJson = new JsonObject();
        synchronized (accounts) {
            for (Map.Entry<String, Integer> entry : accounts.entrySet()) {
                accountsJson.addProperty(entry.getKey(), entry.getValue());
            }
        }
        JsonObject root = new JsonObject();
        root.add("accounts", accountsJson);
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Integer readSeason(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            int season = element.getAsInt();
            return season < 0 ? null : season;
        } catch (Exception e) {
            return null;
        }
    }
}
