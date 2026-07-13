package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.repository.CommitteeRepository;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MemberController {
    private final MemberService memberService;
    private final CommitteeService committeeService;

    public MemberController(MemberService memberService, CommitteeService committeeService) {
        this.memberService = memberService;
        this.committeeService = committeeService;
    }

    @PostMapping("/member")
    public ResponseEntity<Response> createNewMember(@RequestBody(required = true) MemberCreationDto memberDto, Authentication authentication) {
        Member member = memberService.saveNewMember(memberDto, authentication.getName());
        return ResponseEntity.ok(new Response(ResponseMessages.MEMBER_CREATION_SUCCESS, member));
    }

    @GetMapping("/member-details")
    public ResponseEntity<Response> getMemberDetails(@RequestParam(required = true) int memberId, Authentication authentication) {
        MemberDetailsDto memberDetailsDto = memberService.getMemberDetails(memberId, authentication.getName());

        return ResponseEntity.ok(new Response(ResponseMessages.MEMBER_DETAIL_RETRIEVED_SUCCESSFULLY, memberDetailsDto));
    }

    @PatchMapping("/member")
    public ResponseEntity<Response> updateMember(@RequestBody(required = true) MemberCreationDto memberCreationDto, @RequestParam Integer memberId, Authentication authentication) {
        memberService.updateMember(memberId, memberCreationDto, authentication.getName());
        return ResponseEntity.ok(new Response(ResponseMessages.MEMBER_UPDATION_SUCCESS));
    }

    @GetMapping("/possible-invitees")
    public ResponseEntity<Response> getPossibleInvitees(@RequestParam(required = true) int committeeId, Authentication authentication) {

        Committee committee = committeeService.findCommitteeById(committeeId);
        List<MemberSearchResultDto> possibleInvitees = memberService.getPossibleInvitees(committee, authentication.getName());

        return ResponseEntity.ok(new Response("Possible Invitees: ", possibleInvitees));
    }


    //TODO: Create Tests
    @GetMapping("/all-members")
    public ResponseEntity<Response> getAllMembers(Authentication authentication) {
        List<Member> members = memberService.getAllMembers(authentication.getName());
        List<MemberDetailsDto> allMembers = new ArrayList<>();
        members.forEach(member -> allMembers.add(new MemberDetailsDto(member)));
        return ResponseEntity.ok(new Response("Found Members: ", allMembers));
    }
}
