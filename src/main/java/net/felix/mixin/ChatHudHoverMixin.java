package net.felix.mixin;

import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.Aspekte.AspectOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ChatHudHoverMixin {
    
    /**
     * Injects into Screen.render to detect when a HoverEvent tooltip is being displayed
     * Shows the aspect overlay when:
     * 1. Shift is pressed
     * 2. A HoverEvent is being rendered (detected via ChatHud.getTextStyleAt())
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Check if aspect overlay is enabled in config
        boolean aspectOverlayEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled;
        boolean showAspectOverlay = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
        boolean chatAspectOverlayEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled;
        
        if (!aspectOverlayEnabled || !showAspectOverlay || !chatAspectOverlayEnabled) {
            AspectOverlay.onHoverStopped();
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // Note: We don't check client.world because the chat overlay should work
        // even when no world is loaded (e.g., in main menu with chat history visible)
        
        // Check if Shift is pressed - overlay only shows when Shift is held
        boolean isShiftPressed = Screen.hasShiftDown();
        
        if (!isShiftPressed) {
            // Shift not pressed, clear overlay
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // Use Minecraft's built-in method to check if a HoverEvent is being rendered
        // This is the same method Minecraft uses internally to determine tooltip hover
        ChatHud chatHud = client.inGameHud.getChatHud();
        if (chatHud == null) {
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // Convert mouse coordinates to double (as required by getTextStyleAt)
        double mouseXDouble = (double) mouseX;
        double mouseYDouble = (double) mouseY;
        
        // Try to find and call getTextStyleAt method via reflection (may be obfuscated)
        Style hoveredStyle = null;
        Text hoveredMessage = null;
        
        try {
            // Find getTextStyleAt method - it takes two double parameters and returns Style
            java.lang.reflect.Method getTextStyleAtMethod = null;
            for (java.lang.reflect.Method method : ChatHud.class.getDeclaredMethods()) {
                if (method.getParameterCount() == 2 &&
                    method.getParameterTypes()[0] == double.class &&
                    method.getParameterTypes()[1] == double.class &&
                    method.getReturnType() == Style.class) {
                    getTextStyleAtMethod = method;
                    break;
                }
            }
            
            // Also try public methods
            if (getTextStyleAtMethod == null) {
                for (java.lang.reflect.Method method : ChatHud.class.getMethods()) {
                    if (method.getParameterCount() == 2 &&
                        method.getParameterTypes()[0] == double.class &&
                        method.getParameterTypes()[1] == double.class &&
                        method.getReturnType() == Style.class) {
                        getTextStyleAtMethod = method;
                        break;
                    }
                }
            }
            
            if (getTextStyleAtMethod != null) {
                getTextStyleAtMethod.setAccessible(true);
                hoveredStyle = (Style) getTextStyleAtMethod.invoke(chatHud, mouseXDouble, mouseYDouble);
            }
            
            // If we found a HoverEvent, try to get the Text message
            if (hoveredStyle != null && hoveredStyle.getHoverEvent() != null) {
                // Find getTextAt method - it takes two double parameters and returns Text
                java.lang.reflect.Method getTextAtMethod = null;
                for (java.lang.reflect.Method method : ChatHud.class.getDeclaredMethods()) {
                    if (method.getParameterCount() == 2 &&
                        method.getParameterTypes()[0] == double.class &&
                        method.getParameterTypes()[1] == double.class &&
                        method.getReturnType() == Text.class) {
                        getTextAtMethod = method;
                        break;
                    }
                }
                
                // Also try public methods
                if (getTextAtMethod == null) {
                    for (java.lang.reflect.Method method : ChatHud.class.getMethods()) {
                        if (method.getParameterCount() == 2 &&
                            method.getParameterTypes()[0] == double.class &&
                            method.getParameterTypes()[1] == double.class &&
                            method.getReturnType() == Text.class) {
                            getTextAtMethod = method;
                            break;
                        }
                    }
                }
                
                if (getTextAtMethod != null) {
                    getTextAtMethod.setAccessible(true);
                    hoveredMessage = (Text) getTextAtMethod.invoke(chatHud, mouseXDouble, mouseYDouble);
                }
            }
        } catch (Exception e) {
            // Reflection failed, clear overlay
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // Check if a HoverEvent is being rendered
        if (hoveredStyle == null || hoveredStyle.getHoverEvent() == null) {
            // No HoverEvent being rendered, clear overlay
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // If we couldn't get the message via getTextAt, search through chat messages
        // to find the one that has this HoverEvent
        if (hoveredMessage == null) {
            hoveredMessage = findChatMessageWithHoverEvent(client, hoveredStyle.getHoverEvent());
        }
        
        if (hoveredMessage == null) {
            AspectOverlay.onHoverStopped();
            return;
        }
        
        // All conditions are met:
        // 1. Shift is pressed ✓
        // 2. HoverEvent is being rendered (detected via getTextStyleAt) ✓
        // Extract blueprint name from chat message (text with color #FC7E00)
        String blueprintName = InformationenUtility.extractBlueprintNameFromChatMessage(hoveredMessage);
        if (blueprintName != null && !blueprintName.isEmpty()) {
            // Pass the text object to check for Epic colors
            InformationenUtility.updateAspectOverlayFromBlueprintName(blueprintName, hoveredMessage);
            AspectOverlay.renderForegroundForChat(context);
        } else {
            AspectOverlay.onHoverStopped();
        }
    }
    
    /**
     * Finds the chat message that has the given HoverEvent
     * Searches through visible chat messages to find the one with matching HoverEvent
     */
    private Text findChatMessageWithHoverEvent(MinecraftClient client, net.minecraft.text.HoverEvent targetHoverEvent) {
        if (client == null || targetHoverEvent == null) {
            return null;
        }
        
        ChatHud chatHud = client.inGameHud.getChatHud();
        if (chatHud == null) {
            return null;
        }
        
        try {
            // Find the messages field by type (List<ChatHudLine>)
            java.lang.reflect.Field messagesField = null;
            for (java.lang.reflect.Field field : ChatHud.class.getDeclaredFields()) {
                if (java.util.List.class.isAssignableFrom(field.getType())) {
                    java.lang.reflect.Type genericType = field.getGenericType();
                    if (genericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericType;
                        java.lang.reflect.Type[] actualTypes = pt.getActualTypeArguments();
                        if (actualTypes.length > 0 && actualTypes[0] == net.minecraft.client.gui.hud.ChatHudLine.class) {
                            messagesField = field;
                            break;
                        }
                    }
                }
            }
            
            if (messagesField == null) {
                return null;
            }
            
            messagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<net.minecraft.client.gui.hud.ChatHudLine> messages = 
                (java.util.List<net.minecraft.client.gui.hud.ChatHudLine>) messagesField.get(chatHud);
            
            if (messages == null || messages.isEmpty()) {
                return null;
            }
            
            // Search through messages (newest first) to find one with matching HoverEvent
            for (int i = 0; i < Math.min(messages.size(), 100); i++) { // Check last 100 messages
                net.minecraft.client.gui.hud.ChatHudLine line = messages.get(i);
                if (line == null) {
                    continue;
                }
                
                Text message = line.content();
                if (message == null) {
                    continue;
                }
                
                // Check if this message has the target HoverEvent
                if (hasMatchingHoverEvent(message, targetHoverEvent)) {
                    return message;
                }
            }
        } catch (Exception e) {
            // Reflection failed, ignore
        }
        
        return null;
    }
    
    /**
     * Checks if a Text component has a HoverEvent that matches the target HoverEvent
     */
    private boolean hasMatchingHoverEvent(Text text, net.minecraft.text.HoverEvent targetHoverEvent) {
        if (text == null || targetHoverEvent == null) {
            return false;
        }
        
        // Check if this text component has the target HoverEvent
        if (text.getStyle() != null && text.getStyle().getHoverEvent() == targetHoverEvent) {
            return true;
        }
        
        // Recursively check siblings
        for (Text sibling : text.getSiblings()) {
            if (hasMatchingHoverEvent(sibling, targetHoverEvent)) {
                return true;
            }
        }
        
        return false;
    }
    
    
}

