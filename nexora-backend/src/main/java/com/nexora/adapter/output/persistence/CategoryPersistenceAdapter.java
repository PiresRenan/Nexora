package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.CategoryJpaRepository;
import com.nexora.domain.model.Category;
import com.nexora.domain.repository.CategoryRepository;
import com.nexora.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;
    public CategoryPersistenceAdapter(CategoryJpaRepository jpa) { this.jpa = jpa; }

    @Override public Category save(Category c)                    { return jpa.save(CategoryEntity.fromDomain(c)).toDomain(); }
    @Override public Optional<Category> findById(UUID id)         { return jpa.findById(id).map(CategoryEntity::toDomain); }
    @Override public Optional<Category> findByName(String name)   { return jpa.findByName(name).map(CategoryEntity::toDomain); }
    @Override public List<Category> findAll()                     { return jpa.findAll().stream().map(CategoryEntity::toDomain).toList(); }
    @Override public List<Category> findAllActive()               { return jpa.findAllByActiveTrue().stream().map(CategoryEntity::toDomain).toList(); }
    @Override public boolean existsByName(String name)            { return jpa.existsByName(name); }
    @Override public void deleteById(UUID id)                     { jpa.deleteById(id); }
}