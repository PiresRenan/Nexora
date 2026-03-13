-- V1__create_users_table.sql
-- Melhoria: UUID como PK (distribuído, sem hot spot em sequences)
-- Enum como VARCHAR para legibilidade no banco e compatibilidade com migrações futuras
-- Índices criados explicitamente (não delegado ao Hibernate)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id           UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP  NOT NULL DEFAULT NOW(),

CONSTRAINT pk_users PRIMARY KEY (id),
CONSTRAINT uq_users_email UNIQUE (email),
CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'SELLER', 'MANAGER', 'ADMIN'))
        );

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_active ON users (active);

COMMENT ON TABLE users IS 'Usuários do sistema Nexora (clientes e funcionários)';
COMMENT ON COLUMN users.role IS 'Papel do usuário: CUSTOMER, SELLER, MANAGER, ADMIN';
COMMENT ON COLUMN users.active IS 'Soft delete — false = desativado';