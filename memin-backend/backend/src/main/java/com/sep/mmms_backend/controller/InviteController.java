package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.InviteRequestDto;
import com.sep.mmms_backend.dto.RegisterWithTokenDto;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/invite")
    public ResponseEntity<Response> sendInvite(@RequestBody @Valid InviteRequestDto requestDto, Authentication authentication) {
        inviteService.createInvite(requestDto, authentication.getName());
        return ResponseEntity.ok(new Response("Invite sent successfully"));
    }

    @PostMapping("/register-with-token")
    public ResponseEntity<Response> registerWithToken(@RequestBody RegisterWithTokenDto requestDto) {
        inviteService.consumeInvite(requestDto);
        return ResponseEntity.ok(new Response("User registered successfully"));
    }
}
