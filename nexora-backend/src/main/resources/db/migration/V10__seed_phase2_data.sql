-- V10__seed_phase2_data.sql (CORRIGIDO)
-- Seed de categorias, movimentações de estoque e usuários de negócio
-- Compatível com as migrações V1 a V6 e seed V3 corrigida.
-- 10 categorias + 82 movimentos + 20 novos usuários = 112 registros.
-- ATENÇÃO: IDs de usuários com prefixo 'f' (hexadecimal) para não conflitar com seed V3 (prefixo 'a').

---------------------------------------------------
-- CATEGORIES
---------------------------------------------------

INSERT INTO categories (id, name, description, active, created_at, updated_at) VALUES
                                                                                   ('c0000000-0000-0000-0000-000000000001','Computadores','Notebooks, workstations e desktops',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000002','Periféricos','Mouse, teclados e dispositivos de entrada',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000003','Monitores','Monitores profissionais e gamers',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000004','Armazenamento','SSD, HD e dispositivos de armazenamento',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000005','Componentes','Memória, GPU e componentes internos',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000006','Acessórios','Suportes, docks e acessórios diversos',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000007','Áudio','Headsets, microfones e áudio profissional',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000008','Vídeo e Conferência','Webcams e equipamentos de conferência',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000009','Equipamentos Gamer','Equipamentos voltados para gaming',TRUE,NOW(),NOW()),
                                                                                   ('c0000000-0000-0000-0000-000000000010','Produtos Descontinuados','Produtos fora de linha',FALSE,NOW(),NOW());

---------------------------------------------------
-- ASSOCIAÇÃO PRODUTOS -> CATEGORIAS
---------------------------------------------------

-- COMPUTADORES
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000001'
WHERE sku IN ('NB-PRO-001','NB-ULTRA-002','WS-GFX-001');

-- PERIFÉRICOS
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000002'
WHERE sku IN ('MS-ERG-001','MS-GMR-002','KB-MEC-001','KB-CPT-002');

-- MONITORES
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000003'
WHERE sku IN ('MN-24FHD-001','MN-27QHD-002','MN-UW-003');

-- ARMAZENAMENTO
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000004'
WHERE sku IN ('SSD-NVME-001','SSD-NVME-002');

-- COMPONENTES
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000005'
WHERE sku IN ('GPU-RTX-001','RAM-32GB-001');

-- ÁUDIO
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000007'
WHERE sku='HS-PRO-001';

-- VÍDEO
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000008'
WHERE sku='WC-HD-001';

-- ACESSÓRIOS
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000006'
WHERE sku IN ('DK-USBC-001','SP-NB-001');

-- PRODUTOS DESCONTINUADOS
UPDATE products SET category_id='c0000000-0000-0000-0000-000000000010'
WHERE sku IN ('KB-OLD-001','MS-OLD-001');

---------------------------------------------------
-- MOVIMENTAÇÃO INICIAL DE ESTOQUE
---------------------------------------------------

INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
SELECT
    gen_random_uuid(),
    id,
    'ENTRY',
    stock_quantity,
    0,
    stock_quantity,
    'INITIAL_STOCK',
    NULL,                       -- reference_id
    'a0000000-0000-0000-0000-000000000001', -- Admin
    created_at
FROM products
WHERE stock_quantity > 0;  -- 45 produtos com estoque >0 na seed V3

---------------------------------------------------
-- MOVIMENTAÇÕES ADICIONAIS (SIMULAÇÃO REAL)
-- Todas com type 'ENTRY' ou 'EXIT' e quantity positiva.
-- performed_by = admin para simplificar.
---------------------------------------------------

-- Vendas simuladas (EXIT)
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000001','EXIT',2,15,13,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000004','EXIT',5,50,45,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000008','EXIT',3,18,15,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

-- Reposição de estoque (ENTRY)
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000015','ENTRY',10,3,13,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000016','ENTRY',5,2,7,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

-- Devolução de cliente (ENTRY)
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000004','ENTRY',1,45,46,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

-- Ajuste manual (ENTRY pois aumentou estoque)
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000007','ENTRY',2,30,32,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

---------------------------------------------------
-- NOVAS MOVIMENTAÇÕES PARA ATINGIR 100+ REGISTROS
---------------------------------------------------

