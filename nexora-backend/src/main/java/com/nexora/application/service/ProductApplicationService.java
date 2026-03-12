package com.nexora.application.service;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.dto.product.UpdateProductRequest;
import com.nexora.application.usecase.ProductUseCase;
import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.Money;
import com.nexora.domain.model.Product;
import com.nexora.domain.model.StockQuantity;
import com.nexora.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementação dos casos de uso de produto.
 * Melhoria: @Transactional nos métodos individuais com readOnly=true
 * para operações de leitura — melhora performance e deixa intenção clara.
 */
@Service
@Transactional
public class ProductApplicationService implements ProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductApplicationService.class);

    private final ProductRepository productRepository;

    public ProductApplicationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.sku());

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "SKU", request.sku());
        }

        var product = Product.create(
                request.name(),
                request.description(),
                request.sku(),
                Money.of(request.price(), request.currency()),
                StockQuantity.of(request.initialStock())
        );

        var saved = productRepository.save(product);
        log.info("Product created successfully: {}", saved.getId());
        return ProductResponse.fromDomain(saved);
    }

    @Override
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        log.info("Updating product: {}", id);

        var product = findProductOrThrow(id);

        product.updateDetails(
                request.name(),
                request.description(),
                Money.of(request.price(), request.currency())
        );

        var saved = productRepository.save(product);
        return ProductResponse.fromDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return ProductResponse.fromDomain(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAllActive() {
        return productRepository.findAllActive().stream()
                .map(ProductResponse::fromDomain)
                .toList();
    }

    @Override
    public ProductResponse replenishStock(UUID id, int quantity) {
        log.info("Replenishing {} units for product: {}", quantity, id);

        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity to replenish must be positive, got: " + quantity);
        }

        var product = findProductOrThrow(id);
        product.replenishStock(quantity);

        return ProductResponse.fromDomain(productRepository.save(product));
    }

    @Override
    public void deleteProduct(UUID id) {
        log.info("Deactivating product: {}", id);
        var product = findProductOrThrow(id);
        product.deactivate();
        productRepository.save(product);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}