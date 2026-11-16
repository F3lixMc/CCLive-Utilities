package net.felix.utilities.Aincraft.itemviewer.filtermenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.felix.utilities.Aincraft.itemviewer.data.ItemData;

import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterTextFormat.formatRarityLabel;

/**
 * Katalog der Filtergruppen, Kategorien und Optionen.
 */
public final class FilterCatalog {

    private FilterCatalog() {
    }

    static final String[] MODIFIER_CATEGORIES = {
            "Andere", "Herstellung", "Fähigkeiten", "Schaden", "Verteidigung", "Attribute"
    };

    private static final String[] COST_CATEGORY_LABELS = {
            "Amboss", "Ofen", "Material", "Ressource"
    };

    private static final Map<String, String> COST_CATEGORY_KEYS = Map.of(
            "Amboss", "amboss",
            "Ofen", "ofen",
            "Material", "material",
            "Ressource", "ressource"
    );

    static final Map<String, String> COST_LABEL_BY_KEY = Map.of(
            "amboss", "Amboss",
            "ofen", "Ofen",
            "material", "Material",
            "ressource", "Ressource"
    );

    static final String[] ATTRIBUTE_LABELS = {
            "Ebene", "Schaden", "Rüstung", "Abbaugeschwindigkeit", "ItemScore"
    };

    static final Map<String, String> ATTRIBUTE_SEARCH_NAME = Map.of(
            "Ebene", "Ebene",
            "Schaden", "Schaden",
            "Rüstung", "Rüstung",
            "Abbaugeschwindigkeit", "Abbaugeschwindigkeit",
            "ItemScore", "ItemScore"
    );

    public static final List<String> RARITY_ORDER = List.of(
            "common", "uncommon", "rare", "epic", "legendary"
    );

    static final List<List<String>> FILTER_ROW_TITLES = List.of(
            List.of("Rüstung", "Angel-Komponenten", "Typ", "Waffen", "Werkzeuge"),
            List.of("Schmiedezustand", "Modifier", "Seltenheit"),
            List.of("Item", "Operator", "Anzahl"),
            List.of("Attribute", "Att.-Operator", "Att.-Anzahl")
    );

    /** Feste Filter-Gruppen (Titel → Definition). */
    static final List<FilterGroupDef> FILTER_GROUP_DEFS = List.of(
            new FilterGroupDef("Rüstung", List.of(
                    tag("Helm", "helm"),
                    tag("Brust", "brustplatte"),
                    tag("Hose", "hose"),
                    tag("Schuhe", "schuhe"),
                    tag("Ring", "ring"),
                    tag("Handschuhe", "handschuhe"),
                    tag("Schulter", "schulter"),
                    tag("Halskette", "halskette"),
                    tag("Gürtel", "gürtel")
            )),
            new FilterGroupDef("Werkzeuge", List.of(
                    tag("Spitzhacke", "spitzhacke"),
                    tags("Axt", "#werkzeug", "#axt"),
                    tag("Hacke", "hacke"),
                    tag("Angel", "angel")
            )),
            new FilterGroupDef("Waffen", List.of(
                    tag("Schwert", "schwert"),
                    tags("Axt", "#waffe", "#axt"),
                    tag("Bogen", "bogen"),
                    tag("Armbrust", "armbrust")
            )),
            new FilterGroupDef("Modifier", modifierOptions()),
            new FilterGroupDef("Seltenheit", rarityOptions()),
            new FilterGroupDef("Schmiedezustand", forgingOptions()),
            new FilterGroupDef("Angel-Komponenten", List.of(
                    tag("Gewicht", "gewicht"),
                    tag("Haken", "haken"),
                    tag("Rolle", "rolle"),
                    tag("Köder", "köder"),
                    tag("Schnur", "schnur"),
                    tag("Pose", "pose"),
                    tag("Rute", "rute"),
                    tag("Spinner", "spinner")
            )),
            new FilterGroupDef("Typ", List.of(
                    tag("Platte", "platte"),
                    tag("Leder", "leder"),
                    tag("Accessoire", "accessoire"),
                    tag("Waffe", "waffe"),
                    tags("Werkzeug", "#werkzeug", "#werkzeuge")
            )),
            new FilterGroupDef("Verschiedenes", List.of(
                    tag("Module", "modul"),
                    tag("Modultasche", "modultasche"),
                    tag("Machtkristalle", "machtkristall"),
                    tag("Karten", "kartenslot"),
                    tag("Lizenzen", "lizenz"),
                    tag("Fähigkeiten", "fähigkeit")
            )),
            new FilterGroupDef("Item", costCategoryOptions()),
            new FilterGroupDef("Operator", costCategoryOptions()),
            new FilterGroupDef("Anzahl", costCategoryOptions()),
            new FilterGroupDef("Attribute", attributeOptions()),
            new FilterGroupDef("Att.-Operator", attributeOptions()),
            new FilterGroupDef("Att.-Anzahl", attributeOptions())
    );

