package com.demo.bpm.service.helpers;

import com.demo.bpm.dto.TaskDTO;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;

@Component
public class TaskQueryHelper {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;

    public TaskQueryHelper(RuntimeService runtimeService, RepositoryService repositoryService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
    }

    public TaskDTO convertToDTO(Task task, Map<String, Object> variables) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        String businessKey = processInstance != null ? processInstance.getBusinessKey() : null;

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        String processName = processDefinition != null ? processDefinition.getName() : task.getProcessDefinitionId();
        String processKey = processDefinition != null ? processDefinition.getKey() : null;

        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionKey(processKey)
                .processName(processName)
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        : null)
                .priority(task.getPriority())
                .formKey(task.getFormKey())
                .variables(variables)
                .businessKey(businessKey)
                .build();
    }
}
