package com.demo.bpm.mapper;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;

@Component
public class ProcessMapper {

    public ProcessDTO toDTO(ProcessDefinition definition) {
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

    public ProcessInstanceDTO toInstanceDTO(String id, String definitionId, String definitionKey,
                                             String definitionName, String businessKey, java.util.Date startTime,
                                             String startUserId, Map<String, Object> variables, boolean ended, boolean suspended) {
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
                .suspended(suspended)
                .build();
    }
}
