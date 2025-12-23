package net.felix.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Utility zum Anzeigen von Player-Stats in Chat-Nachrichten-Hover-Events
 * Extrahiert Spielernamen aus Chat-Nachrichten und fügt Stats zum Hover-Event hinzu
 */
public class PlayerHoverStatsUtility {
    private static boolean isInitialized = false;
    private static HttpClient httpClient;
    
    // Kein Pattern mehr nötig - wir durchsuchen die Text-Struktur rekursiv
    
    /**
     * Initialisiert die Utility
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        if (leaderboardManager == null) {
            return;
        }
        
        httpClient = leaderboardManager.getHttpClient();
        if (httpClient == null) {
            return;
        }
        
        isInitialized = true;
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging) {
            System.out.println("[PlayerHoverStats] ✅ Initialisiert");
        }
    }
    
    /**
     * Verarbeitet eine eingehende Chat-Nachricht und gibt eine modifizierte Version zurück
     * @param originalMessage Die ursprüngliche Nachricht
     * @return Die modifizierte Nachricht mit Stats-Hover, oder null wenn keine Modifikation nötig
     */
    public static Text processChatMessage(Text originalMessage) {
        if (originalMessage == null || httpClient == null) {
            return null;
        }
        
        // Prüfe ob Player Stats Debugging aktiviert ist
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // Extrahiere Spielernamen aus der Text-Struktur (rekursiv)
        String playerName = extractPlayerNameFromText(originalMessage);
        
        if (playerName == null || playerName.isEmpty()) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Kein Spielername gefunden in Chat-Nachricht");
            }
            return null;
        }
        
        // Filtere offensichtlich keine Spielernamen aus
        if (playerName.length() > 20 || playerName.length() < 2) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ Ungültiger Spielername (Länge): " + playerName);
            }
            return null;
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 Spielername extrahiert: " + playerName);
        }
        
        // Hole Stats vom Server synchron (da wir die Nachricht sofort modifizieren müssen)
        try {
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Hole Stats vom Server für: " + playerName);
            }
            
            JsonObject stats = httpClient.get("/profile/" + playerName);
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Stats-Response: " + (stats != null ? stats.toString() : "null"));
            }
            
            if (stats == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Keine Stats gefunden für: " + playerName);
                }
                return null; // Spieler nicht registriert oder Fehler → kein Hover-Override
            }
            
            // Prüfe ob Stats vorhanden sind
            if (!stats.has("player") || stats.get("player").isJsonNull()) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Spieler nicht registriert: " + playerName);
                }
                return null; // Spieler nicht registriert → kein Hover-Override
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ Stats gefunden, erstelle Hover-Text...");
            }
            
            // Erstelle Hover-Text mit Stats (inkl. bestehender Hover-Info)
            Text hoverText = createStatsHoverText(originalMessage, stats);
            if (hoverText == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Hover-Text konnte nicht erstellt werden");
                }
                return null;
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ Hover-Text erstellt, erstelle Hover-Event...");
                System.out.println("[PlayerHoverStats] 🔍 Hover-Text String: " + hoverText.getString());
                System.out.println("[PlayerHoverStats] 🔍 Hover-Text Siblings: " + hoverText.getSiblings().size());
            }
            
            // Erstelle neues Hover-Event
            HoverEvent newHoverEvent = createHoverEvent(hoverText);
            if (newHoverEvent == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Hover-Event konnte nicht erstellt werden");
                }
                return null;
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ Hover-Event erstellt, modifiziere Nachricht...");
            }
            
            // Modifiziere die Nachricht mit dem neuen Hover-Event
            Text modified = modifyTextWithHoverEvent(originalMessage, newHoverEvent);
            
            if (modified == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Nachricht konnte nicht modifiziert werden");
                }
                return null;
            }
            
            // Füge Icon zwischen Name und >> ein
            modified = insertIconBetweenNameAndArrow(modified, playerName);
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ Stats-Hover erstellt für: " + playerName);
            }
            
            return modified;
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ❌ Fehler beim Abrufen der Stats für " + playerName + ": " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Prüft ob die Utility initialisiert ist
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Extrahiert den Spielernamen aus einer Chat-Nachricht
     * Sucht rekursiv nach einem Text-Element, das direkt vor ">>" steht
     * Format basierend auf JSON: {color: "#7FE4CA", text: "firestarter03"} " " ">>" ...
     * Unterstützt auch verschachtelte extra-Arrays
     */
    private static String extractPlayerNameFromText(Text message) {
        if (message == null) {
            return null;
        }
        
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // Durchsuche die Siblings rekursiv
        List<Text> siblings = message.getSiblings();
        if (siblings != null && !siblings.isEmpty()) {
            // Suche nach dem Pattern: Text-Element mit Spielername, dann Icon-Marker (optional), dann ">>"
            // Gehe durch alle Siblings und prüfe ob das nächste Element ">>" enthält oder Icon-Marker
            for (int i = 0; i < siblings.size(); i++) {
                Text current = siblings.get(i);
                
                // Prüfe ob das nächste Element ">>" enthält (ignoriere Leerzeichen dazwischen)
                // Suche in den nächsten Siblings nach ">>" (max. 2 Siblings weiter, um Leerzeichen zu berücksichtigen)
                for (int j = i + 1; j < siblings.size() && j <= i + 2; j++) {
                    Text next = siblings.get(j);
                    String nextText = next.getString();
                    
                    // Prüfe ob next ">>" enthält
                    boolean isArrow = nextText != null && (nextText.contains(">>") || nextText.trim().equals(">>"));
                    // Prüfe ob next ein Leerzeichen ist und das übernächste ">>" enthält
                    boolean isSpace = nextText != null && (nextText.trim().isEmpty() || nextText.equals(" "));
                    boolean hasArrowAfterSpace = isSpace && j + 1 < siblings.size() && 
                        siblings.get(j + 1).getString() != null && 
                        (siblings.get(j + 1).getString().contains(">>") || siblings.get(j + 1).getString().trim().equals(">>"));
                    
                    if (isArrow || hasArrowAfterSpace) {
                        // current könnte der Spielername sein
                        String currentText = current.getString();
                        if (currentText != null && !currentText.trim().isEmpty()) {
                            String name = currentText.trim();
                            // Filtere offensichtlich keine Spielernamen aus
                            if (name.length() >= 2 && name.length() <= 20 && 
                                !name.equals(">>") && !name.equals("⸫") && !name.equals("ⶐ") &&
                                !name.equals(" ") && !name.matches("^\\s*$") &&
                                !name.equals("⸪") && !name.equals("⸈")) {
                                return name;
                            }
                        }
                        break; // ">>" gefunden, weiter suchen macht keinen Sinn
                    }
                }
                
                // WICHTIG: Prüfe auch verschachtelte Strukturen (extra-Arrays)
                // In verschachtelten Strukturen kann ">>" in einem anderen Sibling sein
                // Suche nach ">>" in allen nachfolgenden Siblings (auch rekursiv)
                // Berücksichtige auch Icon-Marker zwischen Name und ">>"
                for (int j = i + 1; j < siblings.size(); j++) {
                    Text checkNext = siblings.get(j);
                    String checkNextText = checkNext.getString();
                    boolean isArrow = checkNextText != null && (checkNextText.contains(">>") || checkNextText.trim().equals(">>"));
                    // Prüfe ob checkNext ein Leerzeichen ist und das nächste Element ">>" enthält
                    boolean isSpace = checkNextText != null && (checkNextText.trim().isEmpty() || checkNextText.equals(" "));
                    boolean hasArrowAfterSpace = isSpace && j + 1 < siblings.size() && 
                        siblings.get(j + 1).getString() != null && 
                        (siblings.get(j + 1).getString().contains(">>") || siblings.get(j + 1).getString().trim().equals(">>"));
                    
                    if (isArrow || hasArrowAfterSpace) {
                        // Found ">>" - check if current contains a valid name
                        String currentText = current.getString();
                        if (currentText != null && !currentText.trim().isEmpty()) {
                            String name = currentText.trim();
                            if (name.length() >= 2 && name.length() <= 20 && 
                                !name.equals(">>") && !name.equals("⸫") && !name.equals("ⶐ") &&
                                !name.equals(" ") && !name.matches("^\\s*$") &&
                                !name.equals("⸪") && !name.equals("⸈")) {
                                return name;
                            }
                        }
                        // Also check siblings between current and ">>" (ignoriere Leerzeichen)
                        for (int k = i + 1; k < j; k++) {
                            Text between = siblings.get(k);
                            String betweenText = between.getString();
                            if (betweenText != null && !betweenText.trim().isEmpty()) {
                                String name = betweenText.trim();
                                if (name.length() >= 2 && name.length() <= 20 && 
                                    !name.equals(">>") && !name.equals("⸫") && !name.equals("ⶐ") &&
                                    !name.equals(" ") && !name.matches("^\\s*$") &&
                                    !name.equals("⸪") && !name.equals("⸈") &&
                                    !name.equals(" ")) {
                                    return name;
                                }
                            }
                        }
                        break; // Found ">>", stop searching
                    }
                }
                
                // Rekursiv in Siblings suchen (wichtig für verschachtelte Strukturen)
                String found = extractPlayerNameFromText(current);
                if (found != null) {
                    return found;
                }
            }
        }
        
        // Prüfe auch den Haupttext selbst als Fallback (für einfache String-Nachrichten)
        String mainText = message.getString();
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 Haupttext: " + (mainText != null ? mainText.substring(0, Math.min(100, mainText.length())) : "null"));
            System.out.println("[PlayerHoverStats] 🔍 Anzahl Siblings: " + (siblings != null ? siblings.size() : 0));
        }
        if (mainText != null && mainText.contains(">>")) {
            // Versuche Pattern-Matching als Fallback
            // Format: "firestarter03 >> Hallo Welt" oder ähnlich
            String[] parts = mainText.split(">>", 2); // Split nur beim ersten ">>"
            if (parts.length > 0) {
                String beforeArrow = parts[0].trim();
                // Extrahiere den letzten "Wort" vor ">>"
                // Entferne Farbcodes zuerst
                beforeArrow = beforeArrow.replaceAll("§[0-9a-fk-or]", "");
                String[] words = beforeArrow.split("\\s+");
                if (words.length > 0) {
                    // Nimm das letzte Wort (das sollte der Spielername sein)
                    String candidate = words[words.length - 1].trim();
                    // Entferne nur wirklich problematische Sonderzeichen, behalte Zahlen
                    // Spielernamen können Zahlen enthalten (z.B. "firestarter03")
                    candidate = candidate.replaceAll("[^a-zA-Z0-9_]", "");
                    if (candidate.length() >= 2 && candidate.length() <= 20) {
                        return candidate;
                    }
                }
            }
        }
        
        return null;
    }
    
    // Glyphe als Marker für das Icon (Cyrillic letter fita - ѳ)
    private static final char ICON_MARKER = 'ѳ';
    // Font-Identifier für die Custom-Font
    private static final Identifier CUSTOM_FONT = Identifier.of("cclive-utilities", "default");
    
    /**
     * Fügt das Mod-Icon zwischen Spielername und >> ein
     * @param message Die modifizierte Nachricht
     * @param playerName Der Spielername
     * @return Die Nachricht mit eingefügtem Icon-Marker
     */
    private static Text insertIconBetweenNameAndArrow(Text message, String playerName) {
        if (message == null || playerName == null || playerName.isEmpty()) {
            return message;
        }
        
        try {
            List<Text> siblings = message.getSiblings();
            if (siblings == null || siblings.isEmpty()) {
                return message;
            }
            
            // Durchsuche die Siblings und finde den Namen vor ">>"
            for (int i = 0; i < siblings.size(); i++) {
                Text current = siblings.get(i);
                String currentText = current.getString();
                
                // Prüfe ob current den Spielernamen enthält
                if (currentText != null && currentText.trim().equals(playerName)) {
                    // Suche nach ">>" in nachfolgenden Siblings (ignoriere Leerzeichen)
                    for (int j = i + 1; j < siblings.size(); j++) {
                        Text checkNext = siblings.get(j);
                        String checkNextText = checkNext.getString();
                        
                        // Prüfe ob checkNext ">>" enthält (ignoriere Leerzeichen dazwischen)
                        boolean isArrow = checkNextText != null && (checkNextText.contains(">>") || checkNextText.trim().equals(">>"));
                        boolean isSpace = checkNextText != null && checkNextText.trim().isEmpty() && checkNextText.contains(" ");
                        
                        if (isArrow || (isSpace && j + 1 < siblings.size() && siblings.get(j + 1).getString() != null && siblings.get(j + 1).getString().contains(">>"))) {
                            // Prüfe ob bereits ein Leerzeichen nach dem Namen vorhanden ist
                            boolean hasSpace = false;
                            if (j > i + 1) {
                                // Prüfe ob zwischen Name und ">>" bereits ein Leerzeichen ist
                                for (int k = i + 1; k < j; k++) {
                                    String betweenText = siblings.get(k).getString();
                                    if (betweenText != null && (betweenText.trim().isEmpty() || betweenText.equals(" "))) {
                                        hasSpace = true;
                                        break;
                                    }
                                }
                            }
                            
                            if (hasSpace) {
                                return message; // Leerzeichen bereits vorhanden
                            }
                            
                            // Erstelle neue Struktur mit Leerzeichen zwischen Name und ">>"
                            MutableText newMessage = message.copy();
                            newMessage.getSiblings().clear();
                            
                            // Füge alle Siblings bis current hinzu
                            for (int k = 0; k <= i; k++) {
                                newMessage.append(siblings.get(k));
                            }
                            
                            // Füge Leerzeichen und unsichtbaren Marker hinzu
                            // Der Marker wird durch ein Leerzeichen ersetzt, damit er nicht als Rechteck gerendert wird
                            // Das Icon wird im ChatHudRenderMixin an der Position des Markers gerendert
                            MutableText space = Text.literal(" ").setStyle(current.getStyle());
                            // Füge Marker hinzu mit expliziter Font-Referenz
                            // Die Font muss explizit gesetzt werden, damit sie verwendet wird
                            // Farbe explizit auf weiß setzen, damit das Icon nicht die Chat-Farbe erbt
                            Style iconStyle = current.getStyle().withFont(CUSTOM_FONT).withColor(0xFFFFFF);
                            MutableText iconMarker = Text.literal(String.valueOf(ICON_MARKER))
                                .setStyle(iconStyle);
                            newMessage.append(space);
                            newMessage.append(iconMarker);
                            
                            // Füge restliche Siblings hinzu
                            for (int k = i + 1; k < siblings.size(); k++) {
                                newMessage.append(siblings.get(k));
                            }
                            
                            return newMessage;
                        }
                    }
                }
            }
            
            // Rekursiv in Siblings suchen
            for (Text sibling : siblings) {
                Text modified = insertIconBetweenNameAndArrow(sibling, playerName);
                if (modified != sibling) {
                    // Icon wurde eingefügt - erstelle neue Struktur
                    MutableText newMessage = message.copy();
                    newMessage.getSiblings().clear();
                    for (Text s : siblings) {
                        if (s == sibling) {
                            newMessage.append(modified);
                        } else {
                            newMessage.append(s);
                        }
                    }
                    return newMessage;
                }
            }
            
            return message;
        } catch (Exception e) {
            // Bei Fehler: Original-Nachricht zurückgeben
            return message;
        }
    }
    
    /**
     * Modifiziert einen Text mit einem neuen Hover-Event
     * Kopiert von InformationenUtility.modifyTextWithHoverEvent
     */
    private static Text modifyTextWithHoverEvent(Text text, HoverEvent newHoverEvent) {
        if (text == null || newHoverEvent == null) {
            return text;
        }
        
        // Copy the text to preserve its structure - copy() returns MutableText which has setStyle()
        MutableText newText = text.copy();
        
        // Check if this text component has a hover event that needs to be replaced
        net.minecraft.text.Style currentStyle = text.getStyle();
        if (currentStyle != null && currentStyle.getHoverEvent() != null) {
            // This text has a hover event - replace it with the new one
            net.minecraft.text.Style newStyle = currentStyle.withHoverEvent(newHoverEvent);
            newText.setStyle(newStyle);
        }
        // If no hover event, the style is already preserved by copy()
        
        // Recursively process all siblings to preserve their formatting
        newText.getSiblings().clear(); // Clear existing siblings to avoid duplication
        for (Text sibling : text.getSiblings()) {
            Text modifiedSibling = modifyTextWithHoverEvent(sibling, newHoverEvent);
            newText.getSiblings().add(modifiedSibling);
        }
        
        return newText;
    }
    
    /**
     * Erstellt einen Hover-Text mit Player-Stats
     * Extrahiert Kaktusrang und Seelenrang aus dem bestehenden Hover-Text
     */
    private static Text createStatsHoverText(Text originalMessage, JsonObject stats) {
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        if (stats == null) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ createStatsHoverText: stats ist null");
            }
            return null;
        }
        
        try {
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 createStatsHoverText gestartet");
            }
            
            // Extrahiere Kaktusrang und Seelenrang aus dem bestehenden Hover-Text
            int kaktusrang = extractKaktusrang(originalMessage);
            int seelenrang = extractSeelenrang(originalMessage);
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Kaktusrang: " + kaktusrang + ", Seelenrang: " + seelenrang);
            }
            
            // Erstelle neuen Hover-Text
            MutableText hoverText = Text.literal("§e=== Spieler Stats ===\n");
            
            // Zeile 1: Kaktusrang | Seelenrang
            hoverText.append(Text.literal("§a🌵 §f" + kaktusrang + " §7| §d💀 §f" + seelenrang + "\n"));
            
            // Zeile 2: Höchste Ebene | Höchste Welle
            int floor = 0;
            int wave = 0;
            
            if (stats.has("highest_floor") && !stats.get("highest_floor").isJsonNull()) {
                floor = stats.get("highest_floor").getAsInt();
            }
            if (stats.has("highest_wave") && !stats.get("highest_wave").isJsonNull()) {
                wave = stats.get("highest_wave").getAsInt();
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Floor: " + floor + ", Wave: " + wave);
            }
            
            if (floor > 0 || wave > 0) {
                // Verwende ⚔ (U+2694) statt ⚔️ (mit Variation Selector) für bessere Kompatibilität
                hoverText.append(Text.literal("§7⚔ §f" + floor + " §7| §b🌊 §f" + wave + "\n"));
            }
            
            // Zeile 3: Chosen Stat (vom Server, oder Fallback zu playtime)
            // Extrahiere Spielernamen aus der Nachricht für Stat-Lookup
            String playerNameForStat = extractPlayerNameFromText(originalMessage);
            
            // Hole chosen_stat vom Server (vom Sender der Nachricht)
            String chosenStat = "playtime"; // Fallback
            if (stats.has("chosen_stat") && !stats.get("chosen_stat").isJsonNull()) {
                chosenStat = stats.get("chosen_stat").getAsString();
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Chosen Stat vom Server: " + chosenStat);
            }
            
            String statValue = getStatValue(chosenStat, stats, playerNameForStat);
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Stat-Wert (" + chosenStat + "): " + statValue);
            }
            if (statValue != null && !statValue.isEmpty()) {
                hoverText.append(Text.literal(statValue));
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ Stat-Wert zum Hover-Text hinzugefügt");
                }
            } else if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ Kein Stat-Wert gefunden für: " + chosenStat);
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ createStatsHoverText erfolgreich abgeschlossen");
            }
            
            return hoverText;
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ❌ Fehler in createStatsHoverText: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Extrahiert den Kaktusrang aus dem bestehenden Hover-Text
     * Format: [Kaktusrang]: x
     */
    private static int extractKaktusrang(Text message) {
        if (message == null) {
            return 0;
        }
        
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // Finde Hover-Event
        HoverEvent hoverEvent = findHoverEventInText(message);
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractKaktusrang: Kein HoverEvent gefunden");
            }
            return 0;
        }
        
        // Extrahiere Hover-Text
        Text hoverText = extractHoverTextFromEvent(hoverEvent);
        if (hoverText == null) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractKaktusrang: HoverText ist null");
            }
            return 0;
        }
        
        // Konvertiere zu String (inklusive Siblings)
        String hoverString = getFullTextString(hoverText);
        if (hoverString == null || hoverString.isEmpty()) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractKaktusrang: HoverString ist null oder leer");
            }
            return 0;
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 extractKaktusrang: HoverString = '" + hoverString + "'");
        }
        
        // Entferne Farbcodes (§ gefolgt von einem Zeichen)
        String cleanedString = hoverString.replaceAll("§[0-9a-fk-or]", "");
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 extractKaktusrang: CleanedString = '" + cleanedString + "'");
        }
        
        // Suche nach [Kaktusrang]: x (unterstützt auch Kommas als Tausendertrennzeichen)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[Kaktusrang\\]:\\s*([\\d,]+)");
        java.util.regex.Matcher matcher = pattern.matcher(cleanedString);
        if (matcher.find()) {
            try {
                // Entferne Kommas (Tausendertrennzeichen) vor dem Parsen
                String numberString = matcher.group(1).replace(",", "");
                int value = Integer.parseInt(numberString);
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ extractKaktusrang: " + value);
                }
                return value;
            } catch (NumberFormatException e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ extractKaktusrang: NumberFormatException: " + e.getMessage());
                }
                return 0;
            }
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] ⚠️ extractKaktusrang: Pattern nicht gefunden");
        }
        return 0;
    }
    
    /**
     * Extrahiert den Seelenrang aus dem bestehenden Hover-Text
     * Format: [Seelenrang]: x
     */
    private static int extractSeelenrang(Text message) {
        if (message == null) {
            return 0;
        }
        
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // Finde Hover-Event
        HoverEvent hoverEvent = findHoverEventInText(message);
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractSeelenrang: Kein HoverEvent gefunden");
            }
            return 0;
        }
        
        // Extrahiere Hover-Text
        Text hoverText = extractHoverTextFromEvent(hoverEvent);
        if (hoverText == null) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractSeelenrang: HoverText ist null");
            }
            return 0;
        }
        
        // Konvertiere zu String (inklusive Siblings)
        String hoverString = getFullTextString(hoverText);
        if (hoverString == null || hoverString.isEmpty()) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractSeelenrang: HoverString ist null oder leer");
            }
            return 0;
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 extractSeelenrang: HoverString = '" + hoverString + "'");
        }
        
        // Entferne Farbcodes (§ gefolgt von einem Zeichen)
        String cleanedString = hoverString.replaceAll("§[0-9a-fk-or]", "");
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 extractSeelenrang: CleanedString = '" + cleanedString + "'");
        }
        
        // Suche nach [Seelenrang]: x (unterstützt auch Kommas als Tausendertrennzeichen)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[Seelenrang\\]:\\s*([\\d,]+)");
        java.util.regex.Matcher matcher = pattern.matcher(cleanedString);
        if (matcher.find()) {
            try {
                // Entferne Kommas (Tausendertrennzeichen) vor dem Parsen
                String numberString = matcher.group(1).replace(",", "");
                int value = Integer.parseInt(numberString);
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ extractSeelenrang: " + value);
                }
                return value;
            } catch (NumberFormatException e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ extractSeelenrang: NumberFormatException: " + e.getMessage());
                }
                return 0;
            }
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] ⚠️ extractSeelenrang: Pattern nicht gefunden");
        }
        return 0;
    }
    
    /**
     * Konvertiert einen Text (inklusive aller Siblings) zu einem vollständigen String
     */
    private static String getFullTextString(Text text) {
        if (text == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Füge den Haupttext hinzu
        String mainText = text.getString();
        if (mainText != null) {
            sb.append(mainText);
        }
        
        // Füge alle Siblings hinzu
        for (Text sibling : text.getSiblings()) {
            String siblingText = getFullTextString(sibling);
            if (siblingText != null && !siblingText.isEmpty()) {
                sb.append(siblingText);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Findet ein HoverEvent in einem Text rekursiv
     */
    private static HoverEvent findHoverEventInText(Text text) {
        if (text == null) {
            return null;
        }
        
        // Prüfe den Haupttext
        if (text.getStyle() != null) {
            HoverEvent hoverEvent = text.getStyle().getHoverEvent();
            if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
                return hoverEvent;
            }
        }
        
        // Rekursiv in Siblings suchen
        for (Text sibling : text.getSiblings()) {
            HoverEvent found = findHoverEventInText(sibling);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * Extrahiert Text aus einem HoverEvent
     * Nutzt umfassende Logik ähnlich wie InformationenUtility
     */
    private static Text extractHoverTextFromEvent(HoverEvent hoverEvent) {
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return null;
        }
        
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // Versuche getValue() Methode
        try {
            java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue", HoverEvent.Action.class);
            getValueMethod.setAccessible(true);
            Object value = getValueMethod.invoke(hoverEvent, HoverEvent.Action.SHOW_TEXT);
            if (value instanceof Text) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via getValue(Action) gefunden");
                }
                return (Text) value;
            }
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: getValue(Action) fehlgeschlagen: " + e.getMessage());
            }
        }
        
        // Versuche value() Methode (für Records)
        try {
            java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
            valueMethod.setAccessible(true);
            Object value = valueMethod.invoke(hoverEvent);
            if (value instanceof Text) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via value() gefunden");
                }
                return (Text) value;
            }
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: value() fehlgeschlagen: " + e.getMessage());
            }
        }
        
        // Versuche Felder zu lesen
        try {
            java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (Text.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(hoverEvent);
                    if (value instanceof Text) {
                        if (debugging) {
                            System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via Field gefunden");
                        }
                        return (Text) value;
                    }
                }
            }
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Field-Suche fehlgeschlagen: " + e.getMessage());
            }
        }
        
        // Versuche ShowText-Klasse - CRITICAL: In 1.21.7 ist HoverEvent selbst ShowText
        try {
            // Prüfe ob HoverEvent selbst ein ShowText ist
            String className = hoverEvent.getClass().getName();
            Class<?> actualClass = hoverEvent.getClass();
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: HoverEvent Klasse: " + className);
                System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Ist Record: " + actualClass.isRecord());
            }
            
            // Prüfe ob es ein Record ist (direkt auf der tatsächlichen Klasse)
            if (actualClass.isRecord()) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: HoverEvent ist ein Record");
                }
                try {
                    java.lang.reflect.RecordComponent[] components = actualClass.getRecordComponents();
                    if (debugging) {
                        System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Record-Komponenten: " + components.length);
                    }
                    for (java.lang.reflect.RecordComponent component : components) {
                        if (debugging) {
                            System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Prüfe Komponente: " + component.getName() + " (" + component.getType().getName() + ")");
                        }
                        if (Text.class.isAssignableFrom(component.getType())) {
                            try {
                                Object value = component.getAccessor().invoke(hoverEvent);
                                if (value instanceof Text) {
                                    if (debugging) {
                                        System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via Record-Komponente gefunden: " + component.getName());
                                    }
                                    return (Text) value;
                                }
                            } catch (Exception e) {
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Record-Komponente-Zugriff fehlgeschlagen: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (debugging) {
                        System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Record-Komponenten-Zugriff fehlgeschlagen: " + e.getMessage());
                    }
                }
            }
            
            // Versuche auch alle Felder in der tatsächlichen Klasse zu lesen
            try {
                java.lang.reflect.Field[] allFields = actualClass.getDeclaredFields();
                if (debugging) {
                    System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Felder in Klasse: " + allFields.length);
                }
                for (java.lang.reflect.Field field : allFields) {
                    if (debugging) {
                        System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Prüfe Feld: " + field.getName() + " (" + field.getType().getName() + ")");
                    }
                    if (Text.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(hoverEvent);
                            if (value instanceof Text) {
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via Feld gefunden: " + field.getName());
                                }
                                return (Text) value;
                            }
                        } catch (Exception e) {
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Feld-Zugriff fehlgeschlagen: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Feld-Suche fehlgeschlagen: " + e.getMessage());
                }
            }
            
            // Versuche auch Methoden, die Text zurückgeben
            try {
                java.lang.reflect.Method[] methods = actualClass.getDeclaredMethods();
                if (debugging) {
                    System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Methoden in Klasse: " + methods.length);
                }
                for (java.lang.reflect.Method method : methods) {
                    if (Text.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 0) {
                        if (debugging) {
                            System.out.println("[PlayerHoverStats] 🔍 extractHoverTextFromEvent: Prüfe Methode: " + method.getName() + " -> " + method.getReturnType().getName());
                        }
                        try {
                            method.setAccessible(true);
                            Object value = method.invoke(hoverEvent);
                            if (value instanceof Text) {
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ✅ extractHoverTextFromEvent: Text via Methode gefunden: " + method.getName());
                                }
                                return (Text) value;
                            }
                        } catch (Exception e) {
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Methode-Zugriff fehlgeschlagen: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: Methode-Suche fehlgeschlagen: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ⚠️ extractHoverTextFromEvent: ShowText-Klasse-Handling fehlgeschlagen: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] ❌ extractHoverTextFromEvent: Alle Versuche fehlgeschlagen");
        }
        return null;
    }
    
    /**
     * Erstellt ein Hover-Event aus einem Text
     * Verwendet umfassende Logik ähnlich wie InformationenUtility.createHoverEventDirect
     */
    private static HoverEvent createHoverEvent(Text hoverText) {
        if (hoverText == null) {
            return null;
        }
        
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        // First, try to find ShowText class dynamically by searching inner classes
        Class<?> showTextClass = null;
        try {
            // Try direct name first (for non-obfuscated environments)
            showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
        } catch (ClassNotFoundException e) {
            // If direct name fails, search inner classes dynamically
            Class<?>[] innerClasses = HoverEvent.class.getDeclaredClasses();
            for (Class<?> innerClass : innerClasses) {
                // Check if this inner class has a constructor that takes Text
                java.lang.reflect.Constructor<?>[] constructors = innerClass.getDeclaredConstructors();
                for (java.lang.reflect.Constructor<?> constructor : constructors) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
                        // This looks like ShowText!
                        showTextClass = innerClass;
                        break;
                    }
                }
                if (showTextClass != null) break;
            }
        }
        
        // Try using ShowText inner class if we found it
        if (showTextClass != null) {
            try {
                // Check for static factory methods in ShowText
                for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
                            try {
                                method.setAccessible(true);
                                HoverEvent result = (HoverEvent) method.invoke(null, hoverText);
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via ShowText static factory");
                                }
                                return result;
                            } catch (Exception e) {
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ⚠️ ShowText static factory fehlgeschlagen: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
                
                // Try constructors in ShowText
                java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
                for (java.lang.reflect.Constructor<?> constructor : constructors) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
                        try {
                            constructor.setAccessible(true);
                            Object showTextInstance = constructor.newInstance(hoverText);
                            
                            // Check if ShowText is directly assignable to HoverEvent
                            if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via ShowText (direkt assignable)");
                                }
                                return (HoverEvent) showTextInstance;
                            }
                            
                            // Try to create HoverEvent with ShowText instance using reflection
                            java.lang.reflect.Constructor<?>[] hoverEventConstructors = HoverEvent.class.getDeclaredConstructors();
                            for (java.lang.reflect.Constructor<?> hoverEventConstructor : hoverEventConstructors) {
                                Class<?>[] hoverEventParamTypes = hoverEventConstructor.getParameterTypes();
                                if (hoverEventParamTypes.length == 2 && 
                                    hoverEventParamTypes[0] == HoverEvent.Action.class &&
                                    hoverEventParamTypes[1].isAssignableFrom(showTextInstance.getClass())) {
                                    try {
                                        hoverEventConstructor.setAccessible(true);
                                        HoverEvent result = (HoverEvent) hoverEventConstructor.newInstance(HoverEvent.Action.SHOW_TEXT, showTextInstance);
                                        if (debugging) {
                                            System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via HoverEvent constructor mit ShowText");
                                        }
                                        return result;
                                    } catch (Exception e) {
                                        if (debugging) {
                                            System.out.println("[PlayerHoverStats] ⚠️ HoverEvent constructor fehlgeschlagen: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ⚠️ ShowText constructor fehlgeschlagen: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ ShowText class handling fehlgeschlagen: " + e.getMessage());
                }
            }
        }
        
        // Fallback: Try using reflection on HoverEvent directly
        try {
            // Try static factory methods
            for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class && paramTypes[1] == Text.class) {
                        try {
                            method.setAccessible(true);
                            HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, hoverText);
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via HoverEvent static factory (Action, Text)");
                            }
                            return result;
                        } catch (Exception e) {
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ⚠️ HoverEvent static factory (Action, Text) fehlgeschlagen: " + e.getMessage());
                            }
                        }
                    } else if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
                        try {
                            method.setAccessible(true);
                            HoverEvent result = (HoverEvent) method.invoke(null, hoverText);
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via HoverEvent static factory (Text)");
                            }
                            return result;
                        } catch (Exception e) {
                            if (debugging) {
                                System.out.println("[PlayerHoverStats] ⚠️ HoverEvent static factory (Text) fehlgeschlagen: " + e.getMessage());
                            }
                        }
                    }
                }
            }
            
            // Try constructors
            java.lang.reflect.Constructor<?>[] declaredConstructors = HoverEvent.class.getDeclaredConstructors();
            for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class && paramTypes[1] == Text.class) {
                    try {
                        constructor.setAccessible(true);
                        HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
                        if (debugging) {
                            System.out.println("[PlayerHoverStats] ✅ HoverEvent erstellt via HoverEvent constructor (Action, Text)");
                        }
                        return result;
                    } catch (Exception e) {
                        if (debugging) {
                            System.out.println("[PlayerHoverStats] ⚠️ HoverEvent constructor (Action, Text) fehlgeschlagen: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ❌ Alle HoverEvent-Erstellungsversuche fehlgeschlagen: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] ❌ HoverEvent konnte nicht erstellt werden");
        }
        return null;
    }
    
    /**
     * Holt den Wert für die gewählte Stat
     * @param statName Name der Stat ("playtime", "max_coins", "messages_sent", etc.)
     * @param stats Player-Stats JSON (kann null sein)
     * @param playerName Spielername für Playtime-Lookup (kann null sein)
     * @return Formatierter String mit Icon und Wert, oder null wenn nicht verfügbar
     */
    private static String getStatValue(String statName, JsonObject stats, String playerName) {
        if (statName == null || statName.isEmpty()) {
            statName = "playtime"; // Fallback
        }
        
        switch (statName) {
            case "playtime":
                return getPlaytimeValue(playerName);
            case "max_coins":
                return getMaxCoinsValue(stats);
            case "messages_sent":
                return getMessagesSentValue(stats);
            case "blueprints_found":
                return getBlueprintsFoundValue(stats);
            case "max_damage":
                return getMaxDamageValue(stats);
            default:
                return getPlaytimeValue(playerName); // Fallback zu Playtime
        }
    }
    
    /**
     * Holt Playtime vom Leaderboard für einen bestimmten Spieler
     * Da getLeaderboard asynchron ist, versuchen wir es synchron zu machen (mit Timeout)
     * @param playerName Spielername, für den die Playtime geholt werden soll (kann null sein für aktuellen Spieler)
     */
    private static String getPlaytimeValue(String playerName) {
        boolean debugging = CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        
        if (debugging) {
            System.out.println("[PlayerHoverStats] 🔍 getPlaytimeValue() aufgerufen");
        }
        
        try {
            LeaderboardManager manager = LeaderboardManager.getInstance();
            if (manager == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ LeaderboardManager ist null");
                }
                return null;
            }
            
            if (!manager.isRegistered()) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ LeaderboardManager nicht registriert");
                }
                return null;
            }
            
            // Verwende den übergebenen Spielernamen, oder fallback zum aktuellen Spieler
            String searchPlayerName = playerName != null ? playerName : manager.getPlayerName();
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Versuche Playtime für Spieler '" + searchPlayerName + "' zu holen...");
            }
            
            // Hole Playtime direkt für den spezifischen Spieler vom Leaderboard
            // Direkter HTTP-Request für den spezifischen Spieler (synchron)
            String endpoint = "/leaderboard/playtime/" + searchPlayerName;
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 HTTP GET Request: " + endpoint);
                System.out.println("[PlayerHoverStats] 🔍 HttpClient ist " + (httpClient != null ? "initialisiert" : "NULL"));
            }
            
            if (httpClient == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ❌ HttpClient ist null - kann Playtime nicht abrufen");
                }
                return null;
            }
            
            JsonObject result = null;
            try {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] 🔍 Sende HTTP GET Request...");
                }
                result = httpClient.get(endpoint);
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ✅ HTTP GET Request abgeschlossen");
                }
            } catch (java.io.IOException e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ❌ IOException beim HTTP GET: " + e.getMessage());
                    e.printStackTrace();
                } else {
                    // Auch ohne Debug-Modus sollten wir kritische Fehler loggen
                    System.err.println("[PlayerHoverStats] ❌ IOException beim HTTP GET: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ❌ InterruptedException beim HTTP GET: " + e.getMessage());
                    e.printStackTrace();
                } else {
                    System.err.println("[PlayerHoverStats] ❌ InterruptedException beim HTTP GET: " + e.getMessage());
                }
            } catch (Exception e) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ❌ Unerwartete Exception beim HTTP GET: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                } else {
                    System.err.println("[PlayerHoverStats] ❌ Unerwartete Exception beim HTTP GET: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] 🔍 Leaderboard-Result erhalten: " + (result != null ? result.toString() : "null"));
            }
            
            if (result == null) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Kein Leaderboard-Result für playtime (result ist null)");
                    System.out.println("[PlayerHoverStats] 💡 Tipp: Aktiviere 'Leaderboard Debugging' in der Config, um HTTP-Details zu sehen");
                }
                return null;
            }
            
            long playtimeSeconds = 0;
            
            // Versuche zuerst "self" zu holen (für den spezifischen Spieler)
            if (result.has("self") && !result.get("self").isJsonNull()) {
                JsonObject self = result.getAsJsonObject("self");
                if (self.has("score") && !self.get("score").isJsonNull()) {
                    playtimeSeconds = self.get("score").getAsLong();
                    if (debugging) {
                        System.out.println("[PlayerHoverStats] ✅ Playtime aus 'self' geholt: " + playtimeSeconds);
                    }
                }
            }
            
            // Falls "self" null ist, suche im "top"-Array nach dem Spieler (Fallback)
            if (playtimeSeconds == 0 && result.has("top")) {
                com.google.gson.JsonArray top = result.getAsJsonArray("top");
                
                if (debugging) {
                    System.out.println("[PlayerHoverStats] 🔍 Suche Spieler '" + searchPlayerName + "' im top-Array...");
                }
                
                if (top != null && searchPlayerName != null) {
                    for (int i = 0; i < top.size(); i++) {
                        JsonObject entry = top.get(i).getAsJsonObject();
                        if (entry.has("player") && entry.get("player").getAsString().equals(searchPlayerName)) {
                            if (entry.has("score") && !entry.get("score").isJsonNull()) {
                                playtimeSeconds = entry.get("score").getAsLong();
                                if (debugging) {
                                    System.out.println("[PlayerHoverStats] ✅ Playtime aus 'top'-Array geholt für '" + searchPlayerName + "': " + playtimeSeconds);
                                }
                                break;
                            }
                        }
                    }
                }
                
                if (playtimeSeconds == 0 && debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Spieler '" + searchPlayerName + "' nicht im top-Array gefunden");
                }
            }
            
            if (playtimeSeconds <= 0) {
                if (debugging) {
                    System.out.println("[PlayerHoverStats] ⚠️ Playtime ist 0 oder nicht gefunden");
                }
                return null;
            }
            
            // Formatiere Playtime (Tage:Stunden:Minuten oder Stunden:Minuten)
            long days = playtimeSeconds / 86400;
            long hours = (playtimeSeconds % 86400) / 3600;
            long minutes = (playtimeSeconds % 3600) / 60;
            
            StringBuilder playtimeStr = new StringBuilder();
            if (days > 0) {
                playtimeStr.append(days).append("d ");
            }
            if (hours > 0 || days > 0) {
                playtimeStr.append(hours).append("h ");
            }
            playtimeStr.append(minutes).append("m");
            
            if (debugging) {
                System.out.println("[PlayerHoverStats] ✅ Playtime formatiert: " + playtimeStr.toString() + " (aus " + playtimeSeconds + " Sekunden)");
            }
            
            return "§7⏱ §f" + playtimeStr.toString();
        } catch (Exception e) {
            if (debugging) {
                System.out.println("[PlayerHoverStats] ❌ Fehler beim Abrufen von Playtime: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Holt Max Coins aus Stats
     */
    private static String getMaxCoinsValue(JsonObject stats) {
        if (stats == null || !stats.has("max_coins") || stats.get("max_coins").isJsonNull()) {
            return null;
        }
        long coins = stats.get("max_coins").getAsLong();
        if (coins <= 0) {
            return null;
        }
        return "§6💰 §f" + formatNumber(coins);
    }
    
    /**
     * Holt Messages Sent aus Stats
     */
    private static String getMessagesSentValue(JsonObject stats) {
        if (stats == null || !stats.has("messages_sent") || stats.get("messages_sent").isJsonNull()) {
            return null;
        }
        int messages = stats.get("messages_sent").getAsInt();
        return "§7💬 §f" + formatNumber(messages);
    }
    
    /**
     * Holt Blueprints Found aus Stats
     * Format: "x / Gesamtanzahl" (z.B. "364 / 2343")
     */
    private static String getBlueprintsFoundValue(JsonObject stats) {
        if (stats == null || !stats.has("blueprints_found") || stats.get("blueprints_found").isJsonNull()) {
            return null;
        }
        int blueprints = stats.get("blueprints_found").getAsInt();
        if (blueprints <= 0) {
            return null;
        }
        
        // Berechne Gesamtanzahl aller Blueprints
        int totalBlueprints = getTotalBlueprintCount();
        
        // Format: "x / Gesamtanzahl"
        return "§5📃 §f" + blueprints + " / " + totalBlueprints;
    }
    
    /**
     * Berechnet die Gesamtanzahl aller Blueprints aus der blueprints.json
     */
    private static int getTotalBlueprintCount() {
        try {
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath("assets/cclive-utilities/blueprints.json")
                .orElseThrow(() -> new RuntimeException("Blueprints config file not found"));
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new java.io.InputStreamReader(inputStream)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject floors = json.getAsJsonObject("floors");
                    
                    int totalCount = 0;
                    for (String floorKey : floors.keySet()) {
                        JsonObject floorData = floors.getAsJsonObject(floorKey);
                        JsonObject blueprints = floorData.getAsJsonObject("blueprints");
                        
                        // Iterate through all rarities
                        for (String rarityKey : blueprints.keySet()) {
                            JsonObject rarityData = blueprints.getAsJsonObject(rarityKey);
                            com.google.gson.JsonArray itemsArray = rarityData.getAsJsonArray("items");
                            totalCount += itemsArray.size();
                        }
                    }
                    
                    return totalCount;
                }
            }
        } catch (Exception e) {
            // Fallback: Wenn die Datei nicht geladen werden kann, verwende einen Standardwert
            // (kann später angepasst werden, wenn die genaue Anzahl bekannt ist)
            return 2343; // Standardwert basierend auf Benutzerangabe
        }
    }
    
    /**
     * Holt Max Damage aus Stats
     */
    private static String getMaxDamageValue(JsonObject stats) {
        if (stats == null || !stats.has("max_damage") || stats.get("max_damage").isJsonNull()) {
            return null;
        }
        long damage = stats.get("max_damage").getAsLong();
        if (damage <= 0) {
            return null;
        }
        return "§c⚔ §f" + formatNumber(damage);
    }
    
    /**
     * Formatiert eine Zahl im amerikanischen Format (K, M, B, T)
     * Format: 1-999 ohne Suffix, dann 1.000 K, 10.000 K, 100.000 K, 1.000 M, etc.
     * Verwendet Punkte als Tausendertrennzeichen
     */
    private static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }
        
        // Trillion (T): 1.000.000.000.000+
        if (number >= 1_000_000_000_000L) {
            double value = number / 1_000_000_000_000.0;
            return formatWithThousandsSeparator(value, "T");
        }
        
        // Billion (B): 1.000.000.000 - 999.999.999.999
        if (number >= 1_000_000_000L) {
            double value = number / 1_000_000_000.0;
            return formatWithThousandsSeparator(value, "B");
        }
        
        // Million (M): 1.000.000 - 999.999.999
        if (number >= 1_000_000L) {
            double value = number / 1_000_000.0;
            return formatWithThousandsSeparator(value, "M");
        }
        
        // Thousand (K): 1.000 - 999.999
        if (number >= 1_000L) {
            double value = number / 1_000.0;
            return formatWithThousandsSeparator(value, "K");
        }
        
        return String.valueOf(number);
    }
    
    /**
     * Formatiert einen Wert mit Tausendertrennzeichen (Punkte) und Suffix
     * z.B. 1.000 K, 10.000 K, 100.000 K
     */
    private static String formatWithThousandsSeparator(double value, String suffix) {
        // Runde auf ganze Zahl wenn >= 100, sonst mit Dezimalstellen
        long wholeValue;
        if (value >= 100) {
            wholeValue = Math.round(value);
        } else if (value >= 10) {
            // Eine Dezimalstelle
            wholeValue = Math.round(value * 10);
            String formatted = String.format("%.1f", value);
            return addThousandsSeparator(formatted) + " " + suffix;
        } else {
            // Drei Dezimalstellen für Werte unter 10
            String formatted = String.format("%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
            return addThousandsSeparator(formatted) + " " + suffix;
        }
        
        // Für ganze Zahlen: Tausendertrennzeichen hinzufügen
        return addThousandsSeparator(String.valueOf(wholeValue)) + " " + suffix;
    }
    
    /**
     * Fügt Punkte als Tausendertrennzeichen hinzu
     * z.B. 1000 -> 1.000, 10000 -> 10.000, 100000 -> 100.000
     */
    private static String addThousandsSeparator(String number) {
        // Entferne eventuelle Dezimalstellen für die Trennzeichen-Logik
        String[] parts = number.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? "." + parts[1] : "";
        
        // Füge Tausendertrennzeichen hinzu (von rechts nach links)
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int i = integerPart.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                result.insert(0, ".");
            }
            result.insert(0, integerPart.charAt(i));
            count++;
        }
        
        return result.toString() + decimalPart;
    }
}

