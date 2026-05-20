-- V015: Adiciona máscara de níveis em tb_empresa e limpa dados de plano de contas

-- 1. Limpar dados dependentes (em ordem de FK)
TRUNCATE TABLE tb_lancamento_parte_b CASCADE;
TRUNCATE TABLE tb_lancamento_contabil CASCADE;
TRUNCATE TABLE tb_plano_de_contas CASCADE;

-- 2. Adicionar coluna mascara_niveis em tb_empresa
--    Nullable no banco pois empresas existentes ainda não possuem máscara.
--    Obrigatoriedade é enforçada na camada de aplicação.
ALTER TABLE tb_empresa ADD COLUMN IF NOT EXISTS mascara_niveis VARCHAR(50);

COMMENT ON COLUMN tb_empresa.mascara_niveis IS
    'Máscara de formatação do código do plano de contas. '
    'Formato: segmentos de 9s separados por ponto. '
    'Exemplo: "99.999.99.999999" = N1=2dig, N2=3dig, N3=2dig, N4=6dig. '
    'Imutável quando existem contas ativas no plano de contas.';
