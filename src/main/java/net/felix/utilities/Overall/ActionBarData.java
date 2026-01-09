package net.felix.utilities.Overall;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ActionBarData {

    private static final Map<String, Integer> materials = new HashMap<>();
    private static final List<Object> filteredTexts = new ArrayList<>();
    private static final Map<String, net.minecraft.text.Text> materialTexts = new HashMap<>(); // Speichert originale Text-Objekte mit Farbcodes
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("(?:\\+\\d+|\\d+x)\\s+([^\\[]+)\\s*\\[(\\d+)\\]");
    private static String currentDimension = null; // Speichert die aktuelle Dimension
    private static Boolean cachedIsOnFloor = null; // Cache für Floor-Status
    
    public static void reset() {
        materials.clear();
        filteredTexts.clear();
        materialTexts.clear();
        cachedIsOnFloor = null; // Cache zurücksetzen
    }
    
    public static void processActionBarMessage(net.minecraft.text.Text message) {
        if (message == null) {
            return;
        }
        
        String messageString = message.getString();
        if (messageString.trim().isEmpty()) {
            return;
        }
        
        // Check if we're on a floor (using cached value)
        if (!isOnFloor()) {
            return;
        }
        
        Matcher matcher = MATERIAL_PATTERN.matcher(messageString);
        if (matcher.find()) {
            String materialName = matcher.group(1).trim();
            int count = Integer.parseInt(matcher.group(2));
            
            // Update material count and store original Text object with color codes
            materials.put(materialName, count);
            materialTexts.put(materialName, message); // Speichere originale Text-Objekt mit Farbcodes
            updateFilteredTexts();
        }
    }
    
    // Overload for backward compatibility
    public static void processActionBarMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Check if we're on a floor (using cached value)
        if (!isOnFloor()) {
            return;
        }
        
        Matcher matcher = MATERIAL_PATTERN.matcher(message);
        if (matcher.find()) {
            String materialName = matcher.group(1).trim();
            int count = Integer.parseInt(matcher.group(2));
            
            // Update material count and store original message with color codes
            materials.put(materialName, count);
            // Für String-Nachrichten können wir keine Farbcodes speichern
            updateFilteredTexts();
        }
    }
    
    private static boolean isOnFloor() {
        // Verwende den gecachten Wert, falls verfügbar
        if (cachedIsOnFloor != null) {
            return cachedIsOnFloor;
        }
        
        // Nur berechnen, wenn der Cache leer ist (beim ersten Aufruf)
        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
                boolean isFloor = dimensionId.contains("floor");
                cachedIsOnFloor = isFloor; // Cache den Wert
                return isFloor;
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return false;
    }
    
    private static void updateFilteredTexts() {
        filteredTexts.clear();
        
        // Sort materials by count (descending)
        List<Map.Entry<String, Integer>> sortedMaterials = new ArrayList<>(materials.entrySet());
        sortedMaterials.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Add materials to filtered texts with original color codes
        for (Map.Entry<String, Integer> entry : sortedMaterials) {
            String materialName = entry.getKey();
            int count = entry.getValue();
            net.minecraft.text.Text originalText = materialTexts.get(materialName);
            
            if (originalText != null) {
                // Extrahiere nur den Materialteil aus dem Text-Objekt (ohne Multiplikator)
                // aber behalte die Farben
                net.minecraft.text.Text materialOnlyText = extractMaterialOnly(originalText, materialName);
                filteredTexts.add(materialOnlyText);
            } else {
                // Fallback ohne Farbcodes
                filteredTexts.add(materialName + " [" + count + "]");
            }
        }
    }
    
    public static List<Object> getFilteredTexts() {
        return new ArrayList<>(filteredTexts);
    }
    
    public static Map<String, Integer> getMaterials() {
        return new HashMap<>(materials);
    }
    
    public static boolean hasMaterials() {
        return !materials.isEmpty();
    }
    
    public static void checkDimensionChange() {
        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                String newDimension = client.world.getRegistryKey().getValue().toString();
                
                // Materialien werden NICHT mehr beim Dimensionswechsel zurückgesetzt,
                // damit sie im Clipboard erhalten bleiben
                // if (currentDimension != null && !currentDimension.equals(newDimension)) {
                //     reset();
                // }
                
                // Wenn sich die Dimension geändert hat, den Floor-Cache zurücksetzen
                if (currentDimension == null || !currentDimension.equals(newDimension)) {
                    cachedIsOnFloor = null; // Cache zurücksetzen, damit isOnFloor() neu berechnet wird
                }
                
                currentDimension = newDimension;
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    private static net.minecraft.text.Text extractMaterialOnly(net.minecraft.text.Text originalText, String materialName) {
        try {
            // Erstelle eine neue leere Text-Komponente
            net.minecraft.text.MutableText output = net.minecraft.text.Text.literal("");
            
            // Über alle Siblings (Kinder) des originalen Text-Objekts iterieren
            for (net.minecraft.text.Text sibling : originalText.getSiblings()) {
                String content = sibling.getString();
                
                // Überspringe den Multiplikator-Teil (z.B. "3x " oder "+1 ")
                if (content.matches("^\\d+x\\s?") || content.matches("^\\+\\d+\\s?")) {
                    continue; // Überspringe diesen Teil
                }
                
                // Füge alle anderen Teile hinzu (behält die Formatierung/Farbe)
                output.append(sibling);
            }
            
            // Wenn wir nichts gefunden haben, verwende Fallback
            if (output.getString().trim().isEmpty()) {
                return net.minecraft.text.Text.literal(materialName + " [" + materials.get(materialName) + "]");
            }
            
            return output;
        } catch (Exception e) {
            // Fallback: Verwende den Materialnamen ohne spezielle Farbe
            return net.minecraft.text.Text.literal(materialName + " [" + materials.get(materialName) + "]");
        }
    }
    

    

    

} 