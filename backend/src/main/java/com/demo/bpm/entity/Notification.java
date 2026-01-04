package com.demo.bpm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    private String link; // Optional link to task or process
    
    private boolean read = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        TASK_ASSIGNED,
        TASK_DUE_SOON,
        TASK_OVERDUE,
        PROCESS_COMPLETED,
        PROCESS_REJECTED,
        MENTION,
        SYSTEM,
        SLA_WARNING,
        SLA_BREACH
    }
}
