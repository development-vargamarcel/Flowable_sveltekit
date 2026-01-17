package com.demo.bpm.controller;

import com.demo.bpm.dto.SlaStatsDTO;
import com.demo.bpm.entity.SLA;
import com.demo.bpm.service.SLAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/slas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SLAController {

    private final SLAService slaService;

    @PostMapping
    public ResponseEntity<Void> createOrUpdateSLA(
            @RequestParam String name,
            @RequestParam String targetKey,
            @RequestParam SLA.SLATargetType targetType,
            @RequestParam String duration, // ISO-8601 format (e.g. PT4H)
            @RequestParam(required = false) Integer warningThreshold) {
        
        slaService.createOrUpdateSLA(name, targetKey, targetType, Duration.parse(duration), warningThreshold);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check")
    public ResponseEntity<Void> checkBreaches() {
        slaService.checkSLABreaches();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<SlaStatsDTO> getStats() {
        return ResponseEntity.ok(slaService.getSLAStats());
    }
}
