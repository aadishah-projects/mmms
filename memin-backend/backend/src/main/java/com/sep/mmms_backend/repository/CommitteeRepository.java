package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.exceptions.CommitteeDoesNotExistException;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommitteeRepository extends JpaRepository<Committee, Integer>, JpaSpecificationExecutor<Committee> {

    /**
     * if the committee is not found, the caller is responsible to check it and handle it
     */
    default Optional<Committee> findByIdNoException(int committeeId) {
        return this.findById(committeeId);
    }


    /**
     *
     * @param committeeId the id of the committee to be loaded from the database
     */
    default Committee findCommitteeById(int committeeId) {
        Optional<Committee> committee = this.findByIdWithDetails(committeeId);
        if (committee.isEmpty()) {
            throw new CommitteeDoesNotExistException(ExceptionMessages.COMMITTEE_DOES_NOT_EXIST, committeeId);
        }

        return committee.get();
    }

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("SELECT c FROM Committee c WHERE c.id = :id")
    Optional<Committee> findByIdWithDetails(@Param("id") int committeeId);

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("Select c from Committee c where c.createdBy= :createdBy AND c.status= 'ACTIVE'")
    List<Committee> getAllActiveCommittees(@Param("createdBy") String username);


    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("Select c from Committee c where c.createdBy= :createdBy AND c.status= 'INACTIVE'")
    List<Committee> getAllInActiveCommittees(@Param("createdBy") String username);

    @Query("Select c FROM Committee c where c.createdBy= :createdBy")
    List<Committee> getAllCommittees(@Param("createdBy") String username);

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("SELECT c FROM Committee c JOIN c.memberships m WHERE m.member.id = :memberId AND c.status = 'ACTIVE'")
    List<Committee> getMemberActiveCommittees(@Param("memberId") Integer memberId);

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("SELECT c FROM Committee c JOIN c.memberships m WHERE m.member.id = :memberId AND c.status = 'INACTIVE'")
    List<Committee> getMemberInActiveCommittees(@Param("memberId") Integer memberId);

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("Select c from Committee c where c.status= 'ACTIVE'")
    List<Committee> getSystemActiveCommittees();

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("Select c from Committee c where c.status= 'INACTIVE'")
    List<Committee> getSystemInActiveCommittees();

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("SELECT c FROM Committee c WHERE c.secretary.id = :memberId AND c.status = 'ACTIVE'")
    List<Committee> getSecretaryActiveCommittees(@Param("memberId") Integer memberId);

    @EntityGraph(attributePaths = {"meetings", "meetings.decisions", "memberships", "memberships.member", "secretary", "coordinator"})
    @Query("SELECT c FROM Committee c WHERE c.secretary.id = :memberId AND c.status = 'INACTIVE'")
    List<Committee> getSecretaryInActiveCommittees(@Param("memberId") Integer memberId);
}
