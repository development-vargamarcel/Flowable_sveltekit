package com.demo.bpm.service.helpers;

import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskCommonHelper {

    private final TaskService taskService;

    /**
     * Retrieves a task by ID or throws ResourceNotFoundException if not found.
     *
     * @param taskId the ID of the task to retrieve
     * @return the task
     * @throws ResourceNotFoundException if the task is not found
     */
    public Task getTaskOrThrow(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    /**
     * Validates that the user is the assignee of the task or the task is unassigned.
     *
     * @param task the task to validate
     * @param userId the user ID
     * @throws InvalidOperationException if the task is assigned to another user
     */
    public void validateAssigneeOrUnassigned(Task task, String userId) {
        if (task.getAssignee() != null && !task.getAssignee().equals(userId)) {
            throw new InvalidOperationException("Task is assigned to another user");
        }
    }

    /**
     * Validates that the user is the current assignee of the task.
     *
     * @param task the task to validate
     * @param userId the user ID
     * @throws InvalidOperationException if the user is not the assignee
     */
    public void validateIsAssignee(Task task, String userId) {
        if (task.getAssignee() == null || !task.getAssignee().equals(userId)) {
            throw new InvalidOperationException("Only the assignee can perform this action");
        }
    }
}
