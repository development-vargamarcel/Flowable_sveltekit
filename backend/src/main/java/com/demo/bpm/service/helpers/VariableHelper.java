package com.demo.bpm.service.helpers;

import com.demo.bpm.dto.DocumentDTO;
import com.demo.bpm.service.BusinessTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VariableHelper {

    private final RuntimeService runtimeService;
    private final org.flowable.engine.HistoryService historyService;
    private final BusinessTableService businessTableService;

    /**
     * Get merged variables from both Flowable (system vars) and business tables (business data).
     *
     * @param processInstanceId the process instance ID
     * @return a map of merged variables
     */
    public Map<String, Object> getMergedVariables(String processInstanceId) {
        Map<String, Object> mergedVars = new HashMap<>();

        // Get system variables from Flowable (variables starting with _)
        try {
            Map<String, Object> flowableVars = runtimeService.getVariables(processInstanceId);
            if (flowableVars != null) {
                mergedVars.putAll(flowableVars);
            }
        } catch (Exception e) {
            log.debug("Could not get Flowable variables, checking historic variables: {}", e.getMessage());
            try {
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .list()
                        .forEach(var -> mergedVars.put(var.getVariableName(), var.getValue()));
            } catch (Exception e2) {
                 log.debug("Could not get historic variables: {}", e2.getMessage());
            }
        }

        // Get business data from document table
        try {
            Optional<DocumentDTO> documentOpt = businessTableService.getDocument(processInstanceId, "main");
            if (documentOpt.isPresent()) {
                DocumentDTO document = documentOpt.get();

                // Add document fields
                if (document.getFields() != null) {
                    mergedVars.putAll(document.getFields());
                }

                // Add grid data
                if (document.getGrids() != null) {
                    mergedVars.putAll(document.getGrids());
                }
            }
        } catch (Exception e) {
            log.debug("Could not get document data: {}", e.getMessage());
        }

        return mergedVars;
    }
}
