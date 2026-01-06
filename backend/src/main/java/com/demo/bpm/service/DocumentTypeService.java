package com.demo.bpm.service;

import com.demo.bpm.dto.schema.ProcessFieldLibraryDTO;
import com.demo.bpm.entity.DocumentTypeDefinition;
import com.demo.bpm.repository.DocumentTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentTypeService {

    private final DocumentTypeRepository repository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public List<DocumentTypeDefinition> getAllDocumentTypes() {
        return repository.findAll();
    }

    public Optional<DocumentTypeDefinition> getDocumentTypeByKey(String key) {
        return repository.findByKey(key);
    }

    @Transactional
    public DocumentTypeDefinition createDocumentType(DocumentTypeDefinition documentType) {
        if (repository.existsByKey(documentType.getKey())) {
            throw new IllegalArgumentException("Document type with key " + documentType.getKey() + " already exists");
        }
        validateSchema(documentType.getSchemaJson());
        return repository.save(documentType);
    }

    @Transactional
    public DocumentTypeDefinition updateDocumentType(String key, DocumentTypeDefinition updatedInfo) {
        DocumentTypeDefinition existing = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Document type with key " + key + " not found"));

        existing.setName(updatedInfo.getName());
        existing.setDescription(updatedInfo.getDescription());
        validateSchema(updatedInfo.getSchemaJson());
        existing.setSchemaJson(updatedInfo.getSchemaJson());

        return repository.save(existing);
    }

    @Transactional
    public void deleteDocumentType(String key) {
        DocumentTypeDefinition existing = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Document type with key " + key + " not found"));
        repository.delete(existing);
    }

    private void validateSchema(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return; // Allow empty schema
        }
        try {
            ProcessFieldLibraryDTO library = objectMapper.readValue(schemaJson, ProcessFieldLibraryDTO.class);
            Set<ConstraintViolation<ProcessFieldLibraryDTO>> violations = validator.validate(library);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Invalid schema: " + errors);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON schema format", e);
        }
    }
}
