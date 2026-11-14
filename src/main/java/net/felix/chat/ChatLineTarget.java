package net.felix.chat;

import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Eine CCLive-Zeile. Name und Präfix gehören zu derselben Komponente,
 * auch wenn der Chat sie umbricht. Der Name trägt weder ein ClickEvent noch eine Insertion.
 */
public final class ChatLineTarget {
    static final int PREFIX_COLOR = 0x9B029E;
    private static final String ARROW = ">> ";

    public record Hit(String player, Long messageId) {
        public boolean canDelete() {
            return messageId != null && messageId > 0L;
        }
    }

    private record Run(String text, Style style) {
    }

    private ChatLineTarget() {
    }

    public static MutableComponent compose(String player, Long messageId, int[] colors, HoverEvent hover,
                                            Component icon, Component body) {
        Style prefixStyle = Style.EMPTY.withColor(PREFIX_COLOR);
        if (messageId != null && messageId > 0L) {
            prefixStyle = prefixStyle.withInsertion(ChatManager.MESSAGE_ID_PREFIX + messageId);
        }
        Style arrowStyle = Style.EMPTY.withColor(0xFFFFFF);
        if (hover != null) {
            prefixStyle = prefixStyle.withHoverEvent(hover);
            arrowStyle = arrowStyle.withHoverEvent(hover);
        }

        MutableComponent line = Component.empty();
        line.append(Component.literal("[CCLive] ").setStyle(prefixStyle));
        line.append(ChatNameColors.coloredName(player, colors, hover));
        if (icon != null) {
            line.append(Component.literal(" ").setStyle(arrowStyle));
            line.append(icon);
        }
        line.append(Component.literal(ARROW).setStyle(arrowStyle));
        if (body != null) {
            line.append(body);
        }
        return line;
    }

    public static boolean isShare(Style style) {
        if (style == null) {
            return false;
        }
        String insertion = style.getInsertion();
        if (startsWithShare(insertion)) {
            return true;
        }
        String click = ChatStyleHelper.clickEventValue(style.getClickEvent());
        return startsWithShare(click);
    }

    public static Hit resolve(Component line, Style hovered) {
        if (line == null || hovered == null || isShare(hovered)) {
            return null;
        }
        List<Run> runs = runs(line);
        int arrow = arrowAt(runs);
        if (arrow < 0 || !hasCclivePrefix(runs, arrow)) {
            return null;
        }
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).style == hovered && isMenuTarget(runs.get(i), i, arrow)) {
                return new Hit(playerName(runs, arrow), messageId(runs));
            }
        }
        return null;
    }

    public static Hit resolveAmong(List<Component> lines, Style hovered) {
        if (lines == null || hovered == null || isShare(hovered)) {
            return null;
        }
        for (Component line : lines) {
            if (line == null || !containsStyle(line, hovered)) {
                continue;
            }
            return resolve(line, hovered);
        }
        return null;
    }

    public static String playerOf(Component line) {
        List<Run> runs = runs(line);
        int arrow = arrowAt(runs);
        if (arrow < 0 || !hasCclivePrefix(runs, arrow)) {
            return null;
        }
        String name = playerName(runs, arrow);
        return name.isEmpty() ? null : name;
    }

    static Style prefixStyle(Component line) {
        List<Run> runs = runs(line);
        int arrow = arrowAt(runs);
        for (int i = 0; i < runs.size() && (arrow < 0 || i < arrow); i++) {
            if (isPrefix(runs.get(i).text)) {
                return runs.get(i).style;
            }
        }
        return null;
    }

    static Style arrowStyle(Component line) {
        List<Run> runs = runs(line);
        int arrow = arrowAt(runs);
        return arrow < 0 ? null : runs.get(arrow).style;
    }

    /**
     * Dieselbe Regel wie {@code ClickableStyleFinder}: nur ClickEvent oder Insertion.
     * Name und {@code >>} haben beides nicht und brauchen den gerenderten Hover-Treffer.
     */
    static boolean clickFinderAccepts(Style style) {
        if (style == null) {
            return false;
        }
        if (style.getClickEvent() != null) {
            return true;
        }
        return style.getInsertion() != null;
    }

    static Style nameStyle(Component line, int nameRun) {
        List<Run> runs = runs(line);
        int arrow = arrowAt(runs);
        int seen = 0;
        for (int i = 0; i < runs.size() && i < arrow; i++) {
            if (!isName(runs.get(i))) {
                continue;
            }
            if (seen == nameRun) {
                return runs.get(i).style;
            }
            seen++;
        }
        return null;
    }

    private static boolean containsStyle(Component line, Style hovered) {
        for (Run run : runs(line)) {
            if (run.style == hovered) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMenuTarget(Run run, int index, int arrow) {
        if (index == arrow) {
            return true;
        }
        if (index > arrow) {
            return false;
        }
        return isPrefix(run.text) || isName(run);
    }

    private static boolean isName(Run run) {
        if (run.text == null || run.text.isBlank() || isPrefix(run.text)) {
            return false;
        }
        return !PlayerIconUtility.containsChatIconMarker(run.text);
    }

    private static boolean isPrefix(String text) {
        return text != null && (text.equals("[CCLive] ") || text.startsWith("[CCLive]"));
    }

    private static boolean hasCclivePrefix(List<Run> runs, int arrow) {
        for (int i = 0; i < arrow; i++) {
            if (isPrefix(runs.get(i).text)) {
                return true;
            }
        }
        return false;
    }

    private static String playerName(List<Run> runs, int arrow) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < arrow; i++) {
            Run run = runs.get(i);
            if (isName(run)) {
                name.append(run.text);
            }
        }
        return name.toString();
    }

    private static Long messageId(List<Run> runs) {
        for (Run run : runs) {
            String insertion = run.style == null ? null : run.style.getInsertion();
            if (insertion != null && insertion.startsWith(ChatManager.MESSAGE_ID_PREFIX)) {
                try {
                    long id = Long.parseLong(insertion.substring(ChatManager.MESSAGE_ID_PREFIX.length()));
                    return id > 0L ? id : null;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static int arrowAt(List<Run> runs) {
        for (int i = 0; i < runs.size(); i++) {
            if (ARROW.equals(runs.get(i).text)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Run> runs(Component line) {
        List<Run> runs = new ArrayList<>();
        if (line == null) {
            return runs;
        }
        line.visit((style, text) -> {
            runs.add(new Run(text, style));
            return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }

    private static boolean startsWithShare(String value) {
        return value != null && (value.startsWith(ItemShareManager.SHARE_INSERTION_PREFIX)
            || value.startsWith(BuildShareManager.BUILD_INSERTION_PREFIX));
    }
}
