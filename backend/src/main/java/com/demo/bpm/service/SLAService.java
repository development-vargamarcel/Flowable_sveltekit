package com.demo.bpm.service;

import com.demo.bpm.dto.SlaStatsDTO;
import com.demo.bpm.entity.Notification;
import com.demo.bpm.entity.SLA;
import com.demo.bpm.repository.SLARepository;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SLAService {

    private final SLARepository slaRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;

    public void createOrUpdateSLA(String name, String targetKey, SLA.SLATargetType targetType, Duration duration, Integer warningThreshold) {
        Optional<SLA> existing = slaRepository.findByTargetKeyAndTargetType(targetKey, targetType);
        SLA sla = existing.orElse(new SLA());
        sla.setName(name);
        sla.setTargetKey(targetKey);
        sla.setTargetType(targetType);
        sla.setDuration(duration);
        sla.setWarningThresholdPercentage(warningThreshold);
        slaRepository.save(sla);
    }

    @Transactional(readOnly = true)
    public SlaStatsDTO getSLAStats() {
        List<Task> tasks = taskService.createTaskQuery().active().list();
        List<SLA> slas = slaRepository.findAll();
        Map<String, SLA> taskSlaMap = slas.stream()
            .filter(sla -> sla.getTargetType() == SLA.SLATargetType.TASK)
            .collect(Collectors.toMap(SLA::getTargetKey, sla -> sla, (s1, s2) -> s1)); // Handle duplicate keys safely

        long onTrack = 0;
        long atRisk = 0;
        long breached = 0;
        double totalPercentage = 0;
        long tasksWithSla = 0;

        for (Task task : tasks) {
            SLA sla = taskSlaMap.get(task.getTaskDefinitionKey());
            if (sla != null) {
                tasksWithSla++;
                Date createDate = task.getCreateTime();
                if (createDate != null) {
                    LocalDateTime created = LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault());
                    LocalDateTime now = LocalDateTime.now();
                    Duration elapsed = Duration.between(created, now);
                    Duration limit = sla.getDuration();

                    double percentage = (double) elapsed.toMillis() / limit.toMillis() * 100;
                    totalPercentage += percentage;

                    if (elapsed.compareTo(limit) > 0) {
                        breached++;
                    } else if (sla.getWarningThresholdPercentage() != null && percentage >= sla.getWarningThresholdPercentage()) {
                        atRisk++;
                    } else {
                        onTrack++;
                    }
                }
            }
        }

        double avg = tasksWithSla > 0 ? totalPercentage / tasksWithSla : 0;

        List<SlaStatsDTO.StatusCount> breakdown = new ArrayList<>();
        breakdown.add(SlaStatsDTO.StatusCount.builder().status("On Track").count(onTrack).build());
        breakdown.add(SlaStatsDTO.StatusCount.builder().status("At Risk").count(atRisk).build());
        breakdown.add(SlaStatsDTO.StatusCount.builder().status("Breached").count(breached).build());

        return SlaStatsDTO.builder()
                .totalProcesses(tasksWithSla)
                .onTrack(onTrack)
                .atRisk(atRisk)
                .breached(breached)
                .avgCompletionPercentage(avg)
                .processesByStatus(breakdown)
                .build();
    }

    @Scheduled(fixedRate = 300000) // Check every 5 minutes
    @Transactional
    public void checkSLABreaches() {
        // Check Task SLAs
        List<Task> activeTasks = taskService.createTaskQuery().active().list();
        
        for (Task task : activeTasks) {
            String taskDefKey = task.getTaskDefinitionKey();
            Optional<SLA> slaOpt = slaRepository.findByTargetKeyAndTargetType(taskDefKey, SLA.SLATargetType.TASK);
            
            if (slaOpt.isPresent()) {
                checkTaskSLA(task, slaOpt.get());
            }
        }
    }
    
    private void checkTaskSLA(Task task, SLA sla) {
        Date createDate = task.getCreateTime();
        if (createDate == null) return;
        
        LocalDateTime created = LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(created, now);
        Duration limit = sla.getDuration();
        
        if (elapsed.compareTo(limit) > 0) {
            // Breach
            // Check if we already notified (basic check, could be improved with dedicated Breach entity)
            if (task.getAssignee() != null) {
                // Send notification
                String message = "Task '" + task.getName() + "' has breached its SLA of " + formatDuration(limit);
                // Ideally trigger notification once per breach, leaving simplistic for demo
                 notificationService.createNotification(
                        task.getAssignee(),
                        "SLA Breach: " + task.getName(),
                        message,
                        Notification.NotificationType.TASK_OVERDUE,
                        "/tasks/" + task.getId()
                );
            }
        } else if (sla.getWarningThresholdPercentage() != null) {
            // Warning
            long limitMillis = limit.toMillis();
            long elapsedMillis = elapsed.toMillis();
            double percentage = (double) elapsedMillis / limitMillis * 100;
            
            if (percentage >= sla.getWarningThresholdPercentage()) {
                if (task.getAssignee() != null) {
                     // Deduplication logic would be needed here in prod
                     notificationService.createNotification(
                        task.getAssignee(),
                        "SLA Warning: " + task.getName(),
                        "Task is approaching SLA limit (" + (int)percentage + "% used)",
                        Notification.NotificationType.TASK_DUE_SOON,
                        "/tasks/" + task.getId()
                    );
                }
            }
        }
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%d hours, %d minutes", hours, minutes);
    }
}
