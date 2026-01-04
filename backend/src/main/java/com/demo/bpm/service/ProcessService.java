package com.demo.bpm.service;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.entity.Document;
import com.demo.bpm.entity.ProcessConfig;
import com.demo.bpm.repository.DocumentRepository;
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
    private final DocumentRepository documentRepository;
    private final BusinessTableService businessTableService;

    public List<ProcessDTO> getAvailableProcesses() {
        // Return ALL processes (all versions) if needed, but usually we just want active or latest
        // For management, we might want all.
        // The previous implementation returned latest versions only.
        // We'll stick to latest for "available to start", but add a method for "all definitions"
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // New method to get ALL definitions including older versions
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
        // Collect all variables (for business table storage if needed later)
        Map<String, Object> allVars = new HashMap<>(variables != null ? variables : Map.of());

        // Add system metadata with _ prefix so they're stored in Flowable
        allVars.put("_initiator", userId);
        allVars.put("_startedBy", userId);
        allVars.put("_startedAt", java.time.LocalDateTime.now().toString());

        String finalBusinessKey = businessKey != null ? businessKey
                : processKey.toUpperCase() + "-" + System.currentTimeMillis();

        // Only pass system variables (starting with _) to Flowable
        // Business variables will be stored in document/grid_rows tables only
        Map<String, Object> systemVars = VariableStorageUtil.filterSystemVariables(allVars);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processKey,
                finalBusinessKey,
                systemVars
        );

        log.info("Started process {} with business key {} by user {}. System vars: {}, Total vars: {}",
                processKey, finalBusinessKey, userId, systemVars.size(), allVars.size());

        // Save business variables if provided
        if (!allVars.isEmpty()) {
            Optional<ProcessConfig> config = processConfigRepository.findByProcessDefinitionKey(processKey);
            String documentType = config.map(ProcessConfig::getDocumentType).orElse(null);

            businessTableService.saveAllData(
                    instance.getId(),
                    finalBusinessKey,
                    processKey,
                    instance.getProcessDefinitionName(),
                    documentType,
                    allVars,
                    userId
            );
        }

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
                    .startUserId((String) variables.get("_startedBy"))
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

            // Get the deployed process definition
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            // Extract documentType from BPMN
            BpmnModel bpmnModel = repositoryService.getBpmnModel(definition.getId());
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
                // Try to find attribute with namespace
                 List<ExtensionAttribute> attributes = process.getAttributes().values().stream()
                     .flatMap(List::stream)
                     .filter(a -> "documentType".equals(a.getName()))
                     .collect(Collectors.toList());
                 if (!attributes.isEmpty()) {
                     documentType = attributes.get(0).getValue();
                 }
            }

            // Save/Update ProcessConfig
            ProcessConfig config = processConfigRepository.findByProcessDefinitionKey(definition.getKey())
                    .orElse(ProcessConfig.builder()
                            .processDefinitionKey(definition.getKey())
                            .build());

            if (documentType != null && !documentType.isEmpty()) {
                config.setDocumentType(documentType);
                processConfigRepository.save(config);
                log.info("Updated ProcessConfig for {} with documentType: {}", definition.getKey(), documentType);
            }

            return convertToDTO(definition);
        } catch (Exception e) {
            log.error("Error deploying process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy process: " + e.getMessage(), e);
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
}
