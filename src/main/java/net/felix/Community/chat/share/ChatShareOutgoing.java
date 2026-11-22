package net.felix.Community.chat.share;

import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.Community.chat.buildshare.BuildShareManager;

/** Platzhalter im Text, der wirklich an sendChat geht. */
public final class ChatShareOutgoing {
    public record Draft(ChatShareTexts.Replacement replacement, String shareId, Integer nameColor) {
    }

    private ChatShareOutgoing() {
    }

    public static Draft prepare(String message) {
        ItemShareManager items = ItemShareManager.getInstance();
        BuildShareManager builds = BuildShareManager.getInstance();
        String itemName = null;
        String itemId = null;
        Integer itemColor = null;
        if (items.hasValidShare()) {
            ItemData cache = items.getLocalItemCache();
            if (cache != null) {
                itemName = ChatShareNames.itemChatName(cache.name);
                if (itemName.isEmpty()) {
                    itemName = null;
                } else {
                    itemId = items.getCurrentShareId();
                    itemColor = ChatShareNames.itemNameColor(cache.chatTooltip, itemName);
                }
            }
        }
        String buildName = null;
        String buildId = null;
        if (builds.hasValidShare()) {
            String raw = builds.getKitName();
            if ((raw == null || raw.isEmpty()) && builds.getLocalSnapshot() != null) {
                raw = builds.getLocalSnapshot().kitName;
            }
            buildName = ShareTextCleaner.cleanDisplayName(raw);
            if (buildName.isEmpty()) {
                buildName = null;
            } else {
                buildId = builds.getCurrentShareId();
            }
        }
        ChatShareTexts.Replacement replacement = ChatShareTexts.replace(message, itemName, buildName);
        if (!replacement.share()) {
            return new Draft(replacement, null, null);
        }
        String shareId = "BUILD".equals(replacement.shareType()) ? buildId : itemId;
        if (shareId == null || shareId.isEmpty()) {
            return new Draft(ChatShareTexts.Replacement.failed(ChatShareTexts.NO_SHARE), null, null);
        }
        Integer nameColor = "BUILD".equals(replacement.shareType()) ? null : itemColor;
        return new Draft(replacement, shareId, nameColor);
    }
}
