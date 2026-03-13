-- V10__register_and_performance_indexes.sql
-- Ajustes de performance e consistência para suportar os novos endpoints:
--   POST /api/v1/auth/register  (cadastro público)
--   PATCH /api/v1/users/{id}/activate  (reativação)
--   GET  /api/v1/orders?status=...   (filtro por status)
--   GET  /api/v1/stock/{id}/movements (histórico de estoque)

-- Índice composto em orders(customer_id, status) para a query
-- "pedidos PENDING de um cliente específico" — muito comum no fluxo de checkout
CREATE INDEX IF NOT EXISTS idx_orders_customer_status
    ON orders (customer_id, status);

-- Índice em orders(updated_at) para relatórios de pedidos recentemente atualizados
CREATE INDEX IF NOT EXISTS idx_orders_updated
    ON orders (updated_at DESC);

-- Índice em stock_movements(reference_id) para rastrear movimentações
-- de um pedido específico (útil para auditoria pós-cancelamento)
CREATE INDEX IF NOT EXISTS idx_stock_movements_reference
    ON stock_movements (reference_id)
    WHERE reference_id IS NOT NULL;

-- Índice em stock_movements(performed_by) para auditoria por operador
CREATE INDEX IF NOT EXISTS idx_stock_movements_performer
    ON stock_movements (performed_by)
    WHERE performed_by IS NOT NULL;

-- Comentários de documentação
COMMENT ON INDEX idx_orders_customer_status IS
    'Acelera "meus pedidos por status" e fila de trabalho por status do operador.';
COMMENT ON INDEX idx_stock_movements_reference IS
    'Rastreia todos os movimentos de estoque originados de um pedido específico.';