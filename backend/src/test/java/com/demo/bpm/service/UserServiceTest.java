package com.demo.bpm.service;

import com.demo.bpm.dto.RegisterRequest;
import com.demo.bpm.dto.UpdateProfileRequest;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private UserService userService;

    @Mock
    private UserQuery userQuery;

    @Mock
    private GroupQuery groupQuery;

    @Mock
    private User flowableUser;

    @Mock
    private Group group;

    @BeforeEach
    void setUp() {
        lenient().when(identityService.createUserQuery()).thenReturn(userQuery);
        lenient().when(identityService.createGroupQuery()).thenReturn(groupQuery);
    }

    @Test
    void getUserInfo_shouldReturnUserDTO() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("password")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        when(userQuery.userId("testuser")).thenReturn(userQuery);
        when(userQuery.singleResult()).thenReturn(flowableUser);
        when(flowableUser.getFirstName()).thenReturn("Test");
        when(flowableUser.getLastName()).thenReturn("User");
        when(flowableUser.getEmail()).thenReturn("test@example.com");

        UserDTO result = userService.getUserInfo(userDetails);

        assertEquals("testuser", result.getUsername());
        assertEquals("Test User", result.getDisplayName());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.getRoles().contains("USER"));
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userQuery.list()).thenReturn(Collections.singletonList(flowableUser));
        when(flowableUser.getId()).thenReturn("user1");
        when(flowableUser.getFirstName()).thenReturn("John");
        when(flowableUser.getLastName()).thenReturn("Doe");

        List<UserDTO> result = userService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUsername());
    }

    @Test
    void getUserById_whenUserExists_shouldReturnDTO() {
        when(userQuery.userId("user1")).thenReturn(userQuery);
        when(userQuery.singleResult()).thenReturn(flowableUser);
        when(flowableUser.getId()).thenReturn("user1");
        when(flowableUser.getFirstName()).thenReturn("John");
        when(flowableUser.getLastName()).thenReturn("Doe");

        UserDTO result = userService.getUserById("user1");

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldThrowException() {
        when(userQuery.userId("unknown")).thenReturn(userQuery);
        when(userQuery.singleResult()).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("unknown"));
    }

    @Test
    void updateProfile_shouldSaveAndReturnDTO() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setEmail("jane@example.com");

        when(userQuery.userId("user1")).thenReturn(userQuery);
        when(userQuery.singleResult()).thenReturn(flowableUser);
        when(flowableUser.getId()).thenReturn("user1");
        when(flowableUser.getFirstName()).thenReturn("Jane");
        when(flowableUser.getLastName()).thenReturn("Smith");

        when(groupQuery.groupMember("user1")).thenReturn(groupQuery);
        when(groupQuery.list()).thenReturn(Collections.emptyList());

        UserDTO result = userService.updateProfile("user1", request);

        verify(flowableUser).setFirstName("Jane");
        verify(flowableUser).setLastName("Smith");
        verify(flowableUser).setEmail("jane@example.com");
        verify(identityService).saveUser(flowableUser);
        assertEquals("Jane Smith", result.getDisplayName());
    }

    @Test
    void registerUser_whenNew_shouldCreateUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("pass");
        request.setEmail("new@example.com");

        when(userQuery.userId("newuser")).thenReturn(userQuery);
        when(userQuery.count()).thenReturn(0L);
        when(identityService.newUser("newuser")).thenReturn(flowableUser);

        // For the getUserById call inside registerUser
        when(userQuery.singleResult()).thenReturn(flowableUser);
        when(flowableUser.getId()).thenReturn("newuser");

        UserDTO result = userService.registerUser(request);

        verify(identityService).saveUser(flowableUser);
        assertEquals("newuser", result.getUsername());
    }

    @Test
    void registerUser_whenExists_shouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("exists");

        when(userQuery.userId("exists")).thenReturn(userQuery);
        when(userQuery.count()).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }
}
