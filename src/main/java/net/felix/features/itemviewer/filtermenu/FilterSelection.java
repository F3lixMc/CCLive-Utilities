package net.felix.features.itemviewer.filtermenu;

import java.util.List;
import java.util.Map;
import net.felix.features.itemviewer.search.ItemFilterPipeline;

import static net.felix.features.itemviewer.filtermenu.FilterCatalog.ATTRIBUTE_LABELS;
import static net.felix.features.itemviewer.filtermenu.FilterCatalog.FILTER_ROW_TITLES;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.draftInputFieldText;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.draftSelectedOptionKeys;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.inputFieldStates;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.inputFieldText;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.selectedOptionKeys;
import static net.felix.features.itemviewer.filtermenu.SearchQueryBuilder.buildSearchFromSelectedOptions;

/**
 * Auswahl der Filter-Optionen: umschalten, zuruecksetzen, Entwuerfe sichern und wiederherstellen.
 */
public final class FilterSelection {

    private FilterSelection() {
    }

    static FilterGroup findGroupByTitle(String title) {
        for (FilterGroup group : groups) {
            if (group.title.equals(title)) {
                return group;
            }
        }
        return null;
    }

    static List<FilterGroup> getRowGroups(int rowIndex) {
        int start = 0;
        for (int i = 0; i < rowIndex; i++) {
            start += FILTER_ROW_TITLES.get(i).size();
        }
        int end = start + FILTER_ROW_TITLES.get(rowIndex).size();
        return groups.subList(start, end);
    }

    static String optionKey(String groupTitle, String optionLabel) {
        return groupTitle + '\0' + optionLabel;
    }

    static void toggleOption(FilterGroup group, FilterOption option) {
        String key = optionKey(group.title, option.label);
        if (selectedOptionKeys.contains(key)) {
            selectedOptionKeys.remove(key);
        } else {
            if (!isMultiSelectGroup(group.title)) {
                clearGroupSelections(group);
            }
            selectedOptionKeys.add(key);
        }
        applySelections();
    }

    static boolean isMultiSelectGroup(String groupTitle) {
        return "Modifier".equals(groupTitle) || "Item".equals(groupTitle) || "Operator".equals(groupTitle)
                || "Anzahl".equals(groupTitle) || "Attribute".equals(groupTitle) || "Att.-Operator".equals(groupTitle)
                || "Att.-Anzahl".equals(groupTitle);
    }

    static String displayGroupTitle(String groupTitle) {
        return switch (groupTitle) {
            case "Att.-Operator" -> "Operator";
            case "Att.-Anzahl" -> "Anzahl";
            default -> groupTitle;
        };
    }

    static boolean isAttributeLabel(String label) {
        for (String attributeLabel : ATTRIBUTE_LABELS) {
            if (attributeLabel.equals(label)) {
                return true;
            }
        }
        return false;
    }

    static String attributeLabelForStatName(String statName) {
        if (statName == null) {
            return null;
        }
        for (String label : ATTRIBUTE_LABELS) {
            if (label.equalsIgnoreCase(statName)) {
                return label;
            }
        }
        return null;
    }

    private static void clearGroupSelections(FilterGroup group) {
        for (FilterOption groupOption : group.options) {
            selectedOptionKeys.remove(optionKey(group.title, groupOption.label));
        }
    }

    static boolean isOptionSelected(FilterGroup group, FilterOption option) {
        return selectedOptionKeys.contains(optionKey(group.title, option.label));
    }

    static void applySelections() {
        String search = buildSearchFromSelectedOptions();
        ItemFilterPipeline.setSearchFromFilterMenu(search);
    }

    static void persistAllInputDrafts() {
        draftInputFieldText.putAll(inputFieldText);
        draftSelectedOptionKeys.clear();
        for (String key : selectedOptionKeys) {
            if (isPersistedSelectionKey(key)) {
                draftSelectedOptionKeys.add(key);
            }
        }
    }

    static void commitInputFieldChange(String inputKey) {
        draftInputFieldText.put(inputKey, inputFieldText.getOrDefault(inputKey, ""));
        if (selectedOptionKeys.contains(inputKey)) {
            applySelections();
        }
    }

    static void restoreInputDrafts() {
        for (Map.Entry<String, String> entry : draftInputFieldText.entrySet()) {
            if (!selectedOptionKeys.contains(entry.getKey())) {
                inputFieldText.put(entry.getKey(), entry.getValue());
            }
        }
    }

    static void restoreSelectionDrafts() {
        selectedOptionKeys.addAll(draftSelectedOptionKeys);
    }

    private static boolean isPersistedSelectionKey(String key) {
        int separator = key.indexOf('\0');
        if (separator < 0) {
            return false;
        }
        return isMultiSelectGroup(key.substring(0, separator));
    }

    static void ensureInputFields() {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (option.inputType != RowInputType.NONE) {
                    String key = optionKey(group.title, option.label);
                    if (option.inputType == RowInputType.COST_OPERATOR) {
                        inputFieldText.putIfAbsent(key, "=");
                    } else {
                        inputFieldText.putIfAbsent(key, "");
                    }
                }
            }
        }
    }

    static void resetInputFieldsForSync() {
        inputFieldText.clear();
        inputFieldStates.clear();
        ensureInputFields();
    }

    static void clearAllInputFields() {
        resetInputFieldsForSync();
        draftInputFieldText.clear();
        draftSelectedOptionKeys.clear();
    }
}
