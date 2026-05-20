-- Rename table tb_chart_of_account to tb_plano_de_contas (if exists)
ALTER TABLE IF EXISTS tb_chart_of_account RENAME TO tb_plano_de_contas;

-- Rename unique constraint (IF EXISTS applies to the table, not the constraint)
ALTER TABLE IF EXISTS tb_plano_de_contas RENAME CONSTRAINT uk_chart_of_account_company_code_year TO uk_plano_de_contas_company_code_year;
