package com.demo.bpm.controller;

import com.demo.bpm.dto.UpdateProfileRequest;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void getUserById_whenUserExists_shouldReturnUser() throws Exception {
        UserDTO user = UserDTO.builder()
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .build();

        when(userService.getUserById("testuser")).thenReturn(user);

        mockMvc.perform(get("/api/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    void getUserById_whenUserDoesNotExist_shouldReturnNotFound() throws Exception {
        when(userService.getUserById("nonexistent")).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_shouldUpdateAndReturnUser() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        UserDTO updatedUser = UserDTO.builder()
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        when(userService.updateProfile(eq("testuser"), any(UpdateProfileRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }
}
