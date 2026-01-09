package net.felix.utilities.ItemViewer;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verwaltet die Favoriten-Blueprints in favorite_blueprints.json
 */
public class FavoriteBlueprintsManager {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String FAVORITES_FILE_NAME = "favorite_blueprints.json";
    private static File favoritesFile;
    private static Set<String> favoriteBlueprintNames = new HashSet<>();
    private static List<JsonObject> favoriteBlueprints = new ArrayList<>();
    
    /**
     * Initialisiert den Favoriten-Manager
     */
    public static void initialize() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            favoritesFile = configDir.resolve(FAVORITES_FILE_NAME).toFile();
            
            // Lade bestehende Favoriten
            loadFavorites();
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Lädt die Favoriten aus der JSON-Datei
     */
    private static void loadFavorites() {
        if (!favoritesFile.exists()) {
            // Erstelle leere Datei
            saveFavorites();
            return;
        }
        
        try (FileReader reader = new FileReader(favoritesFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            
            if (jsonElement.isJsonArray()) {
                favoriteBlueprints = new ArrayList<>();
                favoriteBlueprintNames = new HashSet<>();
                
                for (JsonElement element : jsonElement.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject blueprint = element.getAsJsonObject();
                        favoriteBlueprints.add(blueprint);
                        
                        // Extrahiere Name für schnelle Suche
                        if (blueprint.has("name") && blueprint.get("name").isJsonPrimitive()) {
                            String name = blueprint.get("name").getAsString();
                            favoriteBlueprintNames.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            favoriteBlueprints = new ArrayList<>();
            favoriteBlueprintNames = new HashSet<>();
        }
    }
    
    /**
     * Speichert die Favoriten in die JSON-Datei
     */
    private static void saveFavorites() {
        try {
            // Erstelle Verzeichnis falls nicht vorhanden
            favoritesFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(favoritesFile)) {
                gson.toJson(favoriteBlueprints, writer);
            }
        } catch (IOException e) {
            // Silent error handling
        }
    }
    
    /**
     * Prüft ob ein Blueprint in den Favoriten ist
     * @param blueprintName Der Name des Blueprints
     * @return true wenn in Favoriten, false sonst
     */
    public static boolean isFavorite(String blueprintName) {
        return favoriteBlueprintNames.contains(blueprintName);
    }
    
    /**
     * Fügt einen Blueprint zu den Favoriten hinzu
     * @param blueprint Der vollständige Blueprint als JsonObject (von { bis })
     */
    public static void addFavorite(JsonObject blueprint) {
        if (blueprint == null) {
            return;
        }
        
        // Extrahiere Name
        String name = null;
        if (blueprint.has("name") && blueprint.get("name").isJsonPrimitive()) {
            name = blueprint.get("name").getAsString();
        }
        
        if (name == null || name.isEmpty()) {
            return;
        }
        
        // Prüfe ob bereits vorhanden
        if (favoriteBlueprintNames.contains(name)) {
            return;
        }
        
        // Füge hinzu
        favoriteBlueprints.add(blueprint);
        favoriteBlueprintNames.add(name);
        saveFavorites();
    }
    
    /**
     * Entfernt einen Blueprint aus den Favoriten
     * @param blueprintName Der Name des Blueprints
     */
    public static void removeFavorite(String blueprintName) {
        if (blueprintName == null || blueprintName.isEmpty()) {
            return;
        }
        
        if (!favoriteBlueprintNames.contains(blueprintName)) {
            return;
        }
        
        // Entferne aus Liste
        favoriteBlueprints.removeIf(blueprint -> {
            if (blueprint.has("name") && blueprint.get("name").isJsonPrimitive()) {
                String name = blueprint.get("name").getAsString();
                return name.equals(blueprintName);
            }
            return false;
        });
        
        favoriteBlueprintNames.remove(blueprintName);
        saveFavorites();
    }
    
    /**
     * Gibt alle Favoriten-Blueprints zurück
     * @return Liste aller Favoriten-Blueprints
     */
    public static List<JsonObject> getFavorites() {
        return new ArrayList<>(favoriteBlueprints);
    }
    
    /**
     * Gibt alle Favoriten-Blueprint-Namen zurück
     * @return Set aller Favoriten-Namen
     */
    public static Set<String> getFavoriteNames() {
        return new HashSet<>(favoriteBlueprintNames);
    }
}

