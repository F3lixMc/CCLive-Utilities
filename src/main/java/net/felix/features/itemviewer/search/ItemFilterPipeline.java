package net.felix.features.itemviewer.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.inventory.FavoriteBlueprintsStore;
import net.felix.features.itemviewer.inventory.FoundItemChecker;
import net.felix.features.itemviewer.inventory.InventoryFilter;
import net.felix.features.itemviewer.tooltip.TooltipCache;
import net.felix.features.itemviewer.ui.ItemViewerGrid;
import net.minecraft.client.Minecraft;

import static net.felix.features.itemviewer.data.ItemCatalog.allItems;
import static net.felix.features.itemviewer.data.ItemCatalog.ensureItemsLoaded;
import static net.felix.features.itemviewer.data.ItemCatalog.itemsLoaded;
import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_INDEX;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitFilterActive;
import static net.felix.features.itemviewer.search.SearchFieldState.clearSelection;
import static net.felix.features.itemviewer.search.SearchFieldState.cursorPosition;
import static net.felix.features.itemviewer.search.SearchFieldState.setSearchFieldFocused;
import static net.felix.features.itemviewer.ui.ViewerLayout.currentGridAvailableHeight;
import static net.felix.features.itemviewer.ui.ViewerLayout.currentGridAvailableWidth;
import static net.felix.features.itemviewer.ui.ViewerState.favoritesMode;

/**
 * Wendet Suche, Filter und Sortierung auf den Katalog an und liefert die aktuelle Seite.
 */
public final class ItemFilterPipeline {

    private ItemFilterPipeline() {
    }

    public static List<ItemData> filteredItems = new ArrayList<>();

    private static final int FILTER_DEBOUNCE_TICKS = 2;

    private static int filterDebounceTicks = 0;

    private static int filterRevision = 0;

    private static List<ItemData> cachedPageItemsList = null;

    private static int cachedPageItemsPage = -1;

    private static int cachedPageItemsRevision = -1;

    private static int cachedPageItemsPerPage = -1;

    public static String currentSearch = "";

    public static SortMode currentSortMode = SortMode.DEFAULT;

    public static int currentPage = 0;

    /**
     * Verzögert Filter-Updates beim Tippen, um Lag bei großen Item-Listen zu vermeiden.
     */
    static void scheduleApplyFilters() {
        filterDebounceTicks = FILTER_DEBOUNCE_TICKS;
    }

    /**
     * Zählt die Entprellzeit pro Client-Tick herunter und wendet die Filter an, sobald sie abgelaufen ist.
     */
    public static void tickDebounce() {
        if (filterDebounceTicks > 0) {
            filterDebounceTicks--;
            if (filterDebounceTicks == 0) {
                applyFilters();
            }
        }
    }

    public static List<ItemData> getCurrentPageItems() {
        int itemsPerPage = ItemViewerGrid.computeItemsPerPage(currentGridAvailableWidth, currentGridAvailableHeight);
        if (cachedPageItemsList != null
                && cachedPageItemsPage == currentPage
                && cachedPageItemsRevision == filterRevision
                && cachedPageItemsPerPage == itemsPerPage) {
            return cachedPageItemsList;
        }
        int start = currentPage * itemsPerPage;
        if (start >= filteredItems.size()) {
            cachedPageItemsList = new ArrayList<>();
        } else {
            int end = Math.min(start + itemsPerPage, filteredItems.size());
            cachedPageItemsList = new ArrayList<>(filteredItems.subList(start, end));
        }
        cachedPageItemsPage = currentPage;
        cachedPageItemsRevision = filterRevision;
        cachedPageItemsPerPage = itemsPerPage;
        return cachedPageItemsList;
    }

    public static void setSearch(String search) {
        currentSearch = search;
        scheduleApplyFilters();
    }

    public static void setSearchFromFilterMenu(String search) {
        currentSearch = search != null ? search : "";
        cursorPosition = currentSearch.length();
        clearSelection();
        setSearchFieldFocused(false);
        scheduleApplyFilters();
    }

