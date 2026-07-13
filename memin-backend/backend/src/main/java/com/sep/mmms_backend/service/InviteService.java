package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.InviteRequestDto;
import com.sep.mmms_backend.dto.RegisterWithTokenDto;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.InviteToken;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.InviteTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteService {

    private final InviteTokenRepository inviteTokenRepository;
    private final EmailService emailService;
    private final AppUserService appUserService;
    private final CommitteeService committeeService;
    
    public InviteService(InviteTokenRepository inviteTokenRepository, EmailService emailService, AppUserService appUserService, CommitteeService committeeService) {
        this.inviteTokenRepository = inviteTokenRepository;
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.committeeService = committeeService;
    }

    @Transactional
    public void createInvite(InviteRequestDto requestDto, String inviterUsername) {
        Committee committee = null;
        if (requestDto.getCommitteeId() != null) {
            committee = committeeService.findCommitteeById(requestDto.getCommitteeId());
        }

        Optional<InviteToken> existing = inviteTokenRepository.findByEmailAndUsedFalse(requestDto.getEmail());
        if (existing.isPresent()) {
            InviteToken token = existing.get();
            token.setUsed(true);
            inviteTokenRepository.save(token);
        }

        InviteToken inviteToken = InviteToken.builder()
                .token(UUID.randomUUID().toString())
                .email(requestDto.getEmail())
                .invitedBy(inviterUsername)
                .role(requestDto.getRole())
                .committee(committee)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();

        inviteTokenRepository.save(inviteToken);
        
        AppUser inviter = appUserService.loadUserByUsername(inviterUsername);
        String inviterName = inviter.getFirstName() + " " + inviter.getLastName();
        emailService.sendInviteEmail(requestDto.getEmail(), inviteToken.getToken(), inviterName);
    }

    @Transactional
    public void consumeInvite(RegisterWithTokenDto requestDto) {
        InviteToken inviteToken = inviteTokenRepository.findByToken(requestDto.getToken())
                .orElseThrow(() -> new IllegalOperationException("Invalid invite token"));

        if (inviteToken.getUsed() || inviteToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalOperationException("Invite token is expired or already used");
        }

        AppUser newUser = new AppUser();
        newUser.setFirstName(requestDto.getFirstName());
        newUser.setLastName(requestDto.getLastName());
        newUser.setUsername(requestDto.getUsername());
        newUser.setEmail(inviteToken.getEmail());
        newUser.setPassword(requestDto.getPassword());
        newUser.setConfirmPassword(requestDto.getConfirmPassword());
        newUser.setRole(inviteToken.getRole());

        appUserService.saveNewUser(newUser);

        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);
    }
}
