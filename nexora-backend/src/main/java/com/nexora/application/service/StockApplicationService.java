package com.nexora.application.service;

import com.nexora.application.dto.stock.StockMovementResponse;
import com.nexora.application.usecase.StockUseCase;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.repository.ProductRepository;
import com.nexora.domain.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso de leitura do histórico de estoque.
 *
 * Responsabilidade única: expor a trilha de auditoria de {@link com.nexora.domain.model.StockMovement}
 * para os papéis autorizados (SELLER+).
 *
 * Toda escrita (ENTRY/EXIT) é gerenciada pelos outros application services
 * durante fluxos de confirmação de pedido, reabastecimento manual, etc.
 */
@Service
@Transactional(readOnly = true)
public class StockApplicationService implements StockUseCase {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository       productRepository;

    public StockApplicationService(StockMovementRepository stockMovementRepository,
                                   ProductRepository productRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository       = productRepository;
    }

    @Override
    public Page<StockMovementResponse> findByProduct(UUID productId, Pageable pageable) {
        // Valida que o produto existe antes de consultar movimentações
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        return stockMovementRepository.findByProductId(productId, pageable)
                .map(StockMovementResponse::fromDomain);
    }

    @Override
    public Page<StockMovementResponse> findAll(Pageable pageable) {
        return stockMovementRepository.findAll(pageable)
                .map(StockMovementResponse::fromDomain);
    }
}