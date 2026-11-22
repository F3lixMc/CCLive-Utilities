package net.felix.utilities.Other.serverpack;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPackRulesTest {

    private static final String HASH = "f3aaa3f670e64320020668db602f7bcd23e9c74e";
    private static final String OLD_HASH = "a2d220a4ffb29cd3b809f775654f45068529396c";

    @Test
    void normalizesOnlyRealSha1Hashes() {
        assertEquals(HASH, ServerPackRules.normalizeHash(" F3AAA3F670E64320020668DB602F7BCD23E9C74E "));
        assertNull(ServerPackRules.normalizeHash(""));
        assertNull(ServerPackRules.normalizeHash(null));
        assertNull(ServerPackRules.normalizeHash("xyz"));
        assertNull(ServerPackRules.normalizeHash(HASH + "0"));
    }

    @Test
    void recognizesOwnFilesAndPackIds() {
        String file = ServerPackRules.fileName(HASH);
        assertEquals("CCLive-Serverpack-" + HASH + ".zip", file);
        assertTrue(ServerPackRules.isCachedFileName(file));
        assertTrue(ServerPackRules.isCachedPackId("file/" + file));
        assertFalse(ServerPackRules.isCachedFileName("CCLive-Serverpack-abc.zip"));
        assertFalse(ServerPackRules.isCachedPackId(file));
        assertFalse(ServerPackRules.isCachedPackId("file/MyPack.zip"));
    }

    @Test
    void onlyCachesPlayLegendUrls() {
        assertTrue(ServerPackRules.isPlayLegendPackUrl(
            "https://s3.playlegend.net/resourcepack/production-system/packs/cactusclicker/pack.zip"));
        assertTrue(ServerPackRules.isPlayLegendPackUrl("https://PLAYLEGEND.NET/pack.zip"));
        assertFalse(ServerPackRules.isPlayLegendPackUrl("https://playlegend.net.evil.example/pack.zip"));
        assertFalse(ServerPackRules.isPlayLegendPackUrl("https://notplaylegend.net/pack.zip"));
        assertFalse(ServerPackRules.isPlayLegendPackUrl("https://lobfile.com/file/mKFTSygM.zip"));
        assertFalse(ServerPackRules.isPlayLegendPackUrl("not a url"));
        assertFalse(ServerPackRules.isPlayLegendPackUrl(null));
    }

    @Test
    void replacesOldCachedPackBelowServerPacks() {
        String oldId = ServerPackRules.packId(OLD_HASH);
        String newId = ServerPackRules.packId(HASH);
        List<String> selected = List.of("vanilla", oldId, "file/Mine.zip", "server/1");

        List<String> result = ServerPackRules.withCachedPack(selected, newId, id -> id.startsWith("server/"));

        assertEquals(List.of("vanilla", "file/Mine.zip", newId, "server/1"), result);
    }

    @Test
    void putsCachedPackOnTopWithoutServerPacks() {
        String newId = ServerPackRules.packId(HASH);
        List<String> result = ServerPackRules.withCachedPack(List.of("vanilla", "file/Mine.zip"), newId, id -> false);
        assertEquals(List.of("vanilla", "file/Mine.zip", newId), result);
        assertEquals(List.of("vanilla", "file/Mine.zip"), ServerPackRules.withoutCachedPacks(result));
    }
}
