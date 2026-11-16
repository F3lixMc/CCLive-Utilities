package net.felix.features.itemviewer.filtermenu;

import java.util.List;
import net.felix.features.itemviewer.search.ModifierFilter;
import net.felix.features.itemviewer.search.SearchQuery;

import static net.felix.features.itemviewer.filtermenu.FilterTextFormat.normalizeTagToken;

public class FilterOptionDef {
    final String label;
    final List<String> tokens;

    FilterOptionDef(String label, String... tokens) {
        this.label = label;
        this.tokens = List.of(tokens);
    }

    boolean matchesQuery(SearchQuery query) {
        for (String token : tokens) {
            if (token.startsWith("+")) {
                String modifier = token.substring(1);
                int colon = modifier.indexOf(':');
                if (colon >= 0) {
                    modifier = modifier.substring(0, colon);
                }
                boolean found = false;
                for (ModifierFilter filter : query.modifierFilters) {
                    if (filter.modifier != null && filter.modifier.equalsIgnoreCase(modifier)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            } else {
                String tag = normalizeTagToken(token);
                boolean found = false;
                for (String queryTag : query.tags) {
                    if (queryTag.equalsIgnoreCase(tag)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }
}
