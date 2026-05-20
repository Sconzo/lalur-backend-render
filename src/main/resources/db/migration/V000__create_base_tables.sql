-- ============================================================================
-- Create Base Tables
-- Version: V000
-- Date: 2026-02-05
-- ============================================================================
--
-- This migration creates the foundational tables required by all other
-- migrations: tb_usuario (users) and tb_empresa (companies).
--
-- These tables were previously created by Hibernate ddl-auto, but for
-- proper Flyway management they need to be created via migration.
--
-- ============================================================================

-- ============================================================================
-- Table: tb_usuario (Users)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_usuario (
    id BIGSERIAL PRIMARY KEY,

    -- User data
    primeiro_nome VARCHAR(255) NOT NULL,
    sobrenome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    funcao VARCHAR(20) NOT NULL CHECK (funcao IN ('ADMIN', 'CONTADOR')),
    deve_mudar_senha BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT
);

CREATE INDEX IF NOT EXISTS idx_usuario_email ON tb_usuario(email);
CREATE INDEX IF NOT EXISTS idx_usuario_status ON tb_usuario(status);
CREATE INDEX IF NOT EXISTS idx_usuario_funcao ON tb_usuario(funcao);

COMMENT ON TABLE tb_usuario IS 'Usuários do sistema (ADR-001)';
COMMENT ON COLUMN tb_usuario.funcao IS 'Papel do usuário: ADMIN ou CONTADOR';
COMMENT ON COLUMN tb_usuario.deve_mudar_senha IS 'Flag para forçar troca de senha no próximo login';

-- ============================================================================
-- Table: tb_empresa (Companies)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tb_empresa (
    id BIGSERIAL PRIMARY KEY,

    -- Company data
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    razao_social VARCHAR(255) NOT NULL,
    periodo_contabil DATE NOT NULL,

    -- Audit fields (BaseEntity)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    criado_por BIGINT NOT NULL DEFAULT 1,
    atualizado_por BIGINT
);

CREATE INDEX IF NOT EXISTS idx_empresa_cnpj ON tb_empresa(cnpj);
CREATE INDEX IF NOT EXISTS idx_empresa_status ON tb_empresa(status);
CREATE INDEX IF NOT EXISTS idx_empresa_razao_social ON tb_empresa(razao_social);

COMMENT ON TABLE tb_empresa IS 'Empresas gerenciadas no sistema (ADR-001)';
COMMENT ON COLUMN tb_empresa.cnpj IS 'CNPJ sem formatação (14 dígitos)';
COMMENT ON COLUMN tb_empresa.periodo_contabil IS 'Data base do período contábil atual';

-- ============================================================================
-- Create SYSTEM user (ID = 1)
-- ============================================================================
-- This user is required for audit fields (criado_por) before any real user exists.
-- Password is disabled (invalid hash) - this user cannot login.

INSERT INTO tb_usuario (
    id,
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
    1,
    'Sistema',
    'Automático',
    'system@lalurecf.com.br',
    '$2a$12$disabled.password.hash.not.usable',
    'ADMIN',
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP,
    1
)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to avoid conflicts with fixed ID
SELECT setval('tb_usuario_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_usuario), true);

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify tables created:
--    SELECT table_name FROM information_schema.tables
--    WHERE table_schema = 'public' AND table_name IN ('tb_usuario', 'tb_empresa');
--
-- 2. Verify SYSTEM user exists:
--    SELECT id, email, funcao FROM tb_usuario WHERE id = 1;
--
-- ============================================================================
