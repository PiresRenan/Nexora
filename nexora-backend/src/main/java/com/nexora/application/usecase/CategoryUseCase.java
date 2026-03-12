package com.nexora.application.usecase;

import com.nexora.application.dto.category.CategoryResponse;
import com.nexora.application.dto.category.CreateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryUseCase {
    CategoryResponse       create(CreateCategoryRequest request);
    CategoryResponse       update(UUID id, CreateCategoryRequest request);
    CategoryResponse       findById(UUID id);
    List<CategoryResponse> findAll();
    List<CategoryResponse> findAllActive();
    void                   delete(UUID id);
}