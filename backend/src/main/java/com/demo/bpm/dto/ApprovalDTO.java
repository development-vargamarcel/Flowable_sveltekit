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
public class ApprovalDTO {
    private String id;
    private String processInstanceId;
    private String taskId;
    private String taskName;
    private String approverId;
    private String approverName;
    private String approverLevel;
    private String decision; // APPROVED, REJECTED, ESCALATED, DE_ESCALATED
    private String comments;
    private LocalDateTime timestamp;
    private Integer stepOrder;
    private Boolean isRequired;
}
