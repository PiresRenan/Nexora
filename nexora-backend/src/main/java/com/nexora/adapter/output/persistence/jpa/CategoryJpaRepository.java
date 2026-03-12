package com.nexora.adapter.output.persistence.jpa;

import com.nexora.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByName(String name);
    List<CategoryEntity> findAllByActiveTrue();
    boolean existsByName(String name);
}