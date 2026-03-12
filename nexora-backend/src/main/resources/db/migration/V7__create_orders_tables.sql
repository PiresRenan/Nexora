-- V7__create_orders_tables.sql

CREATE TABLE orders (
                        id          UUID        NOT NULL,
                        customer_id UUID        NOT NULL REFERENCES users(id),
                        status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        notes       VARCHAR(500),
                        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                        CONSTRAINT pk_orders PRIMARY KEY (id),
                        CONSTRAINT chk_orders_status CHECK (
                            status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')
                            )
);

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status   ON orders (status);
CREATE INDEX idx_orders_created  ON orders (created_at DESC);

-- Itens do pedido: snapshot de produto + preço no momento da compra
-- ElementCollection do JPA — sem tabela de entidade, apenas @Embeddable
CREATE TABLE order_items (
                             order_id     UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id   UUID           NOT NULL,
                             product_name VARCHAR(100)   NOT NULL,
                             product_sku  VARCHAR(20)    NOT NULL,
                             unit_price   NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
                             currency     VARCHAR(3)        NOT NULL,
                             quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order   ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

COMMENT ON TABLE orders      IS 'Pedidos de compra dos clientes Nexora';
COMMENT ON TABLE order_items IS 'Itens de pedido com snapshot de preço no momento da compra';