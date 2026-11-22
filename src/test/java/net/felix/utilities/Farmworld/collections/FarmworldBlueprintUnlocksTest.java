package net.felix.utilities.Farmworld.collections;

import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmworldBlueprintUnlocksTest {
    @Test
    void collectionLevelUnlocksTheCatalogName() {
        var unlocks = FarmworldBlueprintUnlocks.read(JsonParser.parseString("""
                {"licenses":[{"name":"[Kupfermine]","aliases":"Kupfermine [Lizenz]","location":"Kohle Collection III"}],
                 "mining_gadgets":[{"name":"Erzbrecher","aliases":"Erzbrecher [Modul]","level":"I","location":"Kupfer Collection II"}]}
                """).getAsJsonObject());
        assertEquals("Kohle", unlocks.get(0).resource());
        assertEquals(3, unlocks.get(0).level());
        assertTrue(unlocks.get(0).names().contains("Kupfermine Lizenz"));
        assertTrue(unlocks.get(1).names().contains("Erzbrecher #1"));
        assertEquals(3, FarmworldBlueprintUnlocks.reachedLevel(250, Map.of(1, 50, 2, 100, 3, 250, 4, 1000)));
        assertEquals(2, FarmworldBlueprintUnlocks.reachedLevel(249, Map.of(1, 50, 2, 100, 3, 250)));
    }

    @Test
    void itemCollectionUsesTheViewerName() {
        var unlocks = FarmworldBlueprintUnlocks.readItems(JsonParser.parseString("""
                {"modules":[{"name":"Holzhacker #3","foundAt":[{"collection":"Fichtenholz V"}]}]}
                """).getAsJsonObject());
        assertEquals("Fichtenholz", unlocks.get(0).resource());
        assertEquals(5, unlocks.get(0).level());
        assertTrue(unlocks.get(0).names().contains("Holzhacker #3"));
        assertTrue(FarmworldBlueprintUnlocks.resourcesFor("Fichtenholz").contains("Fichtenholz"));
        assertTrue(FarmworldBlueprintUnlocks.resourcesFor("[Fichtenwald]").contains("Fichtenholz"));
    }

    @Test
    void licenseAliasesUseTheViewerName() {
        assertTrue(FarmworldBlueprintUnlocks.catalogNames("Strand [Lizenz]", "").contains("Dschungelholz/Strand Lizenz"));
        assertTrue(FarmworldBlueprintUnlocks.catalogNames("Magnet [Modul]", "II").contains("Eisenmagnet"));
        assertTrue(FarmworldBlueprintUnlocks.catalogNames("Modultasche", "2").contains("Modul-Tasche #2"));
    }
}
