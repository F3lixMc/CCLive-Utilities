package net.felix.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.ItemViewer.ItemViewerGrid;
import net.felix.utilities.Overall.ZeichenUtility;
import net.felix.utilities.Town.BuildSlotMapping;
import net.felix.utilities.Town.BuildSlotMapping.EquipmentCategory;
import net.felix.utilities.Town.BuildSlotMapping.SlotState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Build-Share: Inventar-Erkennung, Session-Zustand, Übersicht, Snapshot, Upload, Chat-Hover.
 */
public final class BuildShareManager {
    private static final Gson GSON = new Gson();
    private static final Identifier ICON =
        Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/icons/buildshare_icon.png");
    /** Basisgröße des Overlay-Icons (doppelt gegenüber früher 18). */
    private static final int ICON_SIZE = 36;
    private static final int PANEL_WIDTH = 168;
    private static final int PANEL_WIDTH_COMPACT = 140;
    private static final int COPY_BUTTON_HEIGHT = 16;
    private static final int CLEAR_BUTTON_HEIGHT = 12;
    private static final int BUTTON_GAP = 3;
    private static final int STABLE_TICKS_REQUIRED = 2;
    private static final int MENU_PAD = 12;
    private static BuildShareManager instance;

    private final HttpClient httpClient;

    private volatile boolean equipmentRecognized;
    private volatile boolean cardsRecognized;
    private volatile boolean modulesScanned;
    private volatile String kitName;
    private final EnumMap<EquipmentCategory, Map<Integer, ItemData>> equipmentByCategory =
        new EnumMap<>(EquipmentCategory.class);
    private final EnumMap<EquipmentCategory, Map<Integer, SlotState>> equipmentStates =
        new EnumMap<>(EquipmentCategory.class);
    /** Paper-/Platzhalter-Namen für leere Slots (z. B. „Helm“). */
    private final EnumMap<EquipmentCategory, Map<Integer, String>> equipmentEmptyLabels =
        new EnumMap<>(EquipmentCategory.class);
    /** LOCKED-Modul-Platzhalter (Darstellung), getrennt von filled items. */
    private final EnumMap<EquipmentCategory, Map<Integer, ItemData>> equipmentLockedPlaceholders =
        new EnumMap<>(EquipmentCategory.class);
    private final Map<Integer, ItemData> cardsBySlot = new ConcurrentHashMap<>();
    private final Map<Integer, SlotState> cardStates = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastEquipmentSignatures = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastCardSignatures = new ConcurrentHashMap<>();

    private volatile String currentShareId;
    private volatile BuildSnapshot localSnapshot;
    private volatile boolean panelExpanded;
    private volatile boolean uploading;
    private volatile boolean cardsDebugDumped;
    private volatile boolean equipmentDebugDumped;

    private final AtomicInteger sessionGeneration = new AtomicInteger(0);
    private AbstractContainerScreen<?> suppressRescanScreen;
    private String lastEquipmentScanHash = "";
    private int equipmentStableTicks;
    private String lastCardsScanHash = "";
    private int cardsStableTicks;

    private int panelScreenX;
    private int panelScreenY;
    private int panelDrawWidth;
    private int panelContentHeight;
    private int panelVisibleHeight;
    private int panelScroll;
    private boolean panelCompact;

