package net.felix.chat;

/**
 * Sichtbarkeit und Schreibziel der Chat-Kanäle — ohne Minecraft-Laufzeit.
 */
public final class ChatChannelRules {
    public record State(boolean showDefault, boolean showCCLive, ChatManager.ChatMode writeMode) {
    }

    private ChatChannelRules() {
    }

    public static ChatManager.ChatMode clamp(ChatManager.ChatMode mode, boolean showDefault, boolean showCCLive) {
        if (mode == ChatManager.ChatMode.CCLIVE && !showCCLive) {
            return ChatManager.ChatMode.DEFAULT;
        }
        if (mode == ChatManager.ChatMode.DEFAULT && !showDefault) {
            return showCCLive ? ChatManager.ChatMode.CCLIVE : ChatManager.ChatMode.DEFAULT;
        }
        return mode;
    }

    public static State toggle(State state, String chatType) {
        boolean showDefault = state.showDefault;
        boolean showCCLive = state.showCCLive;
        ChatManager.ChatMode writeMode = state.writeMode;
        boolean wasDefaultVisible = showDefault;

        if ("default".equalsIgnoreCase(chatType)) {
            showDefault = !showDefault;
            if (!showDefault && !showCCLive) {
                showCCLive = true;
            }
            if (!showDefault && showCCLive) {
                writeMode = ChatManager.ChatMode.CCLIVE;
            } else if (showDefault && !wasDefaultVisible) {
                writeMode = ChatManager.ChatMode.DEFAULT;
            }
        } else if ("cclive".equalsIgnoreCase(chatType)) {
            showCCLive = !showCCLive;
            if (!showDefault && !showCCLive) {
                showDefault = true;
            }
        }

        if (!showDefault && !showCCLive) {
            showDefault = true;
        }
        writeMode = clamp(writeMode, showDefault, showCCLive);
        return new State(showDefault, showCCLive, writeMode);
    }
}
