package com.nexora.application.usecase;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.dto.product.UpdateProductRequest;

import java.util.List;
import java.util.UUID;

/**
 * Input Port — define as operações disponíveis para produtos.
 * Melhoria: interface única por aggregate com métodos coesos.
 * Os controllers dependem desta interface, nunca da implementação direta.
 * Facilita mock em testes de slice (@WebMvcTest).
 */
public interface ProductUseCase {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    ProductResponse findById(UUID id);

    List<ProductResponse> findAll();

    List<ProductResponse> findAllActive();

    ProductResponse replenishStock(UUID id, int quantity);

    void deleteProduct(UUID id);
}