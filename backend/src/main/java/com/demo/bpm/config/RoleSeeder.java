package com.demo.bpm.config;

import com.demo.bpm.entity.AppPermission;
import com.demo.bpm.entity.AppRole;
import com.demo.bpm.repository.AppPermissionRepository;
import com.demo.bpm.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RoleSeeder {

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final IdentityService identityService;

    @Bean
    public CommandLineRunner seedSecurityData() {
        return args -> {
            seedPermissions();
            seedRolesAndGroups();
            seedUsers();
        };
    }

    private void seedPermissions() {
        if (appPermissionRepository.count() == 0) {
            log.info("Seeding App Permissions...");
            
            createPermission("PROCESS_DEF_CREATE", "Can deploy and manage process definitions");
            createPermission("PROCESS_DEF_VIEW", "Can view process definitions");
            createPermission("TASK_ACCESS_ALL", "Can view and manage all tasks (admin)");
            createPermission("ANALYTICS_VIEW", "Can view dashboard analytics");
            createPermission("USER_MANAGE", "Can manage users and roles");
            createPermission("APP_ADMIN", "Full system access");
        }
    }

    private void createPermission(String name, String desc) {
        if (!appPermissionRepository.existsById(name)) {
            appPermissionRepository.save(new AppPermission(name, desc));
        }
    }

    private void seedRolesAndGroups() {
        log.info("Seeding Roles and Flowable Groups...");

        // ADMIN Role
        createRole("ADMIN", "System Administrator", 
            "APP_ADMIN", "PROCESS_DEF_CREATE", "TASK_ACCESS_ALL", "ANALYTICS_VIEW", "USER_MANAGE");

        // MANAGER Role
        createRole("MANAGER", "Department Manager", 
            "PROCESS_DEF_VIEW", "ANALYTICS_VIEW");

        // USER Role
        createRole("USER", "Standard User", 
            "PROCESS_DEF_VIEW");
    }

    private void createRole(String name, String desc, String... permissionNames) {
        // 1. Create AppRole in our DB
        if (!appRoleRepository.existsById(name)) {
            AppRole role = new AppRole();
            role.setName(name);
            role.setDescription(desc);
            
            Set<AppPermission> perms = new HashSet<>();
            for (String pName : permissionNames) {
                appPermissionRepository.findById(pName).ifPresent(perms::add);
            }
            role.setPermissions(perms);
            appRoleRepository.save(role);
            log.info("Created AppRole: {}", name);
        }

        // 2. Create corresponding Group in Flowable
        if (identityService.createGroupQuery().groupId(name).count() == 0) {
            Group group = identityService.newGroup(name);
            group.setName(desc);
            group.setType("assignment");
            identityService.saveGroup(group);
            log.info("Created Flowable Group: {}", name);
        }
    }

    private void seedUsers() {
        if (identityService.createUserQuery().count() == 0) {
            log.info("Seeding Initial Users...");

            // Admin User
            createUser("admin", "Admin", "User", "admin", "ADMIN", "USER", "MANAGER");
            
            // Manager
            createUser("manager", "Manager", "One", "manager", "MANAGER", "USER");
            
            // Standard User
            createUser("user", "Standard", "User", "user", "USER");
        }
    }

    private void createUser(String username, String firstName, String lastName, String password, String... groups) {
        if (identityService.createUserQuery().userId(username).count() == 0) {
            User user = identityService.newUser(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(username + "@flowable.local");
            user.setPassword(password); // Flowable handles hashing if configured, but by default basic
            identityService.saveUser(user);

            for (String groupId : groups) {
                identityService.createMembership(username, groupId);
            }
            log.info("Created User: {}", username);
        }
    }
}
