-- V4__add_category_to_products.sql
-- Adiciona referência de categoria aos produtos (opcional — nullable)
ALTER TABLE products
    ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX idx_products_category ON products (category_id);

COMMENT ON COLUMN products.category_id IS 'Categoria do produto (nullable — produto pode não ter categoria)';