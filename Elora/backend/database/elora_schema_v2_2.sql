-- =====================================================================
-- PROJETO ELORA - SCHEMA V2.2 (padrão: incremental sobre a versão anterior)
-- Aplica sobre um banco já criado com elora_schema_v2.sql. Sem breaking change.
-- Uso manual: mysql -u root -p elora_db < elora_schema_v2_2.sql
-- (ou rode bloco a bloco no seu client MySQL)
-- No Docker Compose este arquivo entra como 02-delta após a v2 (01-schema).
-- Engine: MySQL 8.0+ / InnoDB / utf8mb4
-- =====================================================================

USE elora_db;

-- 1. Atividade do usuário (relatórios/admin)
ALTER TABLE usuario
    ADD COLUMN ultimo_login_em DATETIME NULL
        COMMENT 'v2.2: atualizado a cada login (relatorios/admin)'
        AFTER consentimento_lgpd_at;

-- 2. Fluxo de validação do cuidador (PENDING->UNDER_REVIEW->APPROVED no front)
ALTER TABLE profissional_detalhes
    ADD COLUMN status_verificacao ENUM('pendente','em_analise','aprovado','rejeitado','correcao')
        NOT NULL DEFAULT 'pendente'
        AFTER documento_verificado;

-- 3. Remove SPATIAL INDEX sobre geo_ponto anulável (MySQL 8.0 rejeita
--    índice espacial em coluna que aceita NULL e quebrava o init fresco).
--    Se o seu banco foi criado com a v2 sem erro, o índice existe e o DROP
--    funciona. Se der erro 1091 (índice inexistente), apenas ignore esta
--    linha e siga — o estado final já é o desejado.
DROP INDEX spx_usuario_geo ON usuario;

-- 4. Lookup de refresh por hash em toda chamada de /auth/refresh
CREATE UNIQUE INDEX uq_sessao_refresh ON sessao (refresh_hash);

-- 5. Redefinição de senha (REQ-ELO-002; front POST /auth/password/reset)
CREATE TABLE IF NOT EXISTS redefinicao_senha (
    id_redefinicao INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     INT NOT NULL,
    token_hash     VARCHAR(64) NOT NULL UNIQUE COMMENT 'SHA-256 hex do token opaco',
    expira_em      DATETIME NOT NULL,
    usado_em       DATETIME NULL,
    criado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_redef_usuario ON redefinicao_senha (usuario_id, usado_em, expira_em);

-- 6. View de busca passa a expor status_verificacao
CREATE OR REPLACE VIEW vw_profissional_busca AS
SELECT
    u.id_usuario,
    u.nome,
    u.foto_url,
    u.genero,
    TIMESTAMPDIFF(YEAR, u.data_nascimento, CURDATE()) AS idade,
    u.latitude,
    u.longitude,
    u.status,
    pd.preco_hora,
    pd.nota_media,
    pd.status_verificacao,
    pd.descricao_perfil,
    GROUP_CONCAT(DISTINCT e.nome ORDER BY e.nome SEPARATOR ', ') AS especialidades
FROM usuario u
JOIN usuario_perfil up ON up.usuario_id = u.id_usuario
JOIN perfil pf ON pf.id_perfil = up.perfil_id AND pf.nome = 'profissional'
LEFT JOIN profissional_detalhes pd ON pd.usuario_id = u.id_usuario
LEFT JOIN usuario_especialidade ue ON ue.usuario_id = u.id_usuario
LEFT JOIN especialidade e ON e.id_especialidade = ue.especialidade_id
WHERE u.status = 'ativo' AND u.deleted_at IS NULL
GROUP BY u.id_usuario;

-- =====================================================================
-- FIM SCHEMA V2.2
-- =====================================================================
