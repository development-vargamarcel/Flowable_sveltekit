package com.demo.bpm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class FlowableConfig {

    private final IdentityService identityService;

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

            // Create department groups
            createGroupIfNotExists(identityService, "engineering", "Engineering Department");
            createGroupIfNotExists(identityService, "finance", "Finance Department");
            createGroupIfNotExists(identityService, "hr", "Human Resources Department");
            createGroupIfNotExists(identityService, "sales", "Sales Department");
            createGroupIfNotExists(identityService, "operations", "Operations Department");
            createGroupIfNotExists(identityService, "it", "IT Department");
            createGroupIfNotExists(identityService, "legal", "Legal Department");

            // ===========================================
            // ENGINEERING DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("eng.john", "John", "Chen", "john.chen@demo.com",
                new String[]{"users", "engineering"});
            createUserWithMultipleGroups("eng.sarah", "Sarah", "Miller", "sarah.miller@demo.com",
                new String[]{"users", "engineering"});
            createUserWithMultipleGroups("eng.mike", "Mike", "Johnson", "mike.johnson@demo.com",
                new String[]{"supervisors", "engineering"});
            createUserWithMultipleGroups("eng.lisa", "Lisa", "Wang", "lisa.wang@demo.com",
                new String[]{"managers", "engineering"});

            // ===========================================
            // FINANCE DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("fin.bob", "Bob", "Smith", "bob.smith@demo.com",
                new String[]{"users", "finance"});
            createUserWithMultipleGroups("fin.alice", "Alice", "Brown", "alice.brown@demo.com",
                new String[]{"users", "finance"});
            createUserWithMultipleGroups("fin.carol", "Carol", "Davis", "carol.davis@demo.com",
                new String[]{"supervisors", "finance"});
            createUserWithMultipleGroups("fin.david", "David", "Wilson", "david.wilson@demo.com",
                new String[]{"managers", "finance"});
            createUserWithMultipleGroups("fin.cfo", "Michael", "Taylor", "cfo@demo.com",
                new String[]{"directors", "finance"});

            // ===========================================
            // HR DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("hr.emma", "Emma", "Garcia", "emma.garcia@demo.com",
                new String[]{"users", "hr"});
            createUserWithMultipleGroups("hr.james", "James", "Martinez", "james.martinez@demo.com",
                new String[]{"users", "hr"});
            createUserWithMultipleGroups("hr.nina", "Nina", "Anderson", "nina.anderson@demo.com",
                new String[]{"supervisors", "hr"});
            createUserWithMultipleGroups("hr.tom", "Tom", "Thomas", "tom.thomas@demo.com",
                new String[]{"managers", "hr"});
            createUserWithMultipleGroups("hr.chro", "Patricia", "Jackson", "chro@demo.com",
                new String[]{"directors", "hr"});

            // ===========================================
            // SALES DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("sales.kevin", "Kevin", "White", "kevin.white@demo.com",
                new String[]{"users", "sales"});
            createUserWithMultipleGroups("sales.maria", "Maria", "Harris", "maria.harris@demo.com",
                new String[]{"users", "sales"});
            createUserWithMultipleGroups("sales.peter", "Peter", "Martin", "peter.martin@demo.com",
                new String[]{"supervisors", "sales"});
            createUserWithMultipleGroups("sales.rachel", "Rachel", "Thompson", "rachel.thompson@demo.com",
                new String[]{"managers", "sales"});

            // ===========================================
            // OPERATIONS DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("ops.steve", "Steve", "Robinson", "steve.robinson@demo.com",
                new String[]{"users", "operations"});
            createUserWithMultipleGroups("ops.linda", "Linda", "Clark", "linda.clark@demo.com",
                new String[]{"users", "operations"});
            createUserWithMultipleGroups("ops.frank", "Frank", "Lewis", "frank.lewis@demo.com",
                new String[]{"supervisors", "operations"});
            createUserWithMultipleGroups("ops.grace", "Grace", "Lee", "grace.lee@demo.com",
                new String[]{"managers", "operations"});
            createUserWithMultipleGroups("ops.coo", "Robert", "Walker", "coo@demo.com",
                new String[]{"directors", "operations"});

            // ===========================================
            // IT DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("it.alex", "Alex", "Hall", "alex.hall@demo.com",
                new String[]{"users", "it"});
            createUserWithMultipleGroups("it.diana", "Diana", "Allen", "diana.allen@demo.com",
                new String[]{"users", "it"});
            createUserWithMultipleGroups("it.henry", "Henry", "Young", "henry.young@demo.com",
                new String[]{"supervisors", "it"});
            createUserWithMultipleGroups("it.olivia", "Olivia", "King", "olivia.king@demo.com",
                new String[]{"managers", "it"});
            createUserWithMultipleGroups("it.cto", "William", "Scott", "cto@demo.com",
                new String[]{"directors", "it"});

            // ===========================================
            // LEGAL DEPARTMENT
            // ===========================================
            createUserWithMultipleGroups("legal.amy", "Amy", "Green", "amy.green@demo.com",
                new String[]{"users", "legal"});
            createUserWithMultipleGroups("legal.ben", "Ben", "Adams", "ben.adams@demo.com",
                new String[]{"supervisors", "legal"});
            createUserWithMultipleGroups("legal.claire", "Claire", "Nelson", "claire.nelson@demo.com",
                new String[]{"managers", "legal"});

            // ===========================================
            // EXECUTIVE LEADERSHIP
            // ===========================================
            createUserWithMultipleGroups("exec.ceo", "Elizabeth", "Reynolds", "ceo@demo.com",
                new String[]{"executives"});

            // ===========================================
            // LEGACY USERS (for backward compatibility)
            // ===========================================
            createUserIfNotExists(identityService, "user1", "User", "One", "user1@demo.com", "users");
            createUserIfNotExists(identityService, "user2", "User", "Two", "user2@demo.com", "users");
            createUserIfNotExists(identityService, "supervisor1", "Supervisor", "One", "supervisor1@demo.com", "supervisors");
            createUserIfNotExists(identityService, "supervisor2", "Supervisor", "Two", "supervisor2@demo.com", "supervisors");
            createUserIfNotExists(identityService, "manager1", "Manager", "One", "manager1@demo.com", "managers");
            createUserIfNotExists(identityService, "manager2", "Manager", "Two", "manager2@demo.com", "managers");
            createUserIfNotExists(identityService, "director1", "Director", "One", "director1@demo.com", "directors");
            createUserIfNotExists(identityService, "executive1", "Executive", "One", "executive1@demo.com", "executives");

            log.info("Demo data initialization complete (users and groups created)!");
        } catch (Exception e) {
            log.error("Failed to initialize demo data: {}", e.getMessage(), e);
        }
    }

    private void createUserWithMultipleGroups(String userId, String firstName, String lastName,
                                               String email, String[] groupIds) {
        if (identityService.createUserQuery().userId(userId).count() == 0) {
            User user = identityService.newUser(userId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword("password");
            identityService.saveUser(user);
            for (String groupId : groupIds) {
                identityService.createMembership(userId, groupId);
            }
            log.info("Created user: {} in groups: {}", userId, String.join(", ", groupIds));
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
}
