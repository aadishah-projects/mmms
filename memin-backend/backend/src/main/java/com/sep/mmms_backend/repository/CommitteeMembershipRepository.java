package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.CommitteeMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommitteeMembershipRepository extends JpaRepository<CommitteeMembership, Integer> {

    //UNTESTED

    /**
     * This method returns all the memberships that has memberId in the provided List of memberIds and committeeId = provided committeeId
     */
    @Query("SELECT cm FROM CommitteeMembership cm WHERE cm.member.id IN :accessibleMemberIds AND cm.committee.id = :committeeId")
    List<CommitteeMembership> findExistingMemberships(
            @Param("accessibleMemberIds") List<Integer> accessibleMemberIds,
            @Param("committeeId") Integer committeeId
    );


    /**
     * This method simply returns the membership associated between the provided committeeId and memberId
     */
    @Query("SELECT cm FROM CommitteeMembership cm " +
            "WHERE cm.id.committeeId = :committeeId " +
            "AND cm.id.memberId = :memberId")
    Optional<CommitteeMembership> findMembershipBetweenCommitteeAndMember(
            @Param("committeeId") Integer committeeId,
            @Param("memberId") Integer memberId);
}
