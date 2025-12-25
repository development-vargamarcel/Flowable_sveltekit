package com.demo.bpm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDTO {
    private String username;
    private String displayName;
    private String email;
    private List<String> roles;
}
