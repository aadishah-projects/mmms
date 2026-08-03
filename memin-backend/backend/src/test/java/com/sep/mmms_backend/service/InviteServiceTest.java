package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.RegisterWithTokenDto;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.InviteToken;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.AppRole;
import com.sep.mmms_backend.repository.CommitteeMembershipRepository;
import com.sep.mmms_backend.repository.InviteTokenRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteTokenRepository inviteTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AppUserService appUserService;
    @Mock
    private CommitteeService committeeService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CommitteeMembershipRepository committeeMembershipRepository;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(
                inviteTokenRepository,
                emailService,
                appUserService,
                committeeService,
                memberRepository,
                committeeMembershipRepository
        );
    }

    @Test
    void consumeInviteCreatesMemberAndCommitteeMembership() {
        Committee committee = new Committee();
        committee.setId(12);
        committee.setName("Academic Committee");

        InviteToken inviteToken = InviteToken.builder()
                .token("invite-token")
                .email("new.member@pcampus.edu.np")
                .invitedBy("departmentHead")
                .role(AppRole.COMMITTEE_MEMBER)
                .committee(committee)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(inviteTokenRepository.findByToken("invite-token")).thenReturn(Optional.of(inviteToken));
        when(memberRepository.findFirstByEmailIgnoreCase(inviteToken.getEmail())).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId(34);
            return member;
        });
        when(committeeMembershipRepository.findMembershipBetweenCommitteeAndMember(12, 34))
                .thenReturn(Optional.empty());
        when(appUserService.saveNewUser(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterWithTokenDto request = new RegisterWithTokenDto();
        request.setToken("invite-token");
        request.setFirstName("New");
        request.setLastName("Member");
        request.setUsername("new_member");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        inviteService.consumeInvite(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserService).saveNewUser(userCaptor.capture());
        assertEquals(34, userCaptor.getValue().getLinkedMemberId());

        ArgumentCaptor<CommitteeMembership> membershipCaptor =
                ArgumentCaptor.forClass(CommitteeMembership.class);
        verify(committeeMembershipRepository).save(membershipCaptor.capture());
        assertEquals(committee, membershipCaptor.getValue().getCommittee());
        assertEquals(34, membershipCaptor.getValue().getMember().getId());
        assertEquals("Member", membershipCaptor.getValue().getRole());
        verify(inviteTokenRepository).save(inviteToken);
    }
}
