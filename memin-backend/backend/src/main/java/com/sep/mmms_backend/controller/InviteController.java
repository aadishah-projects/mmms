package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.InviteRequestDto;
import com.sep.mmms_backend.dto.InviteDetailsDto;
import com.sep.mmms_backend.dto.RegisterWithTokenDto;
import com.sep.mmms_backend.entity.InviteToken;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Errors;
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

    @GetMapping("/invite/{token}")
    public ResponseEntity<Response> getInvite(@PathVariable String token) {
        InviteToken inviteToken = inviteService.getValidInvite(token);
        return ResponseEntity.ok(new Response("Invite loaded successfully", new InviteDetailsDto(inviteToken)));
    }

    @PostMapping("/register-with-token")
    public ResponseEntity<Response> registerWithToken(
            @RequestBody @Valid RegisterWithTokenDto requestDto,
            Errors errors) {
        if (errors.hasErrors()) {
            throw new com.sep.mmms_backend.exceptions.ValidationFailureException(
                    com.sep.mmms_backend.exceptions.ExceptionMessages.VALIDATION_FAILED,
                    errors
            );
        }
        inviteService.consumeInvite(requestDto);
        return ResponseEntity.ok(new Response("User registered successfully"));
    }
}
