-- =====================================================================
-- PROJETO ELORA - SCHEMA v2.5 (MySQL) — equivalente ao v2_5_pg.sql
-- Adiciona o parecer da análise à tabela denuncia. Rode uma vez.
-- =====================================================================

ALTER TABLE denuncia ADD COLUMN parecer TEXT NULL;
