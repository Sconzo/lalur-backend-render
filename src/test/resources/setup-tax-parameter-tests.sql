-- Setup test data for tax parameter association tests
-- Create an INACTIVE parameter for testing rejection
INSERT INTO tb_parametros_tributarios (
    codigo,
    tipo,
    descricao,
    status,
    criado_em,
    criado_por,
    atualizado_em
) VALUES (
    '999',
    'FORMA_TRIB_LUCRO_REAL',
    'Parâmetro Inativo para Testes',
    'INACTIVE',
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- Ensure company ID 1 exists for tests
INSERT INTO tb_empresa (
    id,
    cnpj,
    razao_social,
    periodo_contabil,
    status,
    criado_em,
    criado_por,
    atualizado_em
) VALUES (
    1,
    '12345678000190',
    'Empresa Teste LTDA',
    '2025-12-01',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence
SELECT setval('tb_empresa_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_empresa), true);

-- Clean existing associations for company 1 to start fresh
DELETE FROM tb_empresa_parametros_tributarios WHERE empresa_id = 1;
