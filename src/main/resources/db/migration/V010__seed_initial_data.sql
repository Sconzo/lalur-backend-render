-- ============================================================================
-- Seed Initial Data
-- Version: V010
-- Date: 2026-02-05
-- ============================================================================
--
-- This migration adds initial seed data:
-- 1. Admin user for first login
-- 2. Example company for testing
-- 3. Additional tax parameter types and parameters
-- 4. Company-parameter associations and temporal values
--
-- ============================================================================

-- ============================================================================
-- 1. Create ADMIN user (default login)
-- ============================================================================
-- Password: Admin@123 (BCrypt strength 12)
-- IMPORTANT: Change password after first login!

INSERT INTO tb_usuario (
    primeiro_nome,
    sobrenome,
    email,
    senha,
    funcao,
    status,
    deve_mudar_senha,
    criado_em,
    criado_por
) VALUES (
    'Admin',
    'Sistema',
    'admin@gmail.com',
    '$2a$12$qAYdO1nyZ3mndqAv.XvfJecnAwEtdc18gUwuJHfvuD0Gxh7AJntAC',
    'ADMIN',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    1
)
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- 2. Add missing Tax Parameter Types
-- ============================================================================
-- Some types may already exist from V009 migration (migrated from old 'tipo' column)

INSERT INTO tb_tipos_parametros_tributarios (descricao, natureza, status, criado_em, criado_por)
VALUES
    ('FORMA_TRIB_LUCRO_REAL', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('PERIODO_DE_APURACAO', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('QUALIFICACAO_PJ', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('CRITERIO_RECONHECIMENTO__RECEITA', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('ESTIMATIVA_MENSAL', 'MONTHLY', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('FORMA_TRIBUTACAO', 'QUARTERLY', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('NATUREZA_JURIDICA', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1),
    ('CNAE', 'GLOBAL', 'ACTIVE', CURRENT_TIMESTAMP, 1)
ON CONFLICT (descricao) DO NOTHING;

-- ============================================================================
-- 3. Add Tax Parameters (ECF standard codes)
-- ============================================================================

INSERT INTO tb_parametros_tributarios (
    codigo,
    tipo_parametro_id,
    descricao,
    status,
    criado_em,
    criado_por
)
SELECT v.codigo, t.id, v.descricao, 'ACTIVE', CURRENT_TIMESTAMP, 1
FROM (VALUES
    -- FORMA_TRIB_LUCRO_REAL
    ('1', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real'),
    ('2', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Real/Arbitrado'),
    ('3', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real'),
    ('4', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Real/Arbitrado'),
    ('5', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido'),
    ('6', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Arbitrado'),
    ('7', 'FORMA_TRIB_LUCRO_REAL', 'Lucro Presumido/Arbitrado'),
    ('8', 'FORMA_TRIB_LUCRO_REAL', 'Imune de IRPJ'),
    ('9', 'FORMA_TRIB_LUCRO_REAL', 'Isento do IRPJ'),
    -- PERIODO_DE_APURACAO
    ('A', 'PERIODO_DE_APURACAO', 'Anual'),
    ('T', 'PERIODO_DE_APURACAO', 'Trimestral'),
    -- QUALIFICACAO_PJ (complementar ao V002)
    ('01', 'QUALIFICACAO_PJ', 'PJ em Geral'),
    ('02', 'QUALIFICACAO_PJ', 'PJ Componente do Sistema Financeiro'),
    ('03', 'QUALIFICACAO_PJ', 'Sociedades Seguradoras, de Capitalização e Previdência'),
    -- CRITERIO_RECONHECIMENTO__RECEITA
    ('1', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de caixa'),
    ('2', 'CRITERIO_RECONHECIMENTO__RECEITA', 'Regime de competência'),
    -- ESTIMATIVA_MENSAL
    ('1', 'ESTIMATIVA_MENSAL', 'Receita Bruta e Acréscimos'),
    ('2', 'ESTIMATIVA_MENSAL', 'Balanço/Balancete de Suspensão/Redução'),
    -- FORMA_TRIBUTACAO
    ('P', 'FORMA_TRIBUTACAO', 'Presumido'),
    ('R', 'FORMA_TRIBUTACAO', 'Real'),
    ('A', 'FORMA_TRIBUTACAO', 'Arbitrado'),
    -- NATUREZA_JURIDICA (complementar ao V002)
    ('1015', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Federal'),
    ('1023', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Estadual ou do Distrito Federal'),
    ('1031', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Executivo Municipal'),
    ('1040', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Federal'),
    ('1058', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Estadual ou do Distrito Federal'),
    ('1066', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Legislativo Municipal'),
    ('1074', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Federal'),
    ('1082', 'NATUREZA_JURIDICA', 'Órgão Público do Poder Judiciário Estadual'),
    ('1104', 'NATUREZA_JURIDICA', 'Autarquia Federal'),
    ('1112', 'NATUREZA_JURIDICA', 'Autarquia Estadual ou do Distrito Federal'),
    ('1120', 'NATUREZA_JURIDICA', 'Autarquia Municipal'),
    -- CNAE (complementar ao V002)
    ('0111301', 'CNAE', 'Cultivo de arroz'),
    ('0111302', 'CNAE', 'Cultivo de milho'),
    ('0111303', 'CNAE', 'Cultivo de trigo'),
    ('0111399', 'CNAE', 'Cultivo de outros cereais não especificados anteriormente'),
    ('0112101', 'CNAE', 'Cultivo de algodão herbáceo'),
    ('0112102', 'CNAE', 'Cultivo de juta'),
    ('0112199', 'CNAE', 'Cultivo de outras fibras de lavoura temporária não especificadas anteriormente'),
    ('0113000', 'CNAE', 'Cultivo de cana-de-açúcar'),
    ('0114800', 'CNAE', 'Cultivo de fumo'),
    ('0115600', 'CNAE', 'Cultivo de soja'),
    ('0116401', 'CNAE', 'Cultivo de amendoim'),
    ('0116402', 'CNAE', 'Cultivo de girassol')
) AS v(codigo, tipo, descricao)
JOIN tb_tipos_parametros_tributarios t ON t.descricao = v.tipo
WHERE NOT EXISTS (
    SELECT 1 FROM tb_parametros_tributarios p
    WHERE p.codigo = v.codigo AND p.tipo_parametro_id = t.id
);

-- ============================================================================
-- 4. Create example company
-- ============================================================================

INSERT INTO tb_empresa (
    razao_social,
    cnpj,
    periodo_contabil,
    status,
    criado_em,
    criado_por
) VALUES (
    'Empresa Exemplo LTDA',
    '11222333000181',
    '2024-01-01',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
)
ON CONFLICT (cnpj) DO NOTHING;

-- ============================================================================
-- 5. Associate tax parameters to example company
-- ============================================================================

-- ESTIMATIVA_MENSAL parameters
INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id, criado_por, criado_em)
SELECT
    e.id,
    p.id,
    1,
    CURRENT_TIMESTAMP
FROM tb_empresa e
CROSS JOIN tb_parametros_tributarios p
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
WHERE e.cnpj = '11222333000181'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
  AND p.codigo IN ('1', '2')
  AND NOT EXISTS (
      SELECT 1 FROM tb_empresa_parametros_tributarios ept
      WHERE ept.empresa_id = e.id AND ept.parametro_tributario_id = p.id
  );

-- FORMA_TRIBUTACAO parameters
INSERT INTO tb_empresa_parametros_tributarios (empresa_id, parametro_tributario_id, criado_por, criado_em)
SELECT
    e.id,
    p.id,
    1,
    CURRENT_TIMESTAMP
FROM tb_empresa e
CROSS JOIN tb_parametros_tributarios p
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
WHERE e.cnpj = '11222333000181'
  AND t.descricao = 'FORMA_TRIBUTACAO'
  AND p.codigo IN ('P', 'R', 'A')
  AND NOT EXISTS (
      SELECT 1 FROM tb_empresa_parametros_tributarios ept
      WHERE ept.empresa_id = e.id AND ept.parametro_tributario_id = p.id
  );

-- ============================================================================
-- 6. Create temporal values for parameters
-- ============================================================================

-- Monthly values for ESTIMATIVA_MENSAL (code 1) - Year 2024
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    2024,
    m.mes,
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '11222333000181'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
  AND p.codigo = '1'
  AND NOT EXISTS (
      SELECT 1 FROM tb_valores_parametros_temporais vpt
      WHERE vpt.empresa_parametros_tributarios_id = ept.id
        AND vpt.ano = 2024
        AND vpt.mes = m.mes
        AND vpt.trimestre IS NULL
  );

-- Monthly values for ESTIMATIVA_MENSAL (code 2) - Year 2024
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    2024,
    m.mes,
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
JOIN tb_tipos_parametros_tributarios t ON p.tipo_parametro_id = t.id
CROSS JOIN (
    SELECT 1 AS mes UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
    SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) m
WHERE e.cnpj = '11222333000181'
  AND t.descricao = 'ESTIMATIVA_MENSAL'
  AND p.codigo = '2'
  AND NOT EXISTS (
      SELECT 1 FROM tb_valores_parametros_temporais vpt
      WHERE vpt.empresa_parametros_tributarios_id = ept.id
        AND vpt.ano = 2024
        AND vpt.mes = m.mes
        AND vpt.trimestre IS NULL
  );

-- Quarterly values for FORMA_TRIBUTACAO - Years 2023-2025
INSERT INTO tb_valores_parametros_temporais (empresa_parametros_tributarios_id, ano, mes, trimestre, status, criado_em, criado_por)
SELECT
    ept.id,
    y.ano,
    NULL,
    q.trimestre,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    1
FROM tb_empresa_parametros_tributarios ept
JOIN tb_empresa e ON ept.empresa_id = e.id
JOIN tb_parametros_tributarios p ON ept.parametro_tributario_id = p.id
JOIN tb_tipos_parametros_tributarios tp ON p.tipo_parametro_id = tp.id
CROSS JOIN (SELECT 2023 AS ano UNION ALL SELECT 2024 UNION ALL SELECT 2025) y
CROSS JOIN (SELECT 1 AS trimestre UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) q
WHERE e.cnpj = '11222333000181'
  AND tp.descricao = 'FORMA_TRIBUTACAO'
  AND p.codigo IN ('P', 'R', 'A')
  AND NOT EXISTS (
      SELECT 1 FROM tb_valores_parametros_temporais vpt
      WHERE vpt.empresa_parametros_tributarios_id = ept.id
        AND vpt.ano = y.ano
        AND vpt.mes IS NULL
        AND vpt.trimestre = q.trimestre
  );

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify admin user created:
--    SELECT id, email, funcao, deve_mudar_senha FROM tb_usuario WHERE email = 'admin@gmail.com';
--
-- 2. Verify tax parameter types:
--    SELECT * FROM tb_tipos_parametros_tributarios ORDER BY id;
--
-- 3. Verify example company:
--    SELECT * FROM tb_empresa WHERE cnpj = '12345678000199';
--
-- ============================================================================
