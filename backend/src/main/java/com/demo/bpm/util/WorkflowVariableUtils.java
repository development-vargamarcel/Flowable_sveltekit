package com.demo.bpm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class WorkflowVariableUtils {

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getListVariable(Map<String, Object> variables, String variableName, ObjectMapper objectMapper) {
        Object historyObj = variables.get(variableName);
        if (historyObj == null) {
            return new ArrayList<>();
        }
        if (historyObj instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) historyObj);
        }
        try {
            return objectMapper.readValue((String) historyObj,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public static String serializeList(List<?> list, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public static String getStringVariable(Map<String, Object> variables, String variableName, String defaultValue) {
        return (String) variables.getOrDefault(variableName, defaultValue);
    }

    public static int getIntVariable(Map<String, Object> variables, String variableName, int defaultValue) {
        return ((Number) variables.getOrDefault(variableName, defaultValue)).intValue();
    }
}
