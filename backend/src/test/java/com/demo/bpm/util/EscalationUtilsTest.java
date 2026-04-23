package com.demo.bpm.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EscalationUtilsTest {

    @Test
    void getNextLevels_shouldReturnCorrectHierarchy() {
        assertEquals(List.of("MANAGER"), EscalationUtils.getNextLevels("SUPERVISOR"));
        assertEquals(List.of("DIRECTOR"), EscalationUtils.getNextLevels("MANAGER"));
        assertEquals(List.of("EXECUTIVE"), EscalationUtils.getNextLevels("DIRECTOR"));
        assertTrue(EscalationUtils.getNextLevels("EXECUTIVE").isEmpty());
    }

    @Test
    void getPreviousLevels_shouldReturnCorrectHierarchy() {
        assertEquals(List.of("DIRECTOR"), EscalationUtils.getPreviousLevels("EXECUTIVE"));
        assertEquals(List.of("MANAGER"), EscalationUtils.getPreviousLevels("DIRECTOR"));
        assertEquals(List.of("SUPERVISOR"), EscalationUtils.getPreviousLevels("MANAGER"));
        assertTrue(EscalationUtils.getPreviousLevels("SUPERVISOR").isEmpty());
    }

    @Test
    void canEscalate_shouldWorkCorrectly() {
        assertTrue(EscalationUtils.canEscalate("SUPERVISOR", "MANAGER"));
        assertTrue(EscalationUtils.canEscalate("SUPERVISOR", null)); // Default next level
        assertFalse(EscalationUtils.canEscalate("EXECUTIVE", null));
        assertFalse(EscalationUtils.canEscalate("SUPERVISOR", "DIRECTOR")); // Direct jump not allowed by hierarchy map if it only contains direct next
    }

    @Test
    void canDeEscalate_shouldWorkCorrectly() {
        assertTrue(EscalationUtils.canDeEscalate("EXECUTIVE", "DIRECTOR"));
        assertTrue(EscalationUtils.canDeEscalate("EXECUTIVE", null));
        assertFalse(EscalationUtils.canDeEscalate("SUPERVISOR", null));
    }
}
