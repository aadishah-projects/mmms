package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.MemberDoesNotExistException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer>, JpaSpecificationExecutor<Member> {

    default Member findMemberById(int memberId) {
        Optional<Member> member = this.findById(memberId);
        if(member.isEmpty()) {
            throw new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId);
        }
        return member.get();
    }


    default Optional<Member> findMemberByIdNoException(int memberId) {
        return this.findById(memberId);
    }



    public List<Member> findAllMembersByCreatedBy(String username);




    /**
     * returns all members with the provided memberIds if they are accessible by the 'username' by checking the 'createdBy' field
     */
    @Query("SELECT m FROM Member m WHERE m.id IN :memberIds AND m.createdBy = :username")
    List<Member> findAccessibleMembersByIds(
            @Param("memberIds") List<Integer> memberIds,
            @Param("username") String username
    );


    @Query("SELECT m FROM Member m WHERE m.createdBy = :username")
    List<Member> findAllAccessibleMembers(
            @Param("username") String username
    );

    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.createdBy = :username")
    public Optional<Member> getMemberIfAccessible(int memberId, String username);

    @Query("""
    SELECT m FROM Member m
    WHERE m.id NOT IN (
        SELECT mm.id FROM Member mm
        JOIN mm.invitedMeetings meet
        WHERE meet.id = :meetingId
    )
    AND m.id NOT IN (
        SELECT cm.member.id FROM CommitteeMembership cm
        WHERE cm.committee.id = :committeeId
    )
    AND m.id NOT IN (
        SELECT c.coordinator.id FROM Committee c WHERE c.id = :committeeId
    )
    AND m.createdBy = :username
    """)
    public List<Member> getPossibleInviteesForMeeting(Integer meetingId, Integer committeeId, String username);

    @Query("Select m from Member m where m.id = :memberId AND m.createdBy = :username")
    public Optional<Member> findAccessibleMember(Integer memberId, String username);



    /**
     * Checks if all requiredMemberIds in foundMembers
     * If no, throws MemberDoesNotExistException
     * If yes, retrieves all the Members from the database
     * It never returns null becase finaAllMembersById() never returns null
     * NOTE: this method is put in repository as various serivce use this
     */
    public default void validateWhetherAllMembersAreFound(List<Integer> requiredMemberIds, List<Member> foundMembers) {
        if (foundMembers.size() != requiredMemberIds.size()) {
            List<Integer> foundMemberIds = foundMembers.stream().map(Member::getId).toList();
            List<Integer> missingIds = new LinkedList<>(requiredMemberIds);
            missingIds.removeAll(foundMemberIds);
            throw new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, missingIds);
        }
    }
}
