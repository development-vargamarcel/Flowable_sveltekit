package com.demo.bpm.controller;

import com.demo.bpm.entity.AppRole;
import com.demo.bpm.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final AppRoleRepository appRoleRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AppRole>> getAllRoles() {
        return ResponseEntity.ok(appRoleRepository.findAll());
    }
}
