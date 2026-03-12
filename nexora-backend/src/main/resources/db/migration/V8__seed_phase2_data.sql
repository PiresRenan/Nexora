-- V8__seed_phase2_data.sql

-- Categorias iniciais
INSERT INTO categories (id, name, description, active, created_at, updated_at) VALUES
                                                                                   ('c0000000-0000-0000-0000-000000000001', 'Informática',      'Computadores, periféricos e acessórios', TRUE, NOW(), NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000002', 'Periféricos',      'Mouse, teclado e outros periféricos',    TRUE, NOW(), NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000003', 'Smartphones',      'Celulares e acessórios',                 TRUE, NOW(), NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000004', 'Áudio e Vídeo',    'Fones, caixas de som e câmeras',         TRUE, NOW(), NOW());

-- Atribui categorias aos produtos já existentes (seed V3)
UPDATE products SET category_id = 'c0000000-0000-0000-0000-000000000001'
WHERE id = 'b0000000-0000-0000-0000-000000000001'; -- Notebook

UPDATE products SET category_id = 'c0000000-0000-0000-0000-000000000002'
WHERE id = 'b0000000-0000-0000-0000-000000000002'; -- Mouse

UPDATE products SET category_id = 'c0000000-0000-0000-0000-000000000002'
WHERE id = 'b0000000-0000-0000-0000-000000000003'; -- Teclado

-- Registra estoque inicial dos produtos seed como StockMovement
INSERT INTO stock_movements (id, product_id, type, quantity, stock_before, stock_after, reason, occurred_at)
SELECT gen_random_uuid(), id, 'ENTRY', stock_quantity, 0, stock_quantity, 'INITIAL_STOCK', created_at
FROM products
WHERE stock_quantity > 0;

-- Vendedor para testes
INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at) VALUES
                                                                                             (
                                                                                                 'a0000000-0000-0000-0000-000000000003',
                                                                                                 'Maria Vendedora',
                                                                                                 'vendedor@nexora.com',
                                                                                                 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.',
                                                                                                 'SELLER',
                                                                                                 TRUE, NOW(), NOW()
                                                                                             ),
                                                                                             (
                                                                                                 'a0000000-0000-0000-0000-000000000004',
                                                                                                 'Carlos Cliente',
                                                                                                 'cliente@nexora.com',
                                                                                                 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.',
                                                                                                 'CUSTOMER',
                                                                                                 TRUE, NOW(), NOW()
                                                                                             );