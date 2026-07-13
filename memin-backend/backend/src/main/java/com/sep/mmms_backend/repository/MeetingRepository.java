package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {
    public Meeting findMeetingById(Integer id);
    @Query("SELECT m FROM Meeting m WHERE m.id = :meetingId AND m.createdBy = :username")
    public Optional<Meeting> getMeetingIfAccessible(Integer meetingId, String username);
}
