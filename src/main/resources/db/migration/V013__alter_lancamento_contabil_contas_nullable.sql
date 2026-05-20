-- V013: Torna conta_debito_id e conta_credito_id opcionais em tb_lancamento_contabil.
-- Regra de negócio: lançamento deve ter ao menos uma das duas contas (débito OU crédito),
-- podendo ter ambas desde que sejam contas distintas e da classe ANALITICO.

ALTER TABLE tb_lancamento_contabil
    ALTER COLUMN conta_debito_id DROP NOT NULL;

ALTER TABLE tb_lancamento_contabil
    ALTER COLUMN conta_credito_id DROP NOT NULL;
