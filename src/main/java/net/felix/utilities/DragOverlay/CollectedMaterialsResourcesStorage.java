package net.felix.utilities.DragOverlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectedMaterialsResourcesStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "collected_materials-ressources.json";
    private static final String OLD_MATERIALS_FILE = "collected_materials.json";
    private static final String OLD_RESOURCES_FILE = "collected_resources.json";
    private static final Map<String, Long> materials = new HashMap<>();
    private static final Map<String, Long> resources = new HashMap<>();
    /** Normalisierter Name → Menge (O(1)-Lookup statt linearem Scan). */
    private static final Map<String, Long> normalizedMaterials = new HashMap<>();
    private static final Map<String, Long> normalizedResources = new HashMap<>();
    private static final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private static File storageFile;
    private static boolean initialized = false;
    private static long lastKnownModified = -1L;
    private static long lastReloadCheck = 0L;
    private static final long RELOAD_CHECK_INTERVAL_MS = 1500L;
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path modConfigDir = configDir.resolve("cclive-utilities");
            storageFile = modConfigDir.resolve(FILE_NAME).toFile();
            migrateOldFilesIfNeeded(modConfigDir);
            load();
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    public static Map<String, Long> getAllMaterials() {
        ensureInitialized();
        refreshIfChanged();
        synchronized (materials) {
            return new HashMap<>(materials);
        }
    }
    
    public static Map<String, Long> getAllResources() {
        ensureInitialized();
        refreshIfChanged();
        synchronized (resources) {
            return new HashMap<>(resources);
        }
    }
    
    public static long getMaterialAmount(String materialName) {
        ensureInitialized();
        refreshIfChanged();
        if (materialName == null) {
            return 0L;
        }
        synchronized (materials) {
            Long direct = materials.get(materialName);
            if (direct != null) {
                return direct;
            }
            return normalizedMaterials.getOrDefault(normalizeName(materialName), 0L);
        }
    }
    
    public static long getResourceAmount(String resourceName) {
        ensureInitialized();
        refreshIfChanged();
        if (resourceName == null) {
            return 0L;
        }
        synchronized (resources) {
            Long direct = resources.get(resourceName);
            if (direct != null) {
                return direct;
            }
            return normalizedResources.getOrDefault(normalizeName(resourceName), 0L);
        }
    }
    
    /** Kopiert Materialien in {@code target} ohne zusätzliche Map-Allokation pro Lookup. */
    public static void copyMaterialsInto(Map<String, Long> target) {
        ensureInitialized();
        refreshIfChanged();
        if (target == null) {
            return;
        }
        synchronized (materials) {
            target.clear();
            target.putAll(materials);
        }
    }
    
    /** Kopiert Ressourcen in {@code target}. */
    public static void copyResourcesInto(Map<String, Long> target) {
        ensureInitialized();
        refreshIfChanged();
        if (target == null) {
            return;
        }
        synchronized (resources) {
            target.clear();
            target.putAll(resources);
        }
    }
    
    public static void updateMaterials(Map<String, Long> updates) {
        ensureInitialized();
        refreshIfChanged();
        if (updates == null || updates.isEmpty()) {
            return;
        }
        boolean changed = false;
        synchronized (materials) {
            for (Map.Entry<String, Long> entry : updates.entrySet()) {
                String name = entry.getKey();
                Long amount = entry.getValue();
                if (name == null || name.isEmpty() || amount == null) {
                    continue;
                }
                Long current = materials.get(name);
                if (current == null || !current.equals(amount)) {
                    materials.put(name, amount);
                    changed = true;
                }
            }
        }
        if (changed) {
            rebuildNormalizedCaches();
            scheduleSave();
        }
    }
    
    public static void updateResources(Map<String, Long> updates) {
        ensureInitialized();
        refreshIfChanged();
        if (updates == null || updates.isEmpty()) {
            return;
        }
        boolean changed = false;
        synchronized (resources) {
            for (Map.Entry<String, Long> entry : updates.entrySet()) {
                String name = entry.getKey();
                Long amount = entry.getValue();
                if (name == null || name.isEmpty() || amount == null) {
                    continue;
                }
                Long current = resources.get(name);
                if (current == null || !current.equals(amount)) {
                    resources.put(name, amount);
                    changed = true;
                }
            }
        }
        if (changed) {
            rebuildNormalizedCaches();
            scheduleSave();
        }
    }
    
    public static void updateMaterial(String name, long amount) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Map<String, Long> update = new HashMap<>();
        update.put(name, amount);
        updateMaterials(update);
    }
    
    public static void updateResource(String name, long amount) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Map<String, Long> update = new HashMap<>();
        update.put(name, amount);
        updateResources(update);
    }

    /** Addiert Mengen zu bestehenden Material-Einträgen (z. B. Fisch-Belohnungen im Chat). */
    public static void addMaterials(Map<String, Long> deltas) {
        ensureInitialized();
        refreshIfChanged();
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        boolean changed = false;
        synchronized (materials) {
            for (Map.Entry<String, Long> entry : deltas.entrySet()) {
                String name = entry.getKey();
                Long delta = entry.getValue();
                if (name == null || name.isEmpty() || delta == null || delta <= 0) {
                    continue;
                }
                long current = materials.getOrDefault(name, 0L);
                materials.put(name, current + delta);
                changed = true;
            }
        }
        if (changed) {
            rebuildNormalizedCaches();
            scheduleSave();
        }
    }

    public static void addMaterial(String name, long delta) {
        if (name == null || name.isEmpty() || delta <= 0) {
            return;
        }
        Map<String, Long> update = new HashMap<>();
        update.put(name, delta);
        addMaterials(update);
    }

    /** Addiert Mengen zu bestehenden Ressourcen-Einträgen (z. B. Legend+-Sammler). */
    public static void addResources(Map<String, Long> deltas) {
        ensureInitialized();
        refreshIfChanged();
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        boolean changed = false;
        synchronized (resources) {
            for (Map.Entry<String, Long> entry : deltas.entrySet()) {
                String name = entry.getKey();
                Long delta = entry.getValue();
                if (name == null || name.isEmpty() || delta == null || delta <= 0) {
                    continue;
                }
                long current = resources.getOrDefault(name, 0L);
                resources.put(name, current + delta);
                changed = true;
            }
        }
        if (changed) {
            rebuildNormalizedCaches();
            scheduleSave();
        }
    }

    public static void addResource(String name, long delta) {
        if (name == null || name.isEmpty() || delta <= 0) {
            return;
        }
        Map<String, Long> update = new HashMap<>();
        update.put(name, delta);
        addResources(update);
    }

    public static void subtractMaterial(String name, long delta) {
        if (name == null || name.isEmpty() || delta <= 0) {
            return;
        }
        long current = getMaterialAmount(name);
        updateMaterial(name, Math.max(0L, current - delta));
    }

    public static void subtractResource(String name, long delta) {
        if (name == null || name.isEmpty() || delta <= 0) {
            return;
        }
        long current = getResourceAmount(name);
        updateResource(name, Math.max(0L, current - delta));
    }

    /**
     * Besitzmenge für Pinboard-Materialien (materials + resources, normalisierter Lookup).
     */
    public static long getSyncedOwnedAmount(String name) {
        return Math.max(getMaterialAmount(name), getResourceAmount(name));
    }

    /**
     * Setzt Material- und Ressourcen-Eintrag konsistent (gleicher Name, z. B. nach Material-Bag / Actionbar).
     */
    public static void setSyncedOwnedAmount(String name, long amount) {
        if (name == null || name.isEmpty()) {
            return;
        }
        updateMaterial(name, amount);
        updateResource(name, amount);
    }

    public static void subtractSyncedOwnedAmount(String name, long delta) {
        if (name == null || name.isEmpty() || delta <= 0) {
            return;
        }
        long current = getSyncedOwnedAmount(name);
        setSyncedOwnedAmount(name, Math.max(0L, current - delta));
    }
    
    public static void resetAll() {
        ensureInitialized();
        synchronized (materials) {
            materials.clear();
        }
        synchronized (resources) {
            resources.clear();
        }
        rebuildNormalizedCaches();
        save();
    }
    
    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }
    
    private static void refreshIfChanged() {
        if (storageFile == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastReloadCheck < RELOAD_CHECK_INTERVAL_MS) {
            return;
        }
        lastReloadCheck = now;
        
        if (!storageFile.exists()) {
            synchronized (materials) {
                materials.clear();
            }
            synchronized (resources) {
                resources.clear();
            }
            lastKnownModified = 0L;
            return;
        }
        
        long modified = storageFile.lastModified();
        if (modified != lastKnownModified) {
            load();
            lastKnownModified = modified;
        }
    }
    
    private static void load() {
        if (storageFile == null) {
            return;
        }
        synchronized (materials) {
            materials.clear();
        }
        synchronized (resources) {
            resources.clear();
        }
        
        if (!storageFile.exists()) {
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(storageFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                readSection(root, "materials", materials);
                readSection(root, "resources", resources);
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        rebuildNormalizedCaches();
        
        if (storageFile.exists()) {
            lastKnownModified = storageFile.lastModified();
        }
    }
    
    private static void rebuildNormalizedCaches() {
        normalizedMaterials.clear();
        normalizedResources.clear();
        synchronized (materials) {
            for (Map.Entry<String, Long> entry : materials.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                normalizedMaterials.put(normalizeName(entry.getKey()), entry.getValue());
            }
        }
        synchronized (resources) {
            for (Map.Entry<String, Long> entry : resources.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                normalizedResources.put(normalizeName(entry.getKey()), entry.getValue());
            }
        }
    }
    
    /** Gleiche Normalisierung wie im Clipboard-Overlay (Vergleich ohne Leerzeichen/Formatierung). */
    static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }
    
    private static void readSection(JsonObject root, String sectionName, Map<String, Long> target) {
        if (root == null || !root.has(sectionName)) {
            return;
        }
        JsonElement section = root.get(sectionName);
        if (!section.isJsonObject()) {
            return;
        }
        JsonObject obj = section.getAsJsonObject();
        synchronized (target) {
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String name = entry.getKey();
                JsonElement value = entry.getValue();
                if (name == null || name.isEmpty() || value == null) {
                    continue;
                }
                try {
                    long amount = value.getAsLong();
                    target.put(name, amount);
                } catch (Exception ignored) {
                    // Ignore invalid values
                }
            }
        }
    }
    
    private static void save() {
        if (storageFile == null) {
            return;
        }
        try {
            File parent = storageFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(storageFile)) {
                GSON.toJson(buildJsonObject(), writer);
            }
            lastKnownModified = storageFile.lastModified();
        } catch (IOException e) {
            // Silent error handling
        }
    }
    
    private static JsonObject buildJsonObject() {
        JsonObject root = new JsonObject();
        root.add("materials", buildSection(materials));
        root.add("resources", buildSection(resources));
        return root;
    }
    
    private static JsonObject buildSection(Map<String, Long> source) {
        JsonObject obj = new JsonObject();
        synchronized (source) {
            for (Map.Entry<String, Long> entry : source.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return obj;
    }
    
    private static void scheduleSave() {
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }
        Thread saveThread = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            save();
            saveScheduled.set(false);
        }, "CCLive-Utilities-MaterialsResourcesSave");
        saveThread.setDaemon(true);
        saveThread.start();
    }
    
    private static void migrateOldFilesIfNeeded(Path modConfigDir) {
        if (storageFile == null || modConfigDir == null) {
            return;
        }
        if (storageFile.exists()) {
            return;
        }
        File oldMaterials = modConfigDir.resolve(OLD_MATERIALS_FILE).toFile();
        File oldResources = modConfigDir.resolve(OLD_RESOURCES_FILE).toFile();
        boolean hasOldData = (oldMaterials.exists() && oldMaterials.isFile()) ||
                             (oldResources.exists() && oldResources.isFile());
        if (!hasOldData) {
            return;
        }
        readOldFlatFile(oldMaterials, materials);
        readOldFlatFile(oldResources, resources);
        save();
    }
    
    private static void readOldFlatFile(File file, Map<String, Long> target) {
        if (file == null || !file.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                synchronized (target) {
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String name = entry.getKey();
                        JsonElement value = entry.getValue();
                        if (name == null || name.isEmpty() || value == null) {
                            continue;
                        }
                        try {
                            long amount = value.getAsLong();
                            target.put(name, amount);
                        } catch (Exception ignored) {
                            // Ignore invalid values
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
}
