package net.felix.devtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DevToolsRegistry {

    private static final Map<String, DevTool> TOOLS = new LinkedHashMap<>();

    private DevToolsRegistry() {
    }

    public static void initialize() {
        TOOLS.clear();
        register(new NearbyDevTool());
        register(new ItemDataReaderDevTool());
        DevToolsStateStore.ensureLoaded();
        for (DevTool tool : TOOLS.values()) {
            DevToolsStateStore.ToolState state = DevToolsStateStore.get(tool.id());
            if (state.modeId == null && !tool.modes().isEmpty()) {
                state.modeId = tool.modes().get(0).id();
            }
        }
    }

    private static void register(DevTool tool) {
        TOOLS.put(tool.id(), tool);
    }

    public static List<DevTool> all() {
        return Collections.unmodifiableList(new ArrayList<>(TOOLS.values()));
    }

    public static DevTool get(String id) {
        return TOOLS.get(id);
    }
}
