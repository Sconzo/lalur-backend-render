-- ============================================================================
-- Change Auditor Columns to BIGINT
-- Version: V004
-- Date: 2025-12-24
-- ============================================================================
--
-- This migration changes criado_por and atualizado_por columns from VARCHAR
-- to BIGINT to store user IDs instead of email strings.
--
-- CRITICAL: This migration will clear existing audit data (emails) since
-- they cannot be converted to IDs. All records will be assigned to SYSTEM user.
--
-- ============================================================================

-- Step 1: Create SYSTEM user with fixed ID = 1 if not exists
-- ============================================================================

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

-- Reset sequence to avoid conflicts
SELECT setval('tb_usuario_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tb_usuario), true);

-- Step 2: Convert audit columns to BIGINT
-- ============================================================================

DO $$
DECLARE
    table_rec RECORD;
BEGIN
    -- Loop through all tables with criado_por or atualizado_por columns
    FOR table_rec IN
        SELECT DISTINCT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name LIKE 'tb_%'
          AND column_name IN ('criado_por', 'atualizado_por')
    LOOP
        RAISE NOTICE 'Processing table: %', table_rec.table_name;

        -- Change criado_por from VARCHAR to BIGINT
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = table_rec.table_name
              AND column_name = 'criado_por'
              AND data_type = 'character varying'
        ) THEN
            -- Convert to BIGINT and assign SYSTEM user
            EXECUTE format('ALTER TABLE %I ALTER COLUMN criado_por TYPE BIGINT USING 1', table_rec.table_name);
            EXECUTE format('UPDATE %I SET criado_por = 1 WHERE criado_por IS NULL', table_rec.table_name);
            EXECUTE format('ALTER TABLE %I ALTER COLUMN criado_por SET NOT NULL', table_rec.table_name);
            RAISE NOTICE '  - Changed criado_por to BIGINT NOT NULL';
        END IF;

        -- Change atualizado_por from VARCHAR to BIGINT
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = table_rec.table_name
              AND column_name = 'atualizado_por'
              AND data_type = 'character varying'
        ) THEN
            -- Clear existing data (emails) and change type
            EXECUTE format('UPDATE %I SET atualizado_por = NULL WHERE atualizado_por IS NOT NULL', table_rec.table_name);
            EXECUTE format('ALTER TABLE %I ALTER COLUMN atualizado_por TYPE BIGINT USING NULL', table_rec.table_name);
            RAISE NOTICE '  - Changed atualizado_por to BIGINT';
        END IF;

        -- Add foreign key constraints to tb_usuario if not already present
        IF table_rec.table_name != 'tb_usuario' THEN
            -- FK for criado_por (only if column exists)
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = table_rec.table_name
                  AND column_name = 'criado_por'
            ) AND NOT EXISTS (
                SELECT 1
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = table_rec.table_name
                  AND constraint_name = format('fk_%s_criado_por', table_rec.table_name)
            ) THEN
                EXECUTE format('ALTER TABLE %I ADD CONSTRAINT fk_%s_criado_por FOREIGN KEY (criado_por) REFERENCES tb_usuario(id) ON DELETE SET NULL',
                    table_rec.table_name, table_rec.table_name);
                RAISE NOTICE '  - Added FK constraint for criado_por';
            END IF;

            -- FK for atualizado_por (only if column exists)
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = table_rec.table_name
                  AND column_name = 'atualizado_por'
            ) AND NOT EXISTS (
                SELECT 1
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = table_rec.table_name
                  AND constraint_name = format('fk_%s_atualizado_por', table_rec.table_name)
            ) THEN
                EXECUTE format('ALTER TABLE %I ADD CONSTRAINT fk_%s_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES tb_usuario(id) ON DELETE SET NULL',
                    table_rec.table_name, table_rec.table_name);
                RAISE NOTICE '  - Added FK constraint for atualizado_por';
            END IF;
        END IF;
    END LOOP;

    RAISE NOTICE 'Migration completed successfully';
END $$;

-- ============================================================================
-- Update V001 migration for new environments
-- ============================================================================
-- Note: V001__migrate_company_tax_parameters.sql should be updated to use
-- BIGINT for criado_por and atualizado_por columns instead of VARCHAR(255)

-- ============================================================================
-- Verification Queries
-- ============================================================================
--
-- 1. Verify all criado_por and atualizado_por are BIGINT:
--    SELECT table_name, column_name, data_type
--    FROM information_schema.columns
--    WHERE table_schema = 'public'
--      AND column_name IN ('criado_por', 'atualizado_por')
--    ORDER BY table_name, column_name;
--
-- 2. Verify foreign key constraints exist:
--    SELECT tc.table_name, tc.constraint_name, kcu.column_name
--    FROM information_schema.table_constraints tc
--    JOIN information_schema.key_column_usage kcu
--        ON tc.constraint_name = kcu.constraint_name
--    WHERE tc.constraint_type = 'FOREIGN KEY'
--      AND kcu.column_name IN ('criado_por', 'atualizado_por')
--    ORDER BY tc.table_name, kcu.column_name;
--
-- ============================================================================
