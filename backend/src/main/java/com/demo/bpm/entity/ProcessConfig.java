package com.demo.bpm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_definition_key", nullable = false, unique = true, length = 255)
    private String processDefinitionKey;

    @Column(name = "persist_on_task_complete")
    @Builder.Default
    private Boolean persistOnTaskComplete = true;

    @Column(name = "persist_on_process_complete")
    @Builder.Default
    private Boolean persistOnProcessComplete = true;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
