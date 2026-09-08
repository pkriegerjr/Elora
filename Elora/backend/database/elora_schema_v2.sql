-- =====================================================================
-- PROJETO ELORA - SCHEMA V2 (corrige v1)
-- Base: REQ-ELO-001..021 funcionais + RNF-ELO-001..016 + UC10 Busca
-- Mudancas em relacao a v1:
--  1. Usuario unico (heranca p.15) em vez de usuario_interno/externo duplicados
--  2. RBAC dinamico: perfil + permissao + usuario_perfil (p.7, p.15)
--  3. Subsistemas: contrato/proposta/assinatura, pagamento/repasse/taxa,
--     denuncia/evidencia/disputa, notificacao/mensagem/preferencia,
--     documento/especialidade N:N, disponibilidade, favorito, sessao/mfa,
--     auditoria/LGPD, FAQ
--  4. Fixes: cpf CHAR(11) so digitos, avaliacao 1/contrato + trigger de papel,
--     nota_media via trigger, indice geo, deleted_at, consentimento
-- Engine: MySQL 8.0+ / InnoDB / utf8mb4
-- =====================================================================

CREATE DATABASE IF NOT EXISTS elora_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE elora_db;

-- =====================================================================
-- 1. USUARIO UNICO (base para Cliente/Profissional/Operador/Juridico)
-- REQ-ELO-001, 002, 004, 010 | RNF-ELO-004, 005, 006
-- =====================================================================

