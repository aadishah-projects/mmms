package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.MinuteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MinuteTemplateRepository extends JpaRepository<MinuteTemplate, Integer> {
    List<MinuteTemplate> findByCommitteeIdOrderByIdDesc(Integer committeeId);

    Optional<MinuteTemplate> findByIdAndCommitteeId(Integer id, Integer committeeId);

    boolean existsByCommitteeIdAndNameIgnoreCase(Integer committeeId, String name);
}
