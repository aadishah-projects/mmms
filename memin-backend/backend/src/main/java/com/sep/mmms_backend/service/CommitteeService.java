package com.sep.mmms_backend.service;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.CommitteeStatus;
import com.sep.mmms_backend.exceptions.*;
import com.sep.mmms_backend.repository.CommitteeMembershipRepository;
import com.sep.mmms_backend.repository.CommitteeRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.validators.EntityValidator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommitteeService {

    private final CommitteeRepository committeeRepository;
    private final EntityValidator entityValidator;
    private final MemberRepository memberRepository;
    private final AppUserService appUserService;

    public CommitteeService(CommitteeRepository committeeRepository, AppUserService appUserService, EntityValidator entityValidator, MemberRepository memberRepository, CommitteeMembershipRepository committeeMembershipRepository) {
        this.committeeRepository = committeeRepository;
        this.entityValidator = entityValidator;
        this.memberRepository = memberRepository;
        this.appUserService = appUserService;
    }


    private Committee prepareCommitteeFromCommitteeCreationDto(CommitteeCreationDto committeeCreationDto, String username) {
        entityValidator.validate(committeeCreationDto);

        // Convert to LinkedHashMap to preserve order of memberIds
        Map<Integer, String> memberIdAndRoleMap = committeeCreationDto.getMembers().stream()
                .collect(Collectors.toMap(
                        MemberIdWithRoleDto::getMemberId, // key mapper
                        MemberIdWithRoleDto::getRole,     // value mapper
                        (oldValue, newValue) -> oldValue, // merge function (if duplicate keys)
                        LinkedHashMap::new                 // supplier: use LinkedHashMap
                ));


        //remove the coordinator id from the member ids if present
        memberIdAndRoleMap.keySet().removeIf(memberId -> memberId.equals(committeeCreationDto.getCoordinatorId()));

        //check if all the members have role
        if (!memberIdAndRoleMap.values().stream().allMatch(Objects::nonNull)) {
            throw new InvalidMembershipException(ExceptionMessages.MEMBERSHIP_ROLE_MISSING);
        }

        List<Integer> requestedMemberIds = memberIdAndRoleMap.keySet().stream().toList();

        List<Member> foundMembers = memberRepository.findAccessibleMembersByIds(requestedMemberIds, username);


        memberRepository.validateWhetherAllMembersAreFound(requestedMemberIds, foundMembers);

        //sorting the foundMembers in the same order as the requestedMemberIds
        foundMembers.sort(Comparator.comparingInt(member -> requestedMemberIds.indexOf(member.getId())));


        Committee committee = new Committee();
        committee.setCreatedBy(username);

        committee.setName(committeeCreationDto.getName());
        committee.setDescription(committeeCreationDto.getDescription());
        committee.setStatus(committeeCreationDto.getStatus());
        committee.setMinuteLanguage(committeeCreationDto.getMinuteLanguage());

        if (committeeCreationDto.getMaximumNumberOfMeetings() != null)
            committee.setMaxNoOfMeetings(committeeCreationDto.getMaximumNumberOfMeetings());

        //we also set the order for the members in the same sequence as the orderedFoundMembers
        foundMembers.forEach(member -> {
            CommitteeMembership membership = new CommitteeMembership();
            membership.setMember(member);
            membership.setRole(memberIdAndRoleMap.get(member.getId()));
            membership.setOrder(committee.getMemberships().size());
            committee.addMembership(membership);
        });

        Optional<Member> coordinatorOptional = memberRepository.findMemberByIdNoException(committeeCreationDto.getCoordinatorId());

        if (coordinatorOptional.isEmpty()) {
            throw new CoordinatorDoesNotExistException(ExceptionMessages.COORDINATOR_DOES_NOT_EXIST);
        }
        committee.setCoordinator(coordinatorOptional.get());

        if (committeeCreationDto.getSecretaryId() != null) {
            Optional<Member> secretaryOptional = memberRepository.findMemberByIdNoException(committeeCreationDto.getSecretaryId());
            if (secretaryOptional.isEmpty()) {
                throw new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, committeeCreationDto.getSecretaryId());
            }
            committee.setSecretary(secretaryOptional.get());
        }

        return committee;
    }


    @Transactional
    public Committee saveNewCommittee(CommitteeCreationDto committeeCreationDto, String username) {
        Committee committee = prepareCommitteeFromCommitteeCreationDto(committeeCreationDto, username);
        return committeeRepository.save(committee);
    }


    //TODO: Create Tests
    @Transactional
    public Committee updateExistingCommittee(CommitteeCreationDto committeeCreationDto, int committeeId, String username) {
        Committee existingCommittee = this.findCommitteeById(committeeId);

        if (!existingCommittee.getCreatedBy().equals(username)) {
            throw new CommitteeNotAccessibleException(ExceptionMessages.COMMITTEE_NOT_ACCESSIBLE);
        }

        Committee committee = prepareCommitteeFromCommitteeCreationDto(committeeCreationDto, username);

        existingCommittee.setName(committee.getName());
        existingCommittee.setDescription(committee.getDescription());
        existingCommittee.setStatus(committee.getStatus());
        existingCommittee.setMinuteLanguage(committee.getMinuteLanguage());
        existingCommittee.setMaxNoOfMeetings(committee.getMaxNoOfMeetings());
        existingCommittee.setCoordinator(committee.getCoordinator());
        existingCommittee.setSecretary(committee.getSecretary());

        List<CommitteeMembership> newMemberships = committee.getMemberships();

        List<CommitteeMembership> existingMemberships = existingCommittee.getMemberships();

        // Remove memberships that are NOT in the new list
        existingMemberships.removeIf(existing ->
                newMemberships.stream().noneMatch(newMem ->
                        newMem.getMember().getId() == existing.getMember().getId()
                )
        );

        //the newMemberships are already sorted, so assign the order for the membership in the same sequence as the newMemberships

        int order = 1;

        // Add new ones OR Update existing ones
        for (CommitteeMembership newMem : newMemberships) {

            // Check if this member is already in the current list
            CommitteeMembership existingMem = existingMemberships.stream()
                    .filter(e -> e.getMember().getId() == newMem.getMember().getId())
                    .findFirst()
                    .orElse(null);

            if (existingMem == null) {
                // CASE: It's a new member -> Add it
                existingCommittee.addMembership(newMem);
                newMem.setOrder(order++);

            } else {
                // CASE: Member exists -> Update metadata (e.g., Role)
                // Do NOT replace the object, just update the fields
                existingMem.setRole(newMem.getRole());
                existingMem.setOrder(order++);
            }
        }

        return committeeRepository.save(existingCommittee);
    }

    @Transactional
    @CheckCommitteeAccess
    public CommitteeOverviewDto getCommitteeOverview(Committee committee, String username) {
        CommitteeOverviewDto committeeOverview = new CommitteeOverviewDto();
        committeeOverview.setName(committee.getName());
        committeeOverview.setDescription(committee.getDescription());
        committeeOverview.setCreatedDate(committee.getCreatedDate());
        committeeOverview.setMemberCount(committee.getMemberships().size());
        committeeOverview.setMeetingCount(committee.getMeetings().size());
        committeeOverview.setLanguage(committee.getMinuteLanguage());
        int decisionCount = 0;
        for (Meeting meeting : committee.getMeetings()) {
            decisionCount = +meeting.getDecisions().size();
        }
        committeeOverview.setDecisionCount(decisionCount);

        committeeOverview.setCoordinatorName(committee.getCoordinator().getFirstName() + " " + committee.getCoordinator().getLastName());

        if (!committee.getMeetings().isEmpty()) {
            Map<LocalDate, List<Meeting>> dateAndMeetingIdMap = new HashMap<>();

            for(Meeting meeting: committee.getMeetings()) {
                if(dateAndMeetingIdMap.containsKey(meeting.getHeldDate())) {
                    dateAndMeetingIdMap.get(meeting.getHeldDate()).add(meeting);
                } else {
                    List<Meeting> meets  = new ArrayList<Meeting>();
                    meets.add(meeting);
                    dateAndMeetingIdMap.put(meeting.getHeldDate(), meets);
                }
            }

            List<LocalDate> meetingDates = committee.getMeetings().stream().map(Meeting::getHeldDate).collect(Collectors.toList());


            Comparator<LocalDate> comparator = LocalDate::compareTo;
            meetingDates.sort(comparator);

            committeeOverview.setFirstMeetingDate(meetingDates.getFirst());
            committeeOverview.setLastMeetingDate(meetingDates.getLast());
            List<DateAndMeetingIdsDto> dateAndMeetingIdsDtos = new ArrayList<>();
            for(LocalDate date: dateAndMeetingIdMap.keySet()) {
                dateAndMeetingIdsDtos.add(new DateAndMeetingIdsDto(date, dateAndMeetingIdMap.get(date)));
            }
            committeeOverview.setMeetings(dateAndMeetingIdsDtos);
        }
        return committeeOverview;
    }

    public boolean toggleCommitteeStatus(Integer committeeId, String username) {
        Committee committee = getCommitteeIfAccessible(committeeId, username);
        boolean result;
        if (committee.getStatus() == CommitteeStatus.ACTIVE) {
            committee.setStatus(CommitteeStatus.INACTIVE);
            result = true;
        } else {
            committee.setStatus(CommitteeStatus.ACTIVE);
            result = false;
        }
        committeeRepository.save(committee);
        return result;
    }

    public List<Committee> getAllActiveCommittees(String username) {
        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(username);
        if (user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_HEAD) {
            return committeeRepository.getSystemActiveCommittees();
        } else if (user.getRole() == com.sep.mmms_backend.enums.AppRole.COMMITTEE_MEMBER || user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_MEMBER) {
            if (user.getLinkedMemberId() != null) {
                List<Committee> memberCommittees = committeeRepository.getMemberActiveCommittees(user.getLinkedMemberId());
                List<Committee> secretaryCommittees = committeeRepository.getSecretaryActiveCommittees(user.getLinkedMemberId());
                
                Set<Committee> uniqueCommittees = new HashSet<>();
                uniqueCommittees.addAll(memberCommittees);
                uniqueCommittees.addAll(secretaryCommittees);
                
                return new ArrayList<>(uniqueCommittees);
            }
            return new ArrayList<>();
        }
        return committeeRepository.getAllActiveCommittees(username);
    }

    public List<Committee> getAllInactiveCommittees(String username) {
        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(username);
        if (user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_HEAD) {
            return committeeRepository.getSystemInActiveCommittees();
        } else if (user.getRole() == com.sep.mmms_backend.enums.AppRole.COMMITTEE_MEMBER || user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_MEMBER) {
            if (user.getLinkedMemberId() != null) {
                List<Committee> memberCommittees = committeeRepository.getMemberInActiveCommittees(user.getLinkedMemberId());
                List<Committee> secretaryCommittees = committeeRepository.getSecretaryInActiveCommittees(user.getLinkedMemberId());
                
                Set<Committee> uniqueCommittees = new HashSet<>();
                uniqueCommittees.addAll(memberCommittees);
                uniqueCommittees.addAll(secretaryCommittees);
                
                return new ArrayList<>(uniqueCommittees);
            }
            return new ArrayList<>();
        }
        return committeeRepository.getAllInActiveCommittees(username);
    }


    @Transactional
    @CheckCommitteeAccess
    public List<MemberOfCommitteeDto> getMembersOfCommittee(Committee committee, String username) {

        List<MemberOfCommitteeDto> membersOfCommittee = committee.getSortedMemberships().stream().map(membership -> {
            Member member = membership.getMember();
            String memberFullName = member.getFirstName() + " " + member.getLastName();
            return new MemberOfCommitteeDto(member.getId(), memberFullName, membership.getRole());
        }).toList();

        return membersOfCommittee;
    }

    public Optional<Committee> findCommitteeByIdNoException(int committeeId) {
        return committeeRepository.findById(committeeId);
    }

    public Committee getCommitteeIfAccessible(int committeeId, String username) {
        Committee committee = this.findCommitteeByIdNoException(committeeId).orElseThrow(() -> new CommitteeDoesNotExistException(ExceptionMessages.COMMITTEE_DOES_NOT_EXIST, committeeId));
        
        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(username);
        boolean isDeptHead = user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_HEAD;
        boolean isMember = user.getRole() == com.sep.mmms_backend.enums.AppRole.COMMITTEE_MEMBER || user.getRole() == com.sep.mmms_backend.enums.AppRole.DEPARTMENT_MEMBER;
        
        boolean hasAccess = false;
        if (isDeptHead) {
            hasAccess = true;
        } else if (isMember) {
            if (user.getLinkedMemberId() != null) {
                boolean isSecretary = committee.getSecretary() != null && committee.getSecretary().getId().equals(user.getLinkedMemberId());
                boolean isCommitteeMember = committee.getMemberships().stream().anyMatch(m -> m.getMember().getId().equals(user.getLinkedMemberId()));
                hasAccess = isSecretary || isCommitteeMember;
            }
        } else if (committee.getCreatedBy().equals(username)) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new IllegalOperationException("Committee not accessible");
        }
        return committee;
    }

    /**
     * returns all the members belonging to the committee
     */

    public List<MemberSummaryDto> getMembersOfCommittee(Committee committee) {
        List<Integer> memberIds = committee.getMemberships().stream()
                .map(membership -> membership.getMember().getId())
                .collect(Collectors.toList());

        List<Member> members = memberRepository.findAllById(memberIds);

        return members.stream().map(member -> new MemberSummaryDto(member, committee.getId()))
                .collect(Collectors.toList());
    }

    public CommitteeExtendedSummaryDto getCommitteeExtendedSummary(int committeeId, String username) {
        Committee committee = getCommitteeIfAccessible(committeeId, username);
        return new CommitteeExtendedSummaryDto(committee);
    }

    public boolean existsById(int committeeId) {
        return committeeRepository.existsById(committeeId);
    }

    public Committee findCommitteeById(int committeeId) {
        return committeeRepository.findCommitteeById(committeeId);
    }

    @Transactional
    public void assignSecretary(int committeeId, Integer memberId, String username) {
        Committee committee = getCommitteeIfAccessible(committeeId, username);
        com.sep.mmms_backend.entity.AppUser user = appUserService.loadUserByUsername(username);
        
        if (user.getRole() != com.sep.mmms_backend.enums.AppRole.DEPARTMENT_HEAD) {
            throw new IllegalOperationException("Only DEPARTMENT_HEAD can assign a secretary");
        }

        if (memberId == null) {
            committee.setSecretary(null);
        } else {
            Member member = memberRepository.findAccessibleMember(memberId, username)
                    .orElseThrow(() -> new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId));
            committee.setSecretary(member);
        }
        
        committeeRepository.save(committee);
    }
}
