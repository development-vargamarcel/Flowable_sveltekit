# Workflows

The application includes three core BPMN workflows.

## 1. Expense Approval (`expense-approval`)

A threshold-based approval process for expenses.

### Flow
1. **Start**: User submits an expense request with an `amount`.
2. **Supervisor Approval**: A task is assigned to the `SUPERVISOR` group.
   - The supervisor reviews the request.
3. **Gateway Check**:
   - If `amount <= 500`: The process ends (Approved).
   - If `amount > 500`: The process moves to Executive Approval.
4. **Executive Approval**: A task is assigned to the `EXECUTIVE` group.
   - The executive reviews the high-value expense.
5. **End**: Process completes.

### Roles
- **Initiator**: User (user1)
- **Approver 1**: Supervisor (supervisor1)
- **Approver 2**: Executive (executive1) (only if amount > 500)

## 2. Leave Request (`leave-request`)

A sequential approval process based on leave duration.

### Flow
1. **Start**: User submits a leave request with `days`.
2. **Supervisor Approval**: A task is assigned to the `SUPERVISOR` group.
3. **Gateway Check**:
   - If `days <= 5`: The process ends (Approved).
   - If `days > 5`: The process moves to Executive Approval.
4. **Executive Approval**: A task is assigned to the `EXECUTIVE` group.
5. **End**: Process completes.

## 3. Task Assignment (`task-assignment`)

A simple ad-hoc task assignment workflow.

### Flow
1. **Start**: User creates a task with `title`, `description`, and `assignee`.
2. **Task Work**: A user task is created and assigned to the specified `assignee` (or `USER` group).
   - The assignee works on the task and completes it.
3. **End**: Process completes.