    private BuildShareManager() {
        this.httpClient = LeaderboardManager.getInstance().getHttpClient();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            equipmentByCategory.put(category, new ConcurrentHashMap<>());
            equipmentStates.put(category, new ConcurrentHashMap<>());
            equipmentEmptyLabels.put(category, new ConcurrentHashMap<>());
            equipmentLockedPlaceholders.put(category, new ConcurrentHashMap<>());
        }
    }

    public static BuildShareManager getInstance() {
        if (instance == null) {
            instance = new BuildShareManager();
        }
        return instance;
    }

    public void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(this::clearSession));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(this::clearSession));
    }

    public void clearSession() {
        clearLocalCapture(false);
        panelExpanded = false;
        suppressRescanScreen = null;
        debug("Session verworfen (Disconnect/Join).");
    }

    /** Verwirft lokale Erfassung; Server-Shares bleiben. */
    public void clearLocalCapture(boolean suppressUntilMenuClose) {
        sessionGeneration.incrementAndGet();
        equipmentRecognized = false;
        cardsRecognized = false;
        modulesScanned = false;
        kitName = null;
        for (Map<Integer, ItemData> map : equipmentByCategory.values()) {
            map.clear();
        }
        for (Map<Integer, SlotState> map : equipmentStates.values()) {
            map.clear();
        }
        for (Map<Integer, String> map : equipmentEmptyLabels.values()) {
            map.clear();
        }
        for (Map<Integer, ItemData> map : equipmentLockedPlaceholders.values()) {
            map.clear();
        }
        cardsBySlot.clear();
        cardStates.clear();
        lastEquipmentSignatures.clear();
        lastCardSignatures.clear();
        currentShareId = null;
        localSnapshot = null;
        uploading = false;
        cardsDebugDumped = false;
        equipmentDebugDumped = false;
        lastEquipmentScanHash = "";
        equipmentStableTicks = 0;
        lastCardsScanHash = "";
        cardsStableTicks = 0;
        panelScroll = 0;

        if (suppressUntilMenuClose) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.screen instanceof AbstractContainerScreen<?> screen) {
                suppressRescanScreen = screen;
            }
        }
    }

    public boolean isEquipmentRecognized() {
        return equipmentRecognized;
    }

    public boolean isCardsRecognized() {
        return cardsRecognized;
    }

    public boolean canCopy() {
        return equipmentRecognized || cardsRecognized;
    }

    /** True, wenn lokale Erfassung Daten hat, die „Build leeren“ sinnvoll verwerfen kann. */
    public boolean hasClearableData() {
        if (kitName != null && !kitName.isEmpty()) {
            return true;
        }
        if (currentShareId != null && !currentShareId.isEmpty()) {
            return true;
        }
        if (localSnapshot != null) {
            return true;
        }
        if (equipmentRecognized || cardsRecognized || modulesScanned) {
            return true;
        }
        for (EquipmentCategory category : EquipmentCategory.values()) {
            if (getFilledCount(category) > 0) {
                return true;
            }
        }
        return getCardFilledCount() > 0;
    }

    public String getKitName() {
        return kitName;
    }

    public String getCurrentShareId() {
        return currentShareId;
    }

    public BuildSnapshot getLocalSnapshot() {
        return localSnapshot;
    }

    public boolean hasValidShare() {
        return currentShareId != null && !currentShareId.isEmpty() && localSnapshot != null;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean expanded) {
        this.panelExpanded = expanded;
        if (!expanded) {
            panelScroll = 0;
        }
    }

    public int getFilledCount(EquipmentCategory category) {
        Map<Integer, SlotState> states = equipmentStates.get(category);
        if (states == null) {
            return 0;
        }
        int filled = 0;
        for (SlotState state : states.values()) {
            if (state == SlotState.FILLED) {
                filled++;
            }
        }
        return filled;
    }

    public int getCardFilledCount() {
        int filled = 0;
        for (SlotState state : cardStates.values()) {
            if (state == SlotState.FILLED) {
                filled++;
            }
        }
        return filled;
    }

    public boolean isModulesScanned() {
        return modulesScanned;
    }

    private void onClientTick(Minecraft client) {
        if (client == null || client.player == null || !(client.screen instanceof AbstractContainerScreen<?> screen)) {
            if (suppressRescanScreen != null && (client == null || client.screen != suppressRescanScreen)) {
                suppressRescanScreen = null;
            }
            return;
        }
        if (!CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return;
        }
        if (suppressRescanScreen != null) {
            if (screen == suppressRescanScreen) {
                return;
            }
            suppressRescanScreen = null;
        }

        String title = screen.getTitle().getString();
        if (ZeichenUtility.containsEquipmentDisplay(title)) {
            scanEquipment(client, screen, title);
        } else if (ZeichenUtility.isCardsMenuTitle(title)) {
            scanCards(client, screen);
        }
    }

    private void scanEquipment(Minecraft client, AbstractContainerScreen<?> screen, String title) {
        boolean modulesVisible = ZeichenUtility.isEquipmentModulesVisible(title);

        if (!equipmentDebugDumped && CCLiveUtilitiesConfig.HANDLER.instance().chatDebugging) {
            equipmentDebugDumped = true;
            dumpMappedEquipmentSlots(screen, modulesVisible);
        }

        StringBuilder hash = new StringBuilder();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            if (category == EquipmentCategory.MODULES && !modulesVisible) {
                continue;
            }
            for (int slotIndex : category.getSlots()) {
                updateEquipmentSlot(client, screen, category, slotIndex);
                hash.append(slotIndex).append('=')
                    .append(lastEquipmentSignatures.getOrDefault(slotIndex, "")).append(';');
            }
            if (category == EquipmentCategory.MODULES) {
                modulesScanned = true;
            }
        }

        String scanHash = hash.toString();
        if (scanHash.equals(lastEquipmentScanHash)) {
            equipmentStableTicks++;
        } else {
            lastEquipmentScanHash = scanHash;
            equipmentStableTicks = 1;
        }

        if (!equipmentRecognized && equipmentStableTicks >= STABLE_TICKS_REQUIRED) {
            equipmentRecognized = true;
            debug("Ausrüstung erkannt · " + getFilledCount(EquipmentCategory.ARMOR)
                + "/" + EquipmentCategory.ARMOR.getCapacity() + " belegt (Rüstung).");
        }
    }

    private boolean updateEquipmentSlot(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            EquipmentCategory category,
            int slotIndex) {
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return false;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        String signature = stackSignature(stack);
        String previous = lastEquipmentSignatures.get(slotIndex);
        if (signature.equals(previous == null ? "" : previous) && equipmentRecognized) {
            return false;
        }
        lastEquipmentSignatures.put(slotIndex, signature);

        Map<Integer, ItemData> map = equipmentByCategory.get(category);
        Map<Integer, SlotState> states = equipmentStates.get(category);
        Map<Integer, String> emptyLabels = equipmentEmptyLabels.get(category);
        Map<Integer, ItemData> lockedMap = equipmentLockedPlaceholders.get(category);
        SlotClassification classified = classifyEquipmentStack(client, screen, category, stack);
        states.put(slotIndex, classified.state);
        if (classified.state == SlotState.FILLED && classified.data != null) {
            map.put(slotIndex, classified.data);
            emptyLabels.remove(slotIndex);
            lockedMap.remove(slotIndex);
        } else if (classified.state == SlotState.LOCKED) {
            map.remove(slotIndex);
            emptyLabels.remove(slotIndex);
            if (classified.data != null) {
                lockedMap.put(slotIndex, classified.data);
            } else {
                lockedMap.remove(slotIndex);
            }
        } else if (classified.state == SlotState.EMPTY) {
            map.remove(slotIndex);
            lockedMap.remove(slotIndex);
            if (classified.emptyLabel != null && !classified.emptyLabel.isEmpty()) {
                emptyLabels.put(slotIndex, classified.emptyLabel);
            } else {
                emptyLabels.remove(slotIndex);
            }
        } else {
            map.remove(slotIndex);
            emptyLabels.remove(slotIndex);
            lockedMap.remove(slotIndex);
        }
        return true;
    }

    private SlotClassification classifyEquipmentStack(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            EquipmentCategory category,
            ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SlotClassification.empty();
        }

        if (category.usesPlaceholders()) {
            if (isLockedSlot(stack)) {
                ItemData placeholder = ItemViewerUtility.captureInventoryStackForShare(client, screen, stack);
                if (placeholder != null && placeholder.name != null) {
                    placeholder.name = ShareTextCleaner.cleanDisplayName(placeholder.name);
                }
                return SlotClassification.locked(placeholder);
            }
            // Paper = leerer Slot (wie bei Rüstung); zusätzlich Namens-Platzhalter
            if (stack.is(net.minecraft.world.item.Items.PAPER)) {
                return SlotClassification.empty(placeholderSlotLabel(stack));
            }
            String rawName = stack.getHoverName().getString();
            if (BuildSlotMapping.isPlaceholderName(rawName)
                    || BuildSlotMapping.isPlaceholderName(ShareTextCleaner.cleanDisplayName(rawName))) {
                return SlotClassification.empty(placeholderSlotLabel(stack));
            }
            if (ShareTextCleaner.isLayoutOnlyName(rawName)) {
                return SlotClassification.empty();
            }
            ItemData data = ItemViewerUtility.captureInventoryStackForShare(client, screen, stack);
            if (data == null) {
                return SlotClassification.empty();
            }
            data.name = ShareTextCleaner.cleanDisplayName(data.name != null ? data.name : rawName);
            return SlotClassification.filled(data);
        }

        // ARMOR / WEAPONS / TOOLS: leere Slots = Paper-Platzhalter (nicht ItemStack.isEmpty)
        if (isEquipmentEmptyPlaceholder(stack)) {
            return SlotClassification.empty(placeholderSlotLabel(stack));
        }
        String rawName = stack.getHoverName().getString();
        if (ShareTextCleaner.isLayoutOnlyName(rawName)) {
            return SlotClassification.empty();
        }
        ItemData data = ItemViewerUtility.captureInventoryStackForShare(client, screen, stack);
        if (data == null) {
            return SlotClassification.empty();
        }
        if (data.name != null) {
            data.name = ShareTextCleaner.cleanDisplayName(data.name);
        }
        if (data.name == null || data.name.isEmpty()) {
            return SlotClassification.empty();
        }
        return SlotClassification.filled(data);
    }

    /** Hover-Name eines Paper-/Platzhalter-Stacks als Slotbezeichnung (ohne „Slot gesperrt“). */
    private static String placeholderSlotLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String cleaned = ShareTextCleaner.cleanDisplayName(stack.getHoverName().getString());
        if (cleaned == null || cleaned.isEmpty() || BuildSlotMapping.isPlaceholderName(cleaned)) {
            return null;
        }
        return cleaned;
    }

    /** Leere Ausrüstungsslots bei PlayLegend: Paper als Platzhalter. */
    private static boolean isEquipmentEmptyPlaceholder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (stack.is(net.minecraft.world.item.Items.PAPER)) {
            return true;
        }
        String raw = stack.getHoverName().getString();
        if (BuildSlotMapping.isPlaceholderName(raw)
                || BuildSlotMapping.isPlaceholderName(ShareTextCleaner.cleanDisplayName(raw))) {
            return true;
        }
        return ShareTextCleaner.isLayoutOnlyName(raw);
    }

    private void scanCards(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!cardsDebugDumped && CCLiveUtilitiesConfig.HANDLER.instance().chatDebugging) {
            cardsDebugDumped = true;
            dumpMappedCardSlots(screen);
        }

        StringBuilder hash = new StringBuilder();
        for (int slotIndex : BuildSlotMapping.getCardSlots()) {
            if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            String signature = stackSignature(stack);
            hash.append(slotIndex).append('=').append(signature).append(';');

            String previous = lastCardSignatures.get(slotIndex);
            if (signature.equals(previous == null ? "" : previous) && cardsRecognized) {
                continue;
            }
            lastCardSignatures.put(slotIndex, signature);

            if (isUsableCardItem(stack)) {
                ItemData data = ItemViewerUtility.captureInventoryStackForShare(client, screen, stack);
                if (data != null) {
                    if (data.name != null) {
                        data.name = ShareTextCleaner.cleanDisplayName(data.name);
                    }
                    cardsBySlot.put(slotIndex, data);
                    cardStates.put(slotIndex, SlotState.FILLED);
                    continue;
                }
            }
            if (stack != null && !stack.isEmpty() && isLockedSlot(stack)) {
                cardsBySlot.remove(slotIndex);
                cardStates.put(slotIndex, SlotState.LOCKED);
            } else {
                cardsBySlot.remove(slotIndex);
                cardStates.put(slotIndex, SlotState.EMPTY);
            }
        }

        cardsBySlot.keySet().removeIf(idx -> !isMappedCardSlot(idx));

        String scanHash = hash.toString();
        if (scanHash.equals(lastCardsScanHash)) {
            cardsStableTicks++;
        } else {
            lastCardsScanHash = scanHash;
            cardsStableTicks = 1;
        }

        if (!cardsRecognized && cardsStableTicks >= STABLE_TICKS_REQUIRED) {
            cardsRecognized = true;
            debug("Karten erkannt (" + getCardFilledCount() + "/" + BuildSlotMapping.getCardCapacity() + ").");
        }
    }

    private static boolean isMappedCardSlot(int slotIndex) {
        for (int s : BuildSlotMapping.CARD_SLOTS) {
            if (s == slotIndex) {
                return true;
            }
        }
        return false;
    }

    public void handleWardrobeRightClick(AbstractContainerScreen<?> screen, int button) {
        if (button != 1) {
            return;
        }
        if (!CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || screen == null) {
            return;
        }
        String title = screen.getTitle().getString();
        if (!ZeichenUtility.isWardrobeFullTitle(title)) {
            return;
        }

        Slot hoveredSlot = ItemViewerUtility.getLastHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }
        if (hoveredSlot.container == client.player.getInventory()) {
            return;
        }
        int containerEnd = getContainerSlotCount(screen, client);
        if (hoveredSlot.index >= containerEnd) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!stack.is(net.minecraft.world.item.Items.LEATHER_CHESTPLATE)) {
            debug("Rechtsklick ignoriert (kein leather_chestplate): "
                + ShareTextCleaner.cleanDisplayName(stack.getHoverName().getString()));
            return;
        }
        if (stack.isEmpty() || isLockedSlot(stack) || BuildSlotMapping.isPlaceholderName(stack.getHoverName().getString())) {
            return;
        }

        String name = ItemViewerUtility.extractInventoryItemDisplayName(client, screen, stack);
        if (name == null || name.isEmpty()) {
            return;
        }
        name = ShareTextCleaner.cleanDisplayName(stripQuantitySuffix(name));
        if (name.isEmpty() || BuildSlotMapping.isPlaceholderName(name)) {
            return;
        }

        kitName = name;
        debug("Kit-Name gesetzt: " + kitName);
        sendPlayerFeedback("§aKit erkannt: §f" + kitName);
    }

    public boolean handleOverlayClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        if (!ZeichenUtility.isBuildShareContextTitle(screen.getTitle().getString())) {
            return false;
        }

        int x = getOverlayX();
        int y = getOverlayY();
        float iconScale = getScale();
        float localX = (mouseX - x) / iconScale;
        float localY = (mouseY - y) / iconScale;

        if (localX >= 0 && localX <= ICON_SIZE && localY >= 0 && localY <= ICON_SIZE) {
            panelExpanded = !panelExpanded;
            if (panelExpanded) {
                layoutPanel(screen, client);
            }
            return true;
        }

        if (!panelExpanded) {
            return false;
        }

        layoutPanel(screen, client);
        if (mouseX < panelScreenX || mouseX > panelScreenX + panelDrawWidth
                || mouseY < panelScreenY || mouseY > panelScreenY + panelVisibleHeight) {
            return false;
        }

        int buttonsBottom = panelScreenY + panelVisibleHeight;
        int clearTop = buttonsBottom - CLEAR_BUTTON_HEIGHT - 4;
        int copyTop = clearTop - BUTTON_GAP - COPY_BUTTON_HEIGHT;

        if (mouseX >= panelScreenX + 6 && mouseX <= panelScreenX + panelDrawWidth - 6
                && mouseY >= copyTop && mouseY <= copyTop + COPY_BUTTON_HEIGHT) {
            if (canCopy() && !uploading) {
                copyBuildAsync();
            }
            return true;
        }
        if (mouseX >= panelScreenX + 6 && mouseX <= panelScreenX + panelDrawWidth - 6
                && mouseY >= clearTop && mouseY <= clearTop + CLEAR_BUTTON_HEIGHT) {
            if (hasClearableData()) {
                clearLocalCapture(true);
                sendPlayerFeedback("§eBuild-Erfassung geleert.");
            }
            return true;
        }
        return true;
    }

    public boolean handleOverlayScroll(double mouseX, double mouseY, double vertical) {
        if (!panelExpanded || !CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        if (!ZeichenUtility.isBuildShareContextTitle(screen.getTitle().getString())) {
            return false;
        }
        layoutPanel(screen, client);
        if (mouseX < panelScreenX || mouseX > panelScreenX + panelDrawWidth
                || mouseY < panelScreenY || mouseY > panelScreenY + panelVisibleHeight) {
            return false;
        }
        int maxScroll = Math.max(0, panelContentHeight - panelVisibleHeight + COPY_BUTTON_HEIGHT + CLEAR_BUTTON_HEIGHT + 20);
        panelScroll = (int) Math.max(0, Math.min(maxScroll, panelScroll - (int) Math.signum(vertical) * 10));
        return true;
    }

    public void renderInGame(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().buildShareEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (!ZeichenUtility.isBuildShareContextTitle(screen.getTitle().getString())) {
            return;
        }

        int x = getOverlayX();
        int y = getOverlayY();
        float iconScale = getScale();

        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(iconScale, iconScale);

        // Transparenter Hintergrund — nur das Icon zeichnen
        context.blit(RenderPipelines.GUI_TEXTURED, ICON, 0, 0, 0.0f, 0.0f,
            ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        context.pose().popMatrix();

        if (panelExpanded) {
            layoutPanel(screen, client);
            renderPanelAbsolute(context, mouseX, mouseY);
        }
    }

    static int[] computeLeftTopOfContainer(AbstractContainerScreen<?> screen, Minecraft client) {
        int[] geom = readScreenGeometry(screen);
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayScale;
        if (scale <= 0) {
            scale = 1.0f;
        }
        int iconW = Math.max(1, Math.round(ICON_SIZE * scale));
        int iconH = Math.max(1, Math.round(ICON_SIZE * scale));
        int gap = 4;
        // Direkt links AM Container (leftPos), nicht am Bildschirmrand
        int x = geom[0] - iconW - gap;
        int y = geom[1];
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        // Nur soweit klemmen, dass das Icon sichtbar bleibt — Anker bleibt der Container
        if (x < 0) {
            x = 0;
        }
        if (x + iconW > screenW) {
            x = Math.max(0, screenW - iconW);
        }
        if (y < 0) {
            y = 0;
        }
        if (y + iconH > screenH) {
            y = Math.max(0, screenH - iconH);
        }
        return new int[]{x, y};
    }

    private static boolean isContainerAnchored() {
        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        if (cfg.buildShareOverlayX == 8 && cfg.buildShareOverlayY == 40) {
            // Legacy-Default → Sentinel migrieren
            cfg.buildShareOverlayX = CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_X;
            cfg.buildShareOverlayY = CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_Y;
            return true;
        }
        return cfg.buildShareOverlayX == CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_X
            && cfg.buildShareOverlayY == CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_Y;
    }

    /** F6-Reset: wieder Container-Anker (links oben). */
    public static void resetOverlayToContainerAnchor() {
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayX =
            CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_X;
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayY =
            CCLiveUtilitiesConfig.BUILD_SHARE_DEFAULT_Y;
        CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayScale = 1.0f;
    }

    private void layoutPanel(AbstractContainerScreen<?> screen, Minecraft client) {
        panelContentHeight = computePanelContentHeight();
        int buttonsBlock = COPY_BUTTON_HEIGHT + BUTTON_GAP + CLEAR_BUTTON_HEIGHT + 10;
        int preferredH = panelContentHeight + buttonsBlock;
        int preferredW = PANEL_WIDTH;

        int[] geom = readScreenGeometry(screen);
        int left = geom[0];
        int top = geom[1];
        int imgW = geom[2];
        int imgH = geom[3];

        int forbidL = left - MENU_PAD;
        int forbidT = top - MENU_PAD;
        int forbidR = left + imgW + MENU_PAD;
        int forbidB = top + imgH + MENU_PAD;

        int containerEnd = getContainerSlotCount(screen, client);
        for (int i = 0; i < screen.getMenu().slots.size() && i < containerEnd; i++) {
            Slot slot = screen.getMenu().slots.get(i);
            int sx = left + slot.x;
            int sy = top + slot.y;
            forbidL = Math.min(forbidL, sx - 4);
            forbidT = Math.min(forbidT, sy - 4);
            forbidR = Math.max(forbidR, sx + 20);
            forbidB = Math.max(forbidB, sy + 20);
        }

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float iconScale = getScale();
        int iconW = Math.max(1, Math.round(ICON_SIZE * iconScale));
        int iconH = Math.max(1, Math.round(ICON_SIZE * iconScale));

        int[] placed = placePanelNearIcon(
            screenW, screenH,
            forbidL, forbidT, forbidR, forbidB,
            preferredW, preferredH,
            getOverlayX(), getOverlayY(), iconW, iconH);
        panelScreenX = placed[0];
        panelScreenY = placed[1];
        panelDrawWidth = placed[2];
        panelVisibleHeight = placed[3];
        panelCompact = panelDrawWidth < PANEL_WIDTH || panelVisibleHeight < preferredH;
        int maxScroll = Math.max(0, preferredH - panelVisibleHeight);
        panelScroll = Math.min(panelScroll, maxScroll);
    }

    /**
     * Panel nahe am Overlay-Icon (nicht darauf): unten → oben → rechts → links,
     * ohne Inventarfläche und ohne Icon zu überdecken.
     */
    private static int[] placePanelNearIcon(
            int screenW, int screenH,
            int fL, int fT, int fR, int fB,
            int prefW, int prefH,
            int iconX, int iconY, int iconW, int iconH) {
        final int gap = 4;
        int iL = iconX;
        int iT = iconY;
        int iR = iconX + iconW;
        int iB = iconY + iconH;

        // Kandidaten relativ zum Icon (Anker nahe Icon-Kante)
        int[][] anchors = {
            // unten
            {iconX, iB + gap, prefW, prefH},
            // oben
            {iconX, iT - prefH - gap, prefW, prefH},
            // rechts
            {iR + gap, iconY, prefW, prefH},
            // links
            {iL - prefW - gap, iconY, prefW, prefH}
        };

        int[] best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int[] a : anchors) {
            int[] fitted = fitPanelCandidate(
                a[0], a[1], a[2], a[3],
                screenW, screenH,
                fL, fT, fR, fB,
                iL, iT, iR, iB,
                prefW, prefH);
            if (fitted == null) {
                continue;
            }
            // Nähe zum Icon belohnen + Fläche nahe Wunschgröße
            int cx = fitted[0] + fitted[2] / 2;
            int cy = fitted[1] + fitted[3] / 2;
            int iconCx = iconX + iconW / 2;
            int iconCy = iconY + iconH / 2;
            int dist = Math.abs(cx - iconCx) + Math.abs(cy - iconCy);
            int areaScore = fitted[2] * fitted[3];
            int score = areaScore * 2 - dist;
            if (score > bestScore) {
                bestScore = score;
                best = fitted;
            }
        }

        if (best != null) {
            return best;
        }

        // Fallback: kompakt möglichst nah am Icon, ohne Überdeckung
        int w = Math.min(PANEL_WIDTH_COMPACT, Math.max(80, prefW));
        int h = Math.min(prefH, Math.max(60, screenH / 2));
        int x = clamp(iconX, 2, screenW - w - 2);
        int y = clamp(iB + gap, 2, screenH - h - 2);
        int[] fallback = fitPanelCandidate(
            x, y, w, h, screenW, screenH, fL, fT, fR, fB, iL, iT, iR, iB, w, h);
        if (fallback != null) {
            return fallback;
        }
        return new int[]{
            clamp(iconX, 2, screenW - 80),
            clamp(iB + gap, 2, screenH - 60),
            Math.min(w, screenW - 4),
            Math.min(h, Math.max(60, screenH - iB - 6))
        };
    }

    /** Klemmt und schrumpft ein Panel-Rechteck, bis es Icon und Menü meidet. */
    private static int[] fitPanelCandidate(
            int x, int y, int w, int h,
            int screenW, int screenH,
            int fL, int fT, int fR, int fB,
            int iL, int iT, int iR, int iB,
            int maxW, int maxH) {
        w = Math.min(w, maxW);
        h = Math.min(h, maxH);
        w = Math.min(w, screenW - 4);
        h = Math.min(h, screenH - 4);
        if (w < 80 || h < 60) {
            return null;
        }

        x = clamp(x, 2, screenW - w - 2);
        y = clamp(y, 2, screenH - h - 2);

        // Bis zu wenige Passes: bei Konflikt schrumpfen / verschieben
        for (int pass = 0; pass < 6; pass++) {
            boolean overlapsMenu = rectsOverlap(x, y, x + w, y + h, fL, fT, fR, fB);
            boolean overlapsIcon = rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB);
            if (!overlapsMenu && !overlapsIcon) {
                return new int[]{x, y, w, h};
            }
            if (overlapsIcon) {
                // Vom Icon wegschieben (Priorität: unten, dann rechts)
                if (y < iB) {
                    y = iB + 4;
                }
                if (rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB) && x < iR) {
                    x = iR + 4;
                }
                if (rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB) && x + w > iL) {
                    x = iL - w - 4;
                }
                if (rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB) && y + h > iT) {
                    y = iT - h - 4;
                }
            }
            if (overlapsMenu) {
                // Aus der Menüfläche heraus
                if (x < fR && x + w > fL && y < fB && y + h > fT) {
                    int pushRight = fR + 4 - x;
                    int pushLeft = (x + w) - (fL - 4);
                    int pushDown = fB + 4 - y;
                    int pushUp = (y + h) - (fT - 4);
                    int bestPush = Math.min(Math.min(pushRight, pushLeft), Math.min(pushDown, pushUp));
                    if (bestPush == pushRight) {
                        x = fR + 4;
                    } else if (bestPush == pushLeft) {
                        x = fL - w - 4;
                    } else if (bestPush == pushDown) {
                        y = fB + 4;
                    } else {
                        y = fT - h - 4;
                    }
                }
            }
            x = clamp(x, 2, screenW - w - 2);
            y = clamp(y, 2, screenH - h - 2);
            if (rectsOverlap(x, y, x + w, y + h, fL, fT, fR, fB)
                    || rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB)) {
                // Schrumpfen als letzte Option in diesem Pass
                if (w > 80) {
                    w = Math.max(80, w - 16);
                }
                if (h > 60) {
                    h = Math.max(60, h - 16);
                }
                x = clamp(x, 2, screenW - w - 2);
                y = clamp(y, 2, screenH - h - 2);
            }
        }

        if (rectsOverlap(x, y, x + w, y + h, fL, fT, fR, fB)
                || rectsOverlap(x, y, x + w, y + h, iL, iT, iR, iB)) {
            return null;
        }
        return new int[]{x, y, w, h};
    }

    private static boolean rectsOverlap(int aL, int aT, int aR, int aB, int bL, int bT, int bR, int bB) {
        return aL < bR && aR > bL && aT < bB && aB > bT;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void renderPanelAbsolute(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        int x = panelScreenX;
        int y = panelScreenY;
        int w = panelDrawWidth;
        int h = panelVisibleHeight;

        context.fill(x, y, x + w, y + h, 0xE0101018);
        context.fill(x, y, x + w, y + 1, 0xFF55AAFF);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF55AAFF);

        int lineH = panelCompact ? 9 : 10;
        int textY = y + 6 - panelScroll;
        int clipTop = y + 2;
        int buttonsBlock = COPY_BUTTON_HEIGHT + BUTTON_GAP + CLEAR_BUTTON_HEIGHT + 8;
        int clipBottom = y + h - buttonsBlock;

        int textMaxW = Math.max(40, w - 12);
        textY = drawClippedLine(context, client, ellipsize(client, "Build-Share", textMaxW),
            x + 6, textY, 0xFFFFFFFF, clipTop, clipBottom, lineH + 2);
        String kitLine = "Kit: " + (kitName != null && !kitName.isEmpty() ? kitName : "—");
        textY = drawClippedLine(context, client, ellipsize(client, kitLine, textMaxW),
            x + 6, textY, 0xFFCCCCCC, clipTop, clipBottom, lineH + 4);

        textY = drawClippedLine(context, client, ellipsize(client, "Ausrüstung", textMaxW),
            x + 6, textY, 0xFFFFD700, clipTop, clipBottom, lineH + 1);
        if (equipmentRecognized) {
            int armorFilled = getFilledCount(EquipmentCategory.ARMOR);
            String status = "Ausrüstung erkannt · " + armorFilled + "/"
                + EquipmentCategory.ARMOR.getCapacity() + " belegt";
            textY = drawClippedLine(context, client, ellipsize(client, status, textMaxW),
                x + 6, textY, 0xFF55FF55, clipTop, clipBottom, lineH + 1);
            for (EquipmentCategory category : EquipmentCategory.values()) {
                String line;
                if (category == EquipmentCategory.MODULES && !modulesScanned) {
                    line = category.getDisplayName() + "  —";
                } else {
                    line = category.getDisplayName() + "  "
                        + getFilledCount(category) + "/" + category.getCapacity();
                }
                textY = drawClippedLine(context, client, ellipsize(client, line, textMaxW - 4),
                    x + 10, textY, 0xFFDDDDDD, clipTop, clipBottom, lineH);
            }
        } else {
            textY = drawClippedLine(context, client, ellipsize(client, "✗ nicht erkannt", textMaxW),
                x + 6, textY, 0xFFFF5555, clipTop, clipBottom, lineH + 1);
        }

        textY = drawClippedLine(context, client, ellipsize(client, "Karten", textMaxW),
            x + 6, textY + 2, 0xFFFFD700, clipTop, clipBottom, lineH + 1);
        if (cardsRecognized) {
            String cardsLine = "Karten erkannt · "
                + getCardFilledCount() + "/" + BuildSlotMapping.getCardCapacity() + " belegt";
            textY = drawClippedLine(context, client, ellipsize(client, cardsLine, textMaxW),
                x + 6, textY, 0xFF55FF55, clipTop, clipBottom, lineH + 1);
        } else {
            textY = drawClippedLine(context, client, ellipsize(client, "✗ nicht erkannt", textMaxW),
                x + 6, textY, 0xFFFF5555, clipTop, clipBottom, lineH + 1);
        }

        if (currentShareId != null) {
            drawClippedLine(context, client, ellipsize(client, "ID: " + currentShareId, textMaxW),
                x + 6, textY + 2, 0xFF88AAFF, clipTop, clipBottom, lineH);
        }

        int clearTop = y + h - CLEAR_BUTTON_HEIGHT - 4;
        int copyTop = clearTop - BUTTON_GAP - COPY_BUTTON_HEIGHT;
        boolean can = canCopy() && !uploading;
        boolean copyHover = mouseX >= x + 6 && mouseX <= x + w - 6
            && mouseY >= copyTop && mouseY <= copyTop + COPY_BUTTON_HEIGHT;
        int copyColor = !can ? 0xFF444444 : (copyHover ? 0xFF2E7D32 : 0xFF1B5E20);
        context.fill(x + 6, copyTop, x + w - 6, copyTop + COPY_BUTTON_HEIGHT, copyColor);
        String btnText = uploading ? "Upload…" : "Build kopieren";
        int tw = client.font.width(btnText);
        context.text(client.font, btnText, x + (w - tw) / 2, copyTop + 4, can ? 0xFFFFFFFF : 0xFF888888, false);

        boolean canClear = hasClearableData();
        boolean clearHover = canClear && mouseX >= x + 6 && mouseX <= x + w - 6
            && mouseY >= clearTop && mouseY <= clearTop + CLEAR_BUTTON_HEIGHT;
        context.fill(x + 6, clearTop, x + w - 6, clearTop + CLEAR_BUTTON_HEIGHT,
            !canClear ? 0xFF444444 : (clearHover ? 0xFF6D4C41 : 0xFF4E342E));
        String clearText = "Build leeren";
        int ctw = client.font.width(clearText);
        context.text(client.font, clearText, x + (w - ctw) / 2, clearTop + 2,
            canClear ? 0xFFFFCCBC : 0xFF888888, false);
    }

    private int drawClippedLine(
            GuiGraphicsExtractor context,
            Minecraft client,
            String text,
            int textX,
            int textY,
            int color,
            int clipTop,
            int clipBottom,
            int advance) {
        if (textY >= clipTop && textY + 8 <= clipBottom) {
            context.text(client.font, text, textX, textY, color, false);
        }
        return textY + advance;
    }

    private static String ellipsize(Minecraft client, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (client.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = client.font.width(ellipsis);
        if (maxWidth <= ellipsisW) {
            return ellipsis;
        }
        String truncated = text;
        while (!truncated.isEmpty() && client.font.width(truncated) + ellipsisW > maxWidth) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated.isEmpty() ? ellipsis : truncated + ellipsis;
    }

    private int computePanelContentHeight() {
        int lineH = panelCompact ? 9 : 10;
        int h = 6 + 12 + 14;
        h += lineH + 1;
        if (equipmentRecognized) {
            h += lineH + 1 + EquipmentCategory.values().length * lineH;
        } else {
            h += lineH + 1;
        }
        h += 4 + lineH + 1;
        h += lineH + 1;
        if (currentShareId != null) {
            h += 12;
        }
        return h;
    }

    private int getPanelHeight() {
        return computePanelContentHeight() + COPY_BUTTON_HEIGHT + BUTTON_GAP + CLEAR_BUTTON_HEIGHT + 10;
    }

    public int getOverlayX() {
        if (isContainerAnchored()) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.screen instanceof AbstractContainerScreen<?> screen) {
                return computeLeftTopOfContainer(screen, client)[0];
            }
        }
        return CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayX;
    }

    public int getOverlayY() {
        if (isContainerAnchored()) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.screen instanceof AbstractContainerScreen<?> screen) {
                return computeLeftTopOfContainer(screen, client)[1];
            }
        }
        return CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayY;
    }

    public float getScale() {
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().buildShareOverlayScale;
        return scale <= 0 ? 1.0f : scale;
    }

    public int getUnscaledWidth() {
        return ICON_SIZE;
    }

    public int getUnscaledHeight() {
        return ICON_SIZE;
    }

    public static final String BUILD_INSERTION_PREFIX = "cclive-build:";

    private final Map<String, BuildSnapshot> resolvedCache = new ConcurrentHashMap<>();
    /** Zuletzt im Chat gehoverter Build (für Klick, falls Style-Lookup die Insertion verliert). */
    private static volatile String lastHoveredShareId;
    private static final AtomicInteger openRequestGeneration = new AtomicInteger(0);

    public BuildSnapshot getResolvedBuild(String shareId) {
        if (shareId == null || shareId.isEmpty()) {
            return null;
        }
        BuildSnapshot cached = resolvedCache.get(shareId);
        if (cached != null) {
            return cached;
        }
        if (localSnapshot != null && shareId.equals(currentShareId)) {
            return localSnapshot;
        }
        return null;
    }

    public static String getLastHoveredShareId() {
        return lastHoveredShareId;
    }

    public static void setLastHoveredShareId(String shareId) {
        lastHoveredShareId = shareId;
    }

    public static void clearLastHoveredShareId() {
        lastHoveredShareId = null;
    }

    public static String extractShareIdFromStyle(Style style) {
        if (style == null) {
            return null;
        }
        String insertion = style.getInsertion();
        if (insertion != null && insertion.startsWith(BUILD_INSERTION_PREFIX)) {
            return insertion.substring(BUILD_INSERTION_PREFIX.length());
        }
        String clickVal = ChatStyleHelper.clickEventValue(style.getClickEvent());
        if (clickVal != null && clickVal.startsWith(BUILD_INSERTION_PREFIX)) {
            return clickVal.substring(BUILD_INSERTION_PREFIX.length());
        }
        return null;
    }

    public Component resolveLinkComponent(String shareId, boolean allowRetry) {
        BuildSnapshot data = getResolvedBuild(shareId);
        if (data == null) {
            data = fetchBuildSync(shareId);
            if (data == null && allowRetry) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                data = fetchBuildSync(shareId);
            }
            if (data != null) {
                resolvedCache.put(shareId, data);
            }
        }

        if (data == null) {
            Style style = Style.EMPTY.withColor(ChatFormatting.GRAY)
                .withInsertion(BUILD_INSERTION_PREFIX + shareId);
            return Component.literal("[?]").setStyle(style);
        }

        // Im Cache halten, damit Hover-Panel und Klick denselben Snapshot finden
        resolvedCache.put(shareId, data);

        String displayName = data.kitName != null && !data.kitName.isEmpty()
            ? ShareTextCleaner.cleanDisplayName(data.kitName) : "Build";
        String token = BUILD_INSERTION_PREFIX + shareId;

        // HoverEvent nötig, damit getClickedComponentStyleAt den Link überhaupt liefert
        // (ohne Hover/Click bleibt Insertion in 1.21 oft unsichtbar für Hit-Tests).
        // Inhalt = Spaltenübersicht; kein SuggestCommand (sonst ID im Eingabefeld).
        Component hoverContent = BuildShareOverviewLayout.toHoverComponent(data);
        HoverEvent hover = ItemShareManager.createShowTextHoverPublic(hoverContent);

        Style linkStyle = Style.EMPTY
            .withColor(0x55FFFF)
            .withUnderlined(true)
            .withInsertion(token);
        if (hover != null) {
            linkStyle = linkStyle.withHoverEvent(hover);
        }

        debug("Build-Link resolved id=" + shareId + " kit=" + displayName);
        return Component.literal("[" + displayName + "]").setStyle(linkStyle);
    }

    public boolean tryOpenFromStyle(Style style) {
        String shareId = extractShareIdFromStyle(style);
        if (shareId == null || shareId.isEmpty()) {
            return false;
        }
        return openBuildScreen(shareId);
    }

    /** Öffnet die Vorschau; lädt bei Bedarf asynchron und verwirft veraltete Ergebnisse. */
    public boolean openBuildScreen(String shareId) {
        if (shareId == null || shareId.isEmpty()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        if (BuildShareViewScreen.isActive()) {
            return true;
        }

        BuildSnapshot data = getResolvedBuild(shareId);
        if (data != null) {
            openPreviewNow(client, data);
            return true;
        }

        int gen = openRequestGeneration.incrementAndGet();
        sendPlayerFeedback("§7Build wird geladen…");
        CompletableFuture.runAsync(() -> {
            BuildSnapshot loaded = fetchBuildSync(shareId);
            if (loaded != null) {
                resolvedCache.put(shareId, loaded);
            }
            final BuildSnapshot snap = loaded;
            client.execute(() -> {
                if (gen != openRequestGeneration.get()) {
                    return; // veraltet
                }
                if (BuildShareViewScreen.isActive()) {
                    return;
                }
                // Nur öffnen, wenn noch Chat (oder kein anderer Vollbild-Wechsel)
                if (snap == null) {
                    sendPlayerFeedback("§cBuild konnte nicht geladen werden.");
                    return;
                }
                openPreviewNow(client, snap);
            });
        });
        return true;
    }

    private void openPreviewNow(Minecraft client, BuildSnapshot snapshot) {
        clearLastHoveredShareId();
        net.minecraft.client.gui.screens.Screen previous = client.screen;
        debug("Build-Vorschau öffnen kit="
            + (snapshot != null && snapshot.kitName != null ? snapshot.kitName : "?"));
        client.setScreen(new BuildShareViewScreen(snapshot, previous));
    }

    /** Klick aus Chat: nur wenn Maus auf dem Build-Link-Style liegt. */
    public boolean tryOpenFromChatClick(double mouseX, double mouseY) {
        String shareId = ChatStyleHelper.resolveBuildShareIdAt(mouseX, mouseY);
        debug("Build-Klick mouse=(" + (int) mouseX + "," + (int) mouseY
            + ") shareId=" + (shareId != null ? shareId : "null"));
        if (shareId == null || shareId.isEmpty()) {
            return false;
        }
        debug("Build-Klick → openBuildScreen id=" + shareId);
        return openBuildScreen(shareId);
    }

    public void invalidatePendingOpens() {
        openRequestGeneration.incrementAndGet();
    }

    private BuildSnapshot fetchBuildSync(String shareId) {
        try {
            if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
                return null;
            }
            LeaderboardManager lb = LeaderboardManager.getInstance();
            if (!lb.ensureRegisteredSync()) {
                return null;
            }
            JsonObject response = httpClient.getWithToken(
                "/share/build/" + shareId,
                lb.getPlayerToken()
            );
            if (response == null || !response.has("data")) {
                return null;
            }
            return GSON.fromJson(response.get("data"), BuildSnapshot.class);
        } catch (Exception e) {
            debug("Resolve fehlgeschlagen für " + shareId + ": " + e.getMessage());
            return null;
        }
    }

    static Component buildBuildHoverTooltip(BuildSnapshot data) {
        MutableComponent combined = Component.empty();
        String title = data.kitName != null && !data.kitName.isEmpty()
            ? ShareTextCleaner.cleanDisplayName(data.kitName) : "Build";
        combined.append(Component.literal(title).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        if (!data.equipmentRecognized) {
            combined.append(Component.literal("\n"));
            combined.append(Component.literal("Ausrüstung: nicht erfasst").withStyle(ChatFormatting.GRAY));
        } else if (data.equipment != null) {
            appendCategoryHover(combined, "Rüstung", data.equipment.get("armor"), true);
            appendCategoryHover(combined, "Waffen", data.equipment.get("weapons"), true);
            appendCategoryHover(combined, "Werkzeuge", data.equipment.get("tools"), true);
            boolean modulesOk = data.modulesScanned;
            appendCategoryHover(combined, "Module", data.equipment.get("modules"), modulesOk);
        }
        if (data.cardsRecognized) {
            appendCategoryHover(combined, "Karten", data.cards, true);
        } else {
            combined.append(Component.literal("\n"));
            combined.append(Component.literal("Karten: nicht erfasst").withStyle(ChatFormatting.GRAY));
        }

        combined.append(Component.literal("\n"));
        combined.append(Component.literal("Klicken für mehr Details").withStyle(ChatFormatting.GRAY));
        return combined;
    }

    private static void appendCategoryHover(
            MutableComponent out,
            String label,
            CategoryBucket bucket,
            boolean recognized) {
        out.append(Component.literal("\n"));
        if (!recognized || bucket == null) {
            out.append(Component.literal(label + ": —").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        out.append(Component.literal(label + " " + bucket.filled + "/" + bucket.total)
            .withStyle(ChatFormatting.YELLOW));

        // Karten: nur belegte, kompakt (keine leeren/gesperrten Zeilen)
        boolean cardsCompact = "Karten".equals(label);
        if (cardsCompact) {
            if (bucket.items != null) {
                for (ItemData item : bucket.items) {
                    if (item == null) {
                        continue;
                    }
                    out.append(Component.literal("\n  "));
                    out.append(ItemShareManager.nameFromSnapshot(item));
                }
            }
            return;
        }

        if (bucket.slotStatuses != null && !bucket.slotStatuses.isEmpty()) {
            for (int i = 0; i < bucket.slotStatuses.size(); i++) {
                String status = bucket.slotStatuses.get(i);
                String slotLabel = bucket.slotLabels != null && i < bucket.slotLabels.size()
                    ? bucket.slotLabels.get(i) : ("Slot " + (i + 1));
                if ("unknown".equalsIgnoreCase(status)) {
                    out.append(Component.literal("\n  "));
                    out.append(Component.literal(slotLabel + " (nicht erfasst)")
                        .withStyle(ChatFormatting.DARK_GRAY));
                } else if ("empty".equalsIgnoreCase(status)) {
                    out.append(Component.literal("\n  "));
                    out.append(Component.literal(slotLabel + " (leer)").withStyle(ChatFormatting.DARK_GRAY));
                } else if ("locked".equalsIgnoreCase(status)) {
                    out.append(Component.literal("\n  "));
                    out.append(Component.literal(slotLabel + " (gesperrt)").withStyle(ChatFormatting.DARK_RED));
                } else if ("filled".equalsIgnoreCase(status)) {
                    ItemData item = findItemForSlot(bucket, i);
                    out.append(Component.literal("\n  "));
                    if (item != null) {
                        out.append(ItemShareManager.nameFromSnapshot(item));
                    } else {
                        out.append(Component.literal(slotLabel).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        } else if (bucket.items != null) {
            for (ItemData item : bucket.items) {
                if (item == null) {
                    continue;
                }
                out.append(Component.literal("\n  "));
                out.append(ItemShareManager.nameFromSnapshot(item));
            }
        }
    }

    private static ItemData findItemForSlot(CategoryBucket bucket, int statusIndex) {
        if (bucket.items == null || bucket.slotIndices == null || bucket.statusesSlotIndices == null) {
            if (bucket.items != null && statusIndex < bucket.items.size()) {
                return bucket.items.get(statusIndex);
            }
            return null;
        }
        if (statusIndex >= bucket.statusesSlotIndices.size()) {
            return null;
        }
        int slot = bucket.statusesSlotIndices.get(statusIndex);
        for (int i = 0; i < bucket.slotIndices.size(); i++) {
            if (bucket.slotIndices.get(i) != null && bucket.slotIndices.get(i) == slot
                    && i < bucket.items.size()) {
                return bucket.items.get(i);
            }
        }
        return null;
    }

    private void copyBuildAsync() {
        if (!canCopy()) {
            sendPlayerFeedback("§cKein Build-Bereich erkannt.");
            return;
        }

        final int generation = sessionGeneration.incrementAndGet();
        BuildSnapshot snapshot = createSnapshot();
        localSnapshot = snapshot;
        currentShareId = null;
        uploading = true;

        String label = snapshot.kitName != null ? snapshot.kitName : "Build";
        sendPlayerFeedback("§aBuild kopiert: §f" + label);
        debug("Snapshot erstellt (Equipment=" + snapshot.equipmentRecognized
            + ", Cards=" + snapshot.cardsRecognized + ").");

        CompletableFuture.runAsync(() -> {
            try {
                if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
                    finishUpload(generation, null, snapshot, "§cUpload fehlgeschlagen (Datentracking aus). Build bleibt lokal.");
                    return;
                }
                LeaderboardManager lb = LeaderboardManager.getInstance();
                if (!lb.ensureRegisteredSync()) {
                    finishUpload(generation, null, snapshot, "§cUpload fehlgeschlagen (nicht registriert). Build bleibt lokal.");
                    return;
                }

                JsonObject body = new JsonObject();
                body.add("data", GSON.toJsonTree(snapshot));

                JsonObject response = httpClient.postWithToken(
                    "/share/build",
                    body,
                    lb.getPlayerToken()
                );
                if (response != null && response.has("id")) {
                    String id = response.get("id").getAsString();
                    finishUpload(generation, id, snapshot, "§aBuild-Share: §f" + id);
                } else {
                    finishUpload(generation, null, snapshot,
                        "§cUpload fehlgeschlagen. Build bleibt lokal. §7(Server-Route /share/build fehlt?)");
                }
            } catch (Exception e) {
                finishUpload(generation, null, snapshot, "§cUpload fehlgeschlagen. Build bleibt lokal.");
            }
        });
    }

    private void finishUpload(int generation, String shareId, BuildSnapshot snapshot, String feedback) {
        if (generation != sessionGeneration.get()) {
            debug("Upload-Ergebnis ignoriert (veraltete Generation " + generation + ").");
            return;
        }
        uploading = false;
        if (shareId != null) {
            currentShareId = shareId;
            localSnapshot = snapshot;
            resolvedCache.put(shareId, snapshot);
            debug("Upload erfolgreich: " + shareId);
        }
        sendPlayerFeedback(feedback);
    }

    private BuildSnapshot createSnapshot() {
        BuildSnapshot snapshot = new BuildSnapshot();
        snapshot.kitName = kitName;
        snapshot.equipmentRecognized = equipmentRecognized;
        snapshot.cardsRecognized = cardsRecognized;
        snapshot.modulesScanned = modulesScanned;

        snapshot.equipment = new LinkedHashMap<>();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            List<ItemData> items = new ArrayList<>();
            List<Integer> slotIndices = new ArrayList<>();
            List<String> statuses = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> statusSlots = new ArrayList<>();

            Map<Integer, ItemData> map = equipmentByCategory.get(category);
            Map<Integer, SlotState> states = equipmentStates.get(category);
            Map<Integer, String> emptyLabels = equipmentEmptyLabels.get(category);
            Map<Integer, ItemData> lockedMap = equipmentLockedPlaceholders.get(category);
            List<ItemData> lockedItems = new ArrayList<>();
            List<Integer> lockedSlotIndices = new ArrayList<>();
            int[] slots = category.getSlots();
            for (int i = 0; i < slots.length; i++) {
                int slot = slots[i];
                SlotState state = states != null ? states.getOrDefault(slot, SlotState.UNKNOWN) : SlotState.UNKNOWN;
                if (category == EquipmentCategory.MODULES && !modulesScanned) {
                    state = SlotState.UNKNOWN;
                } else if (!equipmentRecognized) {
                    state = SlotState.UNKNOWN;
                }
                String label = category.slotLabel(i);
                if (state == SlotState.EMPTY && emptyLabels != null) {
                    String paperLabel = emptyLabels.get(slot);
                    if (paperLabel != null && !paperLabel.isEmpty()) {
                        label = paperLabel;
                    }
                }
                labels.add(label);
                statusSlots.add(slot);
                statuses.add(state.name().toLowerCase());
                if (state == SlotState.FILLED && map != null) {
                    ItemData data = map.get(slot);
                    if (data != null) {
                        if (data.name != null) {
                            data.name = ShareTextCleaner.cleanDisplayName(data.name);
                        }
                        items.add(data);
                        slotIndices.add(slot);
                    }
                } else if (state == SlotState.LOCKED && lockedMap != null) {
                    ItemData locked = lockedMap.get(slot);
                    if (locked != null) {
                        lockedItems.add(locked);
                        lockedSlotIndices.add(slot);
                    }
                }
            }

            CategoryBucket bucket = new CategoryBucket();
            bucket.filled = (int) statuses.stream().filter(s -> "filled".equals(s)).count();
            bucket.total = category.getCapacity();
            bucket.items = items;
            bucket.slotIndices = slotIndices;
            bucket.slotStatuses = statuses;
            bucket.slotLabels = labels;
            bucket.statusesSlotIndices = statusSlots;
            bucket.lockedItems = lockedItems.isEmpty() ? null : lockedItems;
            bucket.lockedSlotIndices = lockedSlotIndices.isEmpty() ? null : lockedSlotIndices;
            snapshot.equipment.put(category.name().toLowerCase(), bucket);
        }

        List<ItemData> cardItems = new ArrayList<>();
        List<Integer> cardSlots = new ArrayList<>();
        List<String> cardStatuses = new ArrayList<>();
        List<String> cardLabels = new ArrayList<>();
        List<Integer> cardStatusSlots = new ArrayList<>();
        int[] mappedCards = BuildSlotMapping.getCardSlots();
        for (int i = 0; i < mappedCards.length; i++) {
            int slot = mappedCards[i];
            SlotState state = cardStates.getOrDefault(slot, cardsRecognized ? SlotState.EMPTY : SlotState.UNKNOWN);
            cardLabels.add("Karte " + (i + 1));
            cardStatusSlots.add(slot);
            cardStatuses.add(state.name().toLowerCase());
            if (state == SlotState.FILLED) {
                ItemData data = cardsBySlot.get(slot);
                if (data != null) {
                    if (data.name != null) {
                        data.name = ShareTextCleaner.cleanDisplayName(data.name);
                    }
                    cardItems.add(data);
                    cardSlots.add(slot);
                }
            }
        }
        CategoryBucket cards = new CategoryBucket();
        cards.filled = (int) cardStatuses.stream().filter(s -> "filled".equals(s)).count();
        cards.total = BuildSlotMapping.getCardCapacity();
        cards.items = cardItems;
        cards.slotIndices = cardSlots;
        cards.slotStatuses = cardStatuses;
        cards.slotLabels = cardLabels;
        cards.statusesSlotIndices = cardStatusSlots;
        snapshot.cards = cards;
        return snapshot;
    }

    private static int getContainerSlotCount(AbstractContainerScreen<?> screen, Minecraft client) {
        int total = screen.getMenu().slots.size();
        if (client.player == null) {
            return total;
        }
        int container = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory()) {
                break;
            }
            container++;
        }
        return container > 0 ? container : Math.max(0, total - 36);
    }

    private static int[] readScreenGeometry(AbstractContainerScreen<?> screen) {
        if (ContainerScreenBounds.isValid()) {
            return ContainerScreenBounds.asArray();
        }
        int left = 0;
        int top = 0;
        int width = 176;
        int height = 166;
        try {
            left = readIntField(screen, "leftPos", "field_2776", "x");
            top = readIntField(screen, "topPos", "field_2777", "y");
            width = readIntField(screen, "imageWidth", "field_2792", "backgroundWidth");
            height = readIntField(screen, "imageHeight", "field_2779", "backgroundHeight");
        } catch (Exception ignored) {
        }
        Minecraft client = Minecraft.getInstance();
        if (width <= 0) {
            width = 176;
        }
        if (height <= 0) {
            height = 166;
        }
        if (left == 0 && top == 0 && client != null) {
            left = (client.getWindow().getGuiScaledWidth() - width) / 2;
            top = (client.getWindow().getGuiScaledHeight() - height) / 2;
        }
        return new int[]{left, top, width, height};
    }

    private static int readIntField(Object target, String... names) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Field field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.getInt(target);
                } catch (NoSuchFieldException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchFieldException(names[0]);
    }

    private static void dumpMappedEquipmentSlots(AbstractContainerScreen<?> screen, boolean modulesVisible) {
        StringBuilder sb = new StringBuilder("Equipment-Slots:");
        for (EquipmentCategory category : EquipmentCategory.values()) {
            if (category == EquipmentCategory.MODULES && !modulesVisible) {
                sb.append(" [Module=nicht sichtbar]");
                continue;
            }
            sb.append(' ').append(category.getDisplayName()).append('{');
            for (int i : category.getSlots()) {
                if (i >= screen.getMenu().slots.size()) {
                    sb.append(i).append("=? ");
                    continue;
                }
                ItemStack stack = screen.getMenu().slots.get(i).getItem();
                if (stack == null || stack.isEmpty()) {
                    sb.append(i).append("=leer ");
                } else {
                    String name = ShareTextCleaner.cleanDisplayName(stack.getHoverName().getString());
                    sb.append(i).append('=').append(name.isEmpty() ? "<layout>" : name).append(' ');
                }
            }
            sb.append('}');
        }
        debug(sb.toString());
    }

    private static void dumpMappedCardSlots(AbstractContainerScreen<?> screen) {
        StringBuilder sb = new StringBuilder("Ausgerüstete Kartenslots: ");
        for (int i : BuildSlotMapping.getCardSlots()) {
            if (i >= screen.getMenu().slots.size()) {
                sb.append("[").append(i).append("=?] ");
                continue;
            }
            ItemStack stack = screen.getMenu().slots.get(i).getItem();
            if (stack == null || stack.isEmpty()) {
                sb.append("[").append(i).append("=leer] ");
                continue;
            }
            String name = ShareTextCleaner.cleanDisplayName(stack.getHoverName().getString());
            sb.append("[").append(i).append("=").append(name);
            if (!isUsableCardItem(stack)) {
                sb.append(" IGNORE");
            }
            sb.append("] ");
        }
        debug(sb.toString());
    }

    private static boolean isUsableCardItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String name = stack.getHoverName().getString();
        if (BuildSlotMapping.isPlaceholderName(name)
                || BuildSlotMapping.isPlaceholderName(ShareTextCleaner.cleanDisplayName(name))) {
            return false;
        }
        return !isLockedSlot(stack);
    }

    private static boolean isLockedSlot(ItemStack stack) {
        return stackTextContains(stack, BuildSlotMapping.LOCKED_SLOT_LORE)
            || stackTextContains(stack, BuildSlotMapping.LOCKED_SLOT_LORE_ALT);
    }

    private static boolean stackTextContains(ItemStack stack, String needle) {
        if (stack == null || needle == null || needle.isEmpty()) {
            return false;
        }
        String hover = stack.getHoverName().getString();
        if (hover != null && hover.contains(needle)) {
            return true;
        }
        try {
            net.minecraft.world.item.component.ItemLore lore =
                stack.get(net.minecraft.core.component.DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    if (line != null && line.getString().contains(needle)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String stripQuantitySuffix(String name) {
        if (name == null) {
            return null;
        }
        return name.replaceAll("\\s*\\(x\\d+\\)\\s*$", "").trim();
    }

    private static String stackSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return stack.getHoverName().getString() + "|" + stack.getCount() + "|" + stack.getItem().toString();
    }

    private static void debug(String msg) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().chatDebugging) {
            return;
        }
        System.out.println("[CCLive-Build] " + msg);
        sendPlayerFeedback("§7[Build-Debug] " + msg);
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

    private static final class SlotClassification {
        final SlotState state;
        final ItemData data;
        final String emptyLabel;

        private SlotClassification(SlotState state, ItemData data, String emptyLabel) {
            this.state = state;
            this.data = data;
            this.emptyLabel = emptyLabel;
        }

        static SlotClassification empty() {
            return new SlotClassification(SlotState.EMPTY, null, null);
        }

        static SlotClassification empty(String label) {
            return new SlotClassification(SlotState.EMPTY, null, label);
        }

        static SlotClassification locked(ItemData placeholder) {
            return new SlotClassification(SlotState.LOCKED, placeholder, null);
        }

        static SlotClassification filled(ItemData data) {
            return new SlotClassification(SlotState.FILLED, data, null);
        }
    }

    public static final class BuildSnapshot {
        public String kitName;
        public boolean equipmentRecognized;
        public boolean cardsRecognized;
        public boolean modulesScanned;
        public Map<String, CategoryBucket> equipment;
        public CategoryBucket cards;
    }

    public static final class CategoryBucket {
        public int filled;
        public int total;
        public List<ItemData> items;
        public List<Integer> slotIndices;
        /** Parallel zu allen Kategorie-Slots: filled/empty/locked/unknown */
        public List<String> slotStatuses;
        public List<String> slotLabels;
        public List<Integer> statusesSlotIndices;
        /** LOCKED-Platzhalter (nur Darstellung), parallel zu lockedSlotIndices. */
        public List<ItemData> lockedItems;
        public List<Integer> lockedSlotIndices;
    }
}
