-- Adiciona campos de obrigatoriedade e ordem de exibição aos tipos de parâmetros tributários.
-- ordem_exibicao NULL = tipo não aparece como coluna na listagem de empresas.
-- obrigatorio = TRUE = validação dinâmica no create/update de empresa.

ALTER TABLE tb_tipos_parametros_tributarios
    ADD COLUMN obrigatorio BOOLEAN DEFAULT FALSE,
    ADD COLUMN ordem_exibicao INT DEFAULT NULL;

-- Atualizar tipos existentes com obrigatoriedade e ordem de exibição
UPDATE tb_tipos_parametros_tributarios SET obrigatorio = TRUE, ordem_exibicao = 1 WHERE descricao = 'CNAE';
UPDATE tb_tipos_parametros_tributarios SET obrigatorio = TRUE, ordem_exibicao = 2 WHERE descricao = 'NATUREZA_JURIDICA';
UPDATE tb_tipos_parametros_tributarios SET obrigatorio = TRUE, ordem_exibicao = 3 WHERE descricao = 'QUALIFICACAO_PJ';
