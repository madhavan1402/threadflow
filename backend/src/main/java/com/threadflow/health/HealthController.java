package com.threadflow.health;

import com.threadflow.common.dto.HealthResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DatabaseHealthService databaseHealthService;

    public HealthController(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        boolean databaseUp = databaseHealthService.isUp();
        String status = databaseUp ? "UP" : "DOWN";
        HealthResponse response = new HealthResponse(
                status,
                "threadflow-api",
                Instant.now(),
                new HealthResponse.DatabaseHealth(status, databaseHealthService.vendor())
        );
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
