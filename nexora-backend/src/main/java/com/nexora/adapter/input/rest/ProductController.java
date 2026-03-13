package com.nexora.adapter.input.rest;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.dto.product.UpdateProductRequest;
import com.nexora.application.usecase.ProductUseCase;
import com.nexora.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

        import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Products", description = "Product management")
public class ProductController {

    private final ProductUseCase productUseCase;
    public ProductController(ProductUseCase productUseCase) { this.productUseCase = productUseCase; }

    @PostMapping
    @Operation(summary = "Create product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest req) {
        var r = productUseCase.createProduct(req);
        return ResponseEntity.created(URI.create("/api/v1/products/" + r.id())).body(r);
    }

    @GetMapping
    @Operation(summary = "List products (paginated)")
    public Page<ProductResponse> findAll(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        if (categoryId != null) return productUseCase.findByCategory(categoryId, pageable);
        return activeOnly ? productUseCase.findAllActive(pageable) : productUseCase.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productUseCase.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        return ResponseEntity.ok(productUseCase.updateProduct(id, req));
    }

    @PatchMapping("/{id}/category/{categoryId}")
    @Operation(summary = "Assign category to product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> assignCategory(@PathVariable UUID id, @PathVariable UUID categoryId) {
        return ResponseEntity.ok(productUseCase.assignCategory(id, categoryId));
    }

    @PatchMapping("/{id}/stock/replenish")
    @Operation(summary = "Replenish stock", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> replenishStock(
            @PathVariable UUID id,
            @RequestParam @Min(1) int quantity,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(productUseCase.replenishStock(id, quantity, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate product", security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteProduct(@PathVariable UUID id) { productUseCase.deleteProduct(id); }
}