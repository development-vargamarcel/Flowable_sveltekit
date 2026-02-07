package com.demo.bpm.controller;

import com.demo.bpm.entity.DocumentTypeDefinition;
import com.demo.bpm.service.DocumentTypeService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-types")
@RequiredArgsConstructor
@Validated
public class DocumentTypeController {

    private final DocumentTypeService service;

    @GetMapping
    public ResponseEntity<List<DocumentTypeDefinition>> getAllDocumentTypes() {
        return ResponseEntity.ok(service.getAllDocumentTypes());
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getDocumentType(@PathVariable String key) {
        return service.getDocumentTypeByKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDocumentType(@Validated({DocumentTypeDefinition.ValidationGroups.Create.class, Default.class}) @RequestBody DocumentTypeDefinition documentType) {
        try {
            return ResponseEntity.ok(service.createDocumentType(documentType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> updateDocumentType(@PathVariable String key, @Validated({DocumentTypeDefinition.ValidationGroups.Update.class, Default.class}) @RequestBody DocumentTypeDefinition documentType) {
        try {
            documentType.setKey(key); // Ensure body aligns with path to avoid accidental key changes
            return ResponseEntity.ok(service.updateDocumentType(key, documentType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteDocumentType(@PathVariable String key) {
        try {
            service.deleteDocumentType(key);
            return ResponseEntity.ok(Map.of("message", "Document type deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
