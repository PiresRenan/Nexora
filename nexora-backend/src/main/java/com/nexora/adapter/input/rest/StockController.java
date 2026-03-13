package com.nexora.adapter.input.rest;

import com.nexora.application.dto.stock.StockMovementResponse;
import com.nexora.application.usecase.StockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Expõe a trilha de auditoria de movimentações de estoque.
 *
 * Todas as rotas são protegidas — estoque é informação operacional sensível.
 * Vendedores (SELLER) podem consultar movimentações de produtos específicos.
 * Gerentes e administradores (MANAGER/ADMIN) têm visão global.
 *
 * Escrita (replenish) continua em PATCH /api/v1/products/{id}/stock/replenish.
 */
@RestController
@RequestMapping("/api/v1/stock")
@Tag(name = "Stock", description = "Histórico e auditoria de movimentações de estoque")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private final StockUseCase stockUseCase;

    public StockController(StockUseCase stockUseCase) {
        this.stockUseCase = stockUseCase;
    }

    /**
     * Histórico de movimentações de um produto específico.
     * Ordenado por data desc por padrão — mais recentes primeiro.
     */
    @GetMapping("/{productId}/movements")
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "Movimentações de estoque de um produto",
            description = """
            Retorna o histórico paginado de todas as entradas e saídas do estoque
            de um produto. Inclui: tipo (ENTRY/EXIT), quantidade, estoque antes/depois,
            motivo (ORDER_CONFIRMED, MANUAL_REPLENISHMENT, ORDER_CANCELLED, INITIAL_STOCK),
            referência (orderId quando aplicável) e executor.
            
            Ordenação padrão: mais recentes primeiro (`occurredAt,desc`).
            """
    )
    public ResponseEntity<Page<StockMovementResponse>> findByProduct(
            @PathVariable UUID productId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(stockUseCase.findByProduct(productId, pageable));
    }

    /**
     * Histórico global de movimentações — todos os produtos.
     * Restrito a gerentes e administradores.
     */
    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(
            summary = "Histórico global de movimentações (MANAGER+)",
            description = "Visão completa de todas as movimentações de estoque. Útil para auditoria e relatórios operacionais."
    )
    public ResponseEntity<Page<StockMovementResponse>> findAll(
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(stockUseCase.findAll(pageable));
    }
}