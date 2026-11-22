package net.felix.Community.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import net.felix.Community.chat.buildshare.BuildShareManager;
import net.felix.Community.chat.share.ItemShareManager;

/**
 * Ermittelt den Chat-{@link Style} unter der Maus (26.1+: {@link ActiveTextCollector.ClickableStyleFinder}).
 */
public final class ChatStyleHelper {
    private ChatStyleHelper() {
    }

    /**
     * Style des Glyphen unter der Maus, mit derselben Chat-Geometrie wie der gerenderte Chat.
     * Anders als der Vanilla-Finder zählen auch Hover-Stile ohne ClickEvent und ohne Insertion.
     */
    public static Style getRenderedChatStyle(double mouseX, double mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.font == null || client.getWindow() == null) {
            return null;
        }
        ChatComponent chatHud = client.gui.hud.getChat();
        if (chatHud == null) {
            return null;
        }
        try {
            ChatComponent.DisplayMode mode = ChatComponent.DisplayMode.FOREGROUND;
            if (client.gui.screen() instanceof ChatScreen) {
                mode = resolveChatDisplayMode(client.gui.screen());
            }
            RenderedStyleFinder finder = new RenderedStyleFinder(client.font, (int) mouseX, (int) mouseY);
            chatHud.captureClickableText(
                finder,
                client.getWindow().getGuiScaledHeight(),
                client.gui.hud.getGuiTicks(),
                mode);
            return finder.result;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Style getHoveredChatStyle(double mouseX, double mouseY) {
        Style fromFinder = getStyleViaClickableFinder(mouseX, mouseY);
        if (fromFinder != null) {
            return fromFinder;
        }
        return getStyleViaLegacyReflection(mouseX, mouseY);
    }

    private static Style getStyleViaClickableFinder(double mouseX, double mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.font == null || client.getWindow() == null) {
            return null;
        }
        ChatComponent chatHud = client.gui.hud.getChat();
        if (chatHud == null) {
            return null;
        }
        try {
            ChatComponent.DisplayMode mode = ChatComponent.DisplayMode.FOREGROUND;
            if (client.gui.screen() instanceof ChatScreen) {
                // displayMode-Feld ist privat — FOREGROUND deckt den Normalfall ab
                mode = resolveChatDisplayMode(client.gui.screen());
            }
            ActiveTextCollector.ClickableStyleFinder finder =
                new ActiveTextCollector.ClickableStyleFinder(client.font, (int) mouseX, (int) mouseY)
                    .includeInsertions(true);
            chatHud.captureClickableText(
                finder,
                client.getWindow().getGuiScaledHeight(),
                client.gui.hud.getGuiTicks(),
                mode);
            return finder.result();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ChatComponent.DisplayMode resolveChatDisplayMode(net.minecraft.client.gui.screens.Screen screen) {
        try {
            for (java.lang.reflect.Field field : screen.getClass().getDeclaredFields()) {
                if (field.getType() == ChatComponent.DisplayMode.class) {
                    field.setAccessible(true);
                    Object v = field.get(screen);
                    if (v instanceof ChatComponent.DisplayMode mode) {
                        return mode;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ChatComponent.DisplayMode.FOREGROUND;
    }

    /** Fallback für ältere Mappings mit getTextStyleAt(double, double). */
    private static Style getStyleViaLegacyReflection(double mouseX, double mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }
        ChatComponent chatHud = client.gui.hud.getChat();
        if (chatHud == null) {
            return null;
        }
        Style best = null;
        try {
            for (java.lang.reflect.Method method : ChatComponent.class.getDeclaredMethods()) {
                if (method.getParameterCount() != 2
                        || method.getParameterTypes()[0] != double.class
                        || method.getParameterTypes()[1] != double.class) {
                    continue;
                }
                if (method.getReturnType() != Style.class) {
                    continue;
                }
                method.setAccessible(true);
                Object result = method.invoke(chatHud, mouseX, mouseY);
                if (!(result instanceof Style style) || style == null) {
                    continue;
                }
                if (BuildShareManager.extractShareIdFromStyle(style) != null) {
                    return style;
                }
                String insertion = style.getInsertion();
                if (insertion != null && insertion.startsWith(ItemShareManager.SHARE_INSERTION_PREFIX)) {
                    return style;
                }
                if (style.getHoverEvent() != null || style.getClickEvent() != null
                        || (insertion != null && !insertion.isEmpty())) {
                    best = style;
                } else if (best == null) {
                    best = style;
                }
            }
        } catch (Exception ignored) {
        }
        return best;
    }

    /**
     * Build-Share-ID nur wenn die Maus wirklich auf dem Link liegt (aktueller Hit-Test, kein lastHovered).
     */
    public static String resolveBuildShareIdAt(double mouseX, double mouseY) {
        Style style = getHoveredChatStyle(mouseX, mouseY);
        return BuildShareManager.extractShareIdFromStyle(style);
    }

    public static Component getHoveredChatComponent(double mouseX, double mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud.getChat() == null) {
            return null;
        }
        ChatComponent chatHud = client.gui.hud.getChat();
        try {
            for (java.lang.reflect.Method method : ChatComponent.class.getDeclaredMethods()) {
                if (method.getParameterCount() != 2
                        || method.getParameterTypes()[0] != double.class
                        || method.getParameterTypes()[1] != double.class) {
                    continue;
                }
                if (method.getReturnType() != Component.class) {
                    continue;
                }
                method.setAccessible(true);
                Object result = method.invoke(chatHud, mouseX, mouseY);
                if (result instanceof Component component) {
                    return component;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String findBuildShareIdInComponent(Component root) {
        if (root == null) {
            return null;
        }
        String id = BuildShareManager.extractShareIdFromStyle(root.getStyle());
        if (id != null) {
            return id;
        }
        for (Component sibling : root.getSiblings()) {
            id = findBuildShareIdInComponent(sibling);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    /** ClickEvent-Wert als String lesen (Record-Felder je nach MC-Version). */
    public static String clickEventValue(ClickEvent event) {
        if (event == null) {
            return null;
        }
        try {
            if (event instanceof Record record) {
                for (java.lang.reflect.RecordComponent rc : record.getClass().getRecordComponents()) {
                    Object v = rc.getAccessor().invoke(record);
                    if (v instanceof String s && !s.isEmpty()) {
                        return s;
                    }
                    if (v instanceof java.net.URI uri) {
                        return uri.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String s = String.valueOf(event);
        return s.isEmpty() || "null".equals(s) ? null : s;
    }

    /**
     * Wie {@link ActiveTextCollector.ClickableStyleFinder}, aber jeder Glyphen-Stil unter dem Cursor zählt.
     * Der Chat liefert dafür dieselben umbrochenen Zeilen wie beim Zeichnen.
     */
    private static final class RenderedStyleFinder implements ActiveTextCollector {
        private final Font font;
        private final int testX;
        private final int testY;
        private ActiveTextCollector.Parameters defaultParameters = new ActiveTextCollector.Parameters(new Matrix3x2f());
        private Style result;

        private RenderedStyleFinder(Font font, int testX, int testY) {
            this.font = font;
            this.testX = testX;
            this.testY = testY;
        }

        @Override
        public ActiveTextCollector.Parameters defaultParameters() {
            return defaultParameters;
        }

        @Override
        public void defaultParameters(ActiveTextCollector.Parameters parameters) {
            defaultParameters = parameters;
        }

        @Override
        public void accept(TextAlignment alignment, int x, int y, ActiveTextCollector.Parameters parameters,
                           FormattedCharSequence text) {
            int left = alignment.calculateLeft(x, font, text);
            GuiTextRenderState state = new GuiTextRenderState(
                font,
                text,
                parameters.pose(),
                left,
                y,
                ARGB.white(parameters.opacity()),
                0,
                true,
                true,
                parameters.scissor());
            ActiveTextCollector.findElementUnderCursor(state, testX, testY, style -> result = style);
        }

        @Override
        public void acceptScrolling(Component text, int x, int y, int width, int height, int offset,
                                    ActiveTextCollector.Parameters parameters) {
            defaultScrollingHelper(text, x, y, width, height, offset, font.width(text), 9, parameters);
        }
    }
}
