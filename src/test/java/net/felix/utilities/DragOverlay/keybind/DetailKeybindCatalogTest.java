package net.felix.utilities.DragOverlay.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.felix.utilities.DragOverlay.detail.DetailSettingsFrame;
import java.util.List;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailKeybindCatalogTest {

    @BeforeEach
    void resetCapture() {
        DetailKeybindCapture.reset();
    }

    @Test
    void translationKeysFollowTheExistingBindings() {
        assertEquals(List.of(
                "key.cclive-utilities.toggle-blueprint-viewer",
                "key.cclive-utilities.toggle-blueprint-missing-mode",
                "key.cclive-utilities.next-blueprint-rarity",
                "key.cclive-utilities.previous-blueprint-rarity"),
                DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.BLUEPRINT));
        assertEquals(List.of(
                "key.cclive-utilities.toggle-clipboard",
                "key.cclive-utilities.clipboard.pin",
                "key.cclive-utilities.clipboard.previous-page",
                "key.cclive-utilities.clipboard.next-page"),
                DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.CLIPBOARD));
        assertEquals(List.of(
                "key.cclive-utilities.kills-toggle",
                "key.cclive-utilities.kills-reset"),
                DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.KILLS));
        assertEquals(List.of(
                "key.cclive-utilities.material-toggle",
                "key.cclive-utilities.material-rate-reset"),
                DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.MATERIAL));
    }

    @Test
    void cardsAndStatuesShareOneBinding() {
        List<String> cards = DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.CARDS);
        assertEquals(List.of("key.cclive-utilities.cards-toggle"), cards);
        assertEquals(cards, DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.STATUES));
    }

    @Test
    void overlaysWithoutTheirOwnBindingsHaveNoKeybindSection() {
        assertEquals(List.of(), DetailKeybindCatalog.translationKeys(DetailKeybindCatalog.Panel.NONE));
        assertFalse(DetailKeybindCatalog.hasKeybinds(DetailKeybindCatalog.Panel.NONE));
        assertEquals(16, DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.NONE));
        assertEquals(16 + 2 * 30 + 16, DetailKeybindCatalog.introducedHeight(DetailKeybindCatalog.Panel.KILLS));
    }

    @Test
    void escapeBackspaceAndModifiersDoNotAssign() {
        DetailKeybindCapture.begin();
        assertEquals(DetailKeybindCapture.Decision.IGNORE, DetailKeybindCapture.onKey(InputConstants.KEY_LSHIFT));
        assertTrue(DetailKeybindCapture.isCapturing());
        assertEquals("...", DetailKeybindCapture.bindingLabel(true, false, "F"));

        assertEquals(DetailKeybindCapture.Decision.CANCEL, DetailKeybindCapture.onKey(InputConstants.KEY_ESCAPE));
        assertFalse(DetailKeybindCapture.isCapturing());
        assertTrue(DetailKeybindCapture.blocksModHotkeys());

        DetailKeybindCapture.reset();
        DetailKeybindCapture.begin();
        assertEquals(DetailKeybindCapture.Decision.UNBIND, DetailKeybindCapture.onKey(InputConstants.KEY_BACKSPACE));
        DetailKeybindCapture.reset();
        DetailKeybindCapture.begin();
        assertEquals(DetailKeybindCapture.Decision.UNBIND, DetailKeybindCapture.onKey(InputConstants.KEY_DELETE));
        assertEquals("—", DetailKeybindCapture.bindingLabel(false, true, "F"));
    }

    @Test
    void sameBindingCanTakeItsPreviousKeyBack() {
        KeyMapping edited = new KeyMapping(
                "key.cclive-utilities.test-rebind",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                net.minecraft.client.KeyMapping.Category.MISC);
        KeyMapping controlsCopy = new KeyMapping(
                "key.cclive-utilities.test-rebind",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_G,
                net.minecraft.client.KeyMapping.Category.MISC);
        KeyMapping other = new KeyMapping(
                "key.cclive-utilities.test-other",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_J,
                net.minecraft.client.KeyMapping.Category.MISC);
        java.util.List<KeyMapping> mappings = java.util.List.of(edited, controlsCopy, other);

        assertFalse(DetailKeybindActions.conflictsWith(InputConstants.KEY_G, edited, mappings));
        assertTrue(DetailKeybindActions.conflictsWith(InputConstants.KEY_J, edited, mappings));
        assertFalse(DetailKeybindActions.conflictsWith(InputConstants.KEY_H, edited, mappings));
    }

    @Test
    void debugCategoryDoesNotBlockTheBlueprintDefaultKey() {
        KeyMapping edited = new KeyMapping(
                "key.cclive-utilities.toggle-blueprint-viewer",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                KeyMapping.Category.MISC);
        KeyMapping showHitboxes = new KeyMapping(
                "key.debug.showHitboxes",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                KeyMapping.Category.DEBUG);
        KeyMapping realConflict = new KeyMapping(
                "key.cclive-utilities.test-real-conflict",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                KeyMapping.Category.MISC);

        assertFalse(DetailKeybindActions.conflictsWith(
                InputConstants.KEY_B, edited, java.util.List.of(edited, showHitboxes)));
        assertTrue(DetailKeybindActions.conflictsWith(
                InputConstants.KEY_B, edited, java.util.List.of(edited, showHitboxes, realConflict)));
    }

    @Test
    void occupiedKeyDoesNotAssignAndKeepsCaptureOpen() {
        DetailKeybindCapture.begin();
        assertEquals(DetailKeybindCapture.Decision.CONFLICT,
                DetailKeybindCapture.onKey(InputConstants.KEY_F6, true));
        assertTrue(DetailKeybindCapture.isCapturing());
        assertTrue(DetailKeybindCapture.blocksModHotkeys());
        assertEquals(Integer.MIN_VALUE, DetailKeybindCapture.heldKey());
        assertEquals("Belegt", DetailKeybindCapture.bindingLabel(true, false, "F6", true));

        assertEquals(DetailKeybindCapture.Decision.ASSIGN,
                DetailKeybindCapture.onKey(InputConstants.KEY_F7, false));
        assertFalse(DetailKeybindCapture.isCapturing());
    }

    @Test
    void lockStaysUntilTheCapturedKeyIsReleased() {
        DetailKeybindCapture.begin();
        assertEquals(DetailKeybindCapture.Decision.ASSIGN, DetailKeybindCapture.onKey(InputConstants.KEY_F6));
        assertFalse(DetailKeybindCapture.isCapturing());
        assertTrue(DetailKeybindCapture.blocksModHotkeys());
        assertEquals(InputConstants.KEY_F6, DetailKeybindCapture.heldKey());

        DetailKeybindCapture.pollRelease(true);
        assertTrue(DetailKeybindCapture.blocksModHotkeys());

        DetailKeybindCapture.abort();
        assertTrue(DetailKeybindCapture.blocksModHotkeys());

        DetailKeybindCapture.pollRelease(false);
        assertFalse(DetailKeybindCapture.blocksModHotkeys());
        assertEquals("—", DetailKeybindCapture.bindingLabel(false, false, " "));
        assertEquals("F6", DetailKeybindCapture.bindingLabel(false, false, "F6"));
    }

    @Test
    void tallPanelScrollsInsteadOfShrinkingBelowTheWidthLimit() {
        float scale = DetailSettingsFrame.scale(800, 400, 300, 2000);
        assertEquals(Math.min(0.85f, (800 - 24) / 300f), scale, 0.0001f);
        int visible = DetailSettingsFrame.visibleHeight(400, scale, 2000);
        assertTrue(visible < 2000);
        assertEquals(30, DetailSettingsFrame.clampScroll(30, 2000, visible));
        assertEquals(2000 - visible, DetailSettingsFrame.clampScroll(99_999, 2000, visible));
        assertEquals(0, DetailSettingsFrame.clampScroll(-10, 2000, visible));
    }
}
