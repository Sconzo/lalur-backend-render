-- ============================================================================
-- Create ECF File Table
-- Version: V014
-- Date: 2026-03-11
-- ============================================================================
--
-- Stores all three types of ECF files:
--   ARQUIVO_PARCIAL  - generated from LancamentosParteB
--   IMPORTED_ECF     - uploaded by user from Receita Federal
--   COMPLETE_ECF     - merge result of the two above
--
-- Unique constraint: (file_type, company_id, fiscal_year) enforces one file
-- per type per company per year (upsert / saveOrReplace semantics).
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_ecf_file (
    id BIGSERIAL PRIMARY KEY,

    -- File identification
    file_type   VARCHAR(30)  NOT NULL,
    company_id  BIGINT       NOT NULL REFERENCES tb_empresa(id),
    fiscal_year INTEGER      NOT NULL,

    -- File content (stored as TEXT, ISO-8859-1 encoding)
    content     TEXT         NOT NULL,
    file_name   VARCHAR(255),

    -- Lifecycle status
    file_status VARCHAR(20),

    -- Validation errors (JSON array, populated when file_status = 'ERROR')
    validation_errors TEXT,

    -- Generation metadata
    generated_at  TIMESTAMP,
    generated_by  VARCHAR(255),

    -- Source references for COMPLETE_ECF (self-referential FKs)
    source_imported_ecf_id  BIGINT REFERENCES tb_ecf_file(id),
    source_parcial_file_id  BIGINT REFERENCES tb_ecf_file(id),

    -- Audit fields (BaseEntity)
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por   BIGINT       NOT NULL DEFAULT 1,
    atualizado_por BIGINT,

    -- Unique constraint: one file per type per company per year
    CONSTRAINT uk_ecf_file_type_company_year UNIQUE (file_type, company_id, fiscal_year)
);

CREATE INDEX IF NOT EXISTS idx_ecf_file_company_year
    ON tb_ecf_file(company_id, fiscal_year);

CREATE INDEX IF NOT EXISTS idx_ecf_file_status
    ON tb_ecf_file(status);

COMMENT ON TABLE tb_ecf_file IS 'Arquivos ECF (Parcial, Importado e Completo) por empresa e ano fiscal';
COMMENT ON COLUMN tb_ecf_file.content IS 'Conteúdo do arquivo em texto ISO-8859-1 (SPED ECF)';
COMMENT ON COLUMN tb_ecf_file.validation_errors IS 'JSON array de erros quando file_status = ERROR';
