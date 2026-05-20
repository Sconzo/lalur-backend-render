-- ============================================================================
-- Create Missing Tables (previously created by JPA ddl-auto)
-- Version: V012
-- Date: 2026-02-18
-- ============================================================================
--
-- This migration creates all tables that were previously managed by
-- Hibernate ddl-auto: update. After this migration, ddl-auto will be
-- set to 'validate' to ensure Flyway is the single source of truth.
--
-- Tables created:
-- 1. tb_conta_referencial
-- 2. tb_plano_de_contas
-- 3. tb_conta_parte_b
-- 4. tb_lancamento_contabil
-- 5. tb_lancamento_parte_b
--
-- ============================================================================

-- ============================================================================
-- 1. Table: tb_conta_referencial (Contas Referenciais RFB)
-- ============================================================================
-- Tabela mestra global - não vinculada a empresas.

CREATE TABLE IF NOT EXISTS tb_conta_referencial (
    id BIGSERIAL PRIMARY KEY,

    -- Dados da conta referencial
    codigo_rfb VARCHAR(50) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    ano_validade INTEGER,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT,

    -- Unique constraint: mesmo código pode existir em anos diferentes
    CONSTRAINT uk_conta_referencial_codigo_ano UNIQUE (codigo_rfb, ano_validade)
);

CREATE INDEX IF NOT EXISTS idx_conta_referencial_codigo ON tb_conta_referencial(codigo_rfb);
CREATE INDEX IF NOT EXISTS idx_conta_referencial_status ON tb_conta_referencial(status);

COMMENT ON TABLE tb_conta_referencial IS 'Tabela mestra de Contas Referenciais RFB (universal, não vinculada a empresas)';

-- ============================================================================
-- 2. Table: tb_plano_de_contas (Plano de Contas Contábil)
-- ============================================================================
-- Contas contábeis por empresa e ano fiscal.

CREATE TABLE IF NOT EXISTS tb_plano_de_contas (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    company_id BIGINT NOT NULL REFERENCES tb_empresa(id),
    conta_referencial_id BIGINT REFERENCES tb_conta_referencial(id),

    -- Dados da conta
    code VARCHAR(50) NOT NULL,
    name VARCHAR(500) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    classe VARCHAR(50) NOT NULL,
    nivel INTEGER NOT NULL,
    natureza VARCHAR(20) NOT NULL,
    afeta_resultado BOOLEAN NOT NULL,
    dedutivel BOOLEAN NOT NULL,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT,

    -- Unique constraint: sem duplicação de código por empresa + ano
    CONSTRAINT uk_plano_de_contas_company_code_year UNIQUE (company_id, code, fiscal_year)
);

CREATE INDEX IF NOT EXISTS idx_plano_de_contas_company ON tb_plano_de_contas(company_id);
CREATE INDEX IF NOT EXISTS idx_plano_de_contas_fiscal_year ON tb_plano_de_contas(fiscal_year);
CREATE INDEX IF NOT EXISTS idx_plano_de_contas_status ON tb_plano_de_contas(status);

COMMENT ON TABLE tb_plano_de_contas IS 'Plano de Contas contábil por empresa e ano fiscal';

-- ============================================================================
-- 3. Table: tb_conta_parte_b (Contas da Parte B e-Lalur/e-Lacs)
-- ============================================================================
-- Contas fiscais específicas de IRPJ/CSLL por empresa.

CREATE TABLE IF NOT EXISTS tb_conta_parte_b (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    company_id BIGINT NOT NULL REFERENCES tb_empresa(id),

    -- Dados da conta
    codigo_conta VARCHAR(50) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    ano_base INTEGER NOT NULL,
    data_vigencia_inicio DATE NOT NULL,
    data_vigencia_fim DATE,
    tipo_tributo VARCHAR(20) NOT NULL,
    saldo_inicial NUMERIC(19, 2),
    tipo_saldo VARCHAR(20),

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT,

    -- Unique constraint: sem duplicação de código por empresa + ano
    CONSTRAINT uk_conta_parte_b_company_codigo_ano UNIQUE (company_id, codigo_conta, ano_base)
);

