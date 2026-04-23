package com.demo.bpm.service.helpers;

import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCommonHelperTest {

    @Mock
    private TaskService taskService;

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private TaskCommonHelper taskCommonHelper;

    @Test
    void getTaskOrThrow_shouldReturnTask_whenFound() {
        String taskId = "task1";
        Task mockTask = mock(Task.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(mockTask);

        Task result = taskCommonHelper.getTaskOrThrow(taskId);

        assertEquals(mockTask, result);
    }

    @Test
    void getTaskOrThrow_shouldThrowException_whenNotFound() {
        String taskId = "invalid";
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> taskCommonHelper.getTaskOrThrow(taskId));
    }

    @Test
    void validateAssigneeOrUnassigned_shouldNotThrow_whenAssigneeMatches() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn("user1");

        assertDoesNotThrow(() -> taskCommonHelper.validateAssigneeOrUnassigned(mockTask, "user1"));
    }

    @Test
    void validateAssigneeOrUnassigned_shouldNotThrow_whenUnassigned() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn(null);

        assertDoesNotThrow(() -> taskCommonHelper.validateAssigneeOrUnassigned(mockTask, "user1"));
    }

    @Test
    void validateAssigneeOrUnassigned_shouldThrow_whenAssignedToOther() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn("otherUser");

        assertThrows(InvalidOperationException.class, () -> taskCommonHelper.validateAssigneeOrUnassigned(mockTask, "user1"));
    }

    @Test
    void validateIsAssignee_shouldNotThrow_whenAssigneeMatches() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn("user1");

        assertDoesNotThrow(() -> taskCommonHelper.validateIsAssignee(mockTask, "user1"));
    }

    @Test
    void validateIsAssignee_shouldThrow_whenUnassigned() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn(null);

        assertThrows(InvalidOperationException.class, () -> taskCommonHelper.validateIsAssignee(mockTask, "user1"));
    }

    @Test
    void validateIsAssignee_shouldThrow_whenAssignedToOther() {
        Task mockTask = mock(Task.class);
        when(mockTask.getAssignee()).thenReturn("otherUser");

        assertThrows(InvalidOperationException.class, () -> taskCommonHelper.validateIsAssignee(mockTask, "user1"));
    }
}
