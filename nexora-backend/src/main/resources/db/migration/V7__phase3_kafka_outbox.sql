-- V7__phase3_kafka_outbox.sql
-- Transactional Outbox Pattern — garante entrega confiável de eventos de domínio.
-- Um job (ou Change Data Capture) lê esta tabela e publica no Kafka,
-- evitando o problema de "dual write" (escrever no DB e no Kafka atomicamente).

CREATE TABLE domain_events_outbox (
                                      id              UUID         NOT NULL,
                                      aggregate_type  VARCHAR(50)  NOT NULL,
                                      aggregate_id    UUID         NOT NULL,
                                      event_type      VARCHAR(100) NOT NULL,
                                      payload         TEXT         NOT NULL,   -- JSON serializado do evento
                                      occurred_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                                      published_at    TIMESTAMP,               -- NULL = pendente de publicação
                                      retry_count     INT          NOT NULL DEFAULT 0,

                                      CONSTRAINT pk_domain_events_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_pending ON domain_events_outbox (published_at)
    WHERE published_at IS NULL;

CREATE INDEX idx_outbox_aggregate ON domain_events_outbox (aggregate_type, aggregate_id);

COMMENT ON TABLE domain_events_outbox IS
    'Transactional Outbox: eventos de domínio persistidos na mesma transação do aggregate.';
COMMENT ON COLUMN domain_events_outbox.published_at IS
    'NULL = aguardando publicação no Kafka; preenchido pelo relay job após envio bem-sucedido.';

-- Tabela de cache de sessões Redis (opcional — para auditoria de tokens revogados)
CREATE TABLE revoked_tokens (
                                jti             UUID         NOT NULL,   -- JWT ID
                                user_id         UUID         NOT NULL,
                                revoked_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                                expires_at      TIMESTAMP    NOT NULL,

                                CONSTRAINT pk_revoked_tokens PRIMARY KEY (jti)
);

CREATE INDEX idx_revoked_tokens_user ON revoked_tokens (user_id);
CREATE INDEX idx_revoked_tokens_expires ON revoked_tokens (expires_at);

COMMENT ON TABLE revoked_tokens IS
    'Tokens JWT revogados antes da expiração (logout, troca de senha). TTL gerenciado por job de limpeza.';