package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.TabInfoUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für einzelne Tab-Info Overlays
 */
public class TabInfoSeparateDraggableOverlay implements DraggableOverlay {
    
    private final String configKey;
    private final String displayName;
    
    public TabInfoSeparateDraggableOverlay(String infoName, String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }
    
    @Override
    public String getOverlayName() {
        return "Tab Info: " + displayName;
    }
    
    @Override
    public int getX() {
        return getXFromConfig();
    }
    
    @Override
    public int getY() {
        return getYFromConfig();
    }
    
    @Override
    public int getWidth() {
        // Berechne die tatsächliche Breite basierend auf dem Inhalt
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 200;
        
        String text = getDisplayText();
        String percentText = getPercentText();
        boolean showPercent = getShowPercent();
        boolean showWarning = getShowWarning();
        
        int width = client.textRenderer.getWidth(text);
        if (showPercent && percentText != null) {
            width += client.textRenderer.getWidth(" " + percentText);
        }
        if (showWarning) {
            width += client.textRenderer.getWidth(" !");
        }
        
        int padding = 5;
        return width + (padding * 2);
    }
    
    @Override
    public int getHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 20;
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        
        // Für Machtkristalle: mehrere Zeilen möglich
        if ("machtkristalle".equals(configKey)) {
            int lineCount = TabInfoUtility.getMachtkristallCount();
            return (lineCount * lineHeight) + (padding * 2);
        }
        
