package com.nexora.application.usecase;

import com.nexora.application.dto.stock.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Input Port — consulta do histórico de movimentações de estoque.
 *
 * As operações de escrita (replenish, decrement) vivem no ProductUseCase
 * e são disparadas pelo OrderApplicationService.
 * Este caso de uso é exclusivo de leitura/auditoria.
 */
public interface StockUseCase {

    /**
     * Retorna o histórico de movimentações de um produto específico.
     * Acesso: SELLER, MANAGER, ADMIN.
     */
    Page<StockMovementResponse> findByProduct(UUID productId, Pageable pageable);

    /**
     * Retorna todas as movimentações de estoque (todos os produtos).
     * Acesso: MANAGER, ADMIN.
     */
    Page<StockMovementResponse> findAll(Pageable pageable);
}