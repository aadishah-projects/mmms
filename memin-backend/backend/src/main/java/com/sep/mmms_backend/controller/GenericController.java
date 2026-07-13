package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class GenericController {

    private final AppUserService appUserService;

    public GenericController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/isAuthenticated")
    public ResponseEntity<Response> isAuthenticated(Authentication authentication) {

        if (authentication != null && !authentication.getName().equals("anonymous") && !authentication.getName().equals("anonymousUser")) {
            if (authentication.isAuthenticated()) {
                log.info("The user: {} is authenticated", authentication.getName());
                com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(authentication.getName());
                java.util.Map<String, Object> data = java.util.Map.of("role", user.getRole().name());
                return ResponseEntity.ok(new Response("true", data));
            }
        }
        return new ResponseEntity<Response>(new Response("false"), HttpStatus.UNAUTHORIZED);
    }
}
