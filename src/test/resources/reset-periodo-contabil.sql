-- Reset período contábil for company ID 1 to allow test to run multiple times
UPDATE tb_empresa SET periodo_contabil = '2025-12-01' WHERE id = 1;