    public static void setSortMode(SortMode mode) {
        SortMode previous = currentSortMode;
        currentSortMode = mode;
        if (previous.filtersNotFoundOnly() || mode.filtersNotFoundOnly()) {
            applyFilters();
        } else {
            applySorting();
            invalidatePageItemsCache();
            TooltipCache.invalidateTooltipCache();
        }
    }

    public static void scheduleApplyFiltersAfterLoad() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(ItemFilterPipeline::applyFilters);
        } else {
            applyFilters();
        }
    }

    public static void applyFilters() {
        filteredItems = new ArrayList<>(buildFilteredItemList());
        applySorting();
        currentPage = 0;
        invalidatePageItemsCache();
        InventoryFilter.onFilteredItemsChanged();
    }

    /**
     * Berechnet die gefilterte Item-Liste (Favoriten, Kit, Suche, „Nicht Gefunden“) ohne UI-Seitenzurücksetzung.
     */
    private static List<ItemData> buildFilteredItemList() {
        ensureItemsLoaded();
        if (!itemsLoaded || allItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<ItemData> itemsToFilter = allItems;

        if (favoritesMode) {
            Set<String> favoriteNames = FavoriteBlueprintsStore.getFavoriteNames();
            itemsToFilter = allItems.stream()
                .filter(item -> item.info != null &&
                               Boolean.TRUE.equals(item.info.blueprint) &&
                               item.name != null &&
                               favoriteNames.contains(item.name))
                .collect(Collectors.toList());
        }

        if (kitFilterActive) {
            net.felix.utilities.Town.kits.filter.KitFilterUtility.KitSelection kitSelection =
                net.felix.utilities.Town.kits.filter.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
            if (kitSelection != null) {
                java.util.Set<String> kitItemNames = net.felix.utilities.Town.kits.filter.KitFilterUtility.getKitItemNamesForSelection(
                    kitSelection);

                if (!kitItemNames.isEmpty()) {
                    itemsToFilter = itemsToFilter.stream()
                        .filter(item -> {
                            if (item.name == null || item.name.isEmpty()) {
                                return false;
                            }
                            String cleanItemName = item.name.replaceAll("§[0-9a-fk-or]", "");
                            for (String kitItemName : kitItemNames) {
                                String cleanKitItemName = kitItemName.replaceAll("§[0-9a-fk-or]", "");
                                if (cleanItemName.equalsIgnoreCase(cleanKitItemName)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                } else {
                    itemsToFilter = new ArrayList<>();
                }
            } else {
                kitFilterActive = false;
            }
        }

        List<ItemData> result;
        if (currentSearch == null || currentSearch.trim().isEmpty()) {
            result = new ArrayList<>(itemsToFilter);
        } else {
            SearchQuery query = ItemSearchParser.parse(currentSearch);
            result = itemsToFilter.stream()
                .filter(item -> ItemFilter.matchesSearch(item, query))
                .collect(Collectors.toList());
        }

        if (currentSortMode.filtersNotFoundOnly()) {
            result = result.stream()
                .filter(item -> !FoundItemChecker.isFound(item))
                .collect(Collectors.toList());
        }

        return result;
    }

    /**
     * Gibt die bereinigten Namen der aktuell gefilterten Item-Viewer-Einträge zurück.
     * Berechnet die Liste frisch, damit z. B. „Nicht Gefunden“ auch ohne Suchtext greift.
     */
    public static java.util.Set<String> getFilteredItemNamesForInventory(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        java.util.Set<String> names = new java.util.HashSet<>();
        String categoryFilter = screen != null
            ? InventoryFilter.resolveItemCategoryForInventory(screen)
            : null;

        for (ItemData item : buildFilteredItemList()) {
            if (item.name == null || item.name.isEmpty()) {
                continue;
            }
            if (categoryFilter != null && item.category != null
                    && !categoryFilter.equalsIgnoreCase(item.category)) {
                continue;
            }
            names.add(cleanItemNameForInventoryMatch(item.name));
        }
        return names;
    }

    private static String cleanItemNameForInventoryMatch(String name) {
        return name.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "").trim();
    }

    private static void applySorting() {
        Comparator<ItemData> comparator = currentSortMode.getComparator();
        filteredItems.sort(comparator);
    }

    public static void invalidatePageItemsCache() {
        filterRevision++;
        cachedPageItemsList = null;
    }
}
