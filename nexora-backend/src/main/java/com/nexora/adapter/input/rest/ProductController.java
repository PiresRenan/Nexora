package com.nexora.adapter.input.rest;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.dto.product.UpdateProductRequest;
import com.nexora.application.usecase.ProductUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Input Adapter REST para produtos.
 * Melhoria: controller magro — zero lógica de negócio aqui.
 * Apenas: recebe DTO → valida → delega ao UseCase → retorna resposta HTTP.
 * Retorna URI no header Location para recursos criados (REST idiomático).
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
@Tag(name = "Products", description = "Product management operations")
public class ProductController {

    private final ProductUseCase productUseCase;

    public ProductController(ProductUseCase productUseCase) {
        this.productUseCase = productUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new product", responses = {
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        var response = productUseCase.createProduct(request);
        var location = URI.create("/api/v1/products/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List all products")
    public ResponseEntity<List<ProductResponse>> findAll(
            @Parameter(description = "Filter by active status only")
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        var products = activeOnly
                ? productUseCase.findAllActive()
                : productUseCase.findAll();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find product by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productUseCase.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productUseCase.updateProduct(id, request));
    }

    @PatchMapping("/{id}/stock/replenish")
    @Operation(summary = "Add units to product stock")
    public ResponseEntity<ProductResponse> replenishStock(
            @PathVariable UUID id,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity
    ) {
        return ResponseEntity.ok(productUseCase.replenishStock(id, quantity));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate product (soft delete)")
    public void deleteProduct(@PathVariable UUID id) {
        productUseCase.deleteProduct(id);
    }
}