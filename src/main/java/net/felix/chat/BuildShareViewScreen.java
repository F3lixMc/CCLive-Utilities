package net.felix.chat;

import net.felix.CCLiveUtilities;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerGrid;
import net.felix.utilities.Town.BuildSlotMapping;
import net.felix.utilities.Town.BuildSlotMapping.EquipmentCategory;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lokale Build-Share-Vorschau mit equipmentinventory_open_both.png.
 * Nur Anschauen/Hovern — keine Inventaraktionen.
 * Slot 0 im Kartenraster = Toggle für Prozent-/Flat-Overlays (links/rechts).
 */
public class BuildShareViewScreen extends Screen {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        CCLiveUtilities.MOD_ID, "textures/gui/equipmentinventory_open_both.png");
    private static final int TEX_SIZE = 256;

    /** Oberes Doppelkisten-Raster: Itemfläche Slot 0 relativ zum PNG. */
    private static final int EQUIP_ORIGIN_X = 48;
    private static final int EQUIP_ORIGIN_Y = 52;
    private static final int SLOT_STRIDE = 18;
    private static final int ITEM_SIZE = 16;

    /**
     * Unteres Kartenraster: Ursprung Spielerinventar-Slot 0 (PNG gemessen).
     * Layout (· = leer, Zahl = Kartenplatz 1–20):
     *  ·  1  2  3  4  5  6  7  ·
     *  ·  8  9 10 11 12 13 14  ·
     *  · 15 16 17  · 18 19 20  ·
     * Stats-Toggle: linke · der 3. Zeile (Slot 18), zusätzlich −2px X.
     * Save-Kit: rechte · der 3. Zeile (Slot 26), zusätzlich +2px X.
     */
    private static final int CARD_ORIGIN_X = 48;
    private static final int CARD_ORIGIN_Y = 173;
    private static final int STATS_TOGGLE_SLOT = 18;
    private static final int STATS_TOGGLE_X_NUDGE = -2;
    private static final int SAVE_KIT_SLOT = 26;
    private static final int SAVE_KIT_X_NUDGE = 2;
    private static final int[] CARD_TARGET_SLOTS = {
        1, 2, 3, 4, 5, 6, 7,
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 23, 24, 25
    };

    private static volatile BuildShareViewScreen activeInstance;

    private final BuildShareManager.BuildSnapshot snapshot;
    private final Screen previousScreen;
    private final Map<Integer, ItemData> equipmentSlots = new HashMap<>();
    private final Map<Integer, ItemData> cardSlots = new HashMap<>();

    private float uiScale = 1f;
    private int leftPos;
    private int topPos;
    private int drawSize;
    private ItemData hoveredItem;
    private boolean statsToggleHovered;
    private boolean saveKitHovered;
    /** Nach erfolgreichem Speichern kurz grünes Feedback-Highlight. */
    private long saveKitFlashUntilMs;
    /** Standard: Overlays aus; Button toggelt links/rechts. */
    private boolean showStatOverlays;
    private EquipmentDisplayUtility.AggregatedStats cachedStats;
    private int leftScrollOffset;
    private int rightScrollOffset;
    /** Erst nach Loslassen des Öffnen-Klicks: sonst schließt der gleiche Klick sofort wieder. */
    private boolean acceptOutsideClose;
    private final long openedAtMs = System.currentTimeMillis();

    public static boolean isActive() {
        return activeInstance != null;
    }

    public static BuildShareViewScreen getActive() {
        return activeInstance;
    }

    public BuildShareViewScreen(BuildShareManager.BuildSnapshot snapshot, Screen previousScreen) {
        super(Component.literal(
            snapshot != null && snapshot.kitName != null && !snapshot.kitName.isEmpty()
                ? snapshot.kitName
                : "Build"));
        this.snapshot = snapshot;
        this.previousScreen = previousScreen;
        activeInstance = this;
        BuildShareManager.clearLastHoveredShareId();
        loadItems();
        cachedStats = computeStatsFromFakeItems();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getWindow() != null) {
            this.width = client.getWindow().getGuiScaledWidth();
            this.height = client.getWindow().getGuiScaledHeight();
            recomputeLayout();
        }
    }

    private void loadItems() {
        if (snapshot == null) {
            return;
        }
        if (snapshot.equipment != null) {
            placeCategory("armor", EquipmentCategory.ARMOR);
            placeCategory("weapons", EquipmentCategory.WEAPONS);
            placeCategory("tools", EquipmentCategory.TOOLS);
            placeCategory("modules", EquipmentCategory.MODULES);
            placeLockedModules();
        }
        placeCardsCompact();
    }

    private void placeCategory(String key, EquipmentCategory category) {
        BuildShareManager.CategoryBucket bucket = snapshot.equipment.get(key);
        if (bucket == null || bucket.items == null) {
            return;
        }
        int[] mapped = category.getSlots();
        for (int i = 0; i < bucket.items.size(); i++) {
            ItemData item = bucket.items.get(i);
            if (item == null) {
                continue;
            }
            int slot;
            if (bucket.slotIndices != null && i < bucket.slotIndices.size()
                    && bucket.slotIndices.get(i) != null) {
                slot = bucket.slotIndices.get(i);
            } else if (i < mapped.length) {
                slot = mapped[i];
            } else {
                continue;
            }
            equipmentSlots.put(slot, item);
        }
    }

    private void placeLockedModules() {
        BuildShareManager.CategoryBucket bucket = snapshot.equipment.get("modules");
        if (bucket == null || bucket.lockedItems == null || bucket.lockedSlotIndices == null) {
            return;
        }
        for (int i = 0; i < bucket.lockedItems.size() && i < bucket.lockedSlotIndices.size(); i++) {
            ItemData item = bucket.lockedItems.get(i);
            Integer slot = bucket.lockedSlotIndices.get(i);
            if (item != null && slot != null) {
                equipmentSlots.put(slot, item);
            }
        }
    }

    /** Nur belegte Karten, kompakt auf die 20 Zielplätze (Reihenfolge 1–20). */
    private void placeCardsCompact() {
        if (snapshot.cards == null || snapshot.cards.items == null) {
            return;
        }
        int placed = 0;
        for (ItemData item : snapshot.cards.items) {
            if (item == null || placed >= CARD_TARGET_SLOTS.length) {
                continue;
            }
            cardSlots.put(CARD_TARGET_SLOTS[placed], item);
            placed++;
        }
    }

    /**
     * Wie EquipmentDisplayUtility: Stats aus Ausrüstungs-Slots (ohne Module),
     * Tooltip-Zeilen aus Share-Snapshot ({@code chatTooltip}).
     */
    private EquipmentDisplayUtility.AggregatedStats computeStatsFromFakeItems() {
        EquipmentDisplayUtility.AggregatedStats out = new EquipmentDisplayUtility.AggregatedStats();
        Set<Integer> statSlots = new HashSet<>();
        for (int s : BuildSlotMapping.getEquipmentStatSlots()) {
            statSlots.add(s);
        }
        for (Map.Entry<Integer, ItemData> entry : equipmentSlots.entrySet()) {
            if (entry.getKey() == null || !statSlots.contains(entry.getKey())) {
                continue;
            }
            ItemData item = entry.getValue();
            if (item == null) {
                continue;
            }
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
            if (item.chatTooltip != null && !item.chatTooltip.isEmpty()) {
                for (ChatTooltipLine tip : ChatTooltipLine.filterShareDisplayTooltipLines(item.chatTooltip)) {
                    if (tip != null) {
                        lines.add(tip.toComponent());
                    }
                }
            } else if (item.name != null) {
                lines.add(Component.literal(item.name));
            }
            EquipmentDisplayUtility.accumulateTooltipLines(out, lines);
        }
        return out;
    }

    @Override
    protected void init() {
        super.init();
        recomputeLayout();
    }

    private void recomputeLayout() {
        int availW = Math.max(1, this.width - 8);
        int availH = Math.max(1, this.height - 8);
        uiScale = Math.min(1f, Math.min(availW / (float) TEX_SIZE, availH / (float) TEX_SIZE));
        drawSize = Math.max(1, Math.round(TEX_SIZE * uiScale));
        leftPos = (this.width - drawSize) / 2;
        topPos = (this.height - drawSize) / 2;
    }

    private int slotScreenX(int texX) {
        return leftPos + Math.round(texX * uiScale);
    }

    private int slotScreenY(int texY) {
        return topPos + Math.round(texY * uiScale);
    }

    private int slotScreenSize() {
        return Math.max(1, Math.round(ITEM_SIZE * uiScale));
    }

    private int equipTexX(int containerSlot) {
        return EQUIP_ORIGIN_X + (containerSlot % 9) * SLOT_STRIDE;
    }

    private int equipTexY(int containerSlot) {
        return EQUIP_ORIGIN_Y + (containerSlot / 9) * SLOT_STRIDE;
    }

    private int cardTexX(int playerInvSlot) {
        return CARD_ORIGIN_X + (playerInvSlot % 9) * SLOT_STRIDE;
    }

    private int cardTexY(int playerInvSlot) {
        return CARD_ORIGIN_Y + (playerInvSlot / 9) * SLOT_STRIDE;
    }

    private int statsButtonX() {
        return slotScreenX(cardTexX(STATS_TOGGLE_SLOT)) + Math.round(STATS_TOGGLE_X_NUDGE * uiScale);
    }

    private int statsButtonY() {
        return slotScreenY(cardTexY(STATS_TOGGLE_SLOT));
    }

    private boolean isOverStatsToggle(double mouseX, double mouseY) {
        int x = statsButtonX();
        int y = statsButtonY();
        int size = slotScreenSize();
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private int saveKitButtonX() {
        return slotScreenX(cardTexX(SAVE_KIT_SLOT)) + Math.round(SAVE_KIT_X_NUDGE * uiScale);
    }

    private int saveKitButtonY() {
        return slotScreenY(cardTexY(SAVE_KIT_SLOT));
    }

    private boolean isOverSaveKit(double mouseX, double mouseY) {
        int x = saveKitButtonX();
        int y = saveKitButtonY();
        int size = slotScreenSize();
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        recomputeLayout();
        hoveredItem = null;
        statsToggleHovered = isOverStatsToggle(mouseX, mouseY);
        saveKitHovered = isOverSaveKit(mouseX, mouseY);

        try {
            context.pose().pushMatrix();
            context.pose().translate(leftPos, topPos);
            context.pose().scale(uiScale, uiScale);
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                0, 0, 0.0f, 0.0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
            context.pose().popMatrix();
        } catch (Exception e) {
            context.fill(leftPos, topPos, leftPos + drawSize, topPos + drawSize, 0xFF2A2A2A);
        }

        // Stats-Toggle-Slot: Hover-Highlight (Icon steckt in der PNG)
        int btnX = statsButtonX();
        int btnY = statsButtonY() + 2; // Highlight 2px tiefer als Slot-Origin
        int btnSize = slotScreenSize();
        if (statsToggleHovered) {
            context.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, 0x80FFFFFF);
        }
        if (showStatOverlays) {
            context.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, 0x4022AAFF);
        }

        // Save-Kit (Slot 26): Icon in PNG; Hover transparent/weiß, nach Speichern kurz grün
        int saveX = saveKitButtonX();
        int saveY = saveKitButtonY() + 2;
        if (System.currentTimeMillis() < saveKitFlashUntilMs) {
            context.fill(saveX, saveY, saveX + btnSize, saveY + btnSize, 0x8022AA55);
        } else if (saveKitHovered) {
            context.fill(saveX, saveY, saveX + btnSize, saveY + btnSize, 0x80FFFFFF);
        }

        for (Map.Entry<Integer, ItemData> entry : equipmentSlots.entrySet()) {
            int slot = entry.getKey();
            int x = slotScreenX(equipTexX(slot));
            int y = slotScreenY(equipTexY(slot));
            renderSlotItem(context, entry.getValue(), x, y, mouseX, mouseY);
        }

        for (Map.Entry<Integer, ItemData> entry : cardSlots.entrySet()) {
            int slot = entry.getKey();
            int x = slotScreenX(cardTexX(slot));
            int y = slotScreenY(cardTexY(slot));
            renderSlotItem(context, entry.getValue(), x, y, mouseX, mouseY);
        }

        if (showStatOverlays && cachedStats != null) {
            // Höhe = equipmentinventory_open_both (drawSize), bündig mit der PNG
            EquipmentDisplayUtility.renderOverlaysAt(
                context,
                leftPos, topPos, drawSize, drawSize,
                cachedStats.percentageStats,
                cachedStats.absoluteStats,
                cachedStats.totalArmor,
                true,
                leftScrollOffset,
                rightScrollOffset,
                true);
        }

        if (hoveredItem != null) {
            renderShareTooltip(context, hoveredItem, mouseX, mouseY);
        } else if (saveKitHovered) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("Als eigenes Kit speichern")
            ), mouseX, mouseY);
        } else if (statsToggleHovered) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal(showStatOverlays
                    ? "Stats-Ansicht ausblenden"
                    : "Stats-Ansicht einblenden (Prozent / Flat)")
            ), mouseX, mouseY);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderSlotItem(
            GuiGraphicsExtractor context, ItemData item, int x, int y, int mouseX, int mouseY) {
        if (item == null) {
            return;
        }
        ItemStack stack = ItemViewerGrid.createDisplayStack(item);
        int size = slotScreenSize();
        if (stack != null && !stack.isEmpty()) {
            if (uiScale >= 0.99f) {
                context.item(stack, x, y, 0);
            } else {
                context.pose().pushMatrix();
                context.pose().translate(x, y);
                context.pose().scale(uiScale, uiScale);
                context.item(stack, 0, 0, 0);
                context.pose().popMatrix();
            }
        }
        if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
            context.fill(x, y, x + size, y + size, 0x80FFFFFF);
            hoveredItem = item;
        }
    }

    private void renderShareTooltip(GuiGraphicsExtractor context, ItemData item, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (item.chatTooltip != null && !item.chatTooltip.isEmpty()) {
            for (ChatTooltipLine line : ChatTooltipLine.filterShareDisplayTooltipLines(item.chatTooltip)) {
                if (line == null) {
                    continue;
                }
                String plain = line.plainText();
                if (plain.isEmpty()) {
                    lines.add(Component.literal(" "));
                } else {
                    lines.add(line.toComponent());
                }
            }
        } else {
            lines.addAll(ItemViewerGrid.buildTooltipLinesForItem(item));
        }
        if (!lines.isEmpty()) {
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }

    private boolean isInsideTexture(double mouseX, double mouseY) {
        return mouseX >= leftPos && mouseX < leftPos + drawSize
            && mouseY >= topPos && mouseY < topPos + drawSize;
    }

    /** Scroll auf Prozent-/Flat-Overlays (wenn eingeblendet). */
    public boolean handleStatOverlayScroll(double mouseX, double mouseY, double vertical) {
        if (!showStatOverlays || cachedStats == null) {
            return false;
        }
        boolean[] hover = EquipmentDisplayUtility.computeOverlayHover(
            (int) mouseX, (int) mouseY,
            leftPos, topPos, drawSize, drawSize,
            cachedStats.percentageStats, cachedStats.absoluteStats,
            true);
        int scrollAmount = (int) (vertical * 12);
        int baseVisible = 14;
        if (hover[0]) {
            leftScrollOffset -= scrollAmount;
            leftScrollOffset = Math.max(0, leftScrollOffset);
            int maxScroll = Math.max(0, cachedStats.percentageStats.size() - baseVisible) * 12;
            leftScrollOffset = Math.min(leftScrollOffset, maxScroll);
            return true;
        }
        if (hover[1]) {
            rightScrollOffset -= scrollAmount;
            rightScrollOffset = Math.max(0, rightScrollOffset);
            int maxScroll = Math.max(0, cachedStats.absoluteStats.size() - baseVisible) * 12;
            rightScrollOffset = Math.min(rightScrollOffset, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        recomputeLayout();
        // Öffnen-Klick aus dem Chat nicht als „außerhalb“ werten
        if (!acceptOutsideClose || System.currentTimeMillis() - openedAtMs < 250L) {
            return true;
        }
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0 && isOverSaveKit(mx, my)) {
            ShareKitImportHelper.SaveResult result = ShareKitImportHelper.saveSnapshotAsCustomKit(snapshot);
            ShareKitImportHelper.sendFeedback(result);
            if (result != null && result.ok) {
                saveKitFlashUntilMs = System.currentTimeMillis() + 900L;
            }
            return true;
        }
        if (event.button() == 0 && isOverStatsToggle(mx, my)) {
            showStatOverlays = !showStatOverlays;
            if (!showStatOverlays) {
                leftScrollOffset = 0;
                rightScrollOffset = 0;
            } else if (cachedStats == null) {
                cachedStats = computeStatsFromFakeItems();
            }
            return true;
        }
        if (!isInsideTexture(mx, my)) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        acceptOutsideClose = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return true;
    }

    @Override
    public void onClose() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        BuildShareManager.clearLastHoveredShareId();
        BuildShareManager.getInstance().invalidatePendingOpens();
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(previousScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
