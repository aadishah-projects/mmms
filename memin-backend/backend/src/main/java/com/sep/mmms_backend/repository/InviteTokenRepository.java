package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, Integer> {
    Optional<InviteToken> findByToken(String token);
    Optional<InviteToken> findByEmailAndUsedFalse(String email);
}
