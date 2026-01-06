package com.demo.bpm.integration;

import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.service.ProcessService;
import com.demo.bpm.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskServiceIT extends BaseIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessService processService;

    @Test
    void getTaskById_whenTaskExists_shouldReturnTask() {
        // Start a process to create a task
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 100);
        processService.startProcess("expense-approval", "EXP-123", variables, "user1");

        // Find the task
        TaskDTO task = taskService.getClaimableTasks("supervisor1").get(0);

        TaskDTO foundTask = taskService.getTaskById(task.getId());

        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getId()).isEqualTo(task.getId());
        assertThat(foundTask.getName()).isEqualTo("Approve Expense");
    }
}
