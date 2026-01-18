package com.demo.bpm.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EscalationUtils {

    private static final Map<String, String[]> ESCALATION_HIERARCHY = Map.of(
            "SUPERVISOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"EXECUTIVE"},
            "EXECUTIVE", new String[]{}
    );

    private static final Map<String, String[]> DE_ESCALATION_HIERARCHY = Map.of(
            "EXECUTIVE", new String[]{"DIRECTOR"},
            "DIRECTOR", new String[]{"MANAGER"},
            "MANAGER", new String[]{"SUPERVISOR"},
            "SUPERVISOR", new String[]{}
    );

    private EscalationUtils() {
        // Private constructor to hide the implicit public one
    }

    public static List<String> getNextLevels(String currentLevel) {
        return Arrays.asList(ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
    }

    public static List<String> getPreviousLevels(String currentLevel) {
        return Arrays.asList(DE_ESCALATION_HIERARCHY.getOrDefault(currentLevel, new String[]{}));
    }

    public static boolean canEscalate(String currentLevel, String targetLevel) {
        List<String> nextLevels = getNextLevels(currentLevel);
        return !nextLevels.isEmpty() && (targetLevel == null || nextLevels.contains(targetLevel));
    }

    public static boolean canDeEscalate(String currentLevel, String targetLevel) {
        List<String> prevLevels = getPreviousLevels(currentLevel);
        return !prevLevels.isEmpty() && (targetLevel == null || prevLevels.contains(targetLevel));
    }

    public static String getDefaultNextLevel(String currentLevel) {
        List<String> nextLevels = getNextLevels(currentLevel);
        return nextLevels.isEmpty() ? null : nextLevels.get(0);
    }

    public static String getDefaultPreviousLevel(String currentLevel) {
        List<String> prevLevels = getPreviousLevels(currentLevel);
        return prevLevels.isEmpty() ? null : prevLevels.get(0);
    }
}
