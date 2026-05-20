-- ============================================================================
-- Fix Database Constraints
-- Version: V003
-- Date: 2025-12-24
-- ============================================================================
--
-- This migration fixes two critical issues:
-- 1. Remove incorrect UNIQUE constraint on 'codigo' column (should be composite)
-- 2. Fix 'atualizado_em' columns to accept NULL (required by JPA Auditing)
--
-- These issues occurred because Hibernate ddl-auto:update created incorrect
-- constraints that were never cleaned up.
--
-- ============================================================================

-- ============================================================================
-- Part 1: Fix tb_parametros_tributarios - Remove single-column UNIQUE on codigo
-- ============================================================================

DO $$
DECLARE
    constraint_name_var VARCHAR;
BEGIN
    -- Find and drop any UNIQUE constraint on just 'codigo' column
    -- (excluding the correct composite constraint on codigo+tipo)
    FOR constraint_name_var IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'tb_parametros_tributarios'
          AND tc.constraint_type = 'UNIQUE'
          -- Only constraints that have exactly 1 column
          AND tc.constraint_name IN (
              SELECT constraint_name
              FROM information_schema.key_column_usage
              WHERE table_name = 'tb_parametros_tributarios'
              GROUP BY constraint_name
              HAVING COUNT(*) = 1
          )
          -- And that single column is 'codigo'
          AND kcu.column_name = 'codigo'
    LOOP
        EXECUTE format('ALTER TABLE tb_parametros_tributarios DROP CONSTRAINT %I', constraint_name_var);
        RAISE NOTICE 'Dropped incorrect UNIQUE constraint: %', constraint_name_var;
    END LOOP;

    -- Ensure composite UNIQUE constraint exists
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'tb_parametros_tributarios'
          AND tc.constraint_type = 'UNIQUE'
          AND tc.constraint_name IN (
              SELECT constraint_name
              FROM information_schema.key_column_usage
              WHERE table_name = 'tb_parametros_tributarios'
                AND column_name IN ('codigo', 'tipo')
              GROUP BY constraint_name
              HAVING COUNT(*) = 2
          )
    ) THEN
        ALTER TABLE tb_parametros_tributarios
            ADD CONSTRAINT uk_parametros_codigo_tipo UNIQUE (codigo, tipo);
        RAISE NOTICE 'Created composite UNIQUE constraint on (codigo, tipo)';
    ELSE
        RAISE NOTICE 'Composite UNIQUE constraint already exists';
    END IF;
END $$;

-- ============================================================================
-- Part 2: Fix atualizado_em columns to accept NULL
-- ============================================================================
-- JPA @LastModifiedDate should be NULL on entity creation (only set on update)

DO $$
DECLARE
    rec RECORD;
BEGIN
    -- Fix tb_usuario
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_usuario'
          AND column_name = 'atualizado_em'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE tb_usuario ALTER COLUMN atualizado_em DROP NOT NULL;
        RAISE NOTICE 'Fixed tb_usuario.atualizado_em to accept NULL';
    ELSE
        RAISE NOTICE 'tb_usuario.atualizado_em already accepts NULL';
    END IF;

    -- Fix tb_empresa
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_empresa'
          AND column_name = 'atualizado_em'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE tb_empresa ALTER COLUMN atualizado_em DROP NOT NULL;
        RAISE NOTICE 'Fixed tb_empresa.atualizado_em to accept NULL';
    ELSE
        RAISE NOTICE 'tb_empresa.atualizado_em already accepts NULL';
    END IF;

    -- Fix tb_parametros_tributarios
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tb_parametros_tributarios'
          AND column_name = 'atualizado_em'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE tb_parametros_tributarios ALTER COLUMN atualizado_em DROP NOT NULL;
        RAISE NOTICE 'Fixed tb_parametros_tributarios.atualizado_em to accept NULL';
    ELSE
        RAISE NOTICE 'tb_parametros_tributarios.atualizado_em already accepts NULL';
    END IF;

    -- Fix any other tables with atualizado_em (future-proof)
    -- This will handle any future entities that extend BaseEntity
    FOR rec IN
        SELECT table_name
        FROM information_schema.columns
        WHERE column_name = 'atualizado_em'
          AND is_nullable = 'NO'
          AND table_name LIKE 'tb_%'
          AND table_name NOT IN ('tb_usuario', 'tb_empresa', 'tb_parametros_tributarios')
    LOOP
        EXECUTE format('ALTER TABLE %I ALTER COLUMN atualizado_em DROP NOT NULL', rec.table_name);
        RAISE NOTICE 'Fixed %.atualizado_em to accept NULL', rec.table_name;
    END LOOP;
END $$;

-- ============================================================================
-- Part 3: Add helpful indexes if they don't exist
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_parametros_tributarios_codigo_tipo
    ON tb_parametros_tributarios(codigo, tipo);

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify tb_parametros_tributarios has only composite UNIQUE:
--    SELECT tc.constraint_name, string_agg(kcu.column_name, ', ') as columns
--    FROM information_schema.table_constraints tc
--    JOIN information_schema.key_column_usage kcu
--        ON tc.constraint_name = kcu.constraint_name
--    WHERE tc.table_name = 'tb_parametros_tributarios'
--      AND tc.constraint_type = 'UNIQUE'
--    GROUP BY tc.constraint_name;
--
-- 2. Verify all atualizado_em columns accept NULL:
--    SELECT table_name, column_name, is_nullable
--    FROM information_schema.columns
--    WHERE column_name = 'atualizado_em'
--      AND table_name LIKE 'tb_%'
--    ORDER BY table_name;
--
-- ============================================================================
