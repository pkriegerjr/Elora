-- =====================================================================
-- PROJETO ELORA - SCHEMA V2.3 (MySQL 8.0+ / InnoDB / utf8mb4)
-- Incremental sobre v2+v2.2. Sem breaking change: só ADICIONA constraint.
-- Uso manual: mysql -u root -p elora_db < elora_schema_v2_3.sql
-- =====================================================================

USE elora_db;

-- 1. Par único da avaliação (módulo Avaliação, §4 do escopo):
--    um usuário avalia um cuidador UMA vez. App checa antes (422);
--    o banco garante em caso de corrida (vira 409 no GlobalExceptionHandler).
ALTER TABLE avaliacao
    ADD CONSTRAINT uq_avaliacao_avaliador_avaliado UNIQUE (avaliador_id, avaliado_id);

-- =====================================================================
-- FIM SCHEMA V2.3
-- =====================================================================
