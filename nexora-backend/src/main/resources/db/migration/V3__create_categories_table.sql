-- V3__create_categories_table.sql
CREATE TABLE categories (
                            id          UUID        NOT NULL,
                            name        VARCHAR(80) NOT NULL,
                            description VARCHAR(300),
                            active      BOOLEAN     NOT NULL DEFAULT TRUE,
                            created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                            CONSTRAINT pk_categories PRIMARY KEY (id),
                            CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE INDEX idx_categories_name   ON categories (name);
CREATE INDEX idx_categories_active ON categories (active);

COMMENT ON TABLE categories IS 'Categorias de produtos do catálogo Nexora';