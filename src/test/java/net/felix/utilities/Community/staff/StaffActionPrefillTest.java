package net.felix.utilities.Community.staff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaffActionPrefillTest {
    @Test
    void deleteRequestUsesOnlyTheClickedLineId() {
        assertEquals("11", StaffActionScreen.messageText(11L));
        assertEquals("", StaffActionScreen.messageText(null));
        assertEquals(11, StaffActionScreen.deleteRequest(StaffActionScreen.messageText(11L), "Spam").get("messageId").getAsInt());
        assertEquals(10, StaffActionScreen.deleteRequest(StaffActionScreen.messageText(10L), "Spam").get("messageId").getAsInt());
    }
}
