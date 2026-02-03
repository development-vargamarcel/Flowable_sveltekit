package com.demo.bpm.service;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.exception.InvalidOperationException;
import com.demo.bpm.exception.ResourceNotFoundException;
import com.demo.bpm.repository.ProcessConfigRepository;
import com.demo.bpm.util.VariableStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final ProcessConfigRepository processConfigRepository;
    private final BusinessTableService businessTableService;

    public List<ProcessDTO> getAvailableProcesses() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProcessDTO getProcessById(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();

        if (processDefinition == null) {
            throw new ResourceNotFoundException("Process not found with id: " + processDefinitionId);
        }

        return convertToDTO(processDefinition);
    }

    public List<ProcessDTO> getAllProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProcessInstanceDTO startProcess(String processKey, String businessKey,
                                           Map<String, Object> variables, String userId) {
        Map<String, Object> allVars = new HashMap<>(variables != null ? variables : Map.of());

        allVars.put("_initiator", userId);
        allVars.put("_startedBy", userId);
        allVars.put("_startedAt", java.time.LocalDateTime.now().toString());

        String finalBusinessKey = businessKey != null ? businessKey
                : processKey.toUpperCase() + "-" + System.currentTimeMillis();

        Map<String, Object> systemVars = VariableStorageUtil.filterSystemVariables(allVars);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processKey,
                finalBusinessKey,
                systemVars
        );

        log.info("Started process {} with business key {} by user {}. System vars: {}, Total vars: {}",
                processKey, finalBusinessKey, userId, systemVars.size(), allVars.size());

        if (!allVars.isEmpty()) {
            saveBusinessData(processKey, finalBusinessKey, instance, allVars, userId);
        }

        return getProcessInstance(instance.getId());
    }

    private void saveBusinessData(String processKey, String businessKey, ProcessInstance instance, Map<String, Object> allVars, String userId) {
        Optional<ProcessConfig> config = processConfigRepository.findByProcessDefinitionKey(processKey);
        String documentType = config.map(ProcessConfig::getDocumentType).orElse(null);

        businessTableService.saveAllData(
                instance.getId(),
                businessKey,
                processKey,
                instance.getProcessDefinitionName(),
                documentType,
                allVars,
                userId
        );
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

            return buildProcessInstanceDTO(
                    instance.getId(),
                    instance.getProcessDefinitionId(),
                    instance.getProcessDefinitionKey(),
                    definition != null ? definition.getName() : null,
                    instance.getBusinessKey(),
                    instance.getStartTime(),
                    (String) variables.get("_startedBy"),
                    variables,
                    false
            );
        }

        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            throw new RuntimeException("Process instance not found: " + processInstanceId);
        }

        return buildProcessInstanceDTO(
                historicInstance.getId(),
                historicInstance.getProcessDefinitionId(),
                historicInstance.getProcessDefinitionKey(),
                historicInstance.getProcessDefinitionName(),
                historicInstance.getBusinessKey(),
                historicInstance.getStartTime(),
                historicInstance.getStartUserId(),
                null, // Variables usually not needed or expensive to fetch for history here, unless specifically requested
                historicInstance.getEndTime() != null
        );
    }

    private ProcessInstanceDTO buildProcessInstanceDTO(String id, String definitionId, String definitionKey,
                                                       String definitionName, String businessKey, java.util.Date startTime,
                                                       String startUserId, Map<String, Object> variables, boolean ended) {
        return ProcessInstanceDTO.builder()
                .id(id)
                .processDefinitionId(definitionId)
                .processDefinitionKey(definitionKey)
                .processDefinitionName(definitionName)
                .businessKey(businessKey)
                .startTime(startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .startUserId(startUserId)
                .variables(variables)
                .ended(ended)
                .build();
    }

    public Page<ProcessInstanceDTO> getActiveProcesses(String userId, Pageable pageable) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("_startedBy", userId)
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        long total = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("_startedBy", userId)
                .count();

        List<ProcessInstanceDTO> dtos = instances.stream()
                .map(instance -> getProcessInstance(instance.getId()))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }

    private ProcessDTO convertToDTO(ProcessDefinition definition) {
        return ProcessDTO.builder()
                .id(definition.getId())
                .key(definition.getKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .version(definition.getVersion())
                .category(definition.getCategory())
                .suspended(definition.isSuspended())
                .build();
    }

    @Transactional
    public ProcessDTO deployProcess(String processName, String bpmnXml) {
        try {
            String deploymentName = processName + "-" + System.currentTimeMillis();

            org.flowable.engine.repository.Deployment deployment = repositoryService
                    .createDeployment()
                    .name(deploymentName)
                    .addString(processName + ".bpmn20.xml", bpmnXml)
                    .deploy();

            log.info("Deployed process definition: {}", deploymentName);

            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            String documentType = extractDocumentTypeFromBpmn(definition.getId());
            updateProcessConfig(definition.getKey(), documentType);

            return convertToDTO(definition);
        } catch (Exception e) {
            log.error("Error deploying process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy process: " + e.getMessage(), e);
        }
    }

    private String extractDocumentTypeFromBpmn(String processDefinitionId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
        String documentType = null;

        if (process.getAttributes().containsKey("documentType")) {
            List<ExtensionAttribute> attributes = process.getAttributes().get("documentType");
            if (!attributes.isEmpty()) {
                documentType = attributes.get(0).getValue();
            }
        } else if (process.getAttributes().containsKey("flowable:documentType")) {
             List<ExtensionAttribute> attributes = process.getAttributes().get("flowable:documentType");
             if (!attributes.isEmpty()) {
                 documentType = attributes.get(0).getValue();
             }
        } else {
             List<ExtensionAttribute> attributes = process.getAttributes().values().stream()
                 .flatMap(List::stream)
                 .filter(a -> "documentType".equals(a.getName()))
                 .collect(Collectors.toList());
             if (!attributes.isEmpty()) {
                 documentType = attributes.get(0).getValue();
             }
        }
        return documentType;
    }

    private void updateProcessConfig(String processDefinitionKey, String documentType) {
        ProcessConfig config = processConfigRepository.findByProcessDefinitionKey(processDefinitionKey)
                .orElse(ProcessConfig.builder()
                        .processDefinitionKey(processDefinitionKey)
                        .build());

        if (documentType != null && !documentType.isEmpty()) {
            config.setDocumentType(documentType);
            processConfigRepository.save(config);
            log.info("Updated ProcessConfig for {} with documentType: {}", processDefinitionKey, documentType);
        }
    }

    public void deleteProcessDefinition(String processDefinitionId, boolean cascade) {
        try {
            repositoryService.deleteDeployment(
                    repositoryService.createProcessDefinitionQuery()
                            .processDefinitionId(processDefinitionId)
                            .singleResult()
                            .getDeploymentId(),
                    cascade
            );
            log.info("Deleted process definition: {}", processDefinitionId);
        } catch (Exception e) {
            log.error("Error deleting process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete process: " + e.getMessage(), e);
        }
    }

    public String getProcessDefinitionBpmn(String processDefinitionId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (definition == null) {
                throw new RuntimeException("Process definition not found: " + processDefinitionId);
            }

            java.io.InputStream bpmnStream = repositoryService.getProcessModel(processDefinitionId);
            return new String(bpmnStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error retrieving BPMN for process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve process BPMN: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void suspendProcessDefinition(String processDefinitionId) {
        repositoryService.suspendProcessDefinitionById(processDefinitionId);
        log.info("Suspended process definition: {}", processDefinitionId);
    }

    @Transactional
    public void activateProcessDefinition(String processDefinitionId) {
        repositoryService.activateProcessDefinitionById(processDefinitionId);
        log.info("Activated process definition: {}", processDefinitionId);
    }

    @Transactional
    public void updateProcessDefinitionCategory(String processDefinitionId, String category) {
        repositoryService.setProcessDefinitionCategory(processDefinitionId, category);
        log.info("Updated category for process definition {}: {}", processDefinitionId, category);
    }

    @Transactional
    public void cancelProcessInstance(String processInstanceId, String reason, String userId, boolean isAdmin) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (instance == null) {
            throw new ResourceNotFoundException("Active process instance not found: " + processInstanceId);
        }

        String startedBy = (String) runtimeService.getVariable(processInstanceId, "_startedBy");

        if (!isAdmin && (startedBy == null || !startedBy.equals(userId))) {
            throw new InvalidOperationException("You are not authorized to cancel this process instance.");
        }

        runtimeService.deleteProcessInstance(processInstanceId, reason != null ? reason : "Cancelled by user");
        log.info("Process instance {} cancelled by user {} (Admin: {}). Reason: {}", processInstanceId, userId, isAdmin, reason);
    }
}
