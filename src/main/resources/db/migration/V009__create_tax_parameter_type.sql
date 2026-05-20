-- ============================================================================
-- V009: Criar tabela TaxParameterType e refatorar TaxParameter
-- Entidade TaxParameterType e Refatoração de TaxParameter
-- ============================================================================

-- 1. Criar tabela tb_tipos_parametros_tributarios
CREATE TABLE tb_tipos_parametros_tributarios (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL UNIQUE,
    natureza VARCHAR(20) NOT NULL CHECK (natureza IN ('GLOBAL', 'MONTHLY', 'QUARTERLY')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por BIGINT REFERENCES tb_usuario(id),
    atualizado_por BIGINT REFERENCES tb_usuario(id)
);

CREATE INDEX idx_tipo_param_natureza ON tb_tipos_parametros_tributarios(natureza);
CREATE INDEX idx_tipo_param_status ON tb_tipos_parametros_tributarios(status);

-- 2. Migrar dados: criar tipos únicos a partir de valores existentes
-- Agrupa por tipo e pega a natureza correspondente (todos parâmetros do mesmo tipo têm mesma natureza)
INSERT INTO tb_tipos_parametros_tributarios (descricao, natureza, status)
SELECT DISTINCT
    tipo as descricao,
    COALESCE(natureza, 'GLOBAL') as natureza,
    'ACTIVE' as status
FROM tb_parametros_tributarios
WHERE tipo IS NOT NULL;

-- 3. Adicionar coluna FK em tb_parametros_tributarios
ALTER TABLE tb_parametros_tributarios
ADD COLUMN tipo_parametro_id BIGINT REFERENCES tb_tipos_parametros_tributarios(id);

-- 4. Popular FK com base no mapeamento tipo -> tipo_parametro_id
UPDATE tb_parametros_tributarios p
SET tipo_parametro_id = t.id
FROM tb_tipos_parametros_tributarios t
WHERE p.tipo = t.descricao;

-- 5. Tornar FK obrigatória (após migração de dados)
ALTER TABLE tb_parametros_tributarios
ALTER COLUMN tipo_parametro_id SET NOT NULL;

-- 6. Remover coluna tipo
ALTER TABLE tb_parametros_tributarios DROP COLUMN IF EXISTS tipo;

-- 7. Remover constraint e índice de natureza (criados em V007)
DROP INDEX IF EXISTS idx_parametros_tributarios_natureza;
ALTER TABLE tb_parametros_tributarios DROP CONSTRAINT IF EXISTS chk_natureza;
ALTER TABLE tb_parametros_tributarios DROP COLUMN IF EXISTS natureza;

-- 8. Criar índice na FK
CREATE INDEX idx_param_tipo_id ON tb_parametros_tributarios(tipo_parametro_id);

-- Nota: A atualização de timestamps é gerenciada pelo JPA Auditing (Spring Data)
