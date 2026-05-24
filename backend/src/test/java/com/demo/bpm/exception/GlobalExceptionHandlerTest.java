package com.demo.bpm.exception;

import com.demo.bpm.controller.UserController;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(UserController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void whenResourceNotFoundExceptionIsThrown_thenReturns404() throws Exception {
        when(userService.getUserById("notfound")).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser
    void whenValidationFails_thenReturns400WithDetails() throws Exception {
        // We need a request that triggers validation.
        // /api/users/profile expects @Valid UpdateProfileRequest which has @NotBlank
        String invalidJson = "{\"firstName\": \"\", \"lastName\": \"\", \"email\": \"\"}";

        mockMvc.perform(put("/api/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
