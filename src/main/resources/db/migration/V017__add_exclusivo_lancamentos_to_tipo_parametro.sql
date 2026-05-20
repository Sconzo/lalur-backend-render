-- Adiciona flag de uso exclusivo na tela de Lançamentos E-LALUR e E-LACS.
-- exclusivo_lancamentos = TRUE = tipo visível APENAS na tela de lançamentos fiscais.
-- exclusivo_lancamentos = FALSE = tipo visível na tela de parâmetros tributários da empresa.

ALTER TABLE tb_tipos_parametros_tributarios
    ADD COLUMN exclusivo_lancamentos BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tb_tipos_parametros_tributarios
SET exclusivo_lancamentos = TRUE
WHERE descricao = 'CÓDIGO LANÇAMENTOS E-LALUR E E-LACS';
