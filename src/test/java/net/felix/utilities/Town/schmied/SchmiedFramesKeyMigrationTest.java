package net.felix.utilities.Town.schmied;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchmiedFramesKeyMigrationTest {

    @Test
    void savedF7IsUnboundOnce() {
        assertEquals(SchmiedFramesKeyMigration.Action.UNBIND, SchmiedFramesKeyMigration.decide(false, true));
    }

    @Test
    void anotherKeyIsKept() {
        assertEquals(SchmiedFramesKeyMigration.Action.MARK, SchmiedFramesKeyMigration.decide(false, false));
    }

    @Test
    void laterChoiceIsLeftAlone() {
        assertEquals(SchmiedFramesKeyMigration.Action.NONE, SchmiedFramesKeyMigration.decide(true, true));
    }
}
