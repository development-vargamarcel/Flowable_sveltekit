package com.demo.bpm.controller;

import com.demo.bpm.dto.UpdateProfileRequest;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody UpdateProfileRequest request, Authentication authentication) {
        String userId = authentication.getName();
        log.info("Updating profile for user '{}'", userId);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}
