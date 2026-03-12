-- V3__seed_initial_data.sql
-- Dados iniciais para facilitar o desenvolvimento e testes manuais
-- Senha: 'admin@123' encodada com BCrypt (strength 12)

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at)
VALUES
    (
        'a0000000-0000-0000-0000-000000000001',
        'Admin Nexora',
        'admin@nexora.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.',
        'ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'a0000000-0000-0000-0000-000000000002',
        'João Gerente',
        'gerente@nexora.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.',
        'MANAGER',
        TRUE,
        NOW(),
        NOW()
    );

INSERT INTO products (id, name, description, sku, price, currency, stock_quantity, active, created_at, updated_at)
VALUES
    (
        'b0000000-0000-0000-0000-000000000001',
        'Notebook Profissional',
        'Notebook de alta performance para uso profissional',
        'NB-PRO-001',
        4999.99,
        'BRL',
        15,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'b0000000-0000-0000-0000-000000000002',
        'Mouse Ergonômico',
        'Mouse sem fio com design ergonômico',
        'MS-ERG-001',
        199.90,
        'BRL',
        50,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'b0000000-0000-0000-0000-000000000003',
        'Teclado Mecânico',
        'Teclado mecânico com switch Cherry MX',
        'KB-MEC-001',
        599.00,
        'BRL',
        0,
        FALSE,
        NOW(),
        NOW()
    );