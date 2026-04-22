package com.demo.bpm.util;

public final class WorkflowConstants {

    private WorkflowConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String VAR_CURRENT_LEVEL = "currentLevel";
    public static final String VAR_ESCALATION_COUNT = "escalationCount";
    public static final String VAR_ESCALATION_HISTORY = "escalationHistory";
    public static final String VAR_APPROVAL_HISTORY = "approvalHistory";
    public static final String VAR_HANDOFF_HISTORY = "handoffHistory";
    public static final String VAR_DECISION = "decision";
    public static final String VAR_ESCALATION_REASON = "escalationReason";
    public static final String VAR_ESCALATED_BY = "escalatedBy";
    public static final String VAR_ESCALATED_AT = "escalatedAt";
    public static final String VAR_DE_ESCALATION_REASON = "deEscalationReason";
    public static final String VAR_DE_ESCALATED_BY = "deEscalatedBy";
    public static final String VAR_DE_ESCALATED_AT = "deEscalatedAt";
    public static final String VAR_COMPLETED_BY = "completedBy";
    public static final String VAR_APPROVAL_COMMENTS = "approvalComments";
    public static final String VAR_STARTED_BY = "startedBy";
    public static final String VAR_EMPLOYEE_NAME = "employeeName";

    public static final String DECISION_ESCALATE = "escalate";
    public static final String DECISION_DE_ESCALATE = "de_escalate";

    public static final String LEVEL_SUPERVISOR = "SUPERVISOR";
    public static final String LEVEL_MANAGER = "MANAGER";
    public static final String LEVEL_DIRECTOR = "DIRECTOR";
    public static final String LEVEL_EXECUTIVE = "EXECUTIVE";

    public static final String TYPE_ESCALATE = "ESCALATE";
    public static final String TYPE_DE_ESCALATE = "DE_ESCALATE";
}
