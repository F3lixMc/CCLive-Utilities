package net.felix.utilities.Overall;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ConcurrentHashMap;

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
