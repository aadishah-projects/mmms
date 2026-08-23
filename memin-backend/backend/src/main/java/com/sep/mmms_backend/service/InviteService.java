package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.InviteRequestDto;
import com.sep.mmms_backend.dto.RegisterWithTokenDto;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.InviteToken;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.CommitteeMembershipRepository;
import com.sep.mmms_backend.repository.InviteTokenRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteService {

    private final InviteTokenRepository inviteTokenRepository;
    private final EmailService emailService;
    private final AppUserService appUserService;
    private final CommitteeService committeeService;
    private final MemberRepository memberRepository;
    private final CommitteeMembershipRepository committeeMembershipRepository;
    
    public InviteService(InviteTokenRepository inviteTokenRepository, EmailService emailService, AppUserService appUserService, CommitteeService committeeService, MemberRepository memberRepository, CommitteeMembershipRepository committeeMembershipRepository) {
        this.inviteTokenRepository = inviteTokenRepository;
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.committeeService = committeeService;
        this.memberRepository = memberRepository;
        this.committeeMembershipRepository = committeeMembershipRepository;
    }

    @Transactional
    public void createInvite(InviteRequestDto requestDto, String inviterUsername) {
        String email = requestDto.getEmail().trim().toLowerCase();

        if (appUserService.emailExists(email)) {
            throw new IllegalOperationException("An account already exists for this email address");
        }

        Committee committee = null;
        if (requestDto.getCommitteeId() != null) {
            committee = committeeService.getCommitteeIfAccessible(requestDto.getCommitteeId(), inviterUsername);
        }

        Optional<InviteToken> existing = inviteTokenRepository.findByEmailAndUsedFalse(email);
        if (existing.isPresent()) {
            InviteToken token = existing.get();
            token.setUsed(true);
            inviteTokenRepository.save(token);
        }

        InviteToken inviteToken = InviteToken.builder()
                .token(UUID.randomUUID().toString())
                .email(email)
                .invitedBy(inviterUsername)
                .role(requestDto.getRole())
                .committee(committee)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();

        inviteTokenRepository.save(inviteToken);
        
        AppUser inviter = appUserService.loadUserByUsername(inviterUsername);
        String inviterName = inviter.getFirstName() + " " + inviter.getLastName();
        if (!emailService.sendInviteEmail(email, inviteToken.getToken(), inviterName)) {
            throw new IllegalOperationException("Could not send the invitation email. Configure the application's SMTP settings and try again.");
        }
    }

    @Transactional(readOnly = true)
    public InviteToken getValidInvite(String token) {
        InviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalOperationException("Invalid invite token"));

        if (inviteToken.getUsed() || inviteToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalOperationException("Invite token is expired or already used");
        }
        if (inviteToken.getCommittee() != null) {
            inviteToken.getCommittee().getName();
        }
        return inviteToken;
    }

    @Transactional
    public void consumeInvite(RegisterWithTokenDto requestDto) {
        InviteToken inviteToken = getValidInvite(requestDto.getToken());

        AppUser newUser = new AppUser();
        newUser.setFirstName(requestDto.getFirstName());
        newUser.setLastName(requestDto.getLastName());
        newUser.setUsername(requestDto.getUsername());
        newUser.setEmail(inviteToken.getEmail());
        newUser.setPassword(requestDto.getPassword());
        newUser.setConfirmPassword(requestDto.getConfirmPassword());
        newUser.setRole(inviteToken.getRole());

        Member linkedMember = memberRepository.findFirstByEmailIgnoreCase(inviteToken.getEmail()).orElse(null);
        if (inviteToken.getCommittee() != null && linkedMember == null) {
            linkedMember = createMemberForInvite(inviteToken, requestDto);
        } else if (linkedMember != null) {
            applyRegistrationDetails(linkedMember, requestDto);
            linkedMember = memberRepository.save(linkedMember);
        }
        if (linkedMember != null) {
            newUser.setLinkedMemberId(linkedMember.getId());
        }

        appUserService.saveNewUser(newUser);

        if (inviteToken.getCommittee() != null) {
            addMemberToCommittee(inviteToken.getCommittee(), linkedMember);
        }

        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);
    }

    private Member createMemberForInvite(InviteToken inviteToken, RegisterWithTokenDto requestDto) {
        LocalDate today = LocalDate.now();
        Member member = new Member();
        applyRegistrationDetails(member, requestDto);
        member.setEmail(inviteToken.getEmail());
        member.setCreatedBy(inviteToken.getInvitedBy());
        member.setCreatedDate(today);
        member.setModifiedBy(inviteToken.getInvitedBy());
        member.setModifiedDate(today);
        return memberRepository.save(member);
    }

    private void applyRegistrationDetails(Member member, RegisterWithTokenDto requestDto) {
        member.setFirstName(requestDto.getFirstName());
        member.setLastName(requestDto.getLastName());
        member.setFirstNameNepali(requestDto.getFirstNameNepali());
        member.setLastNameNepali(requestDto.getLastNameNepali());
        member.setTitle(requestDto.getTitle());
        member.setPost(requestDto.getPost());
    }

    private void addMemberToCommittee(Committee committee, Member member) {
        boolean alreadyMember = committeeMembershipRepository
                .findMembershipBetweenCommitteeAndMember(committee.getId(), member.getId())
                .isPresent();
        if (alreadyMember) {
            return;
        }

        CommitteeMembership membership = new CommitteeMembership();
        membership.setCommittee(committee);
        membership.setMember(member);
        membership.setRole("Member");
        membership.setOrder(committee.getMemberships().size() + 1);
        committeeMembershipRepository.save(membership);
    }
}
