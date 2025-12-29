package com.demo.bpm.controller;

import com.demo.bpm.dto.CompleteTaskRequest;
import com.demo.bpm.dto.FormDefinitionDTO;
import com.demo.bpm.dto.TaskDTO;
import com.demo.bpm.service.FormDefinitionService;
import com.demo.bpm.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final FormDefinitionService formDefinitionService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getMyTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getGroupTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<TaskDTO>> getAssignedTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getAssignedTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/claimable")
    public ResponseEntity<List<TaskDTO>> getClaimableTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getClaimableTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(@PathVariable String taskId) {
        try {
            TaskDTO task = taskService.getTaskById(taskId);
            Map<String, Object> variables = taskService.getTaskVariables(taskId);

            return ResponseEntity.ok(Map.of(
                    "task", task,
                    "variables", variables
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{taskId}/claim")
    public ResponseEntity<?> claimTask(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            taskService.claimTask(taskId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Task claimed successfully",
                    "taskId", taskId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<?> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) CompleteTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> variables = request != null ? request.getVariables() : Map.of();
            taskService.completeTask(taskId, variables, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "Task completed successfully",
                    "taskId", taskId
            ));
        } catch (Exception e) {
            log.error("Error completing task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{taskId}/form")
    public ResponseEntity<?> getTaskFormDefinition(@PathVariable String taskId) {
        try {
            FormDefinitionDTO formDefinition = formDefinitionService.getFormDefinitionForTask(taskId);
            return ResponseEntity.ok(formDefinition);
        } catch (Exception e) {
            log.error("Error retrieving task form definition: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
