package net.felix.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemInfo;
import net.felix.utilities.ItemViewer.ItemViewerGrid;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Overall.KeyCategories;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session-Cache + Server-Shares für CCLive-Chat Item-Links (!item / #ITEM:…).
 * Lokaler Item-Cache und Share-ID sind getrennt: Upload-Fail behält das Item lokal.
 */
public final class ItemShareManager {
    private static final Gson GSON = new Gson();
    private static final Pattern ITEM_TOKEN = Pattern.compile("(?i)!item\\b");
    private static final Pattern BUILD_TOKEN = Pattern.compile("(?i)!build\\b");
    private static final Pattern ITEM_SHARE_REF = Pattern.compile("#ITEM:([A-Za-z0-9_-]+)");
    private static final Pattern ANY_SHARE_REF = Pattern.compile("#(ITEM|BUILD):([A-Za-z0-9_-]+)");
    private static final Pattern ANY_SHARE_TOKEN = Pattern.compile("(?i)!(item|build)\\b");

    private static ItemShareManager instance;

    private final HttpClient httpClient;
    private KeyMapping copyItemKeyMapping;

    /** Zuletzt kopiertes Item (bleibt auch bei Upload-Fail). */
    private volatile ItemData localItemCache;
    /** Share-ID vom Server; null wenn Upload fehlgeschlagen / noch nicht hochgeladen. */
    private volatile String currentShareId;

    private final Map<String, ItemData> resolvedCache = new ConcurrentHashMap<>();

    private ItemShareManager() {
        this.httpClient = LeaderboardManager.getInstance().getHttpClient();
    }

    public static ItemShareManager getInstance() {
        if (instance == null) {
            instance = new ItemShareManager();
        }
        return instance;
    }

    public void initialize() {
        copyItemKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cclive-utilities.chat.copy-item",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyCategories.of("cclive-utilities", "chat")
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (copyItemKeyMapping != null && copyItemKeyMapping.consumeClick()) {
                if (client.screen == null) {
                    tryCopyHoveredItem();
                }
            }
        });
    }

    /** Wird aus ScreenMixin aufgerufen (Inventar geöffnet). */
    public boolean handleKeyPress(int keyCode, int scanCode) {
        Minecraft client = Minecraft.getInstance();
        if (copyItemKeyMapping == null) {
            return false;
        }
        if (!copyItemKeyMapping.matches(new KeyEvent(keyCode, scanCode, 0))) {
            return false;
        }
        if (ItemViewerUtility.isTextFieldFocusedPublic(client)) {
            return false;
        }
        tryCopyHoveredItem();
        return true;
    }

    public String getCurrentShareId() {
        return currentShareId;
    }

    public ItemData getLocalItemCache() {
        return localItemCache;
    }

    public boolean hasValidShare() {
        return currentShareId != null && !currentShareId.isEmpty() && localItemCache != null;
    }

    /**
     * Ersetzt !item für CCLive-Sendung durch #ITEM:id, oder liefert Fehlertext.
     * @return null = ok (outMessage gesetzt), sonst Fehlermeldung
     */
    public String prepareCcliveOutgoing(String message, StringBuilder outMessage) {
        if (message == null) {
            return "Nachricht ist leer.";
        }
        Matcher any = ANY_SHARE_TOKEN.matcher(message);
        int shareTokens = 0;
        while (any.find()) {
            shareTokens++;
        }
        if (shareTokens > 1) {
            return "Es kann nur Item/Build pro Nachricht geteilt werden.";
        }
        if (shareTokens == 0) {
            outMessage.append(message);
            return null;
        }

        Matcher itemOnly = ITEM_TOKEN.matcher(message);
        Matcher buildOnly = BUILD_TOKEN.matcher(message);
        boolean hasItem = itemOnly.find();
        boolean hasBuild = buildOnly.find();

        if (hasItem && hasBuild) {
            return "Es kann nur Item/Build pro Nachricht geteilt werden.";
        }

        if (hasBuild) {
            // Phase 1: Token-Ersetzung vorbereitet; Hover/Fake-UI folgt in Phase 2.
            BuildShareManager builds = BuildShareManager.getInstance();
            if (!builds.hasValidShare()) {
                return "Kein Item/Build zum Teilen vorhanden.";
            }
            outMessage.append(BUILD_TOKEN.matcher(message).replaceFirst("#BUILD:" + builds.getCurrentShareId()));
            return null;
        }

        if (!hasItem) {
            return "Kein Item/Build zum Teilen vorhanden.";
        }
        if (!hasValidShare()) {
            return "Kein Item/Build zum Teilen vorhanden.";
        }
        outMessage.append(ITEM_TOKEN.matcher(message).replaceFirst("#ITEM:" + currentShareId));
        return null;
    }

    /** !item und !build → xD in Nicht-CCLive-Chats (alle Vorkommen). */
    public static String replaceItemTokenWithXd(String message) {
        if (message == null) {
            return null;
        }
        String out = ITEM_TOKEN.matcher(message).replaceAll("xD");
        return BUILD_TOKEN.matcher(out).replaceAll("xD");
    }

    public static boolean containsItemOrBuildToken(String message) {
        return message != null && ANY_SHARE_TOKEN.matcher(message).find();
    }

    private void tryCopyHoveredItem() {
        Minecraft client = Minecraft.getInstance();
        ItemData snapshot = ItemViewerUtility.captureHoveredInventoryItemForShare(client);
        if (snapshot == null) {
            debugChat("✗ Kein Item unter dem Cursor (nur Inventar/Chests).");
            sendPlayerFeedback("§cKein Item unter dem Cursor.");
            return;
        }

        localItemCache = snapshot;
        currentShareId = null;
        debugChat("✓ Item lokal gespeichert: " + snapshot.name);
        sendPlayerFeedback("§aItem kopiert: §f" + snapshot.name);

        uploadCurrentItemAsync();
    }

    private void uploadCurrentItemAsync() {
        final ItemData snapshot = localItemCache;
        if (snapshot == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
                    debugChat("✗ Upload fehlgeschlagen (Datentracking aus). Item bleibt lokal.");
                    sendPlayerFeedback("§cUpload fehlgeschlagen (Datentracking aus). Item bleibt lokal.");
                    return;
                }

                LeaderboardManager lb = LeaderboardManager.getInstance();
                if (!lb.ensureRegisteredSync()) {
                    debugChat("✗ Upload fehlgeschlagen (nicht registriert). Item bleibt lokal.");
                    sendPlayerFeedback("§cUpload fehlgeschlagen (nicht registriert). Item bleibt lokal.");
                    return;
                }

                JsonObject body = new JsonObject();
                body.add("data", GSON.toJsonTree(snapshot));

                JsonObject response = httpClient.postWithToken(
                    "/share/item",
                    body,
                    lb.getPlayerToken()
                );

                // Nur übernehmen wenn immer noch dasselbe lokale Item
                if (localItemCache != snapshot) {
                    return;
                }

                if (response != null && response.has("id")) {
                    currentShareId = response.get("id").getAsString();
                    debugChat("✓ Upload erfolgreich: " + currentShareId);
                    sendPlayerFeedback("§aUpload erfolgreich (§f" + currentShareId + "§a)");
                } else {
                    currentShareId = null;
                    debugChat("✗ Upload fehlgeschlagen. Item bleibt lokal.");
                    sendPlayerFeedback("§cUpload fehlgeschlagen. Item bleibt lokal — Hotkey erneut drücken.");
                }
            } catch (Exception e) {
                if (localItemCache == snapshot) {
                    currentShareId = null;
                }
                debugChat("✗ Upload fehlgeschlagen: " + e.getMessage());
                sendPlayerFeedback("§cUpload fehlgeschlagen. Item bleibt lokal.");
            }
        });
    }

    /**
     * Baut die sichtbare Chat-Zeile inkl. Item-Links (async Resolve + 1 Retry nach 3s).
     */
    /** Standardfarbe für CCLive-Chat-Nachrichtentext (#fba9fc). */
    public static final int CCLIVE_MESSAGE_COLOR = 0xFBA9FC;

    public CompletableFuture<Component> buildMessageBodyAsync(String rawMessage) {
        return buildMessageBodyAsync(rawMessage, null);
    }

    public CompletableFuture<Component> buildMessageBodyAsync(String rawMessage, HoverEvent statsHover) {
        return CompletableFuture.supplyAsync(() -> buildMessageBodySync(rawMessage, true, statsHover));
    }

    private Component buildMessageBodySync(String rawMessage, boolean allowRetry, HoverEvent statsHover) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Component.literal("");
        }

        // statsHover absichtlich nicht auf Body-Klartext — nur Prefix/Name/>> (ChatManager)
        Style messageStyle = Style.EMPTY.withColor(CCLIVE_MESSAGE_COLOR);
        Matcher matcher = ANY_SHARE_REF.matcher(rawMessage);
        if (!matcher.find()) {
            return Component.literal(rawMessage).setStyle(messageStyle);
        }

        MutableComponent result = Component.empty();
        int lastEnd = 0;
        matcher.reset();
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.append(Component.literal(rawMessage.substring(lastEnd, matcher.start()))
                    .setStyle(messageStyle));
            }
            String type = matcher.group(1);
            String shareId = matcher.group(2);
            if ("BUILD".equalsIgnoreCase(type)) {
                result.append(BuildShareManager.getInstance().resolveLinkComponent(shareId, allowRetry));
            } else {
                result.append(resolveLinkComponent(shareId, allowRetry));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < rawMessage.length()) {
            result.append(Component.literal(rawMessage.substring(lastEnd)).setStyle(messageStyle));
        }
        return result;
    }

    private Component resolveLinkComponent(String shareId, boolean allowRetry) {
        ItemData data = resolvedCache.get(shareId);
        if (data == null && localItemCache != null && shareId.equals(currentShareId)) {
            data = localItemCache;
            resolvedCache.put(shareId, data);
        }
        if (data == null) {
            data = fetchShareSync(shareId);
            if (data == null && allowRetry) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                data = fetchShareSync(shareId);
            }
            if (data != null) {
                resolvedCache.put(shareId, data);
            }
        }

        if (data == null) {
            HoverEvent failHover = createShowTextHover(
                Component.literal("Item konnte nicht geladen werden.").withStyle(ChatFormatting.RED));
            Style style = Style.EMPTY.withColor(ChatFormatting.GRAY);
            if (failHover != null) {
                style = style.withHoverEvent(failHover);
            } else {
                debugChat("✗ HoverEvent für [?] ist null");
            }
            return Component.literal("[?]").setStyle(style);
        }

        String displayName = data.name != null && !data.name.isEmpty() ? data.name : "Item";
        String rarity = data.info != null ? data.info.rarity : null;
        Component hoverTooltip = buildHoverTooltip(data);
        HoverEvent itemHover = createShowTextHover(hoverTooltip);
        Component namePart = nameFromSnapshot(data);
        // Klammern = gleiche Primärfarbe wie der Anzeigename (nicht dunkleres getChatLinkColor)
        int linkColor = primaryColorFrom(namePart, ItemViewerGrid.getChatLinkColor(data) & 0xFFFFFF);

        debugChat("Link [" + displayName + "] rarity=" + rarity
            + " forging=" + data.forgingCondition
            + " linkColor=0x" + Integer.toHexString(linkColor)
            + " hoverLines=" + (hoverTooltip != null ? hoverTooltip.getString().split("\n", -1).length : 0)
            + " hoverEvent=" + (itemHover != null ? itemHover.getClass().getName() : "null")
            + " action=" + (itemHover != null ? String.valueOf(itemHover.action()) : "-"));

        Style chromeStyle = Style.EMPTY
            .withColor(linkColor)
            .withUnderlined(true)
            .withInsertion(SHARE_INSERTION_PREFIX + shareId);
        if (itemHover != null) {
            chromeStyle = chromeStyle.withHoverEvent(itemHover);
        } else {
            debugChat("✗ HoverEvent ist null — Tooltip wird nicht angezeigt");
        }

        MutableComponent link = Component.literal("[").setStyle(chromeStyle);
        // Name-Styles behalten eigene Farben; Underline/Hover/Insertion vom Chrome
        Style nameChrome = Style.EMPTY
            .withUnderlined(true)
            .withInsertion(SHARE_INSERTION_PREFIX + shareId);
        if (itemHover != null) {
            nameChrome = nameChrome.withHoverEvent(itemHover);
        }
        link.append(applyChromeWithoutColor(namePart, nameChrome));
        link.append(Component.literal("]").setStyle(chromeStyle));

        if (link.getStyle() == null || link.getStyle().getHoverEvent() == null) {
            debugChat("✓ Mehrfarb-Link mit Hover auf Segmenten");
        } else {
            debugChat("✓ Style.hoverEvent gesetzt: " + link.getStyle().getHoverEvent().getClass().getSimpleName());
        }
        return link;
    }

    /**
     * Dominante Textfarbe (längstes farbiges Segment) — [] sollen nicht dunkler
     * als der sichtbare Itemname wirken (getChatLinkColor oft abweichend).
     */
    private static int primaryColorFrom(Component component, int fallback) {
        if (component == null) {
            return fallback;
        }
        int[] best = { fallback, -1 }; // color, length
        component.visit((style, text) -> {
            if (style != null && style.getColor() != null && text != null) {
                int len = text.trim().length();
                if (len > best[1]) {
                    best[0] = style.getColor().getValue() & 0xFFFFFF;
                    best[1] = len;
                }
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return best[0];
    }

    /** Underline/Hover/Insertion auf alle Segmente legen, ohne bestehende Farben zu überschreiben. */
    private static Component applyChromeWithoutColor(Component source, Style chrome) {
        if (source == null) {
            return Component.empty().setStyle(chrome);
        }
        MutableComponent out = source.copy();
        Style existing = out.getStyle() != null ? out.getStyle() : Style.EMPTY;
        Style merged = existing
            .withUnderlined(true)
            .withInsertion(chrome.getInsertion());
        if (chrome.getHoverEvent() != null) {
            merged = merged.withHoverEvent(chrome.getHoverEvent());
        }
        // Farbe aus existing behalten — chrome.color nicht anwenden
        out.setStyle(merged);
        List<Component> siblings = new ArrayList<>(out.getSiblings());
        out.getSiblings().clear();
        for (Component sibling : siblings) {
            out.append(applyChromeWithoutColor(sibling, chrome));
        }
        return out;
    }

    public static final String SHARE_INSERTION_PREFIX = "cclive-share:";

    public ItemData getResolvedItem(String shareId) {
        if (shareId == null || shareId.isEmpty()) {
            return null;
        }
        ItemData cached = resolvedCache.get(shareId);
        if (cached != null) {
            return cached;
        }
        if (localItemCache != null && shareId.equals(currentShareId)) {
            return localItemCache;
        }
        return null;
    }

    /**
     * Aspect-Overlay für gehoverten Chat-Item-Share (Shift + Hover).
     * @return true wenn Overlay aktualisiert wurde
     */
    public boolean tryShowAspectOverlayForShare(Style hoveredStyle) {
        if (BuildShareViewScreen.isActive()) {
            return false;
        }
        if (hoveredStyle == null) {
            return false;
        }
        String insertion = hoveredStyle.getInsertion();
        if (insertion == null || !insertion.startsWith(SHARE_INSERTION_PREFIX)) {
            return false;
        }
        String shareId = insertion.substring(SHARE_INSERTION_PREFIX.length());
        ItemData data = getResolvedItem(shareId);
        if (data == null) {
            // Lazy resolve (sync, kurz)
            data = fetchShareSync(shareId);
            if (data != null) {
                resolvedCache.put(shareId, data);
            }
        }
        if (data == null || data.name == null || data.name.isEmpty()) {
            return false;
        }

        String lookupName = data.name;
        int bracket = lookupName.indexOf('[');
        if (bracket >= 0) {
            lookupName = lookupName.substring(0, bracket).trim();
        }
        lookupName = lookupName.replaceAll("§[0-9a-fk-or]", "").trim();
        if (lookupName.isEmpty()) {
            return false;
        }

        net.felix.utilities.Overall.InformationenUtility.updateAspectOverlayFromBlueprintName(lookupName);
        return true;
    }

    private ItemData fetchShareSync(String shareId) {
        try {
            if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
                return null;
            }
            LeaderboardManager lb = LeaderboardManager.getInstance();
            if (!lb.ensureRegisteredSync()) {
                return null;
            }
            JsonObject response = httpClient.getWithToken(
                "/share/item/" + shareId,
                lb.getPlayerToken()
            );
            if (response == null || !response.has("data")) {
                return null;
            }
            return GSON.fromJson(response.get("data"), ItemData.class);
        } catch (Exception e) {
            debugChat("✗ Resolve fehlgeschlagen für " + shareId + ": " + e.getMessage());
            return null;
        }
    }

    private static Component buildHoverTooltip(ItemData data) {
        if (data.chatTooltip != null && !data.chatTooltip.isEmpty()) {
            List<ChatTooltipLine> displayLines = ChatTooltipLine.filterShareDisplayTooltipLines(data.chatTooltip);
            MutableComponent combined = Component.empty();
            for (int i = 0; i < displayLines.size(); i++) {
                ChatTooltipLine line = displayLines.get(i);
                if (i > 0) {
                    combined.append(Component.literal("\n"));
                }
                if (line == null) {
                    continue;
                }
                String plain = line.plainText();
                if (plain.isEmpty()) {
                    combined.append(Component.literal(" "));
                } else {
                    combined.append(line.toComponent());
                }
            }
            debugChat("Hover aus chatTooltip (" + displayLines.size() + " Zeilen, Share-gefiltert)");
            return combined;
        }

        debugChat("✗ Kein chatTooltip am Snapshot — Fallback Name");
        return nameFromSnapshot(data);
    }

    /**
     * Mehrfarbiger Anzeigename aus chatTooltip (Zeile 0), sonst Klartext + Link-Farbe.
     * Für Item- und Build-Share-Hover — nicht die Einfarb-Link-Farbe erzwingen.
     */
    public static Component nameFromSnapshot(ItemData data) {
        if (data == null) {
            return Component.literal("Item");
        }
        if (data.chatTooltip != null && !data.chatTooltip.isEmpty()) {
            for (ChatTooltipLine line : data.chatTooltip) {
                if (line == null) {
                    continue;
                }
                String plain = line.plainText();
                if (plain == null || plain.trim().isEmpty()) {
                    continue;
                }
                Component formatted = line.toComponent();
                if (formatted != null && !formatted.getString().isEmpty()) {
                    return formatted;
                }
            }
        }
        String plain = data.name != null && !data.name.isEmpty()
            ? ShareTextCleaner.cleanDisplayName(data.name) : "Item";
        int color = ItemViewerGrid.getChatLinkColor(data) & 0xFFFFFF;
        return Component.literal(plain).setStyle(Style.EMPTY.withColor(color));
    }

    /** Für Build-Share und andere Chat-Links. */
    public static HoverEvent createShowTextHoverPublic(Component hoverText) {
        return createShowTextHover(hoverText);
    }

    private static HoverEvent createShowTextHover(Component hoverText) {
        if (hoverText == null) {
            debugChat("✗ createShowTextHover: hoverText ist null");
            return null;
        }
        try {
            HoverEvent event = net.felix.profile.PlayerHoverStatsUtility.createHoverEvent(hoverText);
            if (event != null) {
                debugChat("✓ HoverEvent via PlayerHoverStatsUtility: " + event.getClass().getSimpleName());
                return event;
            }
            debugChat("✗ PlayerHoverStatsUtility.createHoverEvent → null");
        } catch (Exception e) {
            debugChat("✗ PlayerHoverStatsUtility.createHoverEvent Exception: " + e.getMessage());
        }

        // Fallback (ältere/einfache APIs)
        try {
            java.lang.reflect.Method showText = HoverEvent.class.getDeclaredMethod("showText", Component.class);
            showText.setAccessible(true);
            HoverEvent event = (HoverEvent) showText.invoke(null, hoverText);
            if (event != null) {
                debugChat("✓ HoverEvent via HoverEvent.showText");
                return event;
            }
        } catch (Exception e) {
            debugChat("✗ HoverEvent.showText: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        try {
            for (java.lang.reflect.Constructor<?> constructor : HoverEvent.class.getDeclaredConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2 && params[0] == HoverEvent.Action.class) {
                    constructor.setAccessible(true);
                    HoverEvent event = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
                    debugChat("✓ HoverEvent via Konstruktor (param1=" + params[1].getSimpleName() + ")");
                    return event;
                }
            }
            debugChat("✗ Kein passender HoverEvent-Konstruktor gefunden");
        } catch (Exception e) {
            debugChat("✗ HoverEvent-Konstruktor: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        return null;
    }

    /**
     * Snapshot für Chat-Share: Live-Inventar-Tooltip + minimale Template-Metadaten (Rarity/Icon).
     * Hover kommt ausschließlich aus den ausgelesenen Tooltip-Zeilen, nicht aus dem ItemViewer.
     */
    public static ItemData buildSnapshotFromTooltip(
            String displayName,
            List<ChatTooltipLine> tooltipLines,
            ItemData template) {
        ItemData snapshot = new ItemData();

        if (template != null) {
            snapshot.id = template.id;
            snapshot.customModelData = template.customModelData;
            snapshot.category = template.category;
            snapshot.texture = template.texture;
            if (template.info != null) {
                snapshot.info = new ItemInfo();
                snapshot.info.rarity = template.info.rarity;
                snapshot.info.ability = template.info.ability;
                snapshot.info.power_crystal = template.info.power_crystal;
                snapshot.info.type = template.info.type;
                snapshot.info.piece = template.info.piece;
                snapshot.info.blueprint = template.info.blueprint;
            }
        } else {
            snapshot.id = "minecraft:paper";
            snapshot.customModelData = 0;
            snapshot.category = "items";
            snapshot.info = new ItemInfo();
        }

        snapshot.name = displayName != null ? displayName : "Item";
        if (snapshot.info == null) {
            snapshot.info = new ItemInfo();
        }

        // Schmiedegrad aus Anzeigenamen merken (für Chat-Link-Farbe)
        String forgingFromName = ItemViewerGrid.extractForgingConditionFromName(snapshot.name);
        if (forgingFromName != null) {
            snapshot.forgingCondition = forgingFromName;
        } else if (template != null && template.forgingCondition != null) {
            // nur Template-Max, wenn kein Grad im Namen — Chat-Farbe bleibt dann Rarity
            snapshot.forgingCondition = null;
        } else {
            snapshot.forgingCondition = null;
        }

        List<ChatTooltipLine> storedTooltip = new ArrayList<>();
        if (tooltipLines != null) {
            for (ChatTooltipLine line : tooltipLines) {
                if (line != null) {
                    storedTooltip.add(line);
                }
            }
        }
        if (storedTooltip.isEmpty()) {
            ChatTooltipLine nameLine = new ChatTooltipLine();
            ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
            part.text = snapshot.name;
            part.color = ItemViewerGrid.getChatLinkColor(snapshot) & 0xFFFFFF;
            nameLine.parts.add(part);
            storedTooltip.add(nameLine);
        }
        snapshot.chatTooltip = storedTooltip;

        snapshot.info.stats = null;
        snapshot.info.modifier = null;
        snapshot.info.description = null;
        snapshot.price = null;
        snapshot.foundAt = null;
        snapshot.blueprint_shop = null;
        snapshot.tags = null;
        snapshot.jsonObject = null;
        snapshot.itemScore = null;

        return snapshot;
    }

    private static void debugChat(String msg) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().chatDebugging) {
            return;
        }
        System.out.println("[CCLive-Chat] " + msg);
        sendPlayerFeedback("§7[Chat-Debug] " + msg);
    }

    private static void sendPlayerFeedback(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(message));
            }
        });
    }
}
