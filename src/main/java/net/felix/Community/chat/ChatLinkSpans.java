package net.felix.Community.chat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Dieselbe Link-Erkennung wie der Serverfilter. Klickziele gibt es nur für freigegebene HTTP- und HTTPS-Adressen.
 */
public final class ChatLinkSpans {
    private static final Pattern HTTP = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WWW = Pattern.compile(
        "(?<![\\p{L}\\p{N}_@/])www\\.[^\\s<>\"']+",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern BARE = Pattern.compile(
        "(?<![\\p{L}\\p{N}_@./-])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}]{2,}(?![\\p{L}\\p{N}_-])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SHARE = Pattern.compile("#(ITEM|BUILD):[A-Za-z0-9]+-[A-Za-z0-9]+");
    private static final Pattern TRAIL = Pattern.compile("[.,;:!?\\)\\]]+$");

    public record Link(int start, int end, URI target) {
    }

    private ChatLinkSpans() {
    }

    public static List<Link> find(String message, List<String> domains) {
        String source = blankShares(message == null ? "" : message);
        List<int[]> covered = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        take(HTTP, source, covered, links, domains, Kind.URL);
        take(WWW, source, covered, links, domains, Kind.WWW);
        take(BARE, source, covered, links, domains, Kind.BARE);
        links.sort(Comparator.comparingInt(Link::start));
        return links;
    }

    public static MutableComponent render(String text, Style base, List<String> domains) {
        Style plain = base == null ? Style.EMPTY : base;
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        List<Link> links = find(text, domains);
        if (links.isEmpty()) {
            return Component.literal(text).setStyle(plain);
        }
        MutableComponent out = Component.empty();
        int cursor = 0;
        for (Link link : links) {
            if (link.start() < cursor || link.end() > text.length() || link.start() >= link.end()) {
                continue;
            }
            if (link.start() > cursor) {
                out.append(Component.literal(text.substring(cursor, link.start())).setStyle(plain));
            }
            Style linked = plain.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(link.target()));
            out.append(Component.literal(text.substring(link.start(), link.end())).setStyle(linked));
            cursor = link.end();
        }
        if (cursor < text.length()) {
            out.append(Component.literal(text.substring(cursor)).setStyle(plain));
        }
        return out;
    }

    static boolean hostAllowed(String host, List<String> domains) {
        String name = host == null ? "" : host.toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
        if (name.isEmpty() || domains == null) {
            return false;
        }
        for (String domain : domains) {
            if (domain == null || domain.isEmpty()) {
                continue;
            }
            String allowed = domain.toLowerCase(Locale.ROOT);
            if (name.equals(allowed) || name.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private static void take(Pattern pattern, String source, List<int[]> covered, List<Link> links,
                             List<String> domains, Kind kind) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int start = matcher.start();
            int rawEnd = matcher.end();
            if (overlaps(covered, start, rawEnd)) {
                continue;
            }
            covered.add(new int[] { start, rawEnd });
            String raw = matcher.group();
            String visible = stripTrail(raw);
            if (visible.isEmpty()) {
                continue;
            }
            String host = hostOf(visible, kind);
            if (!hostAllowed(host, domains)) {
                continue;
            }
            URI target = clickTarget(visible, kind);
            if (target == null) {
                continue;
            }
            links.add(new Link(start, start + visible.length(), target));
        }
    }

    private static String hostOf(String visible, Kind kind) {
        if (kind == Kind.BARE) {
            String host = visible.toLowerCase(Locale.ROOT);
            return host.matches("[a-z0-9.-]+") ? host : "";
        }
        return hostFromUrl(visible);
    }

    private static String hostFromUrl(String text) {
        String withScheme = text.regionMatches(true, 0, "www.", 0, 4) ? "https://" + text : text;
        try {
            String host = URI.create(withScheme).getHost();
            if (host == null) {
                return "";
            }
            return host.toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
        } catch (Exception e) {
            return "";
        }
    }

    private static URI clickTarget(String visible, Kind kind) {
        String target = visible;
        if (kind != Kind.URL) {
            target = "https://" + visible;
        }
        try {
            URI uri = URI.create(target);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankShares(String message) {
        Matcher matcher = SHARE.matcher(message);
        StringBuilder out = new StringBuilder(message);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                out.setCharAt(i, ' ');
            }
        }
        return out.toString();
    }

    private static String stripTrail(String value) {
        return TRAIL.matcher(value == null ? "" : value).replaceFirst("");
    }

    private static boolean overlaps(List<int[]> spans, int start, int end) {
        for (int[] span : spans) {
            if (start < span[1] && end > span[0]) {
                return true;
            }
        }
        return false;
    }

    private enum Kind {
        URL,
        WWW,
        BARE
    }
}
