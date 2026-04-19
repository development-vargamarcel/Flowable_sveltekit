package com.demo.bpm.service;

import com.demo.bpm.dto.SlaStatsDTO;
import com.demo.bpm.entity.SLA;
import com.demo.bpm.repository.SLARepository;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SLAServiceTest {

    @Mock
    private SLARepository slaRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private SLAService slaService;

    @Test
    void createOrUpdateSLA_shouldSave() {
        when(slaRepository.findByTargetKeyAndTargetType("task1", SLA.SLATargetType.TASK)).thenReturn(Optional.empty());

        slaService.createOrUpdateSLA("SLA 1", "task1", SLA.SLATargetType.TASK, Duration.ofHours(24), 80);

        verify(slaRepository).save(any(SLA.class));
    }

    @Test
    void getSLAStats_shouldCalculateCorrectly() {
        SLA sla = new SLA();
        sla.setTargetKey("taskKey");
        sla.setTargetType(SLA.SLATargetType.TASK);
        sla.setDuration(Duration.ofHours(10));
        sla.setWarningThresholdPercentage(50);

        Task task = mock(Task.class);
        when(task.getTaskDefinitionKey()).thenReturn("taskKey");
        // Created 6 hours ago -> 60% elapsed (at risk)
        when(task.getCreateTime()).thenReturn(Date.from(Instant.now().minus(Duration.ofHours(6))));

        when(slaRepository.findAll()).thenReturn(Collections.singletonList(sla));
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.singletonList(task));

        SlaStatsDTO stats = slaService.getSLAStats();

        assertEquals(1, stats.getTotalProcesses());
        assertEquals(1, stats.getAtRisk());
        assertEquals(0, stats.getBreached());
        assertEquals(0, stats.getOnTrack());
    }

    @Test
    void checkSLABreaches_shouldNotifyOnBreach() {
        SLA sla = new SLA();
        sla.setTargetKey("taskKey");
        sla.setTargetType(SLA.SLATargetType.TASK);
        sla.setDuration(Duration.ofHours(10));

        Task task = mock(Task.class);
        when(task.getTaskDefinitionKey()).thenReturn("taskKey");
        when(task.getName()).thenReturn("Test Task");
        when(task.getAssignee()).thenReturn("user1");
        // Created 12 hours ago -> breached
        when(task.getCreateTime()).thenReturn(Date.from(Instant.now().minus(Duration.ofHours(12))));

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.singletonList(task));
        when(slaRepository.findByTargetKeyAndTargetType("taskKey", SLA.SLATargetType.TASK)).thenReturn(Optional.of(sla));

        slaService.checkSLABreaches();

        verify(notificationService).createNotification(
                eq("user1"), anyString(), anyString(), any(), anyString());
    }
}
