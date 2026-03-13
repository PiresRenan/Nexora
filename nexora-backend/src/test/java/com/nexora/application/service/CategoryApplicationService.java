package com.nexora.application.service;

import com.nexora.application.dto.category.CategoryResponse;
import com.nexora.application.dto.category.CreateCategoryRequest;
import com.nexora.application.usecase.CategoryUseCase;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.Category;
import com.nexora.domain.repository.CategoryRepository;
import com.nexora.infrastructure.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryApplicationService implements CategoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CategoryApplicationService.class);

    private final CategoryRepository categoryRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category", "name", request.name());
        }
        var category = Category.create(request.name(), request.description());
        log.info("Category created: {}", request.name());
        return CategoryResponse.fromDomain(categoryRepository.save(category));
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse update(UUID id, CreateCategoryRequest request) {
        var category = findOrThrow(id);
        category.update(request.name(), request.description());
        return CategoryResponse.fromDomain(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES, key = "#id")
    public CategoryResponse findById(UUID id) {
        return CategoryResponse.fromDomain(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES, key = "'all'")
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::fromDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActive() {
        return categoryRepository.findAllActive().stream().map(CategoryResponse::fromDomain).toList();
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public void delete(UUID id) {
        var category = findOrThrow(id);
        category.deactivate();
        categoryRepository.save(category);
    }

    private Category findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}