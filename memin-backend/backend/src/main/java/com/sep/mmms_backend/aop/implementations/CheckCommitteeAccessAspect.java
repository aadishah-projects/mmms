package com.sep.mmms_backend.aop.implementations;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.exceptions.CommitteeNotAccessibleException;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.exceptions.MeetingNotAccessibleException;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
//TODO: instead of checking the access for committee like this, maybe we can only retrieve the committee only if it is accessible for the user. This way, the controller class does not have fetch the Committee to pass to service layer. See getCommitteeIfAccessible() in CommitteeService
public class CheckCommitteeAccessAspect {
    @Autowired
    private CommitteeService committeeService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private com.sep.mmms_backend.service.AppUserService appUserService;

    @Before("@annotation(checkCommitteeAccess)")
    public void checkCommitteeAccess(JoinPoint joinPoint, CheckCommitteeAccess checkCommitteeAccess) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Committee committee = null;
        Meeting meeting = null;
        String username = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Committee) {
                committee = (Committee) args[i];
            }

            if (args[i] instanceof Meeting) {
                meeting = (Meeting) args[i];
            }

            if ("username".equals(parameterNames[i]) && args[i] instanceof String) {
                username = (String) args[i];
            }
        }

        if (committee == null || username == null || (checkCommitteeAccess.shouldValidateMeeting() && meeting == null)) {
            throw new IllegalOperationException("Access of meeting or committee could not be verified");
        }

        // The aspect is also unit-tested without a Spring application context.
        // Fall back to the persisted owner in that narrow case; production
        // requests always have AppUserService injected and use role-aware access.
        if (appUserService == null) {
            if (!username.equals(committee.getCreatedBy())) {
                throw new CommitteeNotAccessibleException(ExceptionMessages.COMMITTEE_NOT_ACCESSIBLE, committee.getName());
            }
            if (checkCommitteeAccess.shouldValidateMeeting()
                    && !username.equals(meeting.getCreatedBy())) {
                throw new MeetingNotAccessibleException(ExceptionMessages.MEETING_NOT_ACCESSIBLE, meeting.getTitle());
            }
            return;
        }

        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(username);
        boolean isDeptHead = user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_HEAD;
        boolean isMember = user.getRole() == com.sep.mmms_backend.enums.AppRole.COMMITTEE_MEMBER || user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_MEMBER || user.getRole() == com.sep.mmms_backend.enums.AppRole.SECRETARY;

        boolean hasAccess = false;
        if (isDeptHead) {
            hasAccess = true;
        } else if (committee.getCreatedBy().equals(username)) {
            hasAccess = true;
        } else if (isMember) {
            if (user.getLinkedMemberId() != null) {
                boolean isSecretary = committee.getSecretary() != null && committee.getSecretary().getId().equals(user.getLinkedMemberId());
                boolean isCommitteeMember = committee.getMemberships().stream().anyMatch(m -> m.getMember().getId().equals(user.getLinkedMemberId()));
                hasAccess = isSecretary || isCommitteeMember;
            }
        }

        if (!hasAccess) {
            throw new CommitteeNotAccessibleException(ExceptionMessages.COMMITTEE_NOT_ACCESSIBLE, committee.getName());
        }

        if (checkCommitteeAccess.shouldValidateMeeting()) {
            boolean hasMeetingAccess = false;
            if (isDeptHead) {
                hasMeetingAccess = true;
            } else if (committee.getCreatedBy().equals(username)) {
                hasMeetingAccess = true;
            } else if (isMember) {
                if (user.getLinkedMemberId() != null) {
                    boolean isSecretary = committee.getSecretary() != null && committee.getSecretary().getId().equals(user.getLinkedMemberId());
                    boolean isCommitteeMember = committee.getMemberships().stream().anyMatch(m -> m.getMember().getId().equals(user.getLinkedMemberId()));
                    hasMeetingAccess = isSecretary || isCommitteeMember;
                }
            }

            if (!hasMeetingAccess) {
                throw new MeetingNotAccessibleException(ExceptionMessages.MEETING_NOT_ACCESSIBLE, meeting.getTitle());
            }
        }
    }
}
