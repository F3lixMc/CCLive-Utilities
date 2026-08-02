package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Other.Clipboard.CollectedMaterialsResourcesStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ActionBarData {

    private static final Map<String, Integer> materials = new HashMap<>();
    private static final List<Object> filteredTexts = new ArrayList<>();
    private static final Map<String, Component> materialTexts = new HashMap<>(); // Speichert originale Text-Objekte mit Farbcodes
    /** Ausstehende Clipboard-Syncs – werden einmal pro Tick gebündelt. */
    private static final Map<String, Long> pendingStorageSync = new HashMap<>();
    private static boolean filteredTextsDirty = false;
    private static boolean postProcessScheduled = false;
    private static boolean tickHandlerRegistered = false;
    // Pattern für Materialien in der ActionBar: "zahlx material name [zahl]" oder "+zahl material name [zahl]"
    // Format: (optional "+" oder "x" nach Zahl) + Materialname + [Zahl]
    // Unterstützt formatierte Materialnamen und variable Leerzeichen
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("(?:\\+\\d+|\\d+x)\\s*([^\\[]+?)\\s*\\[(\\d+)\\]", Pattern.CASE_INSENSITIVE);
    private static String currentDimension = null; // Speichert die aktuelle Dimension
    private static Boolean cachedIsOnFloor = null; // Cache für Floor-Status
    
    public static void reset() {
        materials.clear();
        filteredTexts.clear();
        materialTexts.clear();
        pendingStorageSync.clear();
        filteredTextsDirty = false;
        postProcessScheduled = false;
        cachedIsOnFloor = null; // Cache zurücksetzen
    }

    private static void ensureTickHandlerRegistered() {
        if (tickHandlerRegistered) {
            return;
        }
        tickHandlerRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> flushPendingUpdates());
    }

    /**
     * Wendet gebündelte Actionbar-Updates an (max. einmal pro Client-Tick).
     */
    public static void flushPendingUpdates() {
        if (!postProcessScheduled) {
            return;
        }
        postProcessScheduled = false;

        if (!pendingStorageSync.isEmpty()) {
            CollectedMaterialsResourcesStorage.setSyncedOwnedAmounts(pendingStorageSync);
            pendingStorageSync.clear();
        }
        if (filteredTextsDirty) {
            updateFilteredTexts();
            filteredTextsDirty = false;
        }
    }

    private static void schedulePostProcess() {
        postProcessScheduled = true;
        filteredTextsDirty = true;
        ensureTickHandlerRegistered();
    }

    private static void recordMaterialDrop(String materialName, int count, Component message) {
        materials.put(materialName, count);
        if (message != null) {
            materialTexts.put(materialName, message);
        }
        pendingStorageSync.put(materialName, (long) count);
        schedulePostProcess();
    }
    
    public static void processActionBarMessage(net.minecraft.network.chat.Component message) {
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
        
        // Entferne Formatierungscodes für Pattern-Matching
        String cleanMessageString = messageString.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
        
        Matcher matcher = MATERIAL_PATTERN.matcher(cleanMessageString);
        if (matcher.find()) {
            String materialName = matcher.group(1).trim();
            // Entferne Formatierungscodes auch vom Materialnamen
            materialName = materialName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "").trim();
            int count = Integer.parseInt(matcher.group(2));
            recordMaterialDrop(materialName, count, message);
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
        
        // Entferne Formatierungscodes für Pattern-Matching
        String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
        Matcher matcher = MATERIAL_PATTERN.matcher(cleanMessage);
        if (matcher.find()) {
            String materialName = matcher.group(1).trim();
            // Entferne Formatierungscodes auch vom Materialnamen
            materialName = materialName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "").trim();
            int count = Integer.parseInt(matcher.group(2));
            recordMaterialDrop(materialName, count, null);
        }
    }
    
    /**
     * Prüft, ob der Spieler sich auf einer Floor-Ebene befindet
     * @return true wenn auf einem Floor, false sonst
     */
    public static boolean isOnFloor() {
        try {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.level != null) {
                String dimensionId = client.level.dimension().identifier().toString().toLowerCase();
                
                // Prüfe ob sich die Dimension geändert hat
                String currentDim = client.level.dimension().identifier().toString();
                if (currentDimension == null || !currentDimension.equals(currentDim)) {
                    // Dimension hat sich geändert - Cache zurücksetzen und neu berechnen
                    currentDimension = currentDim;
                    cachedIsOnFloor = null;
                }
                
                // Verwende Cache nur wenn verfügbar und Dimension gleich geblieben
                if (cachedIsOnFloor != null) {
                    return cachedIsOnFloor;
                }
                
                // Berechne neu
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
            net.minecraft.network.chat.Component originalText = materialTexts.get(materialName);
            
            if (originalText != null) {
                // Extrahiere nur den Materialteil aus dem Text-Objekt (ohne Multiplikator)
                // aber behalte die Farben
                net.minecraft.network.chat.Component materialOnlyText = extractMaterialOnly(originalText, materialName);
                filteredTexts.add(materialOnlyText);
            } else {
                // Fallback ohne Farbcodes
                filteredTexts.add(materialName + " [" + count + "]");
            }
        }
    }
    
    public static List<Object> getFilteredTexts() {
        flushPendingUpdates();
        return new ArrayList<>(filteredTexts);
    }
    
    public static Map<String, Integer> getMaterials() {
        return new HashMap<>(materials);
    }

    public static List<String> getSortedMaterialNames() {
        return materials.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static Component getMaterialText(String materialName) {
        return materialTexts.get(materialName);
    }

    public static Component getMaterialDisplayText(String materialName) {
        Component originalText = materialTexts.get(materialName);
        if (originalText != null) {
            return extractMaterialOnly(originalText, materialName);
        }
        Integer count = materials.get(materialName);
        if (count != null) {
            return Component.literal(materialName + " [" + count + "]");
        }
        return null;
    }
    
    public static boolean hasMaterials() {
        return !materials.isEmpty();
    }
    
    public static void checkDimensionChange() {
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client.level != null && client.player != null) {
                String newDimension = client.level.dimension().identifier().toString();
                
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
    
    private static final Pattern MULTIPLIER_PREFIX = Pattern.compile("^(?:\\+\\d+|\\d+x)\\s*", Pattern.CASE_INSENSITIVE);

    private static Component extractMaterialOnly(Component originalText, String materialName) {
        try {
            MutableComponent nameColored = Component.empty();
            Style nameStyle = Style.EMPTY;

            for (Component sibling : originalText.getSiblings()) {
                String content = sibling.getString();
                if (content.isEmpty() || MULTIPLIER_PREFIX.matcher(content).lookingAt()) {
                    continue;
                }

                int bracketIndex = content.indexOf('[');
                if (bracketIndex >= 0) {
                    if (bracketIndex > 0) {
                        String namePart = content.substring(0, bracketIndex).stripTrailing();
                        if (!namePart.isEmpty()) {
                            nameStyle = sibling.getStyle();
                            nameColored.append(Component.literal(namePart).setStyle(nameStyle));
                        }
                    }
                    break;
                }

                String namePart = content.stripTrailing();
                if (!namePart.isEmpty()) {
                    nameStyle = sibling.getStyle();
                    nameColored.append(Component.literal(namePart).setStyle(nameStyle));
                }
            }

            if (nameColored.getString().trim().isEmpty()) {
                nameColored = Component.literal(materialName);
                nameStyle = Style.EMPTY;
            } else {
                nameStyle = getLastPartStyle(nameColored, nameStyle);
            }

            Integer count = materials.get(materialName);
            if (count != null) {
                nameColored.append(Component.literal(" [" + count + "]").setStyle(nameStyle));
            }

            return nameColored;
        } catch (Exception e) {
            return buildFallbackDisplayText(materialName);
        }
    }

    private static Style getLastPartStyle(MutableComponent text, Style fallback) {
        if (text.getSiblings().isEmpty()) {
            return text.getStyle();
        }
        Component last = text.getSiblings().get(text.getSiblings().size() - 1);
        return last.getStyle() != null ? last.getStyle() : fallback;
    }

    public static Integer getMaterialNameColorRgb(String materialName) {
        Component originalText = materialTexts.get(materialName);
        if (originalText == null) {
            return null;
        }

        for (Component sibling : originalText.getSiblings()) {
            String content = sibling.getString();
            if (content.isEmpty() || MULTIPLIER_PREFIX.matcher(content).lookingAt()) {
                continue;
            }

            int bracketIndex = content.indexOf('[');
            if (bracketIndex >= 0) {
                if (bracketIndex > 0) {
                    Integer color = colorRgb(sibling.getStyle().getColor());
                    if (color != null) {
                        return color;
                    }
                }
                break;
            }

            Integer color = colorRgb(sibling.getStyle().getColor());
            if (color != null) {
                return color;
            }
        }

        return findFirstNonWhiteColor(originalText);
    }

    private static Integer colorRgb(TextColor color) {
        if (color == null) {
            return null;
        }
        int rgb = color.getValue() & 0xFFFFFF;
        if (rgb == 0xFFFFFF) {
            return null;
        }
        return rgb;
    }

    private static Integer findFirstNonWhiteColor(Component text) {
        if (text == null) {
            return null;
        }

        Integer direct = colorRgb(text.getStyle().getColor());
        if (direct != null) {
            return direct;
        }

        for (Component sibling : text.getSiblings()) {
            Integer nested = findFirstNonWhiteColor(sibling);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static Component buildFallbackDisplayText(String materialName) {
        Integer count = materials.get(materialName);
        if (count == null) {
            return Component.literal(materialName);
        }
        return Component.literal(materialName + " [" + count + "]");
    }
    

    

    

} 