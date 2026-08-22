package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.SystemSettingsDto;
import com.sep.mmms_backend.dto.TestAiRequestDto;
import com.sep.mmms_backend.dto.TestEmailRequestDto;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping
    public ResponseEntity<Response> getSettings() {
        SystemSettingsDto settings = systemSettingService.getSystemSettingsDto();
        return ResponseEntity.ok(new Response("System settings loaded successfully", settings));
    }

    @PutMapping
    public ResponseEntity<Response> updateSettings(
            @RequestBody SystemSettingsDto settingsDto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        SystemSettingsDto updated = systemSettingService.updateSystemSettings(settingsDto, username);
        return ResponseEntity.ok(new Response("System settings saved successfully", updated));
    }

    @PostMapping("/test-email")
    public ResponseEntity<Response> testEmail(
            @Valid @RequestBody TestEmailRequestDto request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "department head";
        systemSettingService.testEmail(request.getToEmail(), username);
        return ResponseEntity.ok(new Response("Test email sent successfully to " + request.getToEmail()));
    }

    @PostMapping("/test-ai")
    public ResponseEntity<Response> testAi(
            @RequestBody(required = false) TestAiRequestDto request) {
        String prompt = request != null ? request.getPrompt() : null;
        String reply = systemSettingService.testAiConnection(prompt);
        return ResponseEntity.ok(new Response("AI connection verified successfully", Map.of("reply", reply)));
    }
}
