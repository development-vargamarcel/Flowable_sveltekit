package com.demo.bpm.service;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    public List<ProcessDTO> getAvailableProcesses() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProcessInstanceDTO startProcess(String processKey, String businessKey,
                                           Map<String, Object> variables, String userId) {
        Map<String, Object> processVars = new HashMap<>(variables != null ? variables : Map.of());
        processVars.put("initiator", userId);
        processVars.put("startedBy", userId);
        processVars.put("startedAt", java.time.LocalDateTime.now().toString());

        String finalBusinessKey = businessKey != null ? businessKey
                : processKey.toUpperCase() + "-" + System.currentTimeMillis();

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processKey,
                finalBusinessKey,
                processVars
        );

        log.info("Started process {} with business key {} by user {}",
                processKey, finalBusinessKey, userId);

        return getProcessInstance(instance.getId());
    }

    public ProcessInstanceDTO getProcessInstance(String processInstanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (instance != null) {
            Map<String, Object> variables = runtimeService.getVariables(processInstanceId);

            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(instance.getProcessDefinitionId())
                    .singleResult();

            return ProcessInstanceDTO.builder()
                    .id(instance.getId())
                    .processDefinitionKey(instance.getProcessDefinitionKey())
                    .processDefinitionName(definition != null ? definition.getName() : null)
                    .businessKey(instance.getBusinessKey())
                    .startTime(instance.getStartTime().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .startUserId((String) variables.get("startedBy"))
                    .variables(variables)
                    .ended(false)
                    .build();
        }

        // Check history if not found in runtime
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            throw new RuntimeException("Process instance not found: " + processInstanceId);
        }

        return ProcessInstanceDTO.builder()
                .id(historicInstance.getId())
                .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                .processDefinitionName(historicInstance.getProcessDefinitionName())
                .businessKey(historicInstance.getBusinessKey())
                .startTime(historicInstance.getStartTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .startUserId(historicInstance.getStartUserId())
                .ended(historicInstance.getEndTime() != null)
                .build();
    }

    public List<ProcessInstanceDTO> getActiveProcesses(String userId) {
        return runtimeService.createProcessInstanceQuery()
                .variableValueEquals("startedBy", userId)
                .orderByStartTime().desc()
                .list()
                .stream()
                .map(instance -> getProcessInstance(instance.getId()))
                .collect(Collectors.toList());
    }

    private ProcessDTO convertToDTO(ProcessDefinition definition) {
        return ProcessDTO.builder()
                .key(definition.getKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .version(definition.getVersion())
                .build();
    }
}
