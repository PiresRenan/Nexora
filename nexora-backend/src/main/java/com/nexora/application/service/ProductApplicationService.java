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
import com.nexora.domain.model.StockMovement;
import com.nexora.domain.model.StockQuantity;
import com.nexora.domain.repository.CategoryRepository;
import com.nexora.domain.repository.ProductRepository;
import com.nexora.domain.repository.StockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ProductApplicationService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.productRepository       = productRepository;
        this.categoryRepository      = categoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
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
        var product = Product.create(
                request.name(), request.description(), request.sku(),
                Money.of(request.price(), request.currency()),
                StockQuantity.of(request.initialStock()),
                categoryId
        );
        // Registra entrada de estoque inicial
        if (request.initialStock() > 0) {
            stockMovementRepository.save(StockMovement.entry(
                    product.getId(), request.initialStock(), 0, request.initialStock(),
                    "INITIAL_STOCK", null, null
            ));
        }
        log.info("Product created: {} (SKU: {})", product.getId(), request.sku());
        return ProductResponse.fromDomain(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        var product = findOrThrow(id);
        product.updateDetails(request.name(), request.description(),
                Money.of(request.price(), request.currency()));
        return ProductResponse.fromDomain(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
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
    public ProductResponse replenishStock(UUID id, int quantity, UUID performedBy) {
        if (quantity <= 0) throw new BusinessRuleException("Quantity must be positive: " + quantity);
        var product = findOrThrow(id);
        int before = product.getStock().value();
        product.replenishStock(quantity);
        productRepository.save(product);
        stockMovementRepository.save(StockMovement.entry(
                product.getId(), quantity, before, product.getStock().value(),
                "MANUAL_REPLENISHMENT", null, performedBy
        ));
        return ProductResponse.fromDomain(product);
    }

    @Override
    public void deleteProduct(UUID id) {
        var product = findOrThrow(id);
        product.deactivate();
        productRepository.save(product);
    }

    @Override
    public ProductResponse assignCategory(UUID productId, UUID categoryId) {
        var product  = findOrThrow(productId);
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