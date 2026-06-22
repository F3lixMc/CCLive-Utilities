package net.felix.utilities.Overall;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.MaterialRateUtility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ActionBarData {

    private static final Map<String, Integer> materials = new HashMap<>();
    private static final List<Object> filteredTexts = new ArrayList<>();
    private static final Map<String, Text> materialTexts = new HashMap<>(); // Speichert originale Text-Objekte mit Farbcodes
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
        
        // Entferne Formatierungscodes für Pattern-Matching
        String cleanMessageString = messageString.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
        
        Matcher matcher = MATERIAL_PATTERN.matcher(cleanMessageString);
        if (matcher.find()) {
            String materialName = matcher.group(1).trim();
            // Entferne Formatierungscodes auch vom Materialnamen
            materialName = materialName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "").trim();
            int count = Integer.parseInt(matcher.group(2));
            
            // Update material count and store original Text object with color codes
            materials.put(materialName, count);
            materialTexts.put(materialName, message); // Speichere originale Text-Objekt mit Farbcodes
            updateFilteredTexts();
            if (CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerRateEnabled) {
                MaterialRateUtility.updateFromActionBar();
            }
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
            
            // Update material count and store original message with color codes
            materials.put(materialName, count);
            // Für String-Nachrichten können wir keine Farbcodes speichern
            updateFilteredTexts();
        }
    }
    
    /**
     * Prüft, ob der Spieler sich auf einer Floor-Ebene befindet
     * @return true wenn auf einem Floor, false sonst
     */
    public static boolean isOnFloor() {
        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
                
                // Prüfe ob sich die Dimension geändert hat
                String currentDim = client.world.getRegistryKey().getValue().toString();
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

    public static List<String> getSortedMaterialNames() {
        return materials.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static Text getMaterialText(String materialName) {
        return materialTexts.get(materialName);
    }

    public static Text getMaterialDisplayText(String materialName) {
        Text originalText = materialTexts.get(materialName);
        if (originalText != null) {
            return extractMaterialOnly(originalText, materialName);
        }
        Integer count = materials.get(materialName);
        if (count != null) {
            return Text.literal(materialName + " [" + count + "]");
        }
        return null;
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
    
    private static final Pattern MULTIPLIER_PREFIX = Pattern.compile("^(?:\\+\\d+|\\d+x)\\s*", Pattern.CASE_INSENSITIVE);

    private static Text extractMaterialOnly(Text originalText, String materialName) {
        try {
            MutableText nameColored = Text.empty();
            Style nameStyle = Style.EMPTY;

            for (Text sibling : originalText.getSiblings()) {
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
                            nameColored.append(Text.literal(namePart).setStyle(nameStyle));
                        }
                    }
                    break;
                }

                String namePart = content.stripTrailing();
                if (!namePart.isEmpty()) {
                    nameStyle = sibling.getStyle();
                    nameColored.append(Text.literal(namePart).setStyle(nameStyle));
                }
            }

            if (nameColored.getString().trim().isEmpty()) {
                nameColored = Text.literal(materialName);
                nameStyle = Style.EMPTY;
            } else {
                nameStyle = getLastPartStyle(nameColored, nameStyle);
            }

            Integer count = materials.get(materialName);
            if (count != null) {
                nameColored.append(Text.literal(" [" + count + "]").setStyle(nameStyle));
            }

            return nameColored;
        } catch (Exception e) {
            return buildFallbackDisplayText(materialName);
        }
    }

    private static Style getLastPartStyle(MutableText text, Style fallback) {
        if (text.getSiblings().isEmpty()) {
            return text.getStyle();
        }
        Text last = text.getSiblings().get(text.getSiblings().size() - 1);
        return last.getStyle() != null ? last.getStyle() : fallback;
    }

    public static Integer getMaterialNameColorRgb(String materialName) {
        Text originalText = materialTexts.get(materialName);
        if (originalText == null) {
            return null;
        }

        for (Text sibling : originalText.getSiblings()) {
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
        int rgb = color.getRgb() & 0xFFFFFF;
        if (rgb == 0xFFFFFF) {
            return null;
        }
        return rgb;
    }

    private static Integer findFirstNonWhiteColor(Text text) {
        if (text == null) {
            return null;
        }

        Integer direct = colorRgb(text.getStyle().getColor());
        if (direct != null) {
            return direct;
        }

        for (Text sibling : text.getSiblings()) {
            Integer nested = findFirstNonWhiteColor(sibling);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static Text buildFallbackDisplayText(String materialName) {
        Integer count = materials.get(materialName);
        if (count == null) {
            return Text.literal(materialName);
        }
        return Text.literal(materialName + " [" + count + "]");
    }
    

    

    

} 