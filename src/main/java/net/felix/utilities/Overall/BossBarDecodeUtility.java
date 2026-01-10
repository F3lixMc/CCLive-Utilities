package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;

/**
 * Utility zum Dekodieren und Anzeigen von Bossbar-Texten
 * Dekodiert chinesische Zeichen aus der Bossbar und zeigt sie auf dem Bildschirm an
 */
public class BossBarDecodeUtility {
    
    private static boolean isInitialized = false;
    private static String lastDecodedText = "";
    private static long lastUpdateTime = 0;
    private static final long DISPLAY_TIMEOUT = 5000; // 5 Sekunden Anzeigedauer
    
    /**
     * Initialisiert die BossBarDecodeUtility
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Registriere HUD-Rendering
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            renderDecodedText(drawContext);
        });
        
        isInitialized = true;
    }
    
    /**
     * Verarbeitet eine Bossbar und dekodiert den Text
     * Wird vom BossBarMixin aufgerufen
     * 
     * @param bossBarText Der Text der Bossbar
     */
    public static void processBossBar(String bossBarText) {
        if (bossBarText == null || bossBarText.isEmpty()) {
            return;
        }
        
        try {
            String decoded = decodeBossBarText(bossBarText);
            if (decoded != null && !decoded.isEmpty()) {
                lastDecodedText = decoded;
                lastUpdateTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    // Pixel Spacer Zeichen für die Struktur-Erkennung
    private static final char VALUE_SEPARATOR = '㓾'; // Trennzeichen zwischen Werten
    private static final char VALUE_PREFIX = '㔘'; // Präfix vor jedem Wert-String
    
    /**
     * Dekodiert einen Bossbar-Text mit chinesischen Zeichen und extrahiert die Werte
     * 
     * @param text Der zu dekodierende Text
     * @return Die extrahierten Werte im Format "Wert1 | Wert2 | Wert3 | HH:MM:SS"
     */
    private static String decodeBossBarText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Prüfe die aktuelle Dimension, um das richtige Mapping zu verwenden
        Map<Character, String> mapping;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isFloorDimension = false;
        
        if (client != null && client.world != null) {
            String dimensionPath = client.world.getRegistryKey().getValue().getPath();
            isFloorDimension = dimensionPath.toLowerCase().contains("floor");
        }
        
        // Verwende Aincraft Font für Floor-Dimensionen, sonst Factory Font
        if (isFloorDimension) {
            mapping = ZeichenUtility.getAincraftFontFirstLine();
        } else {
            mapping = ZeichenUtility.getFactoryFontFirstLine();
        }
        
        if (mapping.isEmpty()) {
            return ""; // Kein Mapping verfügbar
        }
        
        // Hole Pixel Spacer Zeichen für die Struktur-Erkennung
        String pixelSpacer = ZeichenUtility.getPixelSpacer();
        
        // Extrahiere die Werte basierend auf der Struktur (Pixel Spacer Zeichen)
        return extractValuesByStructure(text, mapping, pixelSpacer);
    }
    
    /**
     * Extrahiert die Werte basierend auf der Struktur des Textes
     * Verwendet Pixel Spacer Zeichen - Werte beginnen mit 㔘 gefolgt von Spacer-Zeichen
     * und enden mit Spacer-Zeichen gefolgt von 㓾 oder einem anderen Spacer
     * 
     * @param originalText Der originale Text mit chinesischen Zeichen
     * @param mapping Das Mapping für die Dekodierung
     * @param pixelSpacer String mit allen Pixel Spacer Zeichen
     * @return Die extrahierten Werte im Format "Wert1 | Wert2 | Wert3 | HH:MM:SS"
     */
    private static String extractValuesByStructure(String originalText, Map<Character, String> mapping, String pixelSpacer) {
        if (originalText == null || originalText.isEmpty()) {
            return "";
        }
        
        java.util.List<String> valueStrings = new java.util.ArrayList<>();
        
        // Suche nach Wert-Strings, die mit 㔘 beginnen
        // Die Struktur ist: 㔘 + [Spacer-Zeichen] + [Wert-Zeichen] + [Spacer-Zeichen] + End-Marker
        for (int i = 0; i < originalText.length(); i++) {
            if (originalText.charAt(i) == VALUE_PREFIX) {
                // Finde den Start des eigentlichen Wertes (nach den Spacer-Zeichen)
                int valueStart = i + 1;
                
                // Überspringe die Spacer-Zeichen nach 㔘
                while (valueStart < originalText.length() && 
                       pixelSpacer.indexOf(originalText.charAt(valueStart)) >= 0) {
                    valueStart++;
                }
                
                // Finde das Ende: Suche nach dem nächsten 㔘 oder einem End-Marker
                // End-Marker sind typischerweise: 㔆, 㔃, 㔅 gefolgt von 㓾 oder direkt 㓾 wenn danach 㔘 kommt
                int valueEnd = -1;
                
                for (int j = valueStart; j < originalText.length(); j++) {
                    char c = originalText.charAt(j);
                    
                    // Wenn wir das nächste 㔘 finden, ist das vorherige das Ende
                    if (c == VALUE_PREFIX && j > valueStart) {
                        valueEnd = j;
                        break;
                    }
                    
                    // Prüfe auf End-Marker: 㔆, 㔃, 㔅 gefolgt von 㓾
                    if (j < originalText.length() - 1) {
                        char next = originalText.charAt(j + 1);
                        if ((c == '㔆' || c == '㔃' || c == '㔅') && next == VALUE_SEPARATOR) {
                            valueEnd = j;
                            break;
                        }
                        // Oder 㔆, 㔃, 㔅 direkt gefolgt von 㔘
                        if ((c == '㔆' || c == '㔃' || c == '㔅') && 
                            j + 1 < originalText.length() && 
                            originalText.charAt(j + 1) == VALUE_PREFIX) {
                            valueEnd = j + 1;
                            break;
                        }
                    }
                    
                    // Wenn wir 㓾 finden und danach direkt 㔘 kommt, ist 㓾 das Ende
                    if (c == VALUE_SEPARATOR && j + 1 < originalText.length()) {
                        if (originalText.charAt(j + 1) == VALUE_PREFIX) {
                            valueEnd = j;
                            break;
                        }
                    }
                }
                
                // Wenn wir kein Ende gefunden haben, nimm bis zum Ende des Textes
                if (valueEnd == -1) {
                    valueEnd = originalText.length();
                }
                
                // Extrahiere den Wert-String (nur die dekodierbaren Zeichen)
                if (valueEnd > valueStart) {
                    StringBuilder valueString = new StringBuilder();
                    for (int k = valueStart; k < valueEnd; k++) {
                        char c = originalText.charAt(k);
                        // Nur dekodierbare Zeichen hinzufügen (keine Spacer)
                        if (mapping.containsKey(c)) {
                            valueString.append(c);
                        }
                    }
                    if (valueString.length() > 0) {
                        valueStrings.add(valueString.toString());
                    }
                }
                
                // Setze i auf valueEnd, um den nächsten Wert zu finden
                i = valueEnd - 1;
            }
        }
        
        // Dekodiere die Wert-Strings
        java.util.List<String> decodedValues = new java.util.ArrayList<>();
        for (String valueString : valueStrings) {
            StringBuilder decoded = new StringBuilder();
            for (char c : valueString.toCharArray()) {
                String decodedChar = mapping.get(c);
                if (decodedChar != null) {
                    decoded.append(decodedChar);
                }
            }
            String decodedValue = decoded.toString();
            if (!decodedValue.isEmpty()) {
                decodedValues.add(decodedValue);
            }
        }
        
        // Suche nach der Zeit - dekodiere den gesamten Text für die Zeit-Suche
        String fullDecoded = "";
        for (char c : originalText.toCharArray()) {
            String decodedChar = mapping.get(c);
            if (decodedChar != null) {
                fullDecoded += decodedChar;
            }
        }
        
        // Suche nach Zeit im Format HH:MM:SS
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        java.util.regex.Matcher timeMatcher = timePattern.matcher(fullDecoded);
        
        String time = "";
        if (timeMatcher.find()) {
            time = timeMatcher.group(0); // HH:MM:SS
        }
        
        // Formatiere die Ausgabe mit Labels
        if (decodedValues.size() >= 3 && !time.isEmpty()) {
            return String.format("Souls: %s | Coins: %s | Cactus: %s | %s", 
                decodedValues.get(0), decodedValues.get(1), decodedValues.get(2), time);
        } else if (decodedValues.size() >= 3) {
            return String.format("Souls: %s | Coins: %s | Cactus: %s", 
                decodedValues.get(0), decodedValues.get(1), decodedValues.get(2));
        } else if (!time.isEmpty() && !decodedValues.isEmpty()) {
            // Wenn wir Zeit und Werte haben, aber weniger als 3 Werte
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < decodedValues.size(); i++) {
                if (i > 0) result.append(" | ");
                String label = "";
                if (i == 0) label = "Souls: ";
                else if (i == 1) label = "Coins: ";
                else if (i == 2) label = "Cactus: ";
                result.append(label).append(decodedValues.get(i));
            }
            result.append(" | ").append(time);
            return result.toString();
        } else if (!time.isEmpty()) {
            return time;
        } else if (!decodedValues.isEmpty()) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < decodedValues.size(); i++) {
                if (i > 0) result.append(" | ");
                String label = "";
                if (i == 0) label = "Souls: ";
                else if (i == 1) label = "Coins: ";
                else if (i == 2) label = "Cactus: ";
                result.append(label).append(decodedValues.get(i));
            }
            return result.toString();
        }
        
