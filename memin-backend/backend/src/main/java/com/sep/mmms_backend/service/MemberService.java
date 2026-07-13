package com.sep.mmms_backend.service;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.dto.MemberCreationDto;
import com.sep.mmms_backend.dto.MemberDetailsDto;
import com.sep.mmms_backend.dto.MemberSearchResultDto;
import com.sep.mmms_backend.dto.MemberWithoutCommitteeDto;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.MemberDoesNotExistException;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.validators.EntityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final EntityValidator entityValidator;

    public MemberService(MemberRepository memberRepository, EntityValidator entityValidator) {
        this.memberRepository = memberRepository;
        this.entityValidator = entityValidator;
    }


    //NEW IMPLEMENTATION
    @Transactional
    public Member saveNewMember(MemberCreationDto memberDto, String username) {

        entityValidator.validate(memberDto);

        Member member = new Member();
        member.setFirstName(memberDto.getFirstName());
        member.setLastName(memberDto.getLastName());
        member.setTitle(memberDto.getTitle());
        if(memberDto.getInstitution() != null && !memberDto.getInstitution().isBlank()) {
            member.setInstitution(memberDto.getInstitution());
        }
        if (memberDto.getPost() != null)
            member.setPost(memberDto.getPost());
        return memberRepository.save(member);
    }

    @Transactional
    public void updateMember(int memberId, MemberCreationDto memberCreationDto, String username) {
        entityValidator.validate(memberCreationDto);
        Member member = getMemberIfAccesssible(memberId, username);

        member.setFirstName(memberCreationDto.getFirstName());
        member.setLastName(memberCreationDto.getLastName());
        member.setTitle(memberCreationDto.getTitle());
        member.setPost(memberCreationDto.getPost());

        if(memberCreationDto.getInstitution() != null && !memberCreationDto.getInstitution().isBlank()) {
            member.setInstitution(memberCreationDto.getInstitution());
        }
        memberRepository.save(member);
    }

    @Transactional
    public MemberDetailsDto getMemberDetails(int memberId, String username) {
        Member member = getMemberIfAccesssible(memberId, username);
        MemberDetailsDto memberDetailsDto = new MemberDetailsDto(member);
        return memberDetailsDto;
    }

    private Member getMemberIfAccesssible(int memberId, String username) {
        Optional<Member> optionalMember = memberRepository.getMemberIfAccessible(memberId, username);

        if (optionalMember.isEmpty()) {
            throw new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId);
        }

        return optionalMember.get();
    }

    //NEW IMPLEMENTATION
    @Transactional
    @CheckCommitteeAccess
    public List<MemberSearchResultDto> getPossibleInvitees(Committee committee, String username) {
        List<Member> committeeMembers = committee.getMemberships().stream().map(CommitteeMembership::getMember).toList();

        List<Member> allAccessibleMembers = memberRepository.findAllAccessibleMembers(username);

        allAccessibleMembers.removeAll(committeeMembers);
        allAccessibleMembers.remove(committee.getCoordinator());

        List<MemberSearchResultDto> possibleInvitees = allAccessibleMembers.stream().map(MemberSearchResultDto::new).toList();

        return possibleInvitees;
    }


    public boolean existsById(int memberId) {
        return memberRepository.existsById(memberId);
    }


    public Member findById(int memberId) {
        return memberRepository.findById(memberId).orElseThrow(() ->
                new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId));
    }

    public List<Member> getAllMembers(String username) {
        List<MemberWithoutCommitteeDto> allMembersDto = new ArrayList<>();
        List<Member> allMembers = memberRepository.findAllMembersByCreatedBy(username);
        return allMembers;
    }

    public Member findMemberById(int memberId) {
        return memberRepository.findMemberById(memberId);
    }

}
