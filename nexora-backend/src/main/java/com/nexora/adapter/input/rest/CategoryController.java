package com.nexora.adapter.input.rest;

import com.nexora.application.dto.category.CategoryResponse;
import com.nexora.application.dto.category.CreateCategoryRequest;
import com.nexora.application.usecase.CategoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product category management")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;
    public CategoryController(CategoryUseCase categoryUseCase) { this.categoryUseCase = categoryUseCase; }

    @PostMapping
    @Operation(summary = "Create category", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest req) {
        var r = categoryUseCase.create(req);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + r.id())).body(r);
    }

    @GetMapping
    @Operation(summary = "List categories")
    public ResponseEntity<List<CategoryResponse>> findAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? categoryUseCase.findAllActive() : categoryUseCase.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryUseCase.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.ok(categoryUseCase.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate category", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(@PathVariable UUID id) { categoryUseCase.delete(id); }
}