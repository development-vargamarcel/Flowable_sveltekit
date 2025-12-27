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

            // Start demo process instances
            startDemoProcesses(runtimeService);

            log.info("Demo data initialization complete!");
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

        // =====================================================
        // NEW COMPLEX DEMO PROCESSES
        // =====================================================

        // Demo Incident 1: Critical Security Incident
        Map<String, Object> incident1 = new HashMap<>();
        incident1.put("title", "Production Database Security Breach Detected");
        incident1.put("description", "Unauthorized access detected in production database logs. Potential data exfiltration risk.");
        incident1.put("severity", "CRITICAL");
        incident1.put("category", "SECURITY");
        incident1.put("reportedBy", "it.alex");
        incident1.put("affectedSystem", "Production Database Cluster");
        incident1.put("initiator", "it.alex");
        incident1.put("startedBy", "it.alex");

        try {
            runtimeService.startProcessInstanceByKey("incident-management", "INC-001", incident1);
            log.info("Started demo incident: INC-001 (CRITICAL - Security breach)");
        } catch (Exception e) {
            log.warn("Could not start incident process: {}", e.getMessage());
        }

        // Demo Incident 2: High Priority System Outage
        Map<String, Object> incident2 = new HashMap<>();
        incident2.put("title", "Customer Portal Intermittent Outages");
        incident2.put("description", "Customers reporting intermittent 503 errors on the self-service portal.");
        incident2.put("severity", "HIGH");
        incident2.put("category", "OPERATIONS");
        incident2.put("reportedBy", "ops.steve");
        incident2.put("affectedSystem", "Customer Portal");
        incident2.put("initiator", "ops.steve");
        incident2.put("startedBy", "ops.steve");

        try {
            runtimeService.startProcessInstanceByKey("incident-management", "INC-002", incident2);
            log.info("Started demo incident: INC-002 (HIGH - Portal outages)");
        } catch (Exception e) {
            log.warn("Could not start incident process: {}", e.getMessage());
        }

        // Demo Incident 3: Medium Priority HR System Issue
        Map<String, Object> incident3 = new HashMap<>();
        incident3.put("title", "Payroll System Calculation Discrepancy");
        incident3.put("description", "Overtime calculations showing incorrect values for night shift employees.");
        incident3.put("severity", "MEDIUM");
        incident3.put("category", "HR_SYSTEM");
        incident3.put("reportedBy", "hr.emma");
        incident3.put("affectedSystem", "Payroll Module");
        incident3.put("initiator", "hr.emma");
        incident3.put("startedBy", "hr.emma");

        try {
            runtimeService.startProcessInstanceByKey("incident-management", "INC-003", incident3);
            log.info("Started demo incident: INC-003 (MEDIUM - Payroll discrepancy)");
        } catch (Exception e) {
            log.warn("Could not start incident process: {}", e.getMessage());
        }

        // Demo Budget 1: Large IT Infrastructure Budget
        Map<String, Object> budget1 = new HashMap<>();
        budget1.put("budgetTitle", "FY2025 IT Infrastructure Modernization");
        budget1.put("totalAmount", 750000.00);
        budget1.put("department", "IT");
        budget1.put("budgetType", "CAPITAL");
        budget1.put("fiscalYear", 2025);
        budget1.put("description", "Cloud migration and infrastructure modernization program");
        budget1.put("lineItems", "[{\"item\":\"Cloud Services\",\"amount\":400000},{\"item\":\"Hardware\",\"amount\":200000},{\"item\":\"Consulting\",\"amount\":150000}]");
        budget1.put("initiator", "it.olivia");
        budget1.put("startedBy", "it.olivia");

        try {
            runtimeService.startProcessInstanceByKey("budget-approval", "BUD-001", budget1);
            log.info("Started demo budget: BUD-001 ($750,000 - IT Infrastructure - requires board)");
        } catch (Exception e) {
            log.warn("Could not start budget process: {}", e.getMessage());
        }

        // Demo Budget 2: Marketing Campaign Budget
        Map<String, Object> budget2 = new HashMap<>();
        budget2.put("budgetTitle", "Q1 2025 Product Launch Campaign");
        budget2.put("totalAmount", 125000.00);
        budget2.put("department", "Sales");
        budget2.put("budgetType", "OPERATIONAL");
        budget2.put("fiscalYear", 2025);
        budget2.put("description", "Multi-channel marketing campaign for new product launch");
        budget2.put("lineItems", "[{\"item\":\"Digital Ads\",\"amount\":50000},{\"item\":\"Events\",\"amount\":40000},{\"item\":\"Collateral\",\"amount\":35000}]");
        budget2.put("initiator", "sales.rachel");
        budget2.put("startedBy", "sales.rachel");

        try {
            runtimeService.startProcessInstanceByKey("budget-approval", "BUD-002", budget2);
            log.info("Started demo budget: BUD-002 ($125,000 - Marketing Campaign)");
        } catch (Exception e) {
            log.warn("Could not start budget process: {}", e.getMessage());
        }

        // Demo Budget 3: HR Training Budget with Contract
        Map<String, Object> budget3 = new HashMap<>();
        budget3.put("budgetTitle", "Annual Leadership Development Program");
        budget3.put("totalAmount", 85000.00);
        budget3.put("department", "HR");
        budget3.put("budgetType", "CONTRACT");
        budget3.put("fiscalYear", 2025);
        budget3.put("description", "External vendor contract for leadership training program");
        budget3.put("lineItems", "[{\"item\":\"Training Vendor\",\"amount\":60000},{\"item\":\"Materials\",\"amount\":15000},{\"item\":\"Venue\",\"amount\":10000}]");
        budget3.put("initiator", "hr.tom");
        budget3.put("startedBy", "hr.tom");

        try {
            runtimeService.startProcessInstanceByKey("budget-approval", "BUD-003", budget3);
            log.info("Started demo budget: BUD-003 ($85,000 - HR Training - requires legal review)");
        } catch (Exception e) {
            log.warn("Could not start budget process: {}", e.getMessage());
        }

        // Demo Compliance 1: Annual SOX Compliance Review
        Map<String, Object> compliance1 = new HashMap<>();
        compliance1.put("complianceTitle", "Annual SOX Compliance Audit 2025");
        compliance1.put("complianceType", "ANNUAL");
        compliance1.put("scope", "FULL");
        compliance1.put("regulatoryFramework", "SOX");
        compliance1.put("description", "Annual Sarbanes-Oxley compliance review for fiscal year 2025");
        compliance1.put("dueDate", java.time.LocalDate.now().plusMonths(2).toString());
        compliance1.put("initiator", "legal.claire");
        compliance1.put("startedBy", "legal.claire");

        try {
            runtimeService.startProcessInstanceByKey("compliance-review", "COMP-001", compliance1);
            log.info("Started demo compliance: COMP-001 (Annual SOX Audit - Full Scope)");
        } catch (Exception e) {
            log.warn("Could not start compliance process: {}", e.getMessage());
        }

        // Demo Compliance 2: Security-Focused Compliance
        Map<String, Object> compliance2 = new HashMap<>();
        compliance2.put("complianceTitle", "GDPR Data Protection Assessment");
        compliance2.put("complianceType", "SECURITY");
        compliance2.put("scope", "TARGETED");
        compliance2.put("regulatoryFramework", "GDPR");
        compliance2.put("description", "Quarterly GDPR compliance check for customer data handling");
        compliance2.put("dueDate", java.time.LocalDate.now().plusMonths(1).toString());
        compliance2.put("initiator", "legal.ben");
        compliance2.put("startedBy", "legal.ben");

        try {
            runtimeService.startProcessInstanceByKey("compliance-review", "COMP-002", compliance2);
            log.info("Started demo compliance: COMP-002 (GDPR Assessment - Security Focus)");
        } catch (Exception e) {
            log.warn("Could not start compliance process: {}", e.getMessage());
        }

        // Demo Compliance 3: Operational Compliance
        Map<String, Object> compliance3 = new HashMap<>();
        compliance3.put("complianceTitle", "ISO 27001 Certification Preparation");
        compliance3.put("complianceType", "OPERATIONAL");
        compliance3.put("scope", "TARGETED");
        compliance3.put("regulatoryFramework", "ISO27001");
        compliance3.put("description", "Pre-certification audit for ISO 27001 security management");
        compliance3.put("dueDate", java.time.LocalDate.now().plusMonths(3).toString());
        compliance3.put("initiator", "it.cto");
        compliance3.put("startedBy", "it.cto");

        try {
            runtimeService.startProcessInstanceByKey("compliance-review", "COMP-003", compliance3);
            log.info("Started demo compliance: COMP-003 (ISO 27001 Prep - Operational Focus)");
        } catch (Exception e) {
            log.warn("Could not start compliance process: {}", e.getMessage());
        }

        // Demo Expense from different department users
        Map<String, Object> expense3 = new HashMap<>();
        expense3.put("employeeId", "sales.kevin");
        expense3.put("employeeName", "Kevin White");
        expense3.put("amount", 3500.00);
        expense3.put("category", "Client Entertainment");
        expense3.put("description", "Q4 client appreciation dinner for top 10 accounts");
        expense3.put("submittedDate", java.time.LocalDate.now().toString());

        try {
            runtimeService.startProcessInstanceByKey("expense-approval", "EXP-003", expense3);
            log.info("Started demo expense: EXP-003 (Sales - $3,500 client dinner)");
        } catch (Exception e) {
            log.warn("Could not start expense process: {}", e.getMessage());
        }

        Map<String, Object> expense4 = new HashMap<>();
        expense4.put("employeeId", "eng.john");
        expense4.put("employeeName", "John Chen");
        expense4.put("amount", 890.00);
        expense4.put("category", "Training");
        expense4.put("description", "AWS certification exam and study materials");
        expense4.put("submittedDate", java.time.LocalDate.now().toString());

        try {
            runtimeService.startProcessInstanceByKey("expense-approval", "EXP-004", expense4);
            log.info("Started demo expense: EXP-004 (Engineering - $890 AWS certification)");
        } catch (Exception e) {
            log.warn("Could not start expense process: {}", e.getMessage());
        }

        // Demo Leave from different departments
        Map<String, Object> leave2 = new HashMap<>();
        leave2.put("employeeId", "fin.alice");
        leave2.put("employeeName", "Alice Brown");
        leave2.put("leaveType", "Sick Leave");
        leave2.put("startDate", java.time.LocalDate.now().plusDays(1).toString());
        leave2.put("endDate", java.time.LocalDate.now().plusDays(3).toString());
        leave2.put("days", 2);
        leave2.put("reason", "Medical appointment and recovery");

        try {
            runtimeService.startProcessInstanceByKey("leave-request", "LEAVE-002", leave2);
            log.info("Started demo leave: LEAVE-002 (Finance - 2 days sick leave)");
        } catch (Exception e) {
            log.warn("Could not start leave process: {}", e.getMessage());
        }

        Map<String, Object> leave3 = new HashMap<>();
        leave3.put("employeeId", "hr.james");
        leave3.put("employeeName", "James Martinez");
        leave3.put("leaveType", "Personal Leave");
        leave3.put("startDate", java.time.LocalDate.now().plusDays(14).toString());
        leave3.put("endDate", java.time.LocalDate.now().plusDays(15).toString());
        leave3.put("days", 1);
        leave3.put("reason", "Personal matters");

        try {
            runtimeService.startProcessInstanceByKey("leave-request", "LEAVE-003", leave3);
            log.info("Started demo leave: LEAVE-003 (HR - 1 day personal leave)");
        } catch (Exception e) {
            log.warn("Could not start leave process: {}", e.getMessage());
        }

        log.info("=== All demo processes started successfully ===");
    }
}
