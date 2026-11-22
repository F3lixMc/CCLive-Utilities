package net.felix.utilities.Overall.clipboard.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CollectedMaterialsResourcesStorageTest {
    @TempDir Path directory;

    private final Map<String, Map<?, ?>> originalMaps = new LinkedHashMap<>();
    private File originalFile;
    private boolean originalInitialized;
    private long originalModified;
    private long originalGeneration;
    private Path file;

    @BeforeEach
    void prepare() throws Exception {
        originalFile = (File) field("storageFile").get(null);
        originalInitialized = field("initialized").getBoolean(null);
        originalModified = field("lastKnownModified").getLong(null);
        originalGeneration = generation().get();
        for (String name : new String[]{"materials", "resources", "essences", "other", "otherAbbreviated"}) {
            Map<?, ?> values = (Map<?, ?>) field(name).get(null);
            originalMaps.put(name, new HashMap<>(values));
            values.clear();
        }
        file = directory.resolve("materials.json");
        field("storageFile").set(null, file.toFile());
        field("initialized").setBoolean(null, true);
        Files.writeString(file, "{\"materials\":{\"Teststoff\":1234},\"resources\":{\"Testholz\":56},\"essence\":{},\"other\":{}}");
        invoke("load", new Class<?>[0]);
    }

    @AfterEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void restore() throws Exception {
        for (var entry : originalMaps.entrySet()) {
            Map target = (Map) field(entry.getKey()).get(null);
            target.clear();
            target.putAll(entry.getValue());
        }
        invoke("rebuildNormalizedCaches", new Class<?>[0]);
        field("storageFile").set(null, originalFile);
        field("initialized").setBoolean(null, originalInitialized);
        field("lastKnownModified").setLong(null, originalModified);
        field("saveIoDelayMillis").setLong(null, 0L);
        generation().set(originalGeneration);
    }

    @Test
    void failedReadsKeepAllPreviousBalancesAndRetryMarker() throws Exception {
        long modified = field("lastKnownModified").getLong(null);
        for (String damaged : new String[]{"{\"materials\":", "", "[]",
                "{\"materials\":{\"Teststoff\":0},\"resources\":[]}"}) {
            Files.writeString(file, damaged);
            invoke("load", new Class<?>[0]);
            assertBalances(1234, 56);
            assertEquals(modified, field("lastKnownModified").getLong(null));
        }
    }

    @Test
    void invalidBalancesOrUnknownDocumentKeepAllPreviousValues() throws Exception {
        for (String invalid : new String[]{"{}", "{\"materials\":{\"Teststoff\":0},\"resources\":{\"Testholz\":\"invalid\"}}"}) {
            Files.writeString(file, invalid);
            invoke("load", new Class<?>[0]);
            assertBalances(1234, 56);
        }
    }

    @Test
    void temporarilyMissingFileKeepsBalances() throws Exception {
        Files.delete(file);
        field("lastReloadCheck").setLong(null, 0);
        invoke("refreshIfChanged", new Class<?>[0]);
        assertBalances(1234, 56);
    }

    @Test
    void validReplacementIsAdoptedAfterFailedRead() throws Exception {
        Files.writeString(file, "{");
        invoke("load", new Class<?>[0]);
        Files.writeString(file, "{\"materials\":{\"Teststoff\":9876},\"resources\":{\"Testholz\":65}}");
        invoke("load", new Class<?>[0]);
        assertBalances(9876, 65);
    }

    @Test
    void saveProducesCompleteJsonWithoutTemporaryFiles() throws Exception {
        assertEquals(true, invoke("saveReturningSuccess", new Class<?>[]{long.class}, generation().get()));
        JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(1234, saved.getAsJsonObject("materials").get("Teststoff").getAsLong());
        assertEquals(56, saved.getAsJsonObject("resources").get("Testholz").getAsLong());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void readDuringSaveDoesNotWaitForFileIo() throws Exception {
        field("lastReloadCheck").setLong(null, System.currentTimeMillis());
        field("saveIoDelayMillis").setLong(null, 400L);
        ((AtomicBoolean) field("saveIoEntered").get(null)).set(false);
        Thread save = new Thread(() -> {
            try {
                invoke("saveReturningSuccess", new Class<?>[]{long.class}, generation().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        save.start();
        long waitStart = System.nanoTime();
        while (!((AtomicBoolean) field("saveIoEntered").get(null)).get()) {
            if (System.nanoTime() - waitStart > 2_000_000_000L) {
                fail("Speichern hat die Dateiwartezeit nicht erreicht");
            }
            Thread.sleep(5L);
        }
        long readStart = System.nanoTime();
        assertEquals(1234L, CollectedMaterialsResourcesStorage.getMaterialAmount("Teststoff"));
        long readMs = (System.nanoTime() - readStart) / 1_000_000L;
        assertTrue(readMs < 100L, "Lesen hat " + readMs + " ms auf die Datei gewartet");
        save.join(2_000L);
        assertFalse(save.isAlive());
    }

    @Test
    void staleReloadDoesNotRestoreBalancesAfterUpdateOrReset() throws Exception {
        long generation = generation().get();
        long revision = ((AtomicLong) field("memoryRevision").get(null)).get();
        ((AtomicLong) field("memoryRevision").get(null)).incrementAndGet();
        @SuppressWarnings("unchecked")
        Map<String, Long> materials = (Map<String, Long>) field("materials").get(null);
        materials.put("Teststoff", 9999L);
        Files.writeString(file, "{\"materials\":{\"Teststoff\":1234},\"resources\":{\"Testholz\":56}}");
        field("lastKnownModified").setLong(null, -1L);
        invoke("reloadFromDisk", new Class<?>[]{long.class, long.class}, generation, revision);
        assertEquals(9999L, materials.get("Teststoff"));

        long resetGeneration = generation().get();
        long resetRevision = ((AtomicLong) field("memoryRevision").get(null)).get();
        assertTrue(CollectedMaterialsResourcesStorage.resetAllAndFlushSync());
        Files.writeString(file, "{\"materials\":{\"Teststoff\":1234},\"resources\":{\"Testholz\":56}}");
        field("lastKnownModified").setLong(null, -1L);
        invoke("reloadFromDisk", new Class<?>[]{long.class, long.class}, resetGeneration, resetRevision);
        assertTrue(((Map<?, ?>) field("materials").get(null)).isEmpty());
    }

    @Test
    void validLowerAmountReplacesThePreviousBalance() throws Exception {
        Files.writeString(file, "{\"materials\":{\"Teststoff\":10},\"resources\":{\"Testholz\":4}}");
        invoke("load", new Class<?>[0]);
        assertBalances(10, 4);
    }

    @Test
    void explicitResetStillPersistsAndRejectsOldSaves() throws Exception {
        long beforeReset = generation().get();
        assertTrue(CollectedMaterialsResourcesStorage.resetAllAndFlushSync());
        assertEquals(false, invoke("saveReturningSuccess", new Class<?>[]{long.class}, beforeReset));
        invoke("load", new Class<?>[0]);
        assertTrue(((Map<?, ?>) field("materials").get(null)).isEmpty());
        assertTrue(((Map<?, ?>) field("resources").get(null)).isEmpty());
    }

    private void assertBalances(long materials, long resources) throws Exception {
        assertEquals(materials, ((Map<?, ?>) field("materials").get(null)).get("Teststoff"));
        assertEquals(resources, ((Map<?, ?>) field("resources").get(null)).get("Testholz"));
    }

    private static AtomicLong generation() throws Exception {
        return (AtomicLong) field("saveGeneration").get(null);
    }

    private static Field field(String name) throws Exception {
        Field field = CollectedMaterialsResourcesStorage.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = CollectedMaterialsResourcesStorage.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}
