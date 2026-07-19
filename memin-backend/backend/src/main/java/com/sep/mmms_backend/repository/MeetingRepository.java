package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {
    public Meeting findMeetingById(Integer id);
    @Query("SELECT m FROM Meeting m WHERE m.id = :meetingId AND m.createdBy = :username")
    public Optional<Meeting> getMeetingIfAccessible(Integer meetingId, String username);

    // Eagerly fetch each meeting's agendas so they can be exposed on meeting summaries
    // without relying on lazy loading (the app runs with spring.jpa.open-in-view=false).
    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN FETCH m.agendas WHERE m.committee.id = :committeeId")
    List<Meeting> findByCommitteeIdWithAgendas(@Param("committeeId") int committeeId);
}
