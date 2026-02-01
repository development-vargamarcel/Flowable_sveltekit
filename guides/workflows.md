# Workflows

The application includes five core BPMN workflows.

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

## 4. Purchase Request (`purchase-request`)

A multi-level approval process based on purchase amount.

### Flow
1. **Start**: User submits a purchase request with an `amount`.
2. **Auto-Approval Check**:
   - If `amount <= 1000`: Auto-approved.
   - If `amount > 1000`: Moves to Supervisor Approval.
3. **Supervisor Approval**: Assigned to `SUPERVISOR` group.
   - If `amount <= 5000`: Ends after approval.
   - If `amount > 5000`: Moves to Manager Approval.
4. **Manager Approval**: Assigned to `MANAGER` group.
   - If `amount <= 20000`: Ends after approval.
   - If `amount > 20000`: Moves to Director Approval.
5. **Director Approval**: Assigned to `DIRECTOR` group.
   - If `amount <= 50000`: Ends after approval.
   - If `amount > 50000`: Moves to Executive Approval.
6. **Executive Approval**: Assigned to `EXECUTIVE` group.
7. **End**: Process completes.

## 5. Project Approval (`project-approval`)

A parallel approval process for project initiation.

### Flow
1. **Start**: User submits a project proposal with `budget` and `projectType`.
2. **Parallel Review**: The process splits into parallel paths:
   - **Technical Review**: Assigned to `MANAGER`. Always required.
   - **Financial Review**: Assigned to `MANAGER`. Always required.
   - **Legal Review**: Assigned to `MANAGER`. Required if `budget > 25000` OR `projectType` is 'legal' or 'compliance'.
3. **Join**: The process waits for all active reviews to complete.
4. **Budget Check**:
   - If `budget <= 100000`: Process ends.
   - If `budget > 100000`: Moves to Executive Approval.
5. **Executive Approval**: Assigned to `EXECUTIVE` group.
6. **End**: Process completes.

## Feature: Comments

Users can add comments to any active process instance to provide updates, ask questions, or document decisions.

### Usage
1. Open the **Process Details** modal for any active process.
2. Scroll to the **Comments** section.
3. Enter your message in the text area.
4. Click **Add** to submit the comment.

Comments are visible to all users with access to the process instance and are included in the process history.

## Feature: Diagram Visualization

Users can view the live BPMN diagram of a process instance to understand the current progress and pending activities.

### Usage
1. Open the **Process Details** modal for any active or completed process.
2. Click the **Diagram** tab at the top of the modal.
3. The application will render the BPMN 2.0 diagram.
   - **Active Activities**: Highlighted in green.
   - **Zoom/Pan**: Use mouse wheel to zoom, drag to pan.

## Feature: Process Export

Users can export the full history of a process instance, including details, variables, tasks, approvals, escalations, and comments, to a CSV file.

### Usage
1. Open the **Process Details** modal for any active or completed process.
2. Click the **Export CSV** button in the header.
3. The browser will download a CSV file named `process_export_{id}.csv`.
