package net.felix.utilities.Overall.eventsound;

import java.nio.file.Path;
import net.felix.utilities.Overall.eventsound.chat.EventSoundMatcher;
import net.felix.utilities.Overall.eventsound.config.EventSoundPaths;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.data.EventSoundMatchMode;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSoundRulesTest {
    @TempDir
    Path directory;

    @BeforeEach
    void useTemporaryDirectory() {
        EventSoundPaths.useRoot(directory);
        EventSoundStore.resetForTests();
    }

    @Test
    void recognizesSmithingStatesAndAliases() {
        assertEquals("Frostgeschmiedet", EventSoundMatcher.smithingState(
                "[Legend] Schmiedezustand erhalten: [Frostgeschmiedet]"));
        assertEquals("Sternengeschmiedet", EventSoundMatcher.smithingState(
                "[Legend] Schmiedezustand erhalten: [Sterngeschmiedet] extra"));
        assertEquals("Sternengeschmiedet", EventSoundMatcher.smithingState(
                "[Legend] Schmiedezustand erhalten: [Starforged]"));
        assertNull(EventSoundMatcher.smithingState(
                "Spieler >> [Legend] Schmiedezustand erhalten: [Frostgeschmiedet]"));
        assertNull(EventSoundMatcher.smithingState("[Legend] Du erhältst Schwert - [Bauplan]"));
    }

    @Test
    void recognizesBlueprintDrops() {
        assertEquals("Drachenzahn", EventSoundMatcher.blueprintName(
                "[Legend] Du erhältst Drachenzahn - [Bauplan]"));
        assertEquals("Drachenzahn", EventSoundMatcher.blueprintName(
                "[Legend] - Du erhälst Drachenzahn - [Bauplan]"));
        assertEquals("Drachenzahn", EventSoundMatcher.blueprintName(
                "§a+ 1x Drachenzahn §f- §3[Bauplan]".replaceAll("§[0-9a-fk-or]", "")));
        assertNull(EventSoundMatcher.blueprintName(
                "Spieler >> [Legend] Du erhältst Drachenzahn - [Bauplan]"));
    }

    @Test
    void customRespectsSourceAndMatchMode() {
        EventSoundRule rule = custom("Hallo");
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Hallo"));
        assertTrue(EventSoundMatcher.matchesCustom(rule, "xx Hallo yy"));
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Name >> Hallo"));

        rule.matchMode = EventSoundMatchMode.EXACT;
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Hallo"));
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Name >> Hallo"));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "Name >> Hallo extra"));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "xx Hallo yy"));

        rule.fromPlayer = false;
        assertFalse(EventSoundMatcher.matchesCustom(rule, "Name >> Hallo"));
        assertTrue(EventSoundMatcher.matchesCustom(custom("Hallo"), "<Name> Hallo"));
        assertTrue(EventSoundMatcher.matchesCustom(custom("Hallo"), "Name » Hallo"));
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Hallo"));

        rule.fromServer = false;
        rule.fromPlayer = false;
        assertFalse(EventSoundMatcher.matchesCustom(rule, "Hallo"));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "Name >> Hallo"));
    }

    @Test
    void namePlaceholderMatchesOneWord() {
        EventSoundRule rule = custom("[Legend] Du hast <name> gefunden.");
        rule.matchMode = EventSoundMatchMode.EXACT;
        assertTrue(EventSoundMatcher.matchesCustom(rule, "[Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Felix >> [Legend] Du hast Schmiedekammer-Talismanfragment gefunden."));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "[Legend] Du hast zwei wörter gefunden."));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "[Legend] Du hast Schmiedekammer-Talismanfragment gefunden. extra"));

        rule.matchMode = EventSoundMatchMode.CONTAINS;
        rule.pattern = "<name> und <name>";
        assertTrue(EventSoundMatcher.matchesCustom(rule, "Alpha und Beta"));
        assertFalse(EventSoundMatcher.matchesCustom(rule, "Alpha und"));
    }

    @Test
    void savesRulesAndKeepsOneSmithingState() {
        EventSoundRule frost = new EventSoundRule();
        frost.kind = EventSoundKind.SMITHING;
        frost.target = "Frostgeschmiedet";
        frost.soundFile = "frost.ogg";
        frost.volume = 0.4f;
        EventSoundStore.save(frost);

        EventSoundRule replacement = new EventSoundRule();
        replacement.kind = EventSoundKind.SMITHING;
        replacement.target = "Frostgeschmiedet";
        replacement.soundFile = "neu.ogg";
        replacement.volume = 0.4f;
        EventSoundStore.save(replacement);

        EventSoundRule customRule = custom("Drop");
        customRule.soundFile = "drop.wav";
        EventSoundStore.save(customRule);

        EventSoundStore.resetForTests();
        EventSoundStore.load();

        assertEquals(2, EventSoundStore.rules().size());
        EventSoundRule storedFrost = EventSoundStore.findSmithing("Frostgeschmiedet").orElseThrow();
        assertEquals("neu.ogg", storedFrost.soundFile);
        assertEquals(0.4f, storedFrost.volume);
        assertTrue(EventSoundStore.rules().stream().anyMatch(rule -> rule.kind == EventSoundKind.CUSTOM && "Drop".equals(rule.pattern)));
    }

    private static EventSoundRule custom(String pattern) {
        EventSoundRule rule = new EventSoundRule();
        rule.kind = EventSoundKind.CUSTOM;
        rule.pattern = pattern;
        return rule;
    }
}
