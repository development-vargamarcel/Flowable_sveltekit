package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRequest {
    private String reason;
    private String targetUserId; // Optional: specific user to escalate to
    private String targetLevel; // Optional: specific level to escalate to (SUPERVISOR, MANAGER, DIRECTOR, EXECUTIVE)
    private String comments;
}
