package net.felix.features.itemviewer.filtermenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.felix.features.itemviewer.search.CostFilter;
import net.felix.features.itemviewer.search.FloorFilter;
import net.felix.features.itemviewer.search.ItemSearchParser;
import net.felix.features.itemviewer.search.ModifierFilter;
import net.felix.features.itemviewer.search.SearchQuery;
import net.felix.features.itemviewer.search.StatFilter;

import static net.felix.features.itemviewer.filtermenu.FilterCatalog.COST_LABEL_BY_KEY;
import static net.felix.features.itemviewer.filtermenu.FilterCatalog.FILTER_GROUP_DEFS;
import static net.felix.features.itemviewer.filtermenu.FilterCatalog.MODIFIER_CATEGORIES;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.selectedOptionKeys;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.attributeLabelForStatName;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.isMultiSelectGroup;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.resetInputFieldsForSync;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.COST_MENU_OPERATORS;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.setInputFieldText;
import static net.felix.features.itemviewer.filtermenu.SearchQueryBuilder.formatStatValueForSync;

/**
 * Uebernimmt einen Suchtext zurueck in die Auswahl des Filter-Menues.
 */
public final class SearchQuerySync {

    private SearchQuerySync() {
    }

    static void syncSelectionsFromSearch(String currentSearch) {
        selectedOptionKeys.clear();
        resetInputFieldsForSync();
        if (currentSearch == null || currentSearch.isBlank()) {
            return;
        }
        SearchQuery query = ItemSearchParser.parse(currentSearch.trim());
        for (ModifierFilter filter : query.modifierFilters) {
            if (filter.modifier == null) {
                continue;
            }
            for (String modifier : MODIFIER_CATEGORIES) {
                if (modifier.equalsIgnoreCase(filter.modifier)) {
                    String key = optionKey("Modifier", modifier);
                    if (filter.count != null) {
                        int count = filter.count;
                        String digit = String.valueOf(Math.abs(count) % 10);
                        setInputFieldText(key, digit);
                    }
                    selectedOptionKeys.add(key);
                    break;
                }
            }
        }
        for (CostFilter filter : query.costFilters) {
            if (filter.category == null) {
                continue;
            }
            String category = filter.category.toLowerCase(Locale.ROOT);
            if (category.startsWith("material") && !category.equals("material")) {
                category = "material";
            }
            String label = COST_LABEL_BY_KEY.get(category);
            if (label == null) {
                continue;
            }
            if (filter.itemName != null && !filter.itemName.isBlank()) {
                String key = optionKey("Item", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, filter.itemName.trim());
            } else if (filter.amount != null) {
                String key = optionKey("Anzahl", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, String.valueOf(filter.amount));
                String opKey = optionKey("Operator", label);
                String operator = filter.amountOperator != null && COST_MENU_OPERATORS.contains(filter.amountOperator)
                        ? filter.amountOperator
                        : "=";
                setInputFieldText(opKey, operator);
                if (!"=".equals(operator)) {
                    selectedOptionKeys.add(opKey);
                }
            }
        }
        for (StatFilter filter : query.statFilters) {
            String label = attributeLabelForStatName(filter.statName);
            if (label == null) {
                continue;
            }
            if (filter.operator != null && filter.value != null) {
                String key = optionKey("Att.-Anzahl", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, formatStatValueForSync(filter.value));
                String opKey = optionKey("Att.-Operator", label);
                String operator = filter.operator;
                setInputFieldText(opKey, COST_MENU_OPERATORS.contains(operator) ? operator : "=");
                if (!"=".equals(operator)) {
                    selectedOptionKeys.add(opKey);
                }
            } else {
                selectedOptionKeys.add(optionKey("Attribute", label));
            }
        }
        for (FloorFilter filter : query.floorFilters) {
            if (filter.operator == null || filter.value == null) {
                continue;
            }
            String label = "Ebene";
            String key = optionKey("Att.-Anzahl", label);
            selectedOptionKeys.add(key);
            setInputFieldText(key, String.valueOf(filter.value));
            String opKey = optionKey("Att.-Operator", label);
            String operator = filter.operator;
            setInputFieldText(opKey, COST_MENU_OPERATORS.contains(operator) ? operator : "=");
            if (!"=".equals(operator)) {
                selectedOptionKeys.add(opKey);
            }
        }
        List<FilterOptionDef> matched = new ArrayList<>();
        List<String> matchedGroupTitles = new ArrayList<>();
        for (FilterGroupDef def : FILTER_GROUP_DEFS) {
            for (FilterOptionDef optionDef : def.options) {
                if (optionDef.matchesQuery(query)) {
                    matched.add(optionDef);
                    matchedGroupTitles.add(def.title);
                }
            }
        }
        Map<String, String> singleSelectByGroup = new LinkedHashMap<>();
        Set<String> modifierKeys = new LinkedHashSet<>();
        for (int i = 0; i < matched.size(); i++) {
            FilterOptionDef optionDef = matched.get(i);
            String groupTitle = matchedGroupTitles.get(i);
            boolean subsetOfAnother = false;
            for (int j = 0; j < matched.size(); j++) {
                if (i == j) {
                    continue;
                }
                FilterOptionDef other = matched.get(j);
                if (other.tokens.size() > optionDef.tokens.size()
                        && other.tokens.containsAll(optionDef.tokens)) {
                    subsetOfAnother = true;
                    break;
                }
            }
            if (!subsetOfAnother) {
                String key = optionKey(groupTitle, optionDef.label);
                if (isMultiSelectGroup(groupTitle)) {
                    modifierKeys.add(key);
                } else {
                    singleSelectByGroup.put(groupTitle, key);
                }
            }
        }
        selectedOptionKeys.addAll(singleSelectByGroup.values());
        selectedOptionKeys.addAll(modifierKeys);
    }
}
