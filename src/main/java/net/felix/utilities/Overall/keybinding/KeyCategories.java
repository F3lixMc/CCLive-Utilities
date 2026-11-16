package net.felix.utilities.Overall.keybinding;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KeyCategories {
    private static final ConcurrentHashMap<Identifier, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();

    private KeyCategories() {
    }

    public static KeyMapping.Category of(String namespace, String path) {
        return of(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static KeyMapping.Category of(Identifier id) {
        return CATEGORIES.computeIfAbsent(id, KeyMapping.Category::register);
    }
}
