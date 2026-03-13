-- Sobrescreve funções PostgreSQL-específicas para compatibilidade H2
-- Este script roda APÓS as migrations principais (V90 >> V8)

-- H2 não suporta TIMESTAMPTZ: já definimos as colunas como TIMESTAMP no DDL abaixo
-- Recria as tabelas usando sintaxe H2 se necessário
-- Na prática, com H2 MODE=PostgreSQL, as migrations funcionam exceto por:
--   1. gen_random_uuid() → RANDOM_UUID()
--   2. TIMESTAMPTZ → TIMESTAMP (H2 aceita TIMESTAMP WITH TIME ZONE, mas não TIMESTAMPTZ)

-- Não precisa de DDL extra: as migrations V1-V7 são re-executadas com
-- compatibilidade via spring.flyway.locations incluindo ambas as pastas.
-- Este arquivo serve como placeholder e pode receber patches futuros.

-- Alias para gen_random_uuid caso seja chamado no contexto H2
CREATE ALIAS IF NOT EXISTS GEN_RANDOM_UUID FOR "java.util.UUID.randomUUID";
SQL