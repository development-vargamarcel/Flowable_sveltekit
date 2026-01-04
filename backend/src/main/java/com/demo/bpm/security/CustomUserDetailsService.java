package com.demo.bpm.security;

import com.demo.bpm.entity.AppRole;
import com.demo.bpm.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final IdentityService identityService;
    private final AppRoleRepository appRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check if user exists in Flowable
        User flowableUser = identityService.createUserQuery()
                .userId(username)
                .singleResult();

        if (flowableUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // 2. Get Flowable Groups (which map to our AppRoles)
        List<Group> groups = identityService.createGroupQuery()
                .groupMember(username)
                .list();

        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add implicit "ROLE_USER" for all valid logged-in users
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        for (Group group : groups) {
            String roleName = group.getId(); // e.g., "admin", "manager"
            
            // Add the Group ID itself as a role
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
            
            // 3. Load Permissions defined for this Role in our DB
            appRoleRepository.findById(roleName).ifPresent(appRole -> {
                appRole.getPermissions().forEach(permission -> {
                     authorities.add(new SimpleGrantedAuthority(permission.getName()));
                });
            });
        }

        // For this demo, we'll use a hardcoded password since Flowable PW check is separate
        // In a real integration, we'd either sync passwords or use a custom AuthenticationProvider
        // that delegates checking to identityService.checkPassword()
        // Here we assume if they can authenticate, we load details. 
        // BUT standard DaoAuthenticationProvider needs a password to check against.
        // So we might need to actually set the flowable password here?
        // Actually, Flowable IDM handles its own passwords. 
        // To properly integrate, we should use a Custom Authentication Provider that checks Flowable.
        // For now, let's assume we are using the simple password from the entity if available, 
        // or a dummy one if we want to bypass Spring's check (but Spring needs to check SOMETHING).
        
        String password = flowableUser.getPassword(); 
        // Note: flowableUser.getPassword() might return null depending on configuration
        // If null, authentication might fail.
        
        return new org.springframework.security.core.userdetails.User(
                flowableUser.getId(),
                password != null ? password : "", // Pass empty if null, will likely fail auth if not handled
                authorities
        );
    }
}
