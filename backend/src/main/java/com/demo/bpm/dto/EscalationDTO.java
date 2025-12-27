package com.demo.bpm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationDTO {
    private String id;
    private String taskId;
    private String processInstanceId;
    private String fromUserId;
    private String fromUserName;
    private String toUserId;
    private String toUserName;
    private String fromLevel;
    private String toLevel;
    private String reason;
    private String type; // ESCALATE or DE_ESCALATE
    private LocalDateTime timestamp;
}
