package com.orbitalwatcher.controller;

import com.orbitalwatcher.dto.IssTelemetryResponse;
import com.orbitalwatcher.service.TelemetryAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssTelemetryController {

    private final TelemetryAggregatorService telemetryAggregatorService;

    @GetMapping("/iss-telemetry")
    public ResponseEntity<IssTelemetryResponse> getIssTelemetry() {
        return ResponseEntity.ok(telemetryAggregatorService.getTelemetry());
    }
}
