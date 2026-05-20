-- ============================================================================
-- Create Valores Parametros Temporais Table
-- Version: V005
-- Date: 2025-12-24
-- ADR: ADR-001
-- ============================================================================
--
-- This migration creates the tb_valores_parametros_temporais table to store
-- temporal periods (monthly or quarterly) when tax parameters are active
-- for a company.
--
-- ============================================================================

-- Create table
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_valores_parametros_temporais (
    id BIGSERIAL PRIMARY KEY,

    -- Relacionamento
    empresa_parametros_tributarios_id BIGINT NOT NULL
        REFERENCES tb_empresa_parametros_tributarios(id) ON DELETE CASCADE,

    -- Período temporal
    ano INTEGER NOT NULL,  -- Ano (4 dígitos, ex: 2024)
    mes INTEGER,  -- Mês (1-12) para periodicidade mensal, NULL para trimestral
    trimestre INTEGER,  -- Trimestre (1-4) para periodicidade trimestral, NULL para mensal

    -- Campos de auditoria (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1 REFERENCES tb_usuario(id) ON DELETE SET NULL,
    atualizado_por BIGINT REFERENCES tb_usuario(id) ON DELETE SET NULL,

    -- Constraints
    CONSTRAINT uk_valores_temporais_periodo
        UNIQUE (empresa_parametros_tributarios_id, ano, mes, trimestre),

    -- Validações de negócio
    CONSTRAINT chk_valores_temporais_periodicidade
        CHECK (
            (mes IS NOT NULL AND trimestre IS NULL) OR  -- Mensal
            (mes IS NULL AND trimestre IS NOT NULL)     -- Trimestral
        ),
    CONSTRAINT chk_valores_temporais_mes
        CHECK (mes IS NULL OR (mes >= 1 AND mes <= 12)),
    CONSTRAINT chk_valores_temporais_trimestre
        CHECK (trimestre IS NULL OR (trimestre >= 1 AND trimestre <= 4)),
    CONSTRAINT chk_valores_temporais_ano
        CHECK (ano >= 2000 AND ano <= 2100)
);

-- Create indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_valores_temporais_empresa_param
    ON tb_valores_parametros_temporais(empresa_parametros_tributarios_id);

CREATE INDEX IF NOT EXISTS idx_valores_temporais_ano
    ON tb_valores_parametros_temporais(ano);

CREATE INDEX IF NOT EXISTS idx_valores_temporais_periodo
    ON tb_valores_parametros_temporais(ano, mes, trimestre);

CREATE INDEX IF NOT EXISTS idx_valores_temporais_status
    ON tb_valores_parametros_temporais(status);

-- Add comments
-- ============================================================================

COMMENT ON TABLE tb_valores_parametros_temporais IS
    'Períodos (mensais ou trimestrais) em que parâmetros tributários estão ativos para empresas (ADR-001)';

COMMENT ON COLUMN tb_valores_parametros_temporais.empresa_parametros_tributarios_id IS
    'FK para tb_empresa_parametros_tributarios - relacionamento com parâmetro da empresa';

COMMENT ON COLUMN tb_valores_parametros_temporais.ano IS
    'Ano do período (4 dígitos, ex: 2024)';

COMMENT ON COLUMN tb_valores_parametros_temporais.mes IS
    'Mês (1-12) para periodicidade mensal, NULL para trimestral';

COMMENT ON COLUMN tb_valores_parametros_temporais.trimestre IS
    'Trimestre (1-4) para periodicidade trimestral, NULL para mensal';

COMMENT ON CONSTRAINT chk_valores_temporais_periodicidade ON tb_valores_parametros_temporais IS
    'Garante que ou mes OU trimestre está preenchido (nunca ambos ou nenhum)';

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify table structure:
--    SELECT column_name, data_type, is_nullable, column_default
--    FROM information_schema.columns
--    WHERE table_name = 'tb_valores_parametros_temporais'
--    ORDER BY ordinal_position;
--
-- 2. Verify indexes:
--    SELECT indexname, indexdef
--    FROM pg_indexes
--    WHERE tablename = 'tb_valores_parametros_temporais';
--
-- 3. Verify constraints:
--    SELECT constraint_name, constraint_type
--    FROM information_schema.table_constraints
--    WHERE table_name = 'tb_valores_parametros_temporais';
--
-- 4. Test data insertion (monthly):
--    INSERT INTO tb_valores_parametros_temporais
--        (empresa_parametros_tributarios_id, ano, mes, trimestre, criado_por)
--    VALUES (1, 2024, 1, NULL, 1);
--
-- 5. Test data insertion (quarterly):
--    INSERT INTO tb_valores_parametros_temporais
--        (empresa_parametros_tributarios_id, ano, mes, trimestre, criado_por)
--    VALUES (1, 2024, NULL, 1, 1);
--
-- ============================================================================
