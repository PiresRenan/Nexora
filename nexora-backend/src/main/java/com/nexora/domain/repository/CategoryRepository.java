package com.nexora.domain.repository;

import com.nexora.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    Optional<Category> findByName(String name);
    List<Category> findAll();
    List<Category> findAllActive();
    boolean existsByName(String name);
    void deleteById(UUID id);
}