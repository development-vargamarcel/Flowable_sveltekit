package com.demo.bpm.util;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VariableStorageUtilTest {

    @Test
    void filterSystemVariables_shouldOnlyIncludePrefixUnderscore() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("_system1", "val1");
        vars.put("business1", "val2");
        vars.put("_system2", 100);

        Map<String, Object> result = VariableStorageUtil.filterSystemVariables(vars);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("_system1"));
        assertTrue(result.containsKey("_system2"));
        assertFalse(result.containsKey("business1"));
    }

    @Test
    void filterBusinessVariables_shouldOnlyIncludeNonPrefixUnderscore() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("_system1", "val1");
        vars.put("business1", "val2");
        vars.put("business2", true);

        Map<String, Object> result = VariableStorageUtil.filterBusinessVariables(vars);

        assertEquals(2, result.size());
        assertFalse(result.containsKey("_system1"));
        assertTrue(result.containsKey("business1"));
        assertTrue(result.containsKey("business2"));
    }

    @Test
    void isSystemVariable_shouldIdentifyCorrectly() {
        assertTrue(VariableStorageUtil.isSystemVariable("_test"));
        assertFalse(VariableStorageUtil.isSystemVariable("test"));
        assertFalse(VariableStorageUtil.isSystemVariable(null));
        assertFalse(VariableStorageUtil.isSystemVariable(""));
    }

    @Test
    void isBusinessVariable_shouldIdentifyCorrectly() {
        assertFalse(VariableStorageUtil.isBusinessVariable("_test"));
        assertTrue(VariableStorageUtil.isBusinessVariable("test"));
        assertFalse(VariableStorageUtil.isBusinessVariable(null));
        assertTrue(VariableStorageUtil.isBusinessVariable(""));
    }
}