        return "";
    }
    
    /**
     * Rendert den dekodierten Text auf dem Bildschirm
     * 
     * @param context Der DrawContext zum Rendern
     */
    private static void renderDecodedText(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        
        // Prüfe ob der Text noch aktuell ist
        long currentTime = System.currentTimeMillis();
        if (lastDecodedText.isEmpty() || (currentTime - lastUpdateTime) > DISPLAY_TIMEOUT) {
            return;
        }
        
        // Position: Oben links, unter dem Titel
        int x = 10;
        int y = 30; // Etwas unter dem Titel
        
        // Hintergrund für bessere Lesbarkeit
        int textWidth = client.textRenderer.getWidth(lastDecodedText);
        int padding = 4;
        context.fill(
            x - padding, 
            y - padding, 
            x + textWidth + padding, 
            y + 10 + padding, 
            0x80000000 // Halbtransparentes Schwarz
        );
        
        // Rendere den dekodierten Text
        context.drawText(
            client.textRenderer,
            Text.literal(lastDecodedText),
            x,
            y,
            0xFFFFFFFF, // Weiß
            true // Mit Schatten
        );
    }
    
    /**
     * Gibt den letzten dekodierten Text zurück (für Debugging)
     * 
     * @return Der letzte dekodierte Text
     */
    public static String getLastDecodedText() {
        return lastDecodedText;
    }
    
    /**
     * Setzt den dekodierten Text zurück
     */
    public static void clearDecodedText() {
        lastDecodedText = "";
        lastUpdateTime = 0;
    }
}
