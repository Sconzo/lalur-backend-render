-- Migration: Add natureza column to tb_parametros_tributarios
-- Enum de Natureza do Parâmetro Tributário

-- Step 1: Add column as nullable first
ALTER TABLE tb_parametros_tributarios
ADD COLUMN IF NOT EXISTS natureza VARCHAR(20);

-- Step 2: Set default value for all existing rows
UPDATE tb_parametros_tributarios SET natureza = 'GLOBAL' WHERE natureza IS NULL;

-- Step 3: Add NOT NULL constraint
ALTER TABLE tb_parametros_tributarios
ALTER COLUMN natureza SET NOT NULL;

-- Step 4: Set default for future inserts
ALTER TABLE tb_parametros_tributarios
ALTER COLUMN natureza SET DEFAULT 'GLOBAL';

-- Step 5: Add CHECK constraint for valid values (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_natureza') THEN
        ALTER TABLE tb_parametros_tributarios
        ADD CONSTRAINT chk_natureza CHECK (natureza IN ('GLOBAL', 'MONTHLY', 'QUARTERLY'));
    END IF;
END $$;

-- Step 6: Create index for filtering by nature (if not exists)
CREATE INDEX IF NOT EXISTS idx_parametros_tributarios_natureza ON tb_parametros_tributarios(natureza);
