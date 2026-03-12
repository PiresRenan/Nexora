-- V6__create_stock_movements_table.sql
-- Auditoria completa de movimentos de estoque
-- Imutável por design: nunca atualizar, apenas inserir

CREATE TABLE stock_movements (
                                 id           UUID         NOT NULL,
                                 product_id   UUID         NOT NULL REFERENCES products(id),
                                 type         VARCHAR(10)  NOT NULL,           -- ENTRY | EXIT
                                 quantity     INTEGER      NOT NULL CHECK (quantity > 0),
                                 stock_before INTEGER      NOT NULL CHECK (stock_before >= 0),
                                 stock_after  INTEGER      NOT NULL CHECK (stock_after >= 0),
                                 reason       VARCHAR(100),                    -- ORDER_CONFIRMED, MANUAL_REPLENISHMENT, etc.
                                 reference_id UUID,                            -- orderId ou outro contexto
                                 performed_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                 occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                                 CONSTRAINT pk_stock_movements PRIMARY KEY (id),
                                 CONSTRAINT chk_stock_movements_type CHECK (type IN ('ENTRY', 'EXIT'))
);

CREATE INDEX idx_stock_movements_product  ON stock_movements (product_id);
CREATE INDEX idx_stock_movements_occurred ON stock_movements (occurred_at DESC);
CREATE INDEX idx_stock_movements_type     ON stock_movements (type);

COMMENT ON TABLE stock_movements IS 'Log imutável de todas as movimentações de estoque';
COMMENT ON COLUMN stock_movements.reason IS 'Motivo: ORDER_CONFIRMED, ORDER_CANCELLED, MANUAL_REPLENISHMENT, INITIAL_STOCK';