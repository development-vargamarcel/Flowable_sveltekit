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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Get tasks for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the tasks",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaskDTO.class))) }) })
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getMyTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getGroupTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get tasks assigned to the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the assigned tasks",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaskDTO.class))) }) })
    @GetMapping("/assigned")
    public ResponseEntity<List<TaskDTO>> getAssignedTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getAssignedTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get tasks that can be claimed by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the claimable tasks",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaskDTO.class))) }) })
    @GetMapping("/claimable")
    public ResponseEntity<List<TaskDTO>> getClaimableTasks(@AuthenticationPrincipal UserDetails userDetails) {
        List<TaskDTO> tasks = taskService.getClaimableTasks(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get task details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the task",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content) })
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(@Parameter(description = "ID of the task to be obtained") @PathVariable String taskId) {
        try {
            return ResponseEntity.ok(taskService.getTaskDetails(taskId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delegate (reassign) a task to another user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task delegated successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/{taskId}/delegate")
    public ResponseEntity<?> delegateTask(
            @Parameter(description = "ID of the task to be delegated") @PathVariable String taskId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String targetUserId = request.get("targetUserId");
            if (targetUserId == null || targetUserId.isEmpty()) {
                throw new IllegalArgumentException("targetUserId is required");
            }

            taskService.delegateTask(taskId, userDetails.getUsername(), targetUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "Task delegated successfully",
                    "taskId", taskId,
                    "assignee", targetUserId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Claim a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task claimed successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/{taskId}/claim")
    public ResponseEntity<?> claimTask(
            @Parameter(description = "ID of the task to be claimed") @PathVariable String taskId,
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

    @Operation(summary = "Unclaim a task (release assignment)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task unclaimed successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/{taskId}/unclaim")
    public ResponseEntity<?> unclaimTask(
            @Parameter(description = "ID of the task to be unclaimed") @PathVariable String taskId) {
        try {
            taskService.unclaimTask(taskId);
            return ResponseEntity.ok(Map.of(
                    "message", "Task unclaimed successfully",
                    "taskId", taskId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Complete a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task completed successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<?> completeTask(
            @Parameter(description = "ID of the task to be completed") @PathVariable String taskId,
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

    @Operation(summary = "Get the form definition for a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the form definition",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content) })
    @GetMapping("/{taskId}/form")
    public ResponseEntity<?> getTaskFormDefinition(@Parameter(description = "ID of the task to get the form for") @PathVariable String taskId) {
        try {
            // Get task-specific form definition
            FormDefinitionDTO taskFormDefinition = formDefinitionService.getFormDefinitionForTask(taskId);

            // Get the process definition ID from the task
            String processDefinitionId = taskService.getProcessDefinitionIdForTask(taskId);

            // Get process-level form config (field library + condition rules)
            com.demo.bpm.dto.ProcessFormConfigDTO processConfig =
                formDefinitionService.getProcessFormConfig(processDefinitionId);

            // Return both task form and process config
            return ResponseEntity.ok(Map.of(
                    "taskForm", taskFormDefinition,
                    "processConfig", processConfig
            ));
        } catch (Exception e) {
            log.error("Error retrieving task form definition: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
