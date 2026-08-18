package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.AiConfigurationUpdateDto;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.AiConfigurationService;
import com.sep.mmms_backend.service.AiMinuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiConfigurationController {
    private final AiConfigurationService configurationService;
    private final AiMinuteService aiMinuteService;

    public AiConfigurationController(
            AiConfigurationService configurationService,
            AiMinuteService aiMinuteService) {
        this.configurationService = configurationService;
        this.aiMinuteService = aiMinuteService;
    }

    @GetMapping("/api/settings/ai")
    public ResponseEntity<Response> getConfiguration() {
        return ResponseEntity.ok(new Response("AI configuration loaded", configurationService.getConfiguration()));
    }

    @PutMapping("/api/settings/ai")
    public ResponseEntity<Response> updateConfiguration(
            @RequestBody AiConfigurationUpdateDto request,
            Authentication authentication) {
        configurationService.updateConfiguration(request, authentication.getName());
        return ResponseEntity.ok(new Response("AI configuration saved", configurationService.getConfiguration()));
    }

    @PostMapping("/api/settings/ai/test")
    public ResponseEntity<Response> testConnection(@RequestBody AiConfigurationUpdateDto request) {
        var result = aiMinuteService.testConnection(request);
        return ResponseEntity.ok(new Response(result.isSuccess()
                ? "AI connection test succeeded"
                : "AI connection test failed", result));
    }

    @DeleteMapping("/api/settings/ai")
    public ResponseEntity<Response> clearConfiguration() {
        configurationService.clearSavedConfiguration();
        return ResponseEntity.ok(new Response("Saved AI configuration cleared", configurationService.getConfiguration()));
    }
}
