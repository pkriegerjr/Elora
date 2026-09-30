-- =====================================================================
-- PROJETO ELORA - SCHEMA V2.3 (PostgreSQL/Neon) - incremental sobre v2+v2.2
-- Sem breaking change: só ADICIONA constraint. Rode uma vez no banco.
-- =====================================================================

-- 1. Par único da avaliação (módulo Avaliação, §4 do escopo):
--    um usuário avalia um cuidador UMA vez. App checa antes (422);
--    o banco garante em caso de corrida (vira 409 no GlobalExceptionHandler).
ALTER TABLE avaliacao
    ADD CONSTRAINT uq_avaliacao_avaliador_avaliado UNIQUE (avaliador_id, avaliado_id);

-- =====================================================================
-- FIM SCHEMA V2.3 PG
-- =====================================================================
