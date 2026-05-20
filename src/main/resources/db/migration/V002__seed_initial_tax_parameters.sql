-- ============================================================================
-- Seed Initial Tax Parameters
-- Version: V002
-- Date: 2025-12-07
-- ============================================================================
--
-- This script populates the tb_parametros_tributarios table with commonly
-- used tax parameters for CNAE, Qualificação PJ, and Natureza Jurídica.
--
-- ============================================================================

-- CNAE (Classificação Nacional de Atividades Econômicas)
-- ============================================================================

INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status, criado_em, atualizado_em)
VALUES
    ('4618-4/01', 'CNAE', 'Representantes comerciais e agentes do comércio de medicamentos, cosméticos e produtos de perfumaria', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('4618-4/02', 'CNAE', 'Representantes comerciais e agentes do comércio de instrumentos e materiais odonto-médico-hospitalares', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6201-5/00', 'CNAE', 'Desenvolvimento de programas de computer sob encomenda', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6202-3/00', 'CNAE', 'Desenvolvimento e licenciamento de programas de computer customizáveis', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6203-1/00', 'CNAE', 'Desenvolvimento e licenciamento de programas de computer não-customizáveis', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6204-0/00', 'CNAE', 'Consultoria em tecnologia da informação', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6209-1/00', 'CNAE', 'Suporte técnico, manutenção e outros serviços em tecnologia da informação', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6311-9/00', 'CNAE', 'Tratamento de dados, provedores de serviços de aplicação e serviços de hospedagem na internet', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6920-6/01', 'CNAE', 'Atividades de contabilidade', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6920-6/02', 'CNAE', 'Atividades de consultoria e auditoria contábil e tributária', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('7020-4/00', 'CNAE', 'Atividades de consultoria em gestão empresarial', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('7490-1/04', 'CNAE', 'Atividades de intermediação e agenciamento de serviços e negócios em geral, exceto imobiliários', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- Qualificação da Pessoa Jurídica
-- ============================================================================

INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status, criado_em, atualizado_em)
VALUES
    ('DIRETOR', 'QUALIFICACAO_PJ', 'Diretor', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMINISTRADOR', 'QUALIFICACAO_PJ', 'Administrador', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOCIO', 'QUALIFICACAO_PJ', 'Sócio', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOCIO_ADMINISTRADOR', 'QUALIFICACAO_PJ', 'Sócio-Administrador', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOCIO_GERENTE', 'QUALIFICACAO_PJ', 'Sócio-Gerente', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PRESIDENTE', 'QUALIFICACAO_PJ', 'Presidente', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PROCURADOR', 'QUALIFICACAO_PJ', 'Procurador', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('EMPRESARIO', 'QUALIFICACAO_PJ', 'Empresário (Empresa Individual)', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- Natureza Jurídica
-- ============================================================================

INSERT INTO tb_parametros_tributarios (codigo, tipo, descricao, status, criado_em, atualizado_em)
VALUES
    ('206-2', 'NATUREZA_JURIDICA', 'Sociedade Empresária Limitada', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('213-5', 'NATUREZA_JURIDICA', 'Empresário (Individual)', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('214-3', 'NATUREZA_JURIDICA', 'Sociedade Empresária em Nome Coletivo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('215-1', 'NATUREZA_JURIDICA', 'Sociedade Empresária em Comandita Simples', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('216-0', 'NATUREZA_JURIDICA', 'Sociedade Empresária em Comandita por Ações', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('223-2', 'NATUREZA_JURIDICA', 'Sociedade Anônima Aberta', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('224-1', 'NATUREZA_JURIDICA', 'Sociedade Anônima Fechada', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('230-5', 'NATUREZA_JURIDICA', 'Sociedade Simples Limitada', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('231-3', 'NATUREZA_JURIDICA', 'Sociedade Simples em Nome Coletivo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('232-1', 'NATUREZA_JURIDICA', 'Sociedade Simples em Comandita Simples', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('234-8', 'NATUREZA_JURIDICA', 'Sociedade Limitada Unipessoal - SLU', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('399-9', 'NATUREZA_JURIDICA', 'Associação Privada', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('303-4', 'NATUREZA_JURIDICA', 'Serviço Social Autônomo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('306-9', 'NATUREZA_JURIDICA', 'Fundação Privada', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('322-0', 'NATUREZA_JURIDICA', 'Organização Religiosa', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('403-1', 'NATUREZA_JURIDICA', 'Empresa Individual de Responsabilidade Limitada (EIRELI)', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, tipo) DO NOTHING;

-- ============================================================================
-- Verification Query
-- ============================================================================
--
-- SELECT tipo, COUNT(*) as total
-- FROM tb_parametros_tributarios
-- GROUP BY tipo
-- ORDER BY tipo;
--
-- Expected results:
-- CNAE             : 12 records
-- QUALIFICACAO_PJ  :  8 records
-- NATUREZA_JURIDICA: 16 records
--
-- ============================================================================
