package com.demo.bpm.service;

import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.dto.FormFieldDTO;
import com.demo.bpm.dto.FormGridDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormDefinitionService {

    private final RepositoryService repositoryService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    /**
     * Get form definition for a specific task
     */
    public FormDefinitionDTO getFormDefinitionForTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        String processDefinitionId = task.getProcessDefinitionId();
        String taskDefinitionKey = task.getTaskDefinitionKey();

        return getFormDefinitionForElement(processDefinitionId, taskDefinitionKey);
    }

    /**
     * Get form definition for a start event of a process
     */
    public FormDefinitionDTO getStartFormDefinition(String processDefinitionId) {
        return getFormDefinitionForElement(processDefinitionId, null);
    }

    /**
     * Get form definition for a specific element in a process
     */
    public FormDefinitionDTO getFormDefinitionForElement(String processDefinitionId, String elementId) {
        try {
            // Get the BPMN XML
            InputStream bpmnStream = repositoryService.getProcessModel(processDefinitionId);
            String bpmnXml = new String(bpmnStream.readAllBytes(), StandardCharsets.UTF_8);

            // Parse BPMN XML to find form definitions
            return parseFormDefinitionFromXml(bpmnXml, elementId);
        } catch (Exception e) {
            log.error("Error extracting form definition for process {} element {}: {}",
                    processDefinitionId, elementId, e.getMessage(), e);
            throw new RuntimeException("Failed to extract form definition: " + e.getMessage(), e);
        }
    }

    /**
     * Get all form definitions for a process (start event + all user tasks)
     */
    public Map<String, FormDefinitionDTO> getAllFormDefinitions(String processDefinitionId) {
        Map<String, FormDefinitionDTO> formDefinitions = new HashMap<>();

        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            InputStream bpmnStream = repositoryService.getProcessModel(processDefinitionId);
            String bpmnXml = new String(bpmnStream.readAllBytes(), StandardCharsets.UTF_8);

            // Get start event form
            FormDefinitionDTO startForm = parseFormDefinitionFromXml(bpmnXml, null);
            if (startForm != null && hasFormContent(startForm)) {
                formDefinitions.put("startEvent", startForm);
            }

            // Get all user task forms
            for (org.flowable.bpmn.model.Process process : bpmnModel.getProcesses()) {
                for (FlowElement element : process.getFlowElements()) {
                    if (element instanceof UserTask) {
                        FormDefinitionDTO taskForm = parseFormDefinitionFromXml(bpmnXml, element.getId());
                        if (taskForm != null && hasFormContent(taskForm)) {
                            formDefinitions.put(element.getId(), taskForm);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting all form definitions for process {}: {}",
                    processDefinitionId, e.getMessage(), e);
        }

        return formDefinitions;
    }

    private boolean hasFormContent(FormDefinitionDTO form) {
        return (form.getFields() != null && !form.getFields().isEmpty()) ||
               (form.getGrids() != null && !form.getGrids().isEmpty());
    }

    private FormDefinitionDTO parseFormDefinitionFromXml(String bpmnXml, String elementId) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            Element targetElement = null;
            String elementName = null;
            String elementType = null;

            if (elementId == null) {
                // Find start event
                NodeList startEvents = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "startEvent");
                if (startEvents.getLength() > 0) {
                    targetElement = (Element) startEvents.item(0);
                    elementType = "bpmn:StartEvent";
                }
            } else {
                // Find element by ID - search both userTask and startEvent
                NodeList userTasks = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "userTask");
                for (int i = 0; i < userTasks.getLength(); i++) {
                    Element el = (Element) userTasks.item(i);
                    if (elementId.equals(el.getAttribute("id"))) {
                        targetElement = el;
                        elementType = "bpmn:UserTask";
                        break;
                    }
                }

                if (targetElement == null) {
                    NodeList startEvents = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "startEvent");
                    for (int i = 0; i < startEvents.getLength(); i++) {
                        Element el = (Element) startEvents.item(i);
                        if (elementId.equals(el.getAttribute("id"))) {
                            targetElement = el;
                            elementType = "bpmn:StartEvent";
                            break;
                        }
                    }
                }
            }

            if (targetElement == null) {
                return FormDefinitionDTO.builder()
                        .elementId(elementId)
                        .fields(new ArrayList<>())
                        .grids(new ArrayList<>())
                        .build();
            }

            elementName = targetElement.getAttribute("name");

            // Extract form fields
            String formFieldsJson = targetElement.getAttributeNS("http://flowable.org/bpmn", "formFields");
            if (formFieldsJson.isEmpty()) {
                formFieldsJson = targetElement.getAttribute("flowable:formFields");
            }

            // Extract form grids
            String formGridsJson = targetElement.getAttributeNS("http://flowable.org/bpmn", "formGrids");
            if (formGridsJson.isEmpty()) {
                formGridsJson = targetElement.getAttribute("flowable:formGrids");
            }

            List<FormFieldDTO> fields = new ArrayList<>();
            List<FormGridDTO> grids = new ArrayList<>();
            FormDefinitionDTO.GridConfigDTO gridConfig = null;

            if (formFieldsJson != null && !formFieldsJson.isEmpty()) {
                List<Map<String, Object>> fieldsList = objectMapper.readValue(
                        formFieldsJson, new TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> fieldMap : fieldsList) {
                    // Extract grid config from first field if present
                    if (fieldMap.containsKey("_gridConfig")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> gc = (Map<String, Object>) fieldMap.get("_gridConfig");
                        gridConfig = FormDefinitionDTO.GridConfigDTO.builder()
                                .columns(getIntValue(gc, "columns", 2))
                                .gap(getIntValue(gc, "gap", 16))
                                .build();
                    }

                    FormFieldDTO field = mapToFormField(fieldMap);
                    fields.add(field);
                }
            }

            if (formGridsJson != null && !formGridsJson.isEmpty()) {
                List<Map<String, Object>> gridsList = objectMapper.readValue(
                        formGridsJson, new TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> gridMap : gridsList) {
                    FormGridDTO grid = mapToFormGrid(gridMap);
                    grids.add(grid);
                }
            }

            return FormDefinitionDTO.builder()
                    .elementId(targetElement.getAttribute("id"))
                    .elementName(elementName)
                    .elementType(elementType)
                    .fields(fields)
                    .grids(grids)
                    .gridConfig(gridConfig != null ? gridConfig :
                            FormDefinitionDTO.GridConfigDTO.builder().columns(2).gap(16).build())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing form definition from XML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse form definition: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private FormFieldDTO mapToFormField(Map<String, Object> map) {
        return FormFieldDTO.builder()
                .id(getStringValue(map, "id"))
                .name(getStringValue(map, "name"))
                .label(getStringValue(map, "label"))
                .type(getStringValue(map, "type", "text"))
                .required(getBooleanValue(map, "required"))
                .validation((Map<String, Object>) map.get("validation"))
                .options((List<Map<String, String>>) map.get("options"))
                .placeholder(getStringValue(map, "placeholder"))
                .defaultValue(getStringValue(map, "defaultValue"))
                .defaultExpression(getStringValue(map, "defaultExpression"))
                .tooltip(getStringValue(map, "tooltip"))
                .readonly(getBooleanValue(map, "readonly"))
                .hidden(getBooleanValue(map, "hidden"))
                .hiddenExpression(getStringValue(map, "hiddenExpression"))
                .readonlyExpression(getStringValue(map, "readonlyExpression"))
                .requiredExpression(getStringValue(map, "requiredExpression"))
                .gridColumn(getIntValue(map, "gridColumn", 1))
                .gridRow(getIntValue(map, "gridRow", 1))
                .gridWidth(getIntValue(map, "gridWidth", 1))
                .cssClass(getStringValue(map, "cssClass"))
                .onChange(getStringValue(map, "onChange"))
                .onBlur(getStringValue(map, "onBlur"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private FormGridDTO mapToFormGrid(Map<String, Object> map) {
        List<FormGridDTO.GridColumnDTO> columns = new ArrayList<>();

        List<Map<String, Object>> columnsData = (List<Map<String, Object>>) map.get("columns");
        if (columnsData != null) {
            for (Map<String, Object> colMap : columnsData) {
                FormGridDTO.GridColumnDTO column = FormGridDTO.GridColumnDTO.builder()
                        .id(getStringValue(colMap, "id"))
                        .name(getStringValue(colMap, "name"))
                        .label(getStringValue(colMap, "label"))
                        .type(getStringValue(colMap, "type", "text"))
                        .required(getBooleanValue(colMap, "required"))
                        .placeholder(getStringValue(colMap, "placeholder"))
                        .options((List<String>) colMap.get("options"))
                        .min(getDoubleValue(colMap, "min"))
                        .max(getDoubleValue(colMap, "max"))
                        .step(getDoubleValue(colMap, "step"))
                        .validation((Map<String, Object>) colMap.get("validation"))
                        .build();
                columns.add(column);
            }
        }

        return FormGridDTO.builder()
                .id(getStringValue(map, "id"))
                .name(getStringValue(map, "name"))
                .label(getStringValue(map, "label"))
                .description(getStringValue(map, "description"))
                .minRows(getIntValue(map, "minRows", 0))
                .maxRows(getIntValue(map, "maxRows", 0))
                .columns(columns)
                .gridColumn(getIntValue(map, "gridColumn", 1))
                .gridRow(getIntValue(map, "gridRow", 1))
                .gridWidth(getIntValue(map, "gridWidth", 1))
                .cssClass(getStringValue(map, "cssClass"))
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        return getStringValue(map, key, "");
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
