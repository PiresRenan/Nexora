package com.nexora.application.service;

import com.nexora.application.dto.product.*;
import com.nexora.application.usecase.ProductUseCase;
import com.nexora.domain.event.StockReplenishedEvent;
import com.nexora.domain.exception.*;
import com.nexora.domain.model.*;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.*;
import com.nexora.infrastructure.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductApplicationService implements ProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductApplicationService.class);

    private final ProductRepository       productRepository;
    private final CategoryRepository      categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EventPublisher          eventPublisher;

    public ProductApplicationService(
            ProductRepository       productRepository,
            CategoryRepository      categoryRepository,
            StockMovementRepository stockMovementRepository,
            EventPublisher          eventPublisher
    ) {
        this.productRepository       = productRepository;
        this.categoryRepository      = categoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.eventPublisher          = eventPublisher;
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "SKU", request.sku());
        }
        UUID categoryId = null;
        if (request.categoryId() != null) {
            categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            categoryId = request.categoryId();
        }
        var product = Product.create(request.name(), request.description(), request.sku(),
                Money.of(request.price(), request.currency()),
                StockQuantity.of(request.initialStock()), categoryId);

        if (request.initialStock() > 0) {
            stockMovementRepository.save(StockMovement.entry(
                    product.getId(), request.initialStock(), 0, request.initialStock(),
                    "INITIAL_STOCK", null, null));
        }
        log.info("Product created id={} sku={}", product.getId(), request.sku());
        return ProductResponse.fromDomain(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, key = "#id")
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        var product = findOrThrow(id);
        product.updateDetails(request.name(), request.description(),
                Money.of(request.price(), request.currency()));
        return ProductResponse.fromDomain(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PRODUCTS, key = "#id")
    public ProductResponse findById(UUID id) {
        return ProductResponse.fromDomain(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::fromDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAllActive(Pageable pageable) {
        return productRepository.findAllActive(pageable).map(ProductResponse::fromDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(ProductResponse::fromDomain);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, key = "#id")
    public ProductResponse replenishStock(UUID id, int quantity, UUID performedBy) {
        if (quantity <= 0) throw new BusinessRuleException("Quantity must be positive: " + quantity);
        var product = findOrThrow(id);
        int before  = product.getStock().value();
        product.replenishStock(quantity);
        productRepository.save(product);

        stockMovementRepository.save(StockMovement.entry(
                product.getId(), quantity, before, product.getStock().value(),
                "MANUAL_REPLENISHMENT", null, performedBy));

        eventPublisher.publish(StockReplenishedEvent.of(
                product.getId(), product.getSku(), quantity,
                product.getStock().value(), "MANUAL_REPLENISHMENT", performedBy));

        return ProductResponse.fromDomain(product);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, key = "#id")
    public void deleteProduct(UUID id) {
        var product = findOrThrow(id);
        product.deactivate();
        productRepository.save(product);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, key = "#productId")
    public ProductResponse assignCategory(UUID productId, UUID categoryId) {
        var product = findOrThrow(productId);
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        product.assignCategory(categoryId);
        return ProductResponse.fromDomain(productRepository.save(product));
    }

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}