    static void rebuildGroups(List<ItemData> items) {
        groups.clear();
        Map<String, FilterGroupDef> defsByTitle = new LinkedHashMap<>();
        for (FilterGroupDef def : FILTER_GROUP_DEFS) {
            defsByTitle.put(def.title, def);
        }
        for (List<String> rowTitles : FILTER_ROW_TITLES) {
            for (String title : rowTitles) {
                FilterGroupDef def = defsByTitle.get(title);
                if (def == null) {
                    continue;
                }
                List<FilterOption> options = new ArrayList<>();
                RowInputType inputType = rowInputTypeForGroup(def.title);
                for (FilterOptionDef optionDef : def.options) {
                    options.add(new FilterOption(optionDef.label, optionDef.tokens, inputType));
                }
                groups.add(new FilterGroup(def.title, options));
            }
        }
    }

    public static FilterOptionDef tag(String label, String tagName) {
        return new FilterOptionDef(label, "#" + tagName.toLowerCase(Locale.ROOT));
    }

    private static FilterOptionDef tags(String label, String... tokens) {
        return new FilterOptionDef(label, tokens);
    }

    private static List<FilterOptionDef> costCategoryOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String label : COST_CATEGORY_LABELS) {
            String category = COST_CATEGORY_KEYS.get(label);
            options.add(new FilterOptionDef(label, category));
        }
        return options;
    }

    private static List<FilterOptionDef> attributeOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String label : ATTRIBUTE_LABELS) {
            options.add(new FilterOptionDef(label, "@" + label));
        }
        return options;
    }

    private static RowInputType rowInputTypeForGroup(String groupTitle) {
        return switch (groupTitle) {
            case "Modifier" -> RowInputType.MODIFIER_DIGIT;
            case "Operator", "Att.-Operator" -> RowInputType.COST_OPERATOR;
            case "Anzahl", "Att.-Anzahl" -> RowInputType.COST_AMOUNT;
            case "Item" -> RowInputType.COST_ITEM;
            default -> RowInputType.NONE;
        };
    }

    private static List<FilterOptionDef> modifierOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String modifier : MODIFIER_CATEGORIES) {
            options.add(new FilterOptionDef(modifier, "+" + modifier));
        }
        return options;
    }

    private static List<FilterOptionDef> rarityOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String rarity : RARITY_ORDER) {
            options.add(new FilterOptionDef(formatRarityLabel(rarity), "#" + rarity));
        }
        return options;
    }

    private static List<FilterOptionDef> forgingOptions() {
        return List.of(
                tag("Frostgeschmiedet", "frostgeschmiedet"),
                tag("Lavageschmiedet", "lavageschmiedet"),
                tag("Titangeschmiedet", "titangeschmiedet"),
                tag("Drachengeschmiedet", "drachengeschmiedet"),
                tag("Dämonengeschmiedet", "dämonengeschmiedet"),
                tag("Blitzgeschmiedet", "blitzgeschmiedet"),
                tag("Sternengeschmiedet", "sternengeschmiedet")
        );
    }
}
