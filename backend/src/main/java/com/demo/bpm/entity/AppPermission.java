package com.demo.bpm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppPermission {
    @Id
    private String name; // e.g., PROCESS_DEF_CREATE
    
    private String description;
}
