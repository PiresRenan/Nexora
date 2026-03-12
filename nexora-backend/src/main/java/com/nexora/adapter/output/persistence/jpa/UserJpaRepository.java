package com.nexora.adapter.output.persistence.jpa;

import com.nexora.domain.model.UserRole;
import com.nexora.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findAllByRole(UserRole role);

    boolean existsByEmail(String email);
}