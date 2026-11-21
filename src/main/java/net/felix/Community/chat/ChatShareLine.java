package net.felix.Community.chat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Liest Absender, Body und den leeren Datumsbaustein aus einer Systemzeile
 * und setzt Hover und Klick nur auf die gespeicherte Namensspanne.
 */
public final class ChatShareLine {
    public record Parsed(String sender, String body, String timestamp, String plain) {
        public boolean complete() {
            return sender != null && body != null;
        }
    }

    private record Run(String text, Style style) {
    }

    private ChatShareLine() {
    }

    public static Parsed parse(Component root) {
        if (root == null) {
            return new Parsed(null, null, null, "");
        }
        String plain = root.getString();
        if (plain == null) {
            plain = "";
        }
        return new Parsed(
                ChatShareTexts.senderName(plain),
                ChatShareTexts.bodyAfterArrow(plain),
                dateInsertion(root),
                plain);
    }

    /** Genau ein leerer Baustein vor {@code >>} mit 13 Ziffern. Sonst kein Zeitstempel. */
    public static String dateInsertion(Component root) {
        Scan scan = scan(root);
        return scan.found.size() == 1 ? scan.found.get(0) : null;
    }

    /** Kurztext, wenn kein Zeitstempel erkannt wurde: Insertionen vor dem ersten {@code >>}. */
    public static String dateDebug(Component root) {
        Scan scan = scan(root);
        if (!scan.debug.isEmpty()) {
            return scan.debug.toString().trim();
        }
        String first = scan.first == null ? "" : scan.first;
        String kind = root == null || root.getContents() == null
                ? "?"
                : root.getContents().getClass().getSimpleName();
        int siblings = root == null ? 0 : root.getSiblings().size();
        return "keine Insertion vor >> teile=" + scan.parts
                + " erster=[" + first + "] inhalt=" + kind
                + " geschwister=" + siblings
                + treeInsertions(root);
    }

    private static Scan scan(Component root) {
        Scan scan = new Scan();
        if (root != null) {
            scanComponent(root, scan);
        }
        return scan;
    }

