package com.demo.bpm.controller;

import com.demo.bpm.dto.ProcessDTO;
import com.demo.bpm.dto.ProcessInstanceDTO;
import com.demo.bpm.dto.StartProcessRequest;
import com.demo.bpm.service.ProcessService;
import com.demo.bpm.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ProcessDTO>> getAvailableProcesses() {
        List<ProcessDTO> processes = processService.getAvailableProcesses();
        return ResponseEntity.ok(processes);
    }

    @PostMapping("/{processKey}/start")
    public ResponseEntity<?> startProcess(
            @PathVariable String processKey,
            @RequestBody(required = false) StartProcessRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String businessKey = request != null ? request.getBusinessKey() : null;
            Map<String, Object> variables = request != null ? request.getVariables() : Map.of();

            // Add user info to variables
            var userInfo = userService.getUserInfo(userDetails);
            variables.put("employeeId", userDetails.getUsername());
            variables.put("employeeName", userInfo.getDisplayName());

            ProcessInstanceDTO instance = processService.startProcess(
                    processKey,
                    businessKey,
                    variables,
                    userDetails.getUsername()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Process started successfully",
                    "processInstance", instance
            ));
        } catch (Exception e) {
            log.error("Error starting process {}: {}", processKey, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/instance/{processInstanceId}")
    public ResponseEntity<?> getProcessInstance(@PathVariable String processInstanceId) {
        try {
            ProcessInstanceDTO instance = processService.getProcessInstance(processInstanceId);
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-processes")
    public ResponseEntity<List<ProcessInstanceDTO>> getMyProcesses(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProcessInstanceDTO> processes = processService.getActiveProcesses(userDetails.getUsername());
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
