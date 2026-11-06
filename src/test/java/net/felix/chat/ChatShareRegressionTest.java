package net.felix.chat;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatShareRegressionTest {

    @Test
    void defaultOffMakesCcliveWriteTargetAndDefaultOnRestoresWriteTarget() {
        ChatChannelRules.State bothOn = new ChatChannelRules.State(
            true, true, ChatManager.ChatMode.DEFAULT);
        ChatChannelRules.State defaultOff = ChatChannelRules.toggle(bothOn, "default");
        assertFalse(defaultOff.showDefault());
        assertTrue(defaultOff.showCCLive());
        assertEquals(ChatManager.ChatMode.CCLIVE, defaultOff.writeMode());

        ChatChannelRules.State defaultOnAgain = ChatChannelRules.toggle(defaultOff, "default");
        assertTrue(defaultOnAgain.showDefault());
        assertTrue(defaultOnAgain.showCCLive());
        assertEquals(ChatManager.ChatMode.DEFAULT, defaultOnAgain.writeMode());
    }

    @Test
    void vanillaTokensBecomeXdWithoutCcliveShareRefs() {
        String replaced = ItemShareManager.replaceItemTokenWithXd("hi !item and !build there");
        assertEquals("hi xD and xD there", replaced);
        assertFalse(replaced.contains("#ITEM:"));
        assertFalse(replaced.contains("#BUILD:"));
    }

    @Test
    void shareActionHintsAreOmittedOnlyInShareTooltips() {
        assertTrue(ChatTooltipLine.shouldOmitShareInventoryActionHint("[Linksklick] Ausrüstung auswählen"));
        assertTrue(ChatTooltipLine.shouldOmitShareInventoryActionHint("[Rechtsklick] Cosmetic auswählen"));
        assertFalse(ChatTooltipLine.shouldOmitShareInventoryActionHint("Stärke +12"));
        ChatTooltipLine keep = line("Stärke +12");
        ChatTooltipLine drop = line("[Linksklick] Ausrüstung auswählen");
        List<ChatTooltipLine> filtered = ChatTooltipLine.filterShareDisplayTooltipLines(List.of(keep, drop));
        assertEquals(1, filtered.size());
        assertEquals("Stärke +12", filtered.get(0).plainText());
    }

    @Test
    void layoutCleanerKeepsWordSpaces() {
        Component cleaned = ShareTextCleaner.cleanFormatted(Component.literal("Hallo  Welt"));
        assertEquals("Hallo  Welt", cleaned.getString());
        assertEquals("Hallo Welt", ShareTextCleaner.cleanDisplayName("  Hallo   Welt  "));
    }

    @Test
    void nameHoverDoesNotApplyToFollowingBodyText() {
        HoverEvent hover = tryShowTextHover(Component.literal("stats"));
        Component name = ChatNameColors.coloredName("Ab", new int[] { 0xFF0000, 0x00FF00 }, hover);
        Component body = Component.literal("nachricht").setStyle(Style.EMPTY.withColor(0xFBA9FC));
        Component line = Component.empty().append(name).append(Component.literal(">> ")).append(body);

        AtomicBoolean nameHadHover = new AtomicBoolean(false);
        AtomicBoolean bodyHadHover = new AtomicBoolean(false);
        line.visit((style, text) -> {
            boolean hasHover = style != null && style.getHoverEvent() != null;
            if ("Ab".equals(text) || "A".equals(text) || "B".equals(text)) {
                if (hasHover) {
                    nameHadHover.set(true);
                }
            }
            if (text != null && text.contains("nachricht") && hasHover) {
                bodyHadHover.set(true);
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (hover != null) {
            assertTrue(nameHadHover.get());
        }
        assertFalse(bodyHadHover.get());
    }

    @Test
    void ownBroadcastIsSkippedAfterLocalSendButSecondDistinctTimestampIsKept() {
        assertTrue(ChatDisplayDedup.skipOwnBroadcast(
            false, "Steve", "steve", "ok", "ok", 1000L, 1500L));
        assertFalse(ChatDisplayDedup.skipOwnBroadcast(
            true, "Steve", "steve", "ok", "ok", 1000L, 1500L));
        assertFalse(ChatDisplayDedup.skipOwnBroadcast(
            false, "Alex", "steve", "ok", "ok", 1000L, 1500L));
        assertFalse(ChatDisplayDedup.skipExactDuplicate(
            ChatDisplayDedup.exactKey("Alex", "ok", 1L),
            ChatDisplayDedup.exactKey("Alex", "ok", 2L)));
        assertTrue(ChatDisplayDedup.skipExactDuplicate(
            ChatDisplayDedup.exactKey("Alex", "ok", 1L),
            ChatDisplayDedup.exactKey("Alex", "ok", 1L)));
        assertEquals("id:9", ChatDisplayDedup.exactKey(9L, "Alex", "ok", 1L));
        assertTrue(ChatDisplayDedup.skipKnownId(java.util.Set.of(9L), 9L));
        assertFalse(ChatDisplayDedup.skipKnownId(java.util.Set.of(9L), null));
        Component tagged = Component.literal("[CCLive] ").setStyle(
            Style.EMPTY.withInsertion(ChatManager.MESSAGE_ID_PREFIX + "4"));
        assertTrue(ChatHudMessageIds.hasInsertion(tagged, ChatManager.MESSAGE_ID_PREFIX + "4"));
        assertFalse(ChatHudMessageIds.hasInsertion(tagged, ChatManager.MESSAGE_ID_PREFIX + "5"));
    }

    @Test
    void deletedMessageReplacesOnlyTheMatchingChatLine() {
        List<GuiMessage> messages = new ArrayList<>();
        messages.add(structuredLine(4L, "Alex", "eins #ITEM:ITEM-db2ac5"));
        messages.add(structuredLine(5L, "Sam", "zwei"));
        messages.add(new GuiMessage(3, Component.literal("vanilla"), null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system()));
        messages.add(chatLine(6L, ChatManager.PLACEHOLDER));

        int changed = ChatHudMessageIds.replaceMatching(messages, 4L, ChatManager.PLACEHOLDER);

        assertEquals(1, changed);
        String replaced = messages.get(0).content().getString();
        assertTrue(replaced.contains("[CCLive]"));
        assertTrue(replaced.contains("Alex"));
        assertTrue(replaced.contains(">>"));
        assertTrue(replaced.contains(ChatManager.PLACEHOLDER));
        assertEquals(ChatManager.DELETED_BODY_COLOR, textColor(messages.get(0).content(), "gelöscht"));
        assertNotEquals(ChatManager.DELETED_BODY_COLOR, textColor(messages.get(0).content(), "[CCLive]"));
        assertNotEquals(ChatManager.DELETED_BODY_COLOR, textColor(messages.get(0).content(), "Alex"));
        assertFalse(replaced.contains("eins"));
        assertFalse(replaced.contains("ITEM-db2ac5"));
        assertTrue(ChatHudMessageIds.hasInsertion(messages.get(0).content(), ChatManager.MESSAGE_ID_PREFIX + "4"));
        assertFalse(ChatHudMessageIds.hasInsertion(messages.get(0).content(), "cclive-share:ITEM-db2ac5"));
        assertTrue(messages.get(1).content().getString().contains("Sam"));
        assertTrue(messages.get(1).content().getString().contains("zwei"));
        assertTrue(ChatHudMessageIds.hasInsertion(messages.get(1).content(), ChatManager.MESSAGE_ID_PREFIX + "5"));
        assertEquals("vanilla", messages.get(2).content().getString());
        assertEquals(0, ChatHudMessageIds.replaceMatching(messages, 6L, ChatManager.PLACEHOLDER));
        assertTrue(messages.get(3).content().getString().contains(ChatManager.PLACEHOLDER));
        assertEquals(0, ChatHudMessageIds.replaceMatching(messages, 9L, ChatManager.PLACEHOLDER));

        HoverEvent hover = tryShowTextHover(Component.literal("stats"));
        if (hover != null) {
            AtomicBoolean nameHadHover = new AtomicBoolean(false);
            AtomicBoolean bodyHadHover = new AtomicBoolean(false);
            messages.get(0).content().visit((style, text) -> {
                boolean hasHover = style != null && style.getHoverEvent() != null;
                if (text != null && text.contains("Alex") && hasHover) nameHadHover.set(true);
                if (text != null && text.contains("gelöscht") && hasHover) bodyHadHover.set(true);
                return Optional.empty();
            }, Style.EMPTY);
            assertTrue(nameHadHover.get());
            assertFalse(bodyHadHover.get());
        }
    }

    @Test
    void deletionUsesExplicitFlagNotPlaceholderText() {
        com.google.gson.JsonObject typed = new com.google.gson.JsonObject();
        typed.addProperty("message", ChatManager.PLACEHOLDER);
        assertFalse(ChatManager.isExplicitlyDeleted(typed));
        com.google.gson.JsonObject deleted = new com.google.gson.JsonObject();
        deleted.addProperty("message", "hallo #ITEM:ITEM-db2ac5");
        deleted.addProperty("deleted", true);
        assertTrue(ChatManager.isExplicitlyDeleted(deleted));
        deleted.addProperty("deleted", false);
        assertFalse(ChatManager.isExplicitlyDeleted(deleted));
        GuiMessage typedLine = structuredLine(8L, "Sam", ChatManager.PLACEHOLDER);
        assertEquals(0xFBA9FC, textColor(typedLine.content(), "gelöscht"));
        assertEquals(0, ChatHudMessageIds.replaceMatching(new ArrayList<>(java.util.List.of(typedLine)), 4L, ChatManager.PLACEHOLDER));
        assertEquals(0xFBA9FC, textColor(typedLine.content(), "gelöscht"));
        assertEquals(ChatManager.DELETED_BODY_COLOR, ChatManager.deletedPlaceholderBody().getStyle().getColor().getValue());
    }

    @Test
    void websocketUrlConversionAndShareRefsInPlaintext() {
        assertEquals("wss://cclive-utilities.net", ChatWebSocket.toWebSocketUrl("https://cclive-utilities.net/"));
        assertEquals("ws://localhost:2062", ChatWebSocket.toWebSocketUrl("http://localhost:2062"));
        assertTrue("schau #ITEM:abc ende".matches("(?s).*#ITEM:abc.*"));
        assertTrue("a #BUILD:xyz b".contains("#BUILD:xyz"));
    }

    @Test
    void parseNameColorsFromPayloadKeepsGradient() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("name_color", "#FF0000");
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        arr.add("#FF0000");
        arr.add("#00FF00");
        obj.add("name_colors", arr);
        int[] colors = ChatNameColors.parseFromMessage(obj);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 0xFF0000, 0x00FF00 }, colors);
    }

    private static int textColor(Component line, String needle) {
        AtomicInteger color = new AtomicInteger(-1);
        line.visit((style, text) -> {
            if (text != null && text.contains(needle) && style != null && style.getColor() != null) {
                color.set(style.getColor().getValue());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return color.get();
    }

    private static HoverEvent tryShowTextHover(Component text) {
        try {
            Class<?>[] inner = HoverEvent.class.getDeclaredClasses();
            for (Class<?> type : inner) {
                for (java.lang.reflect.Constructor<?> constructor : type.getDeclaredConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();
                    if (params.length == 1 && params[0] == Component.class) {
                        constructor.setAccessible(true);
                        Object value = constructor.newInstance(text);
                        if (value instanceof HoverEvent event) {
                            return event;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static GuiMessage structuredLine(long id, String player, String text) {
        MutableComponent line = Component.empty();
        HoverEvent hover = tryShowTextHover(Component.literal("stats"));
        Style prefix = Style.EMPTY.withColor(0x9B029E).withInsertion(ChatManager.MESSAGE_ID_PREFIX + id);
        if (hover != null) {
            prefix = prefix.withHoverEvent(hover);
        }
        line.append(Component.literal("[CCLive] ").setStyle(prefix));
        Style name = Style.EMPTY.withColor(0xFFFFFF);
        if (hover != null) {
            name = name.withHoverEvent(hover);
        }
        line.append(Component.literal(player).setStyle(name));
        line.append(Component.literal(">> ").setStyle(Style.EMPTY.withColor(0xFFFFFF)));
        Style share = Style.EMPTY.withColor(0xFBA9FC).withInsertion("cclive-share:ITEM-db2ac5");
        HoverEvent shareHover = tryShowTextHover(Component.literal("share"));
        if (shareHover != null) {
            share = share.withHoverEvent(shareHover);
        }
        line.append(Component.literal(text).setStyle(share));
        return new GuiMessage(1, line, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system());
    }

    private static GuiMessage chatLine(long id, String text) {
        Component content = Component.literal("[CCLive] " + text).setStyle(
            Style.EMPTY.withInsertion(ChatManager.MESSAGE_ID_PREFIX + id));
        return new GuiMessage(1, content, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system());
    }

    private static ChatTooltipLine line(String text) {
        ChatTooltipLine row = new ChatTooltipLine();
        ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
        part.text = text;
        part.color = 0xFFFFFF;
        row.parts = new ArrayList<>();
        row.parts.add(part);
        return row;
    }
}
