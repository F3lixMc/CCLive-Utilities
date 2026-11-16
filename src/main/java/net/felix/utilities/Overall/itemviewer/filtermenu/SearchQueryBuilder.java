package net.felix.utilities.Overall.itemviewer.filtermenu;

import java.util.LinkedHashSet;

import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterCatalog.ATTRIBUTE_SEARCH_NAME;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.inputFieldText;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.selectedOptionKeys;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.isAttributeLabel;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.isOptionSelected;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.utilities.Overall.itemviewer.filtermenu.InputFieldEditor.COST_MENU_OPERATORS;

/**
 * Baut aus den gewaehlten Optionen den Suchtext.
 */
public final class SearchQueryBuilder {

    private SearchQueryBuilder() {
    }

    static String buildSearchFromSelectedOptions() {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (!isOptionSelected(group, option)) {
                    continue;
                }
                if (option.inputType == RowInputType.MODIFIER_DIGIT) {
                    tokens.add(formatModifierSearchToken(group, option));
                } else if (option.inputType == RowInputType.COST_ITEM) {
                    String costToken = formatItemCostSearchToken(group, option);
                    if (costToken != null) {
                        tokens.add(costToken);
                    }
                } else if (option.inputType == RowInputType.COST_AMOUNT) {
                    if (isAttributeLabel(option.label)) {
                        String attrToken = formatAttributeAnzahlSearchToken(group, option);
                        if (attrToken != null) {
                            tokens.add(attrToken);
                        }
                    } else {
                        String costToken = formatAnzahlCostSearchToken(group, option);
                        if (costToken != null) {
                            tokens.add(costToken);
                        }
                    }
                } else if ("Attribute".equals(group.title)) {
                    String attrToken = formatAttributeOnlySearchToken(option);
                    if (attrToken != null) {
                        tokens.add(attrToken);
                    }
                } else if (option.inputType != RowInputType.COST_OPERATOR) {
                    tokens.addAll(option.tokens);
                }
            }
        }
        return String.join(", ", tokens);
    }

    private static String formatModifierSearchToken(FilterGroup group, FilterOption option) {
        String base = option.tokens.get(0);
        String count = inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim();
        if (!count.isEmpty()) {
            return base + ":" + count;
        }
        return base;
    }

    private static String formatItemCostSearchToken(FilterGroup group, FilterOption option) {
        String category = option.tokens.get(0);
        String value = inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim();
        if (value.isEmpty()) {
            return null;
        }
        return category + ":" + value;
    }

    private static String formatAnzahlCostSearchToken(FilterGroup group, FilterOption option) {
        String category = option.tokens.get(0);
        String digits = extractAmountDigits(
                inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim());
        if (digits.isEmpty()) {
            return null;
        }
        String operator = getCostOperatorForLabel(option.label);
        if ("=".equals(operator)) {
            return category + ":" + digits;
        }
        return category + ":" + operator + digits;
    }

    private static String getCostOperatorForLabel(String label) {
        String operator = inputFieldText.getOrDefault(optionKey("Operator", label), "=").trim();
        return COST_MENU_OPERATORS.contains(operator) ? operator : "=";
    }

    private static String getAttributeOperatorForLabel(String label) {
        String operator = inputFieldText.getOrDefault(optionKey("Att.-Operator", label), "=").trim();
        return COST_MENU_OPERATORS.contains(operator) ? operator : "=";
    }

    private static String formatAttributeOnlySearchToken(FilterOption option) {
        String label = option.label;
        String anzahlKey = optionKey("Att.-Anzahl", label);
        if (selectedOptionKeys.contains(anzahlKey)) {
            String digits = extractAmountDigits(inputFieldText.getOrDefault(anzahlKey, "").trim());
            if (!digits.isEmpty()) {
                return null;
            }
        }
        String searchName = ATTRIBUTE_SEARCH_NAME.get(label);
        return searchName != null ? "@" + searchName : null;
    }

    private static String formatAttributeAnzahlSearchToken(FilterGroup group, FilterOption option) {
        String label = option.label;
        String searchName = ATTRIBUTE_SEARCH_NAME.get(label);
        if (searchName == null) {
            return null;
        }
        String amountValue = "ItemScore".equals(label)
                ? extractDecimalAmount(inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim())
                : extractAmountDigits(inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim());
        if (amountValue.isEmpty()) {
            return null;
        }
        if ("Ebene".equals(label)) {
            amountValue = clampEbeneDigits(amountValue);
            if (amountValue.isEmpty()) {
                return null;
            }
        }
        String operator = getAttributeOperatorForLabel(label);
        if ("Ebene".equals(label)) {
            if ("=".equals(operator)) {
                return "@Ebene=" + amountValue;
            }
            return "@Ebene" + operator + amountValue;
        }
        if ("=".equals(operator)) {
            return "@" + searchName + "=" + amountValue;
        }
        return "@" + searchName + operator + amountValue;
    }

    static String formatStatValueForSync(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String clampEbeneDigits(String digits) {
        if (digits.isEmpty()) {
            return digits;
        }
        try {
            int value = Integer.parseInt(digits);
            value = Math.max(1, Math.min(100, value));
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return digits;
        }
    }

    private static String extractAmountDigits(String value) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        return digits.toString();
    }

    private static String extractDecimalAmount(String value) {
        StringBuilder amount = new StringBuilder();
        boolean hasSeparator = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                amount.append(c);
            } else if ((c == '.' || c == ',') && !hasSeparator) {
                amount.append('.');
                hasSeparator = true;
            }
        }
        return amount.toString();
    }
}
