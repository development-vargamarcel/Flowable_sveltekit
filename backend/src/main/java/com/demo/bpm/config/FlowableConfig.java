package com.demo.bpm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlowableConfig {

    @Bean
    public CommandLineRunner initFlowableData(
            IdentityService identityService,
            RuntimeService runtimeService,
            TaskService taskService) {

        return args -> {
            log.info("Initializing Flowable demo data...");

            // Create groups
            createGroupIfNotExists(identityService, "users", "Users");
            createGroupIfNotExists(identityService, "supervisors", "Supervisors");
            createGroupIfNotExists(identityService, "executives", "Executives");

            // Create users in Flowable identity
            createUserIfNotExists(identityService, "user1", "User", "One", "user1@demo.com", "users");
            createUserIfNotExists(identityService, "supervisor1", "Supervisor", "One", "supervisor1@demo.com", "supervisors");
            createUserIfNotExists(identityService, "executive1", "Executive", "One", "executive1@demo.com", "executives");

            // Start demo process instances
            startDemoProcesses(runtimeService);

            log.info("Demo data initialization complete!");
        };
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
    }
}
