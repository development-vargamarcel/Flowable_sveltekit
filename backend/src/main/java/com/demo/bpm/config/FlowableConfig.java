package com.demo.bpm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class FlowableConfig {

    private final IdentityService identityService;
    private final RuntimeService runtimeService;

    /**
     * Initialize demo data asynchronously after the application is fully ready.
     * This ensures the health check endpoint is available before demo data loading.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initFlowableData() {
        // Small delay to ensure all services are fully ready
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        log.info("Initializing Flowable demo data (async)...");

        try {
            // Create groups for hierarchical approval levels
            createGroupIfNotExists(identityService, "users", "Users");
            createGroupIfNotExists(identityService, "supervisors", "Supervisors");
            createGroupIfNotExists(identityService, "managers", "Managers");
            createGroupIfNotExists(identityService, "directors", "Directors");
            createGroupIfNotExists(identityService, "executives", "Executives");

            // Create users in Flowable identity with hierarchical roles
            // Level 1: Regular users
            createUserIfNotExists(identityService, "user1", "User", "One", "user1@demo.com", "users");
            createUserIfNotExists(identityService, "user2", "User", "Two", "user2@demo.com", "users");

            // Level 2: Supervisors (can approve up to $1000)
            createUserIfNotExists(identityService, "supervisor1", "Supervisor", "One", "supervisor1@demo.com", "supervisors");
            createUserIfNotExists(identityService, "supervisor2", "Supervisor", "Two", "supervisor2@demo.com", "supervisors");

            // Level 3: Managers (can approve up to $5000, manager of supervisors)
            createUserIfNotExists(identityService, "manager1", "Manager", "One", "manager1@demo.com", "managers");
            createUserIfNotExists(identityService, "manager2", "Manager", "Two", "manager2@demo.com", "managers");

            // Level 4: Directors (can approve up to $20000, manager of managers)
            createUserIfNotExists(identityService, "director1", "Director", "One", "director1@demo.com", "directors");

            // Level 5: Executives (final approval authority)
            createUserIfNotExists(identityService, "executive1", "Executive", "One", "executive1@demo.com", "executives");

            // Start demo process instances
            startDemoProcesses(runtimeService);

            log.info("Demo data initialization complete!");
        } catch (Exception e) {
            log.error("Failed to initialize demo data: {}", e.getMessage(), e);
        }
    }

    private void createGroupIfNotExists(IdentityService identityService, String groupId, String groupName) {
        if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
            Group group = identityService.newGroup(groupId);
            group.setName(groupName);
            group.setType("assignment");
            identityService.saveGroup(group);
            log.info("Created group: {}", groupId);
        }
    }

    private void createUserIfNotExists(IdentityService identityService, String userId, String firstName,
                                        String lastName, String email, String groupId) {
        if (identityService.createUserQuery().userId(userId).count() == 0) {
            User user = identityService.newUser(userId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword("password");
            identityService.saveUser(user);
            identityService.createMembership(userId, groupId);
            log.info("Created user: {} in group: {}", userId, groupId);
        }
    }

    private void startDemoProcesses(RuntimeService runtimeService) {
        // Demo Expense 1: Small amount - pending supervisor approval
        Map<String, Object> expense1 = new HashMap<>();
        expense1.put("employeeId", "user1");
        expense1.put("employeeName", "User One");
        expense1.put("amount", 250.00);
        expense1.put("category", "Travel");
        expense1.put("description", "Taxi to client meeting");
        expense1.put("submittedDate", java.time.LocalDate.now().toString());

        try {
            runtimeService.startProcessInstanceByKey("expense-approval", "EXP-001", expense1);
            log.info("Started demo expense process: EXP-001 (amount: $250)");
        } catch (Exception e) {
            log.warn("Could not start expense process (may not be deployed yet): {}", e.getMessage());
        }

        // Demo Expense 2: Large amount - will need executive approval
        Map<String, Object> expense2 = new HashMap<>();
        expense2.put("employeeId", "user1");
        expense2.put("employeeName", "User One");
        expense2.put("amount", 1500.00);
        expense2.put("category", "Equipment");
        expense2.put("description", "New laptop for development");
        expense2.put("submittedDate", java.time.LocalDate.now().toString());

        try {
            runtimeService.startProcessInstanceByKey("expense-approval", "EXP-002", expense2);
            log.info("Started demo expense process: EXP-002 (amount: $1500)");
        } catch (Exception e) {
            log.warn("Could not start expense process: {}", e.getMessage());
        }

        // Demo Leave Request
        Map<String, Object> leave1 = new HashMap<>();
        leave1.put("employeeId", "user1");
        leave1.put("employeeName", "User One");
        leave1.put("leaveType", "Annual Leave");
        leave1.put("startDate", java.time.LocalDate.now().plusDays(7).toString());
        leave1.put("endDate", java.time.LocalDate.now().plusDays(10).toString());
        leave1.put("days", 3);
        leave1.put("reason", "Family vacation");

        try {
            runtimeService.startProcessInstanceByKey("leave-request", "LEAVE-001", leave1);
            log.info("Started demo leave request: LEAVE-001 (3 days)");
        } catch (Exception e) {
            log.warn("Could not start leave process: {}", e.getMessage());
        }

        // Demo Task
        Map<String, Object> task1 = new HashMap<>();
        task1.put("title", "Review Q4 Report");
        task1.put("description", "Review and provide feedback on the Q4 financial report");
        task1.put("priority", "high");
        task1.put("createdBy", "supervisor1");
        task1.put("createdDate", java.time.LocalDate.now().toString());

        try {
            runtimeService.startProcessInstanceByKey("task-assignment", "TASK-001", task1);
            log.info("Started demo task: TASK-001");
        } catch (Exception e) {
            log.warn("Could not start task process: {}", e.getMessage());
        }

        // Demo Purchase Request 1: Medium amount - needs manager approval
        Map<String, Object> purchase1 = new HashMap<>();
        purchase1.put("employeeId", "user1");
        purchase1.put("employeeName", "User One");
        purchase1.put("amount", 7500.00);
        purchase1.put("department", "Engineering");
        purchase1.put("urgency", "normal");
        purchase1.put("description", "Software licenses for development team");
        purchase1.put("vendor", "TechCorp Inc.");
        purchase1.put("justification", "Required for new project development");
        purchase1.put("initiator", "user1");
        purchase1.put("startedBy", "user1");

        try {
            runtimeService.startProcessInstanceByKey("purchase-request", "PUR-001", purchase1);
            log.info("Started demo purchase request: PUR-001 (amount: $7,500 - needs Manager approval)");
        } catch (Exception e) {
            log.warn("Could not start purchase process: {}", e.getMessage());
        }

        // Demo Purchase Request 2: Large amount - needs director approval
        Map<String, Object> purchase2 = new HashMap<>();
        purchase2.put("employeeId", "user2");
        purchase2.put("employeeName", "User Two");
        purchase2.put("amount", 35000.00);
        purchase2.put("department", "Marketing");
        purchase2.put("urgency", "high");
        purchase2.put("description", "Annual marketing campaign materials");
        purchase2.put("vendor", "Creative Agency LLC");
        purchase2.put("justification", "Q1 marketing initiative");
        purchase2.put("initiator", "user2");
        purchase2.put("startedBy", "user2");

        try {
            runtimeService.startProcessInstanceByKey("purchase-request", "PUR-002", purchase2);
            log.info("Started demo purchase request: PUR-002 (amount: $35,000 - needs Director approval)");
        } catch (Exception e) {
            log.warn("Could not start purchase process: {}", e.getMessage());
        }

        // Demo Project Approval 1: Medium project - parallel review
        Map<String, Object> project1 = new HashMap<>();
        project1.put("employeeId", "user1");
        project1.put("employeeName", "User One");
        project1.put("projectName", "Customer Portal Redesign");
        project1.put("budget", 45000.00);
        project1.put("timeline", "3 months");
        project1.put("department", "Engineering");
        project1.put("projectType", "standard");
        project1.put("expectedROI", "25% increase in customer engagement");
        project1.put("description", "Modernize the customer self-service portal with new UI/UX");
        project1.put("initiator", "user1");
        project1.put("startedBy", "user1");

        try {
            runtimeService.startProcessInstanceByKey("project-approval", "PROJ-001", project1);
            log.info("Started demo project approval: PROJ-001 (budget: $45,000 - parallel review)");
        } catch (Exception e) {
            log.warn("Could not start project process: {}", e.getMessage());
        }

        // Demo Project Approval 2: Large project - requires executive
        Map<String, Object> project2 = new HashMap<>();
        project2.put("employeeId", "manager1");
        project2.put("employeeName", "Manager One");
        project2.put("projectName", "ERP System Implementation");
        project2.put("budget", 250000.00);
        project2.put("timeline", "12 months");
        project2.put("department", "Operations");
        project2.put("projectType", "enterprise");
        project2.put("expectedROI", "40% operational efficiency improvement");
        project2.put("description", "Full ERP system replacement with modern cloud-based solution");
        project2.put("initiator", "manager1");
        project2.put("startedBy", "manager1");

        try {
            runtimeService.startProcessInstanceByKey("project-approval", "PROJ-002", project2);
            log.info("Started demo project approval: PROJ-002 (budget: $250,000 - requires executive)");
        } catch (Exception e) {
            log.warn("Could not start project process: {}", e.getMessage());
        }
    }
}
