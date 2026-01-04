package com.demo.bpm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppRole {
    @Id
    private String name; // e.g., MANAGER (Matches Flowable Group ID)
    
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "app_role_permissions",
        joinColumns = @JoinColumn(name = "role_name"),
        inverseJoinColumns = @JoinColumn(name = "permission_name")
    )
    private Set<AppPermission> permissions = new HashSet<>();
}