-- Vendas adicionais (mais 10) - todas EXIT
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000002','EXIT',1,22,21,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '5 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000005','EXIT',3,38,35,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '4 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000009','EXIT',2,11,9,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '3 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000011','EXIT',4,27,23,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '2 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000013','EXIT',2,19,17,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '1 day'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000021','EXIT',1,8,7,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000025','EXIT',5,20,15,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000027','EXIT',20,100,80,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000032','EXIT',3,14,11,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000035','EXIT',2,21,19,'SALE',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

-- Reposições de fornecedor (mais 5) - ENTRY
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000001','ENTRY',5,13,18,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '10 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000004','ENTRY',20,46,66,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '8 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000008','ENTRY',10,15,25,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '6 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000014','ENTRY',30,65,95,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '4 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000022','ENTRY',6,12,18,'SUPPLIER_RESTOCK',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '2 days');

-- Devoluções de clientes (mais 5) - ENTRY
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000002','ENTRY',1,21,22,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '7 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000005','ENTRY',2,35,37,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '5 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000009','ENTRY',1,9,10,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '3 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000011','ENTRY',1,23,24,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '1 day'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000035','ENTRY',1,19,20,'CUSTOMER_RETURN',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

-- Ajustes de inventário (mais 5) - converter para ENTRY/EXIT conforme sinal
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000003','EXIT',1,5,4,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '9 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000006','ENTRY',2,12,14,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '7 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000012','EXIT',2,40,38,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '5 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000024','ENTRY',1,7,8,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '3 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000037','EXIT',5,200,195,'INVENTORY_ADJUSTMENT',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '1 day');

-- Perdas (quebras/extraviados) (mais 5) - EXIT
INSERT INTO stock_movements
(id, product_id, type, quantity, stock_before, stock_after, reason, reference_id, performed_by, occurred_at)
VALUES
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000028','EXIT',1,45,44,'LOSS',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '6 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000033','EXIT',1,9,8,'LOSS',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '4 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000038','EXIT',2,25,23,'LOSS',NULL,'a0000000-0000-0000-0000-000000000001',NOW() - INTERVAL '2 days'),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000041','EXIT',1,4,3,'LOSS',NULL,'a0000000-0000-0000-0000-000000000001',NOW()),
    (gen_random_uuid(),'b0000000-0000-0000-0000-000000000046','EXIT',1,7,6,'LOSS',NULL,'a0000000-0000-0000-0000-000000000001',NOW());

---------------------------------------------------
-- USUÁRIOS DE NEGÓCIO (IDs com prefixo 'f' - hexadecimal válido)
-- Roles corrigidas: 'USER' substituído por 'CUSTOMER' ou 'SELLER' conforme contexto.
---------------------------------------------------

INSERT INTO users (id, name, email, password_hash, role, active, created_at, updated_at) VALUES
                                                                                             ('f0000000-0000-0000-0000-000000000021','Maria Vendedora','vendedor@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','SELLER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000022','Carlos Cliente','cliente@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000023','Fernanda Cliente','fernanda@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000024','Ricardo Vendas','ricardo.vendas@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','SELLER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000025','Suporte Nexora','suporte@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','MANAGER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000026','Ana Comercial','ana.comercial@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','SELLER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000027','Bruno Comprador','bruno.comprador@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000028','Carla Parceira','carla.parceira@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000029','Daniel Supervisor','daniel.supervisor@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','MANAGER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000030','Elena Suprimentos','elena.suprimentos@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','SELLER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000031','Fábio Estagiário','fabio.estagiario@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000032','Gabriela Financeiro','gabriela.financeiro@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000033','Hélio Inativo','helio.inativo@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',FALSE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000034','Isabela Marketing','isabela.marketing@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000035','João Representante','joao.representante@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','SELLER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000036','Karina Logística','karina.logistica@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000037','Leandro TI','leandro.ti@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','MANAGER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000038','Marta Cliente Corporativo','marta.corp@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000039','Nelson Fornecedor','nelson.fornecedor@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','CUSTOMER',TRUE,NOW(),NOW()),
                                                                                             ('f0000000-0000-0000-0000-000000000040','Olívia Admin','olivia.admin@nexora.com','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6hsxq8YtS.','ADMIN',TRUE,NOW(),NOW());