CREATE INDEX IF NOT EXISTS idx_conta_parte_b_company ON tb_conta_parte_b(company_id);
CREATE INDEX IF NOT EXISTS idx_conta_parte_b_ano_base ON tb_conta_parte_b(ano_base);
CREATE INDEX IF NOT EXISTS idx_conta_parte_b_status ON tb_conta_parte_b(status);

COMMENT ON TABLE tb_conta_parte_b IS 'Contas da Parte B do e-Lalur/e-Lacs (IRPJ/CSLL)';

-- ============================================================================
-- 4. Table: tb_lancamento_contabil (Lançamentos Contábeis)
-- ============================================================================
-- Lançamentos contábeis com débito e crédito.

CREATE TABLE IF NOT EXISTS tb_lancamento_contabil (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    company_id BIGINT NOT NULL REFERENCES tb_empresa(id),
    conta_debito_id BIGINT NOT NULL REFERENCES tb_plano_de_contas(id),
    conta_credito_id BIGINT NOT NULL REFERENCES tb_plano_de_contas(id),

    -- Dados do lançamento
    data DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    historico VARCHAR(2000) NOT NULL,
    numero_documento VARCHAR(100),
    fiscal_year INTEGER NOT NULL,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT
);

CREATE INDEX IF NOT EXISTS idx_lancamento_contabil_company ON tb_lancamento_contabil(company_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_contabil_data ON tb_lancamento_contabil(data);
CREATE INDEX IF NOT EXISTS idx_lancamento_contabil_fiscal_year ON tb_lancamento_contabil(fiscal_year);
CREATE INDEX IF NOT EXISTS idx_lancamento_contabil_status ON tb_lancamento_contabil(status);

COMMENT ON TABLE tb_lancamento_contabil IS 'Lançamentos contábeis (débito/crédito)';

-- ============================================================================
-- 5. Table: tb_lancamento_parte_b (Lançamentos da Parte B)
-- ============================================================================
-- Lançamentos fiscais da Parte B do e-Lalur/e-Lacs.

CREATE TABLE IF NOT EXISTS tb_lancamento_parte_b (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    company_id BIGINT NOT NULL REFERENCES tb_empresa(id),
    conta_contabil_id BIGINT REFERENCES tb_plano_de_contas(id),
    conta_parte_b_id BIGINT REFERENCES tb_conta_parte_b(id),
    parametro_tributario_id BIGINT NOT NULL REFERENCES tb_parametros_tributarios(id),

    -- Dados do lançamento
    mes_referencia INTEGER NOT NULL,
    ano_referencia INTEGER NOT NULL,
    tipo_apuracao VARCHAR(10) NOT NULL,
    tipo_relacionamento VARCHAR(20) NOT NULL,
    tipo_ajuste VARCHAR(10) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT
);

CREATE INDEX IF NOT EXISTS idx_lancamento_parte_b_company ON tb_lancamento_parte_b(company_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_parte_b_ano_mes ON tb_lancamento_parte_b(ano_referencia, mes_referencia);
CREATE INDEX IF NOT EXISTS idx_lancamento_parte_b_status ON tb_lancamento_parte_b(status);

COMMENT ON TABLE tb_lancamento_parte_b IS 'Lançamentos da Parte B do e-Lalur/e-Lacs';

-- ============================================================================
-- 6. ALTER existing tables (for databases where tables already exist via ddl-auto)
-- ============================================================================
-- These ALTERs run after CREATE TABLE IF NOT EXISTS above, so tables always exist.
-- DROP NOT NULL is idempotent: no error if the column is already nullable.
-- ADD COLUMN IF NOT EXISTS is a no-op if the column already exists.

-- Make conta_referencial_id nullable (was NOT NULL in ddl-auto schema)
ALTER TABLE tb_plano_de_contas ALTER COLUMN conta_referencial_id DROP NOT NULL;

-- Add 'modelo' column to tb_conta_referencial (missing from ddl-auto schema)
ALTER TABLE tb_conta_referencial ADD COLUMN IF NOT EXISTS modelo VARCHAR(50);
