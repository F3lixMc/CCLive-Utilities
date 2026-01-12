package net.felix.utilities.DragOverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility für den F6-Button in Inventaren
 */
public class OverlayEditorButtonUtility {
    
    private static final int BUTTON_WIDTH = 40;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_PADDING = 5; // Abstand vom Rand
    
    // Button-Positionen (werden beim Rendern berechnet)
    private static int buttonX = -1;
    private static int buttonY = -1;
    
    /**
     * Rendert den F6-Button unten links im Inventar
     */
    public static void renderButton(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Berechne Position unten links
        int screenHeight = client.getWindow().getScaledHeight();
        
        buttonX = BUTTON_PADDING;
        buttonY = screenHeight - BUTTON_HEIGHT - BUTTON_PADDING;
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                           mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        
        // Button-Hintergrund
        int backgroundColor = isHovered ? 0xFF5A8A7A : 0xFF4B6A69;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, backgroundColor);
        
        // Button-Rahmen
        int borderColor = isHovered ? 0xFF7AAAA9 : 0xFF6A8A89;
        context.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + 1, borderColor); // Oben
        context.fill(buttonX, buttonY + BUTTON_HEIGHT - 1, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, borderColor); // Unten
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + BUTTON_HEIGHT, borderColor); // Links
        context.fill(buttonX + BUTTON_WIDTH - 1, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, borderColor); // Rechts
        
        // Button-Text: Hole den aktuellen Hotkey aus der KeyBinding
        String hotkeyText = getOverlayEditorHotkeyText();
        Text buttonText = Text.literal(hotkeyText);
        int textX = buttonX + (BUTTON_WIDTH - client.textRenderer.getWidth(buttonText)) / 2;
        int textY = buttonY + (BUTTON_HEIGHT - client.textRenderer.fontHeight) / 2;
        context.drawText(client.textRenderer, buttonText, textX, textY, 0xFFFFFFFF, true);
        
        // Rendere Tooltip wenn Maus über Button ist
        if (isHovered) {
            List<Text> tooltip = new ArrayList<>();
            
            // Berechne Breite beider Texte für Zentrierung
            String line1 = "Inventar";
            String line2 = "Overlay Editor";
            int width1 = client.textRenderer.getWidth(line1);
            int width2 = client.textRenderer.getWidth(line2);
            
            // Berechne die Breite eines Leerzeichens
            int spaceWidth = client.textRenderer.getWidth(" ");
            
            // Zentriere "Inventar" über "Overlay Editor"
            // Berechne die Pixel-Differenz und konvertiere zu Leerzeichen
            int totalPixelDiff = width2 - width1;
            int numSpaces = spaceWidth > 0 ? totalPixelDiff / spaceWidth : 0;
            
            // Teile die Leerzeichen auf: Hälfte am Anfang, Hälfte am Ende
            // Wenn ungerade Anzahl, füge das extra Leerzeichen am Anfang hinzu
            int spacesBefore = (numSpaces + 1) / 2; // Aufrunden
            int spacesAfter = numSpaces / 2; // Abrunden
            
            // Erstelle zentrierten Text mit Leerzeichen am Anfang und Ende
            String centeredLine1 = " ".repeat(Math.max(0, spacesBefore)) + line1 + " ".repeat(Math.max(0, spacesAfter));
            
            tooltip.add(Text.literal(centeredLine1));
            tooltip.add(Text.literal(line2));
            context.drawTooltip(client.textRenderer, tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Behandelt Klicks auf den F6-Button
     * @return true wenn der Klick behandelt wurde
     */
    public static boolean handleButtonClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Nur Linksklick
        
        // Prüfe ob Klick auf Button ist
        if (buttonX >= 0 && buttonY >= 0 &&
            mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
            mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
            // Öffne F6-Menü
            OverlayEditorUtility.openOverlayEditor();
            return true;
        }
        
        return false;
    }
    
    /**
     * Prüft ob der Button sichtbar sein sollte
     */
    public static boolean shouldShowButton(HandledScreen<?> screen) {
        // Button wird in allen HandledScreens angezeigt (außer wenn F6-Menü bereits offen ist)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        // Nicht anzeigen wenn F6-Menü bereits offen ist
        if (client.currentScreen instanceof OverlayEditorScreen) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gibt den formatierten Hotkey-Text für den Overlay Editor zurück
     * @return Formatierter Hotkey-Text (z.B. "F6")
     */
    private static String getOverlayEditorHotkeyText() {
        net.minecraft.client.option.KeyBinding keyBinding = OverlayEditorUtility.getOverlayEditorKeyBinding();
        if (keyBinding == null) {
            return "F6"; // Fallback
        }
        
        try {
            // Verwende getBoundKeyLocalizedText() um den aktuellen Hotkey-Text zu bekommen
            // Dies funktioniert auch nach Änderungen in der Config
            net.minecraft.text.Text localizedText = keyBinding.getBoundKeyLocalizedText();
            if (localizedText != null) {
                String hotkeyString = localizedText.getString();
                // Entferne mögliche Formatierungs-Codes
                hotkeyString = hotkeyString.replaceAll("§[0-9a-fk-or]", "");
                if (!hotkeyString.isEmpty()) {
                    return hotkeyString;
                }
            }
        } catch (Exception e) {
            // Fallback: Versuche über getBoundKey() und KeyCode via Reflection
        }
        
        // Fallback: Versuche über getBoundKey() und KeyCode via Reflection
        try {
            java.lang.reflect.Method getBoundKeyMethod = keyBinding.getClass().getMethod("getBoundKey");
            Object boundKeyObj = getBoundKeyMethod.invoke(keyBinding);
            if (boundKeyObj != null) {
                // Hole den KeyCode
                java.lang.reflect.Method getCodeMethod = boundKeyObj.getClass().getMethod("getCode");
                int keyCode = (Integer) getCodeMethod.invoke(boundKeyObj);
                
                // Konvertiere GLFW Key Code zu lesbarem String
                if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_A && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_Z) {
                    // Buchstaben A-Z
                    return String.valueOf((char) ('A' + (keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_A)));
                } else if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_0 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) {
                    // Zahlen 0-9
                    return String.valueOf((char) ('0' + (keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_0)));
                } else if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_F1 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_F25) {
                    // Funktionstasten F1-F25
                    int fNumber = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_F1 + 1;
                    return "F" + fNumber;
                } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
                    return "SPACE";
                } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) {
                    return "SHIFT";
                } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) {
                    return "CTRL";
                } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) {
                    return "ALT";
                } else {
                    return "Key " + keyCode;
                }
            }
        } catch (Exception e) {
            // Fallback: Standard "F6"
        }
        
        return "F6"; // Fallback
    }
}
