package com.demo.bpm.service;

import com.demo.bpm.entity.ColumnMapping;
import com.demo.bpm.repository.ColumnMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColumnMappingServiceTest {

    @Mock
    private ColumnMappingRepository columnMappingRepository;

    @InjectMocks
    private ColumnMappingService columnMappingService;

    @Test
    void determineFieldType_shouldReturnCorrectTypes() {
        assertEquals(ColumnMapping.FieldType.VARCHAR, columnMappingService.determineFieldType("string"));
        assertEquals(ColumnMapping.FieldType.FLOAT, columnMappingService.determineFieldType(123.45));
        assertEquals(ColumnMapping.FieldType.FLOAT, columnMappingService.determineFieldType(true));
        assertEquals(ColumnMapping.FieldType.DATETIME, columnMappingService.determineFieldType(LocalDateTime.now()));
        assertEquals(ColumnMapping.FieldType.DATETIME, columnMappingService.determineFieldType("2024-01-01T10:00:00"));
    }

    @Test
    void getOrCreateDocumentMapping_whenExists_shouldReturnExisting() {
        ColumnMapping mapping = ColumnMapping.builder().fieldName("field").build();
        when(columnMappingRepository.findDocumentFieldMappings(
                "proc1", "main", "field")).thenReturn(Collections.singletonList(mapping));

        ColumnMapping result = columnMappingService.getOrCreateDocumentMapping("proc1", "main", "field", ColumnMapping.FieldType.VARCHAR);

        assertEquals(mapping, result);
        verify(columnMappingRepository, never()).save(any());
    }

    @Test
    void getOrCreateDocumentMapping_whenNew_shouldCreate() {
        when(columnMappingRepository.findDocumentFieldMappings(any(), any(), any())).thenReturn(Collections.emptyList());
        when(columnMappingRepository.findDocumentFieldMapping(any(), any())).thenReturn(Optional.empty());
        when(columnMappingRepository.findUsedDocumentColumnIndices(any(), any(), any())).thenReturn(Collections.emptySet());
        when(columnMappingRepository.save(any(ColumnMapping.class))).thenAnswer(i -> i.getArguments()[0]);

        ColumnMapping result = columnMappingService.getOrCreateDocumentMapping("proc1", "main", "newField", ColumnMapping.FieldType.VARCHAR);

        assertNotNull(result);
        assertEquals("newField", result.getFieldName());
        assertNotNull(result.getColumnName());
        verify(columnMappingRepository).save(any(ColumnMapping.class));
    }

    @Test
    void convertValueForStorage_shouldHandleTypes() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now, columnMappingService.convertValueForStorage(now, ColumnMapping.FieldType.DATETIME));
        assertEquals(1.0, columnMappingService.convertValueForStorage(true, ColumnMapping.FieldType.FLOAT));
        assertEquals("value", columnMappingService.convertValueForStorage("value", ColumnMapping.FieldType.VARCHAR));
    }

    @Test
    void convertValueFromStorage_shouldHandleTypes() {
        LocalDateTime now = LocalDateTime.now();
        Object result = columnMappingService.convertValueFromStorage(now, ColumnMapping.FieldType.DATETIME, null);
        assertNotNull(result);
        assertTrue(result.toString().contains("T"));

        assertEquals(true, columnMappingService.convertValueFromStorage(1.0, ColumnMapping.FieldType.FLOAT, "boolean"));
    }
}
