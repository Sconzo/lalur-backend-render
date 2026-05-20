-- ============================================================================
-- Create Periodo Contabil Audit Table
-- Version: V006
-- Date: 2025-12-24
-- Período Contábil e Bloqueio Temporal
-- ============================================================================
--
-- This migration creates the tb_periodo_contabil_audit table to store
-- audit trail of all changes made to the período contábil field of companies.
--
-- ============================================================================

-- Create table
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_periodo_contabil_audit (
    id BIGSERIAL PRIMARY KEY,

    -- Relacionamento com empresa
    company_id BIGINT NOT NULL,

    -- Valores do período contábil
    periodo_contabil_anterior DATE NOT NULL,
    periodo_contabil_novo DATE NOT NULL,

    -- Auditoria
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL,

    -- Foreign key constraint (ON DELETE CASCADE - se empresa for deletada, histórico também é)
    CONSTRAINT fk_periodo_contabil_audit_company
        FOREIGN KEY (company_id)
        REFERENCES tb_empresa(id)
        ON DELETE CASCADE
);

-- Create indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_periodo_contabil_audit_company_id
    ON tb_periodo_contabil_audit(company_id);

CREATE INDEX IF NOT EXISTS idx_periodo_contabil_audit_changed_at
    ON tb_periodo_contabil_audit(changed_at DESC);

-- Add comments
-- ============================================================================

COMMENT ON TABLE tb_periodo_contabil_audit IS
    'Log de auditoria de alterações do Período Contábil de empresas';

COMMENT ON COLUMN tb_periodo_contabil_audit.company_id IS
    'FK para tb_empresa - empresa que teve o período contábil alterado';

COMMENT ON COLUMN tb_periodo_contabil_audit.periodo_contabil_anterior IS
    'Valor anterior do período contábil';

COMMENT ON COLUMN tb_periodo_contabil_audit.periodo_contabil_novo IS
    'Novo valor do período contábil';

COMMENT ON COLUMN tb_periodo_contabil_audit.changed_by IS
    'Email do usuário que fez a alteração';

COMMENT ON COLUMN tb_periodo_contabil_audit.changed_at IS
    'Data e hora em que a alteração foi feita';

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify table structure:
--    SELECT column_name, data_type, is_nullable, column_default
--    FROM information_schema.columns
--    WHERE table_name = 'tb_periodo_contabil_audit'
--    ORDER BY ordinal_position;
--
-- 2. Verify indexes:
--    SELECT indexname, indexdef
--    FROM pg_indexes
--    WHERE tablename = 'tb_periodo_contabil_audit';
--
-- 3. Verify constraints:
--    SELECT constraint_name, constraint_type
--    FROM information_schema.table_constraints
--    WHERE table_name = 'tb_periodo_contabil_audit';
--
-- ============================================================================