CREATE TABLE usuario (
    id_usuario           INT AUTO_INCREMENT PRIMARY KEY,
    nome                 VARCHAR(150) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    telefone             VARCHAR(20) NULL,
    foto_url             VARCHAR(255) NULL,

    -- CPF: so digitos na app (VARCHAR(14) formatado quebra unicidade/busca).
    -- NULL permitido (ex.: operador sem CPF, cadastro via Google incompleto).
    cpf                  CHAR(11) NULL UNIQUE,

    genero               ENUM('M','F','outro','prefiro_nao_informar') NULL,
    data_nascimento      DATE NULL,

    senha_hash           VARCHAR(255) NULL COMMENT 'bcrypt/argon2 na aplicacao, nunca texto puro',
    origem_login         ENUM('senha','google') NOT NULL DEFAULT 'senha',
    google_id            VARCHAR(100) NULL UNIQUE,

    status               ENUM('ativo','inativo','suspenso') NOT NULL DEFAULT 'ativo',
    email_verificado     BOOLEAN NOT NULL DEFAULT FALSE,

    latitude             DECIMAL(10,8) NULL COMMENT 'REQ-010 + RNF-ELO-014 geolocalizacao',
    longitude            DECIMAL(11,8) NULL,
    -- Coluna geografica p/ busca por proximidade (UC10 RN2).
    -- Sem SRID-restriction p/ permitir NULL; BTREE abaixo cobre MVP.
    -- Quando lat/long virar NOT NULL, trocar por POINT SRID 4326 NOT NULL + SPATIAL INDEX.
    geo_ponto            POINT GENERATED ALWAYS AS (
        IF(latitude IS NULL OR longitude IS NULL, NULL,
           ST_PointFromText(CONCAT('POINT(', longitude, ' ', latitude, ')')))
    ) STORED NULL,

    consentimento_lgpd   BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'RNF-ELO-005',
    consentimento_lgpd_at DATETIME NULL,
    deleted_at           DATETIME NULL COMMENT 'soft-delete LGPD p.40, nunca DELETE fisico de titular',

    criado_em            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_usuario_login CHECK (
        (origem_login = 'senha' AND senha_hash IS NOT NULL)
        OR (origem_login = 'google')
    ),
    CONSTRAINT chk_usuario_cpf CHECK (
        cpf IS NULL OR cpf REGEXP '^[0-9]{11}$'
    ),
    CONSTRAINT chk_usuario_geo CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE INDEX idx_usuario_status ON usuario (status);
CREATE INDEX idx_usuario_geo ON usuario (latitude, longitude);
CREATE INDEX idx_usuario_nome ON usuario (nome);
CREATE SPATIAL INDEX spx_usuario_geo ON usuario (geo_ponto);

-- =====================================================================
-- 2. RBAC DINAMICO (substitui ENUM hardcodado da v1)
-- REQ-ELO-003: cliente, profissional, operador, juridico, financeiro
-- =====================================================================

CREATE TABLE perfil (
    id_perfil    INT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(50) NOT NULL UNIQUE COMMENT 'ex: admin, moderador, juridico, financeiro, cliente, profissional',
    descricao    VARCHAR(255) NULL,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE permissao (
    id_permissao INT AUTO_INCREMENT PRIMARY KEY,
    codigo       VARCHAR(100) NOT NULL UNIQUE COMMENT 'ex: contrato.aprovar, taxa.editar, denuncia.analisar',
    descricao    VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE perfil_permissao (
    perfil_id    INT NOT NULL,
    permissao_id INT NOT NULL,
    PRIMARY KEY (perfil_id, permissao_id),
    FOREIGN KEY (perfil_id) REFERENCES perfil (id_perfil) ON DELETE CASCADE,
    FOREIGN KEY (permissao_id) REFERENCES permissao (id_permissao) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE usuario_perfil (
    usuario_id   INT NOT NULL,
    perfil_id    INT NOT NULL,
    atribuido_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atribuido_por INT NULL COMMENT 'id_usuario do admin que atribuiu',
    PRIMARY KEY (usuario_id, perfil_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (perfil_id) REFERENCES perfil (id_perfil) ON DELETE CASCADE,
    FOREIGN KEY (atribuido_por) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- Extensoes por papel (1:1 com usuario, so quando o papel existir)
CREATE TABLE moderador_regiao (
    usuario_id   INT PRIMARY KEY,
    regiao       VARCHAR(100) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE juridico_detalhes (
    usuario_id               INT PRIMARY KEY,
    oab                      VARCHAR(20) NULL,
    permissao_editar_contrato BOOLEAN NOT NULL DEFAULT FALSE,
    permissao_aprovar_termo   BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE contratante_detalhes (
    usuario_id            INT PRIMARY KEY,
    observacoes_cuidado   TEXT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE profissional_detalhes (
    usuario_id            INT PRIMARY KEY,
    descricao_perfil      TEXT NULL,
    preco_hora            DECIMAL(10,2) NULL CHECK (preco_hora IS NULL OR preco_hora >= 0),
    documento_verificado  BOOLEAN NOT NULL DEFAULT FALSE,
    nota_media            DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT 'mantido por trigger trg_avaliacao_*',
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 3. ESPECIALIDADE / DOCUMENTO PROFISSIONAL (N:N, REQ-ELO-004/006)
-- v1 tinha especialidade VARCHAR(150) — quebra filtro UC10 RN4
-- =====================================================================

CREATE TABLE especialidade (
    id_especialidade INT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL UNIQUE COMMENT 'ex: Alzheimer, Parkinson, mobilidade reduzida, higiene, alimentacao'
) ENGINE=InnoDB;

CREATE TABLE usuario_especialidade (
    usuario_id       INT NOT NULL,
    especialidade_id INT NOT NULL,
    PRIMARY KEY (usuario_id, especialidade_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (especialidade_id) REFERENCES especialidade (id_especialidade) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE documento_profissional (
    id_documento  INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id    INT NOT NULL COMMENT 'profissional dono do documento',
    tipo          VARCHAR(80) NOT NULL COMMENT 'ex: RG, CPF, certificado_curso, referencia',
    arquivo_url   VARCHAR(500) NOT NULL,
    status        ENUM('pendente','aprovado','rejeitado') NOT NULL DEFAULT 'pendente',
    verificado_por INT NULL,
    verificado_em DATETIME NULL,
    criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (verificado_por) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;
CREATE INDEX idx_docprof_usuario_status ON documento_profissional (usuario_id, status);

-- Disponibilidade do cuidador (UC10 8.2: DISPONIBILIDADES x CUIDADORES)
CREATE TABLE disponibilidade (
    id_disponibilidade INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id         INT NOT NULL,
    data               DATE NOT NULL,
    periodo            ENUM('matutino','vespertino','noturno') NOT NULL,
    status             ENUM('disponivel','ocupado','indisponivel') NOT NULL DEFAULT 'disponivel',
    criado_em          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_disp (usuario_id, data, periodo),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Favoritos (REQ-ELO-018)
CREATE TABLE favorito (
    cliente_id      INT NOT NULL,
    profissional_id INT NOT NULL,
    criado_em       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cliente_id, profissional_id),
    FOREIGN KEY (cliente_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT chk_fav_diferente CHECK (cliente_id <> profissional_id)
) ENGINE=InnoDB;

-- =====================================================================
-- 4. CONTRATO / PROPOSTA / NEGOCIACAO / ASSINATURA (REQ-008, 009, 013, 015)
-- =====================================================================

CREATE TABLE contrato (
    id_contrato        INT AUTO_INCREMENT PRIMARY KEY,
    codigo             VARCHAR(40) NOT NULL UNIQUE COMMENT 'ex: ELO-2026-000123, gerado na app',
    cliente_id         INT NOT NULL,
    profissional_id    INT NOT NULL,
    titulo             VARCHAR(150) NOT NULL,
    descricao_necessidade TEXT NULL,
    valor_hora         DECIMAL(10,2) NOT NULL CHECK (valor_hora >= 0),
    valor_total        DECIMAL(12,2) NULL CHECK (valor_total IS NULL OR valor_total >= 0),
    endereco_atendimento VARCHAR(255) NULL,
    data_inicio        DATE NULL,
    data_fim           DATE NULL,
    status             ENUM('rascunho','proposta','negociacao','aguard_assinatura','ativo','concluido','rescindido','cancelado','em_disputa')
                       NOT NULL DEFAULT 'rascunho',
    criado_por         INT NULL,
    criado_em          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (criado_por) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_contrato_partes CHECK (cliente_id <> profissional_id),
    CONSTRAINT chk_contrato_datas CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
) ENGINE=InnoDB;
CREATE INDEX idx_contrato_cliente ON contrato (cliente_id, status);
CREATE INDEX idx_contrato_prof ON contrato (profissional_id, status);

-- Historico de negociacao (cada contraproposta preservada)
CREATE TABLE proposta (
    id_proposta          INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id          INT NOT NULL,
    autor_id             INT NOT NULL,
    mensagem             TEXT NULL,
    valor_hora_proposto  DECIMAL(10,2) NULL CHECK (valor_hora_proposto IS NULL OR valor_hora_proposto >= 0),
    data_inicio_proposta DATE NULL,
    data_fim_proposta    DATE NULL,
    status               ENUM('aberta','aceita','recusada','expirada') NOT NULL DEFAULT 'aberta',
    criado_em            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (autor_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;
CREATE INDEX idx_proposta_contrato ON proposta (contrato_id, criado_em);

-- Mensagens do contrato (REQ p.23 Mensagem)
CREATE TABLE mensagem_contrato (
    id_mensagem    INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id    INT NOT NULL,
    remetente_id   INT NOT NULL,
    destinatario_id INT NOT NULL,
    corpo          TEXT NOT NULL,
    lida           BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em        DATETIME NULL,
    criado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (remetente_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (destinatario_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;
CREATE INDEX idx_msg_contrato ON mensagem_contrato (contrato_id, criado_em);

-- Assinatura digital (REQ-008: registro eletronico da contratacao)
CREATE TABLE assinatura_contrato (
    id_assinatura  INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id    INT NOT NULL,
    usuario_id     INT NOT NULL,
    papel          ENUM('cliente','profissional','juridico','testemunha') NOT NULL,
    hash_documento VARCHAR(128) NOT NULL COMMENT 'SHA-256 do PDF/termo assinado',
    provedor       VARCHAR(50) NOT NULL DEFAULT 'interno' COMMENT 'interno, govbr, clicksign, docusign',
    ip_assinatura  VARCHAR(45) NULL,
    assinado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_assinatura (contrato_id, usuario_id, papel),
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- Escala de trabalho do contrato (REQ-ELO-013 turnos)
CREATE TABLE escala_trabalho (
    id_escala   INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id INT NOT NULL,
    data        DATE NOT NULL,
    periodo     ENUM('matutino','vespertino','noturno') NOT NULL,
    status      ENUM('prevista','executada','faltou','cancelada') NOT NULL DEFAULT 'prevista',
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    UNIQUE KEY uq_escala (contrato_id, data, periodo)
) ENGINE=InnoDB;

-- =====================================================================
-- 5. FINANCEIRO: TAXA / PAGAMENTO / REPASSE (REQ-005, 007, 011, 012, 014)
-- RNF-ELO-011: sem duplicidade/perda -> idempotency_key UNIQUE
-- =====================================================================

CREATE TABLE taxa_servico (
    id_taxa      INT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL,
    percentual   DECIMAL(5,2) NOT NULL DEFAULT 0.00 CHECK (percentual >= 0 AND percentual <= 100),
    valor_fixo   DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (valor_fixo >= 0),
    vigente_de   DATE NOT NULL,
    vigente_ate  DATE NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por   INT NULL,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (criado_por) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

CREATE TABLE pagamento (
    id_pagamento    INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id     INT NOT NULL,
    pagador_id      INT NOT NULL COMMENT 'cliente que paga a Elora',
    taxa_id         INT NULL,
    valor_bruto     DECIMAL(12,2) NOT NULL CHECK (valor_bruto >= 0),
    valor_taxa      DECIMAL(12,2) NOT NULL DEFAULT 0.00 CHECK (valor_taxa >= 0),
    valor_liquido   DECIMAL(12,2) NOT NULL CHECK (valor_liquido >= 0),
    metodo          ENUM('pix','cartao','boleto') NOT NULL,
    status          ENUM('pendente','aprovado','recusado','estornado') NOT NULL DEFAULT 'pendente',
    gateway_id      VARCHAR(120) NULL COMMENT 'id no gateway externo',
    idempotency_key VARCHAR(80) NOT NULL UNIQUE COMMENT 'RNF-ELO-011: retry seguro',
    criado_em       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato),
    FOREIGN KEY (pagador_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (taxa_id) REFERENCES taxa_servico (id_taxa)
) ENGINE=InnoDB;
CREATE INDEX idx_pag_contrato ON pagamento (contrato_id, status);

CREATE TABLE repasse (
    id_repasse      INT AUTO_INCREMENT PRIMARY KEY,
    pagamento_id    INT NOT NULL,
    profissional_id INT NOT NULL,
    valor           DECIMAL(12,2) NOT NULL CHECK (valor >= 0),
    status          ENUM('pendente','processado','falha') NOT NULL DEFAULT 'pendente',
    processado_em   DATETIME NULL,
    FOREIGN KEY (pagamento_id) REFERENCES pagamento (id_pagamento) ON DELETE CASCADE,
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- =====================================================================
-- 6. DENUNCIA / EVIDENCIA / DISPUTA (REQ-016, REQ-015 painel juridico)
-- =====================================================================

CREATE TABLE denuncia (
    id_denuncia    INT AUTO_INCREMENT PRIMARY KEY,
    denunciante_id INT NOT NULL,
    denunciado_id  INT NOT NULL,
    contrato_id    INT NULL,
    motivo         VARCHAR(150) NOT NULL,
    descricao      TEXT NOT NULL,
    status         ENUM('aberta','em_analise','resolvida','arquivada') NOT NULL DEFAULT 'aberta',
    analisado_por  INT NULL,
    criado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (denunciante_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (denunciado_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato),
    FOREIGN KEY (analisado_por) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_denuncia_partes CHECK (denunciante_id <> denunciado_id)
) ENGINE=InnoDB;
CREATE INDEX idx_denuncia_status ON denuncia (status, criado_em);
CREATE INDEX idx_denuncia_denunciado ON denuncia (denunciado_id, status);

CREATE TABLE evidencia_denuncia (
    id_evidencia INT AUTO_INCREMENT PRIMARY KEY,
    denuncia_id  INT NOT NULL,
    tipo         ENUM('texto','imagem','audio','video','documento') NOT NULL,
    arquivo_url  VARCHAR(500) NULL COMMENT 'NULL quando tipo=texto',
    descricao    TEXT NULL,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (denuncia_id) REFERENCES denuncia (id_denuncia) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE disputa (
    id_disputa   INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id  INT NOT NULL,
    aberta_por    INT NOT NULL,
    motivo       TEXT NOT NULL,
    status       ENUM('aberta','mediacao','resolvida','encerrada') NOT NULL DEFAULT 'aberta',
    resolvida_em DATETIME NULL,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (aberta_por) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- =====================================================================
-- 7. AVALIACAO (REQ-019, RNF-ELO-011) — 1 por contrato, so cliente->profissional
-- =====================================================================

CREATE TABLE avaliacao (
    id_avaliacao INT AUTO_INCREMENT PRIMARY KEY,
    contrato_id  INT NOT NULL UNIQUE COMMENT 'uma avaliacao por contrato evita spam/media manipulada',
    avaliador_id INT NOT NULL COMMENT 'deve ser o cliente do contrato (trigger)',
    avaliado_id  INT NOT NULL COMMENT 'deve ser o profissional do contrato (trigger)',
    nota         TINYINT NOT NULL,
    comentario   TEXT NULL,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (avaliador_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (avaliado_id) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_avaliacao_nota CHECK (nota BETWEEN 1 AND 5),
    CONSTRAINT chk_avaliacao_partes CHECK (avaliador_id <> avaliado_id)
) ENGINE=InnoDB;
CREATE INDEX idx_avaliacao_avaliado ON avaliacao (avaliado_id, nota);

-- =====================================================================
-- 8. NOTIFICACAO / PREFERENCIA (REQ-020, RNF-ELO-001/016 + LGPD)
-- =====================================================================

CREATE TABLE preferencia_notificacao (
    usuario_id   INT PRIMARY KEY,
    push         BOOLEAN NOT NULL DEFAULT TRUE,
    email        BOOLEAN NOT NULL DEFAULT TRUE,
    sms          BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notificacao (
    id_notificacao  INT AUTO_INCREMENT PRIMARY KEY,
    destinatario_id INT NOT NULL,
    canal           ENUM('push','email','sms','sistema') NOT NULL,
    titulo          VARCHAR(150) NOT NULL,
    corpo           TEXT NOT NULL,
    referencia_tipo VARCHAR(50) NULL COMMENT 'ex: contrato, pagamento, denuncia',
    referencia_id   INT NULL,
    lida            BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em         DATETIME NULL,
    enviado_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (destinatario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_notif_dest ON notificacao (destinatario_id, lida, enviado_em);

-- =====================================================================
-- 9. SESSAO / MFA (REQ-017, RNF-ELO-006)
-- =====================================================================

CREATE TABLE sessao (
    id_sessao    INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   INT NOT NULL,
    refresh_hash VARCHAR(255) NOT NULL COMMENT 'hash do refresh token, nunca o token puro',
    ip           VARCHAR(45) NULL,
    user_agent   VARCHAR(255) NULL,
    expira_em    DATETIME NOT NULL,
    revogada     BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_sessao_usuario ON sessao (usuario_id, revogada, expira_em);

CREATE TABLE mfa_segredo (
    usuario_id  INT PRIMARY KEY COMMENT 'obrigatorio p/ juridico/financeiro (RNF-ELO-006)',
    segredo     VARCHAR(255) NOT NULL COMMENT 'seed TOTP criptografado (AES-256) na app',
    ativo       BOOLEAN NOT NULL DEFAULT FALSE,
    ativado_em  DATETIME NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 10. AUDITORIA / LGPD / CONHECIMENTO (RNF-012, RNF-005, REQ-021)
-- =====================================================================

CREATE TABLE trilha_auditoria (
    id_auditoria  BIGINT AUTO_INCREMENT PRIMARY KEY,
    ator_id       INT NULL COMMENT 'NULL = acao do sistema',
    acao          VARCHAR(80) NOT NULL COMMENT 'ex: usuario.criar, contrato.assinar, pagamento.aprovar',
    entidade      VARCHAR(80) NOT NULL,
    entidade_id   VARCHAR(60) NULL,
    dados_antes   JSON NULL,
    dados_depois  JSON NULL,
    ip            VARCHAR(45) NULL,
    criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ator_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;
CREATE INDEX idx_auditoria_entidade ON trilha_auditoria (entidade, entidade_id, criado_em);
CREATE INDEX idx_auditoria_ator ON trilha_auditoria (ator_id, criado_em);

CREATE TABLE consentimento_lgpd (
    id_consentimento INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       INT NOT NULL,
    tipo             ENUM('termos','privacidade','marketing','notificacao','geolocalizacao') NOT NULL,
    versao           VARCHAR(20) NOT NULL COMMENT 'versao do texto aceito',
    aceito           BOOLEAN NOT NULL,
    aceito_em        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip               VARCHAR(45) NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE artigo_conhecimento (
    id_artigo   INT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    corpo       TEXT NOT NULL,
    categoria   VARCHAR(80) NULL,
    autor_id    INT NULL,
    publicado   BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (autor_id) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- =====================================================================
-- 11. TRIGGERS: valida papel da avaliacao + mantem nota_media
-- =====================================================================

DELIMITER $$

CREATE TRIGGER trg_avaliacao_valida_papel
BEFORE INSERT ON avaliacao
FOR EACH ROW
BEGIN
    DECLARE v_cliente INT;
    DECLARE v_prof INT;
    SELECT cliente_id, profissional_id INTO v_cliente, v_prof
      FROM contrato WHERE id_contrato = NEW.contrato_id;
    IF v_cliente IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contrato da avaliacao nao existe';
    END IF;
    IF NEW.avaliador_id <> v_cliente OR NEW.avaliado_id <> v_prof THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'avaliacao deve ser cliente->profissional do contrato';
    END IF;
END$$

CREATE TRIGGER trg_avaliacao_valida_papel_upd
BEFORE UPDATE ON avaliacao
FOR EACH ROW
BEGIN
    DECLARE v_cliente INT;
    DECLARE v_prof INT;
    SELECT cliente_id, profissional_id INTO v_cliente, v_prof
      FROM contrato WHERE id_contrato = NEW.contrato_id;
    IF NEW.avaliador_id <> v_cliente OR NEW.avaliado_id <> v_prof THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'avaliacao deve ser cliente->profissional do contrato';
    END IF;
END$$

CREATE TRIGGER trg_avaliacao_media_ins
AFTER INSERT ON avaliacao
FOR EACH ROW
BEGIN
    UPDATE profissional_detalhes pd
       SET nota_media = (SELECT COALESCE(ROUND(AVG(a.nota), 2), 0.00)
                           FROM avaliacao a WHERE a.avaliado_id = NEW.avaliado_id)
     WHERE pd.usuario_id = NEW.avaliado_id;
END$$

CREATE TRIGGER trg_avaliacao_media_upd
AFTER UPDATE ON avaliacao
FOR EACH ROW
BEGIN
    UPDATE profissional_detalhes pd
       SET nota_media = (SELECT COALESCE(ROUND(AVG(a.nota), 2), 0.00)
                           FROM avaliacao a WHERE a.avaliado_id = NEW.avaliado_id)
     WHERE pd.usuario_id = NEW.avaliado_id;
END$$

CREATE TRIGGER trg_avaliacao_media_del
AFTER DELETE ON avaliacao
FOR EACH ROW
BEGIN
    UPDATE profissional_detalhes pd
       SET nota_media = (SELECT COALESCE(ROUND(AVG(a.nota), 2), 0.00)
                           FROM avaliacao a WHERE a.avaliado_id = OLD.avaliado_id)
     WHERE pd.usuario_id = OLD.avaliado_id;
END$$

DELIMITER ;

-- =====================================================================
-- 12. SEEDS RBAC + TAXA PADRAO
-- =====================================================================

INSERT INTO perfil (nome, descricao) VALUES
 ('admin','Acesso total'),
 ('moderador','Modera usuarios/denuncias por regiao'),
 ('juridico','Contratos, disputas, termos'),
 ('financeiro','Taxas, pagamentos, repasses, relatorios'),
 ('cliente','Contratante de cuidadores'),
 ('profissional','Cuidador contratado')
ON DUPLICATE KEY UPDATE descricao = VALUES(descricao);

INSERT INTO permissao (codigo, descricao) VALUES
 ('usuario.criar','Cadastrar usuarios'),
 ('usuario.suspender','Suspender/reativar usuarios'),
 ('perfil.atribuir','Atribuir perfis'),
 ('contrato.criar','Criar proposta/contrato'),
 ('contrato.assinar','Assinar contrato'),
 ('contrato.rescindir','Rescindir contrato'),
 ('taxa.editar','Criar/alterar taxas (financeiro)'),
 ('pagamento.processar','Processar pagamento/repasse'),
 ('denuncia.analisar','Analisar denuncias/disputas'),
 ('documento.validar','Validar documentos do profissional'),
 ('relatorio.financeiro','Ver relatorios financeiros')
ON DUPLICATE KEY UPDATE descricao = VALUES(descricao);

-- admin recebe tudo; demais, escopo minimo (ajuste via UPDATE, sem ALTER TABLE)
INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p CROSS JOIN permissao pe WHERE p.nome = 'admin';

INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('denuncia.analisar','usuario.suspender') WHERE p.nome = 'moderador';

INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('contrato.criar','contrato.assinar','contrato.rescindir','denuncia.analisar','documento.validar') WHERE p.nome = 'juridico';

INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('taxa.editar','pagamento.processar','relatorio.financeiro') WHERE p.nome = 'financeiro';

INSERT IGNORE INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('contrato.criar','contrato.assinar') WHERE p.nome IN ('cliente','profissional');

INSERT INTO taxa_servico (nome, percentual, valor_fixo, vigente_de, ativo) VALUES
 ('Taxa padrao Elora', 10.00, 0.00, CURDATE(), TRUE)
ON DUPLICATE KEY UPDATE id_taxa = id_taxa;

-- =====================================================================
-- 13. VIEW DE BUSCA (UC10: nome/foto/distancia/avaliacao + filtros)
-- RN7: so profissional ativo e nao deletado; ordenacao por proximidade
-- na app com ST_Distance_Sphere(geo_ponto, ST_PointFromText(...))
-- =====================================================================

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
-- FIM V2. Migracao v1->v2: unificar usuario_interno+usuario_externo em
-- usuario (deduplicar por email), mapear tipo_perfil p/ perfil via
-- usuario_perfil, converter especialidade VARCHAR p/ N:N, recalcular
-- nota_media (trigger assume a partir daqui).
-- =====================================================================
