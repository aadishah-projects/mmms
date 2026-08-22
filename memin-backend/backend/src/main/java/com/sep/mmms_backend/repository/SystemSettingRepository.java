package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Integer> {

    default Optional<SystemSetting> findDefaultSettings() {
        return findById(1);
    }
}
