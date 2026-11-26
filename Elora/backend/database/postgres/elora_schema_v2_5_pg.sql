-- =====================================================================
-- PROJETO ELORA - SCHEMA v2.5 (PostgreSQL/Neon)
-- Adiciona o parecer da análise à tabela denuncia, usada pelo módulo
-- jurídico persistente (entity Denuncia + DTO DenunciaResponse).
-- Sem breaking change: apenas ADICIONA uma coluna nullable. Rode uma vez.
-- =====================================================================

ALTER TABLE denuncia ADD COLUMN IF NOT EXISTS parecer TEXT NULL;
