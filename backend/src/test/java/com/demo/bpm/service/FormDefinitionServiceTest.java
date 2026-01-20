package com.demo.bpm.service;

import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.dto.ProcessFormConfigDTO;
import com.demo.bpm.repository.DocumentTypeRepository;
import com.demo.bpm.repository.ProcessConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormDefinitionServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private TaskService taskService;

    @Mock
    private ProcessConfigRepository processConfigRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;

    @InjectMocks
    private FormDefinitionService formDefinitionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        formDefinitionService = new FormDefinitionService(repositoryService, taskService, objectMapper, processConfigRepository, documentTypeRepository);
    }

    @Test
    void getProcessFormConfig_shouldReturnEmptyConfig_whenExceptionOccurs() {
        when(repositoryService.getProcessModel(anyString())).thenThrow(new RuntimeException("Error"));

        ProcessFormConfigDTO config = formDefinitionService.getProcessFormConfig("procDefId");

        assertNotNull(config);
        assertEquals("procDefId", config.getProcessDefinitionId());
        assertTrue(config.getFieldLibrary().getFields().isEmpty());
    }

    @Test
    void getProcessFormConfig_shouldParseFieldLibrary() {
        String bpmnXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" \n" +
                "  xmlns:flowable=\"http://flowable.org/bpmn\" \n" +
                "  targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"testProcess\" name=\"Test Process\" isExecutable=\"true\" \n" +
                "    flowable:fieldLibrary='{\"fields\":[{\"id\":\"f1\",\"name\":\"field1\"}],\"grids\":[]}' \n" +
                "    flowable:conditionRules='[{\"id\":\"r1\",\"condition\":\"true\",\"effect\":\"hidden\"}]'>\n" +
                "    <startEvent id=\"start\"/>\n" +
                "  </process>\n" +
                "</definitions>";

        when(repositoryService.getProcessModel("procDefId")).thenReturn(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(null);

        ProcessFormConfigDTO config = formDefinitionService.getProcessFormConfig("procDefId");

        assertNotNull(config);
        assertEquals(1, config.getFieldLibrary().getFields().size());
        assertEquals("f1", config.getFieldLibrary().getFields().get(0).getId());
        assertEquals(1, config.getGlobalConditions().size());
        assertEquals("r1", config.getGlobalConditions().get(0).getId());
    }

    @Test
    void getFormDefinitionForElement_shouldParseFormFields() {
        String bpmnXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" \n" +
                "  xmlns:flowable=\"http://flowable.org/bpmn\" \n" +
                "  targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"testProcess\">\n" +
                "    <startEvent id=\"start\" name=\"Start\" \n" +
                "      flowable:formFields='[{\"id\":\"f1\",\"name\":\"field1\"}]'/>\n" +
                "  </process>\n" +
                "</definitions>";

        when(repositoryService.getProcessModel("procDefId")).thenReturn(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

        FormDefinitionDTO form = formDefinitionService.getFormDefinitionForElement("procDefId", "start");

        assertNotNull(form);
        assertEquals("start", form.getElementId());
        assertEquals(1, form.getFields().size());
        assertEquals("f1", form.getFields().get(0).getId());
    }

    @Test
    void getFormDefinitionForElement_shouldHandleMissingElement() {
        String bpmnXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n" +
                "  <process id=\"testProcess\">\n" +
                "    <startEvent id=\"start\"/>\n" +
                "  </process>\n" +
                "</definitions>";

        when(repositoryService.getProcessModel("procDefId")).thenReturn(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

        FormDefinitionDTO form = formDefinitionService.getFormDefinitionForElement("procDefId", "missing");

        assertNotNull(form);
        assertEquals("missing", form.getElementId());
        assertTrue(form.getFields().isEmpty());
    }
}