    /**
     * Nur der eigene Literaltext zählt. {@code getString()} enthält schon die Kinder,
     * und damit würde {@code >>} den Datumsbaustein davor abschneiden.
     */
    private static void scanComponent(Component component, Scan scan) {
        if (component == null || scan.seen.indexOf(">>") >= 0) {
            return;
        }
        Style style = component.getStyle();
        String insertion = style == null ? null : style.getInsertion();
        var contents = component.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.PlainTextContents plain) {
            note(scan, plain.text() == null ? "" : plain.text(), insertion);
        } else if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (scan.seen.indexOf(">>") >= 0) {
                    return;
                }
                if (arg instanceof Component child) {
                    scanComponent(child, scan);
                } else if (arg != null) {
                    note(scan, String.valueOf(arg), null);
                }
            }
        }
        if (scan.seen.indexOf(">>") >= 0) {
            return;
        }
        for (Component sibling : component.getSiblings()) {
            scanComponent(sibling, scan);
            if (scan.seen.indexOf(">>") >= 0) {
                return;
            }
        }
    }

    private static void note(Scan scan, String part, String insertion) {
        if (part == null) {
            part = "";
        }
        scan.parts++;
        if (scan.first == null && !part.isEmpty()) {
            scan.first = part.length() > 40 ? part.substring(0, 40) : part;
        }
        if (part.isEmpty() && insertion != null && insertion.length() == 13 && digits(insertion)) {
            scan.found.add(insertion);
        }
        if (scan.debugCount < 6 && insertion != null && !insertion.isEmpty()) {
            scan.debugCount++;
            scan.debug.append(" own=").append(part.length()).append(" ins=")
                    .append(insertion, 0, Math.min(24, insertion.length()));
        }
        scan.seen.append(part);
    }

    private static String treeInsertions(Component component) {
        StringBuilder out = new StringBuilder();
        int[] shown = {0};
        collectInsertions(component, out, shown);
        return out.isEmpty() ? " baum=keine" : out.toString();
    }

    private static void collectInsertions(Component component, StringBuilder out, int[] shown) {
        if (component == null || shown[0] >= 4) {
            return;
        }
        Style style = component.getStyle();
        String insertion = style == null ? null : style.getInsertion();
        if (insertion != null && !insertion.isEmpty()) {
            shown[0]++;
            out.append(" baum=").append(insertion.length() > 16 ? insertion.substring(0, 16) : insertion);
        }
        var contents = component.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child) {
                    collectInsertions(child, out, shown);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectInsertions(sibling, out, shown);
        }
    }

    private static final class Scan {
        private final List<String> found = new ArrayList<>();
        private final StringBuilder seen = new StringBuilder();
        private final StringBuilder debug = new StringBuilder();
        private int parts;
        private int debugCount;
        private String first;
    }

    public static Component decorate(Component root, int nameStart, int nameLength, Style linkStyle) {
        if (root == null || linkStyle == null || nameLength < 1) {
            return root;
        }
        String plain = root.getString();
        if (plain == null) {
            return root;
        }
        int arrow = plain.indexOf(">>");
        if (arrow < 0) {
            return root;
        }
        int bodyOffset = arrow + 2;
        if (bodyOffset < plain.length() && plain.charAt(bodyOffset) == ' ') {
            bodyOffset++;
        }
        return decorateRange(root, plain, bodyOffset + nameStart, nameLength, linkStyle);
    }

    /** Hover und Klick auf eine absolute Spanne im Klartext der Komponente. */
    public static Component decorateRange(Component root, int absStart, int nameLength, Style linkStyle) {
        if (root == null || linkStyle == null || nameLength < 1) {
            return root;
        }
        String plain = root.getString();
        if (plain == null) {
            return root;
        }
        return decorateRange(root, plain, absStart, nameLength, linkStyle);
    }

    private static Component decorateRange(Component root, String plain, int absStart, int nameLength, Style linkStyle) {
        int absEnd = absStart + nameLength;
        if (absStart < 0 || absEnd > plain.length()) {
            return root;
        }
        List<Run> runs = new ArrayList<>();
        collect(root, Style.EMPTY, runs);
        int covered = 0;
        for (Run run : runs) {
            covered += run.text.length();
        }
        if (covered != plain.length() || absEnd > covered) {
            return root;
        }
        MutableComponent out = Component.empty();
        int cursor = 0;
        for (Run run : runs) {
            if (run.text.isEmpty()) {
                out.append(Component.empty().setStyle(run.style));
                continue;
            }
            int runEnd = cursor + run.text.length();
            int from = Math.max(cursor, absStart);
            int to = Math.min(runEnd, absEnd);
            if (from >= to) {
                out.append(Component.literal(run.text).setStyle(run.style));
            } else {
                if (from > cursor) {
                    out.append(Component.literal(run.text.substring(0, from - cursor)).setStyle(run.style));
                }
                out.append(Component.literal(run.text.substring(from - cursor, to - cursor)).setStyle(linked(run.style, linkStyle)));
                if (to < runEnd) {
                    out.append(Component.literal(run.text.substring(to - cursor)).setStyle(run.style));
                }
            }
            cursor = runEnd;
        }
        return out;
    }

    /** Inserts a component at a plain-text boundary while preserving inherited styles and empty metadata. */
    public static Component insertAt(Component root, int offset, Component addition) {
        if (root == null || addition == null || offset < 0 || offset > root.getString().length()) {
            return root;
        }
        List<Run> runs = new ArrayList<>();
        collect(root, Style.EMPTY, runs);
        MutableComponent out = Component.empty();
        int cursor = 0;
        boolean inserted = false;
        for (Run run : runs) {
            int end = cursor + run.text.length();
            if (!inserted && !run.text.isEmpty() && offset >= cursor && offset <= end) {
                int cut = offset - cursor;
                if (cut > 0) {
                    out.append(Component.literal(run.text.substring(0, cut)).setStyle(run.style));
                }
                out.append(addition);
                inserted = true;
                if (cut < run.text.length()) {
                    out.append(Component.literal(run.text.substring(cut)).setStyle(run.style));
                }
            } else {
                out.append(Component.literal(run.text).setStyle(run.style));
            }
            cursor = end;
        }
        if (!inserted) {
            out.append(addition);
        }
        return out;
    }

    private static Style linked(Style original, Style link) {
        Style style = original == null ? Style.EMPTY : original;
        style = style.withUnderlined(Boolean.TRUE).withInsertion(link.getInsertion());
        if (link.getColor() != null) {
            style = style.withColor(link.getColor());
        }
        if (link.getHoverEvent() != null) {
            style = style.withHoverEvent(link.getHoverEvent());
        }
        return style;
    }

    private static void collect(Component component, Style parent, List<Run> runs) {
        if (component == null) {
            return;
        }
        String own = ownLiteral(component);
        Style style = component.getStyle().applyTo(parent);
        if (own == null || own.isEmpty()) {
            if (style != null && (style.getHoverEvent() != null || style.getInsertion() != null || style.getClickEvent() != null)) {
                runs.add(new Run("", style));
            }
        } else {
            runs.add(new Run(own, style == null ? Style.EMPTY : style));
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, style, runs);
        }
    }

    private static String ownLiteral(Component component) {
        String full = component.getString();
        if (full == null) {
            return "";
        }
        StringBuilder siblings = new StringBuilder();
        for (Component sibling : component.getSiblings()) {
            String part = sibling.getString();
            if (part != null) {
                siblings.append(part);
            }
        }
        String suffix = siblings.toString();
        if (!suffix.isEmpty() && full.endsWith(suffix)) {
            return full.substring(0, full.length() - suffix.length());
        }
        return component.getSiblings().isEmpty() ? full : "";
    }

    private static boolean digits(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }
}
