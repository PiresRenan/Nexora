        package com.nexora.application.usecase;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.dto.product.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductUseCase {
    ProductResponse       createProduct(CreateProductRequest request);
    ProductResponse       updateProduct(UUID id, UpdateProductRequest request);
    ProductResponse       findById(UUID id);
    Page<ProductResponse> findAll(Pageable pageable);
    Page<ProductResponse> findAllActive(Pageable pageable);
    Page<ProductResponse> findByCategory(UUID categoryId, Pageable pageable);
    ProductResponse       replenishStock(UUID id, int quantity, UUID performedBy);
    void                  deleteProduct(UUID id);
    ProductResponse       assignCategory(UUID productId, UUID categoryId);
}