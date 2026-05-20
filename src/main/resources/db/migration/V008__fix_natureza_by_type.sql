-- Migration: Fix natureza based on parameter type
-- FORMA_TRIBUTACAO -> QUARTERLY
-- ESTIMATIVA_MENSAL -> MONTHLY

UPDATE tb_parametros_tributarios
SET natureza = 'QUARTERLY'
WHERE tipo = 'FORMA_TRIBUTACAO';

UPDATE tb_parametros_tributarios
SET natureza = 'MONTHLY'
WHERE tipo = 'ESTIMATIVA_MENSAL';