        return lineHeight + (padding * 2);
    }
    
    @Override
    public void setPosition(int x, int y) {
        setXInConfig(x);
        setYInConfig(y);
    }
    
    @Override
    public void setSize(int width, int height) {
        // Größe kann nicht geändert werden
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render background
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render actual content (same as real overlay)
        String text = getDisplayText();
        String percentText = getPercentText();
        boolean showPercent = getShowPercent();
        boolean showWarning = getShowWarning();
        
        int currentX = x + 5;
        int currentY = y + 5;
        int textColor = 0xFFFFFFFF;
        int percentColor = 0xFFFFFF00;
        
        // Render main text
        context.drawText(
            client.textRenderer,
            text,
            currentX, currentY,
            textColor,
            true
        );
        currentX += client.textRenderer.getWidth(text);
        
        // Render percent if enabled
        if (showPercent && percentText != null) {
            context.drawText(
                client.textRenderer,
                " " + percentText,
                currentX, currentY,
                percentColor,
                true
            );
            currentX += client.textRenderer.getWidth(" " + percentText);
        }
        
        // Render warning if active
        if (showWarning) {
            boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
            if (isVisible) {
                int warningColor = 0xFFFF0000;
                context.drawText(
                    client.textRenderer,
                    " !",
                    currentX, currentY,
                    warningColor,
                    true
                );
            }
        }
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled && 
               getSeparateOverlayEnabled();
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Tab Info: " + displayName + " - Separates Overlay");
    }
    
    @Override
    public void resetToDefault() {
        // Setze auf Standard-Positionen basierend auf configKey
        int defaultX = 10;
        int defaultY = getDefaultY();
        setXInConfig(defaultX);
        setYInConfig(defaultY);
    }
    
    @Override
    public void resetSizeToDefault() {
        // Größe kann nicht zurückgesetzt werden
    }
    
    private boolean getSeparateOverlayEnabled() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            default:
                return false;
        }
    }
    
    private int getXFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X;
            default:
                return 10;
        }
    }
    
    private int getYFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y;
            default:
                return 10;
        }
    }
    
    private void setXInConfig(int x) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX = x;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX = x;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX = x;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX = x;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX = x;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX = x;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX = x;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X = x;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X = x;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X = x;
                break;
        }
    }
    
    private void setYInConfig(int y) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY = y;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY = y;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY = y;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY = y;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY = y;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY = y;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY = y;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y = y;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y = y;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y = y;
                break;
        }
    }
    
    private int getDefaultY() {
        switch (configKey) {
            case "forschung":
                return 10;
            case "amboss":
                return 50;
            case "schmelzofen":
                return 90;
            case "jaeger":
                return 130;
            case "seelen":
                return 170;
            case "essenzen":
                return 210;
            case "machtkristalle":
                return 250;
            case "recyclerSlot1":
                return 290;
            case "recyclerSlot2":
                return 330;
            case "recyclerSlot3":
                return 370;
            default:
                return 10;
        }
    }
    
    private String getDisplayText() {
        switch (configKey) {
            case "forschung":
                return "Forschung: " + TabInfoUtility.forschung.getDisplayString();
            case "amboss":
                return "Amboss: " + TabInfoUtility.ambossKapazitaet.getDisplayString();
            case "schmelzofen":
                return "Schmelzofen: " + TabInfoUtility.schmelzofenKapazitaet.getDisplayString();
            case "jaeger":
                return "Jäger: " + TabInfoUtility.jaegerKapazitaet.getDisplayString();
            case "seelen":
                return "Seelen: " + TabInfoUtility.seelenKapazitaet.getDisplayString();
            case "essenzen":
                return "Essenzen: " + TabInfoUtility.essenzenKapazitaet.getDisplayString();
            case "machtkristalle":
                return "Machtkristall: X / Y";
            case "recyclerSlot1":
                return "Recycler Slot 1: " + TabInfoUtility.recyclerSlot1.getDisplayString();
            case "recyclerSlot2":
                return "Recycler Slot 2: " + TabInfoUtility.recyclerSlot2.getDisplayString();
            case "recyclerSlot3":
                return "Recycler Slot 3: " + TabInfoUtility.recyclerSlot3.getDisplayString();
            default:
                return displayName + ": X / Y";
        }
    }
    
    private String getPercentText() {
        switch (configKey) {
            case "amboss":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent && 
                    TabInfoUtility.ambossKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.ambossKapazitaet.current, 
                        TabInfoUtility.ambossKapazitaet.max
                    );
                }
                break;
            case "schmelzofen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent && 
                    TabInfoUtility.schmelzofenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.schmelzofenKapazitaet.current, 
                        TabInfoUtility.schmelzofenKapazitaet.max
                    );
                }
                break;
            case "jaeger":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent && 
                    TabInfoUtility.jaegerKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.jaegerKapazitaet.current, 
                        TabInfoUtility.jaegerKapazitaet.max
                    );
                }
                break;
            case "seelen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent && 
                    TabInfoUtility.seelenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.seelenKapazitaet.current, 
                        TabInfoUtility.seelenKapazitaet.max
                    );
                }
                break;
            case "essenzen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent && 
                    TabInfoUtility.essenzenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.essenzenKapazitaet.current, 
                        TabInfoUtility.essenzenKapazitaet.max
                    );
                }
                break;
            case "recyclerSlot1":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent && 
                    TabInfoUtility.recyclerSlot1.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot1.current, 
                        TabInfoUtility.recyclerSlot1.max
                    );
                }
                break;
            case "recyclerSlot2":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent && 
                    TabInfoUtility.recyclerSlot2.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot2.current, 
                        TabInfoUtility.recyclerSlot2.max
                    );
                }
                break;
            case "recyclerSlot3":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent && 
                    TabInfoUtility.recyclerSlot3.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot3.current, 
                        TabInfoUtility.recyclerSlot3.max
                    );
                }
                break;
        }
        return null;
    }
    
    private boolean getShowPercent() {
        switch (configKey) {
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent;
            default:
                return false;
        }
    }
    
    private boolean getShowWarning() {
        switch (configKey) {
            case "amboss":
                if (TabInfoUtility.ambossKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.ambossKapazitaet.current / 
                        (double)TabInfoUtility.ambossKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "schmelzofen":
                if (TabInfoUtility.schmelzofenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.schmelzofenKapazitaet.current / 
                        (double)TabInfoUtility.schmelzofenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "jaeger":
                if (TabInfoUtility.jaegerKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.jaegerKapazitaet.current / 
                        (double)TabInfoUtility.jaegerKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "seelen":
                if (TabInfoUtility.seelenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.seelenKapazitaet.current / 
                        (double)TabInfoUtility.seelenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "essenzen":
                if (TabInfoUtility.essenzenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.essenzenKapazitaet.current / 
                        (double)TabInfoUtility.essenzenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "recyclerSlot1":
                if (TabInfoUtility.recyclerSlot1.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot1.current / 
                        (double)TabInfoUtility.recyclerSlot1.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1WarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "recyclerSlot2":
                if (TabInfoUtility.recyclerSlot2.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot2.current / 
                        (double)TabInfoUtility.recyclerSlot2.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2WarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "recyclerSlot3":
                if (TabInfoUtility.recyclerSlot3.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot3.current / 
                        (double)TabInfoUtility.recyclerSlot3.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3WarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
        }
        return false;
    }
}

