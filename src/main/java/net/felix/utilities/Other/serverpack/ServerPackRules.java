package net.felix.utilities.Other.serverpack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Pure rules for the cached PlayLegend server resource pack: file naming and
 * selection-list edits. Kept free of Minecraft types so it can be unit tested.
 */
public final class ServerPackRules {

    /** File name prefix inside {@code .minecraft/resourcepacks}; the SHA-1 follows. */
    public static final String FILE_PREFIX = "CCLive-Serverpack-";
    public static final String FILE_SUFFIX = ".zip";
    /** Pack ids of {@code FolderRepositorySource} packs start with this. */
    public static final String FILE_PACK_ID_PREFIX = "file/";

    private ServerPackRules() {
    }

    /** Lowercase SHA-1 hex, or {@code null} if the server sent no usable hash. */
    public static String normalizeHash(String hash) {
        if (hash == null) {
            return null;
        }
        String normalized = hash.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[0-9a-f]{40}") ? normalized : null;
    }

    public static String fileName(String hash) {
        return FILE_PREFIX + hash + FILE_SUFFIX;
    }

    public static String packId(String hash) {
        return FILE_PACK_ID_PREFIX + fileName(hash);
    }

    /** True for any file this feature wrote, whatever its hash. */
    public static boolean isCachedFileName(String fileName) {
        return fileName != null
            && fileName.startsWith(FILE_PREFIX)
            && fileName.endsWith(FILE_SUFFIX)
            && normalizeHash(fileName.substring(FILE_PREFIX.length(), fileName.length() - FILE_SUFFIX.length())) != null;
    }

    public static boolean isCachedPackId(String packId) {
        return packId != null
            && packId.startsWith(FILE_PACK_ID_PREFIX)
            && isCachedFileName(packId.substring(FILE_PACK_ID_PREFIX.length()));
    }

    /**
     * Replaces every cached pack in {@code selected} (bottom to top order) with {@code newPackId}.
     * The new pack goes directly below the first fixed top pack (server packs), else on top.
     */
    public static List<String> withCachedPack(List<String> selected, String newPackId, Predicate<String> isFixedTop) {
        List<String> result = withoutCachedPacks(selected);
        int insertAt = result.size();
        for (int i = 0; i < result.size(); i++) {
            if (isFixedTop.test(result.get(i))) {
                insertAt = i;
                break;
            }
        }
        result.add(insertAt, newPackId);
        return result;
    }

    public static List<String> withoutCachedPacks(List<String> selected) {
        List<String> result = new ArrayList<>(selected.size() + 1);
        for (String id : selected) {
            if (!isCachedPackId(id)) {
                result.add(id);
            }
        }
        return result;
    }
}
