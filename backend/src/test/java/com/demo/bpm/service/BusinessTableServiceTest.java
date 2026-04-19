package com.demo.bpm.service;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.entity.ColumnMapping;
import com.demo.bpm.entity.Document;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.repository.DocumentRepository;
import com.demo.bpm.repository.GridRowRepository;
import com.demo.bpm.repository.ProcessConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessTableServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private GridRowRepository gridRowRepository;

    @Mock
    private ProcessConfigRepository processConfigRepository;

    @Mock
    private ColumnMappingService columnMappingService;

    @InjectMocks
    private BusinessTableService businessTableService;

    @Test
    void getDocument_whenExists_shouldReturnDTO() {
        Document document = new Document();
        document.setId(1L);
        document.setProcessInstanceId("pi1");
        document.setProcessDefinitionKey("proc1");
        document.setType("main");

        when(documentRepository.findByProcessInstanceIdAndType("pi1", "main")).thenReturn(Optional.of(document));
        when(columnMappingService.getDocumentMappings("proc1", "main")).thenReturn(Collections.emptyMap());
        when(gridRowRepository.findByDocumentIdOrderByGridNameAscRowIndexAsc(1L)).thenReturn(Collections.emptyList());

        Optional<DocumentDTO> result = businessTableService.getDocument("pi1", "main");

        assertTrue(result.isPresent());
        assertEquals("pi1", result.get().getProcessInstanceId());
    }

    @Test
    void saveDocument_shouldPersistData() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fieldName", "value");

        ColumnMapping mapping = ColumnMapping.builder()
                .fieldName("fieldName")
                .columnName("varchar_1")
                .fieldType(ColumnMapping.FieldType.VARCHAR)
                .build();

        when(documentRepository.findByProcessInstanceIdAndType("pi1", "main")).thenReturn(Optional.empty());
        when(columnMappingService.determineFieldType("value")).thenReturn(ColumnMapping.FieldType.VARCHAR);
        when(columnMappingService.getOrCreateDocumentMapping("proc1", "main", "fieldName", ColumnMapping.FieldType.VARCHAR))
                .thenReturn(mapping);
        when(columnMappingService.convertValueForStorage("value", ColumnMapping.FieldType.VARCHAR)).thenReturn("value");
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        businessTableService.saveDocument("pi1", "bk1", "proc1", "Proc Name", "main", variables, "user1");

        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void getOrCreateProcessConfig_shouldReturnConfig() {
        ProcessConfig config = ProcessConfig.builder()
                .processDefinitionKey("proc1")
                .build();

        when(processConfigRepository.findByProcessDefinitionKey("proc1")).thenReturn(Optional.of(config));

        ProcessConfig result = businessTableService.getOrCreateProcessConfig("proc1");

        assertNotNull(result);
        assertEquals("proc1", result.getProcessDefinitionKey());
    }

    @Test
    void updateProcessConfig_shouldSave() {
        ProcessConfig config = ProcessConfig.builder()
                .processDefinitionKey("proc1")
                .persistOnTaskComplete(false)
                .build();

        when(processConfigRepository.findByProcessDefinitionKey("proc1")).thenReturn(Optional.of(config));
        when(processConfigRepository.save(any(ProcessConfig.class))).thenReturn(config);

        businessTableService.updateProcessConfig("proc1", true, true);

        verify(processConfigRepository).save(config);
        assertTrue(config.getPersistOnTaskComplete());
    }
}
