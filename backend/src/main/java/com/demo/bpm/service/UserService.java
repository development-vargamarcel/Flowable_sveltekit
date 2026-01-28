package com.demo.bpm.service;

import com.demo.bpm.dto.UpdateProfileRequest;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IdentityService identityService;

    public UserDTO getUserInfo(UserDetails userDetails) {
        User flowableUser = identityService.createUserQuery()
                .userId(userDetails.getUsername())
                .singleResult();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        String displayName = flowableUser != null
                ? flowableUser.getFirstName() + " " + flowableUser.getLastName()
                : userDetails.getUsername();

        String email = flowableUser != null ? flowableUser.getEmail() : null;
        String firstName = flowableUser != null ? flowableUser.getFirstName() : null;
        String lastName = flowableUser != null ? flowableUser.getLastName() : null;

        return UserDTO.builder()
                .username(userDetails.getUsername())
                .firstName(firstName)
                .lastName(lastName)
                .displayName(displayName)
                .email(email)
                .roles(roles)
                .build();
    }

    public List<UserDTO> getAllUsers() {
        return identityService.createUserQuery().list().stream()
                .map(user -> UserDTO.builder()
                        .username(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .displayName(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(String userId) {
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return UserDTO.builder()
                .username(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .build();
    }

    public UserDTO updateProfile(String userId, UpdateProfileRequest request) {
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        identityService.saveUser(user);

        List<String> roles = identityService.createGroupQuery().groupMember(userId).list().stream()
                .map(Group::getId)
                .collect(Collectors.toList());

        return UserDTO.builder()
                .username(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
