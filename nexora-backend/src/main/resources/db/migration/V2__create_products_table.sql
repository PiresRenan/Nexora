-- V2__create_products_table.sql

CREATE TABLE products (
                          id             UUID           NOT NULL,
                          name           VARCHAR(100)   NOT NULL,
                          description    VARCHAR(500),
                          sku            VARCHAR(20)    NOT NULL,
                          price          NUMERIC(12, 2) NOT NULL,
                          currency       VARCHAR(3)        NOT NULL DEFAULT 'BRL',
                          stock_quantity INTEGER        NOT NULL DEFAULT 0,
                          active         BOOLEAN        NOT NULL DEFAULT TRUE,
                          created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

                          CONSTRAINT pk_products PRIMARY KEY (id),
                          CONSTRAINT uq_products_sku UNIQUE (sku),
                          CONSTRAINT chk_products_price CHECK (price >= 0),
                          CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
                          CONSTRAINT chk_products_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX idx_products_sku    ON products (sku);
CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_name   ON products (name);

COMMENT ON TABLE products IS 'Catálogo de produtos da loja Nexora';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit — identificador único do produto';
COMMENT ON COLUMN products.active IS 'Soft delete — false = produto desativado/descontinuado';
COMMENT ON COLUMN products.currency IS 'Código ISO 4217 da moeda (ex: BRL, USD)';