package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    public Optional<AppUser> findByUsername(String username);
    public boolean existsByUsername(String username);
}
