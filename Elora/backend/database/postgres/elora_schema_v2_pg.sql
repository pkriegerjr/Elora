-- =====================================================================
-- PROJETO ELORA - SCHEMA V2 + V2.2 CONVERTIDO PARA POSTGRESQL + POSTGIS
-- Origem: backend/database/elora_schema_v2.sql + elora_schema_v2_2.sql (MySQL 8)
-- Alvo:  Neon (Postgres 14-18) — https://console.neon.tech
--
-- COMO APLICAR NO NEON (banco novo, vazio):
--   1. No dashboard do Neon, abra o SQL Editor do seu projeto/branch.
--   2. Cole este arquivo inteiro e rode (Run). Ele cria extensao, tabelas,
--      indices, triggers, view e seeds RBAC + taxa padrao.
--   3. Alternativa via psql:
--      psql "<connection-string do Neon>" -f elora_schema_v2_pg.sql
--
-- DIFERENCAS EM RELACAO AO MYSQL (equivalencia preservada):
--   a) Sem CREATE DATABASE/USE: no Neon voce roda dentro do database do
--      projeto. Sem ENGINE/CHARSET (Postgres e UTF-8/transacional por padrao).
--   b) INT AUTO_INCREMENT -> INT GENERATED ALWAYS AS IDENTITY (idem BIGINT).
--   c) ENUM(...) -> VARCHAR + CHECK (... IN (...)) — mesmos valores.
--   d) geo_ponto POINT gerado -> GEOGRAPHY(Point,4326) preenchido por trigger
--      (fn_usuario_geo) a partir de latitude/longitude. Indice GIST (aceita
--      NULL — no MySQL o SPATIAL sobre coluna anulavel quebrava e exigiu o
--      workaround da v2.2, aqui desnecessario).
--      Busca por proximidade na app: em vez de
--        ST_Distance_Sphere(geo_ponto, ST_PointFromText(...))   -- MySQL
--      use
--        ST_Distance(geo_ponto, ST_GeogFromText('SRID=4326;POINT(lng lat)'))  -- PG, retorna metros
--   e) DATETIME -> TIMESTAMP; DEFAULT CURRENT_TIMESTAMP mantido.
--      ON UPDATE CURRENT_TIMESTAMP nao existe no PG -> trigger generico
--      fn_touch_updated() nas tabelas com atualizado_em.
--   f) cpf REGEXP ... -> CHECK (cpf ~ '^[0-9]{11}$').
--   g) TINYINT nota -> SMALLINT (CHECK 1..5 mantido); JSON -> JSONB.
--   h) Triggers de avaliacao reescritos em plpgsql (mesmas regras: so
--      cliente->profissional do contrato; nota_media recalculada em
--      INSERT/UPDATE/DELETE por um unico trigger AFTER).
--   i) Seeds: ON DUPLICATE KEY UPDATE / INSERT IGNORE -> ON CONFLICT;
--      CURDATE() -> CURRENT_DATE.
--   j) View: TIMESTAMPDIFF -> DATE_PART('year', AGE(...)); GROUP_CONCAT ->
--      STRING_AGG(DISTINCT ... ORDER BY ...). View ja inclui status_verificacao
--      (v2.2) e a tabela redefinicao_senha + ultimo_login_em (v2.2) inclusas.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- =====================================================================
-- 1. USUARIO UNICO (base para Cliente/Profissional/Operador/Juridico)
-- REQ-ELO-001, 002, 004, 010 | RNF-ELO-004, 005, 006 (+ v2.2: ultimo_login_em)
-- =====================================================================

CREATE TABLE usuario (
    id_usuario           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                 VARCHAR(150) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    telefone             VARCHAR(20) NULL,
    foto_url             VARCHAR(255) NULL,

    -- CPF: so digitos na app. NULL permitido (ex.: operador sem CPF,
    -- cadastro via Google incompleto).
    cpf                  CHAR(11) NULL UNIQUE,

    genero               VARCHAR(24) NULL CHECK (genero IN ('M','F','outro','prefiro_nao_informar')),
    data_nascimento      DATE NULL,

    senha_hash           VARCHAR(255) NULL, -- bcrypt/argon2 na aplicacao, nunca texto puro
    origem_login         VARCHAR(10) NOT NULL DEFAULT 'senha' CHECK (origem_login IN ('senha','google')),
    google_id            VARCHAR(100) NULL UNIQUE,

    status               VARCHAR(10) NOT NULL DEFAULT 'ativo' CHECK (status IN ('ativo','inativo','suspenso')),
    email_verificado     BOOLEAN NOT NULL DEFAULT FALSE,

    latitude             DECIMAL(10,8) NULL, -- REQ-010 + RNF-ELO-014 geolocalizacao
    longitude            DECIMAL(11,8) NULL,
    -- Coluna geografica p/ busca por proximidade (UC10 RN2), mantida pelo
    -- trigger trg_usuario_geo. GIST aceita NULL (fim do workaround v2.2).
    geo_ponto            GEOGRAPHY(Point, 4326) NULL,

    consentimento_lgpd   BOOLEAN NOT NULL DEFAULT FALSE, -- RNF-ELO-005
    consentimento_lgpd_at TIMESTAMP NULL,
    ultimo_login_em      TIMESTAMP NULL, -- v2.2: atualizado a cada login (relatorios/admin)
    deleted_at           TIMESTAMP NULL, -- soft-delete LGPD p.40, nunca DELETE fisico de titular

    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_usuario_login CHECK (
        (origem_login = 'senha' AND senha_hash IS NOT NULL)
        OR (origem_login = 'google')
    ),
    CONSTRAINT chk_usuario_cpf CHECK (
        cpf IS NULL OR cpf ~ '^[0-9]{11}$'
    ),
    CONSTRAINT chk_usuario_geo CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    )
);

CREATE INDEX idx_usuario_status ON usuario (status);
CREATE INDEX idx_usuario_geo ON usuario (latitude, longitude);
CREATE INDEX idx_usuario_nome ON usuario (nome);
CREATE INDEX spx_usuario_geo ON usuario USING GIST (geo_ponto);

-- Mantem geo_ponto sincronizado com latitude/longitude (INSERT e UPDATE).
CREATE OR REPLACE FUNCTION fn_usuario_geo() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NULL OR NEW.longitude IS NULL THEN
        NEW.geo_ponto := NULL;
    ELSE
        NEW.geo_ponto := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::GEOGRAPHY;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_geo
BEFORE INSERT OR UPDATE OF latitude, longitude ON usuario
FOR EACH ROW EXECUTE FUNCTION fn_usuario_geo();

-- Substituto do ON UPDATE CURRENT_TIMESTAMP (generico; reusado abaixo).
CREATE OR REPLACE FUNCTION fn_touch_updated() RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_touch
BEFORE UPDATE ON usuario
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

-- =====================================================================
-- 2. RBAC DINAMICO
-- REQ-ELO-003: cliente, profissional, operador, juridico, financeiro
-- =====================================================================

CREATE TABLE perfil (
    id_perfil    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome         VARCHAR(50) NOT NULL UNIQUE, -- ex: admin, moderador, juridico, financeiro, cliente, profissional
    descricao    VARCHAR(255) NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissao (
    id_permissao INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo       VARCHAR(100) NOT NULL UNIQUE, -- ex: contrato.aprovar, taxa.editar, denuncia.analisar
    descricao    VARCHAR(255) NULL
);

CREATE TABLE perfil_permissao (
    perfil_id    INT NOT NULL,
    permissao_id INT NOT NULL,
    PRIMARY KEY (perfil_id, permissao_id),
    FOREIGN KEY (perfil_id) REFERENCES perfil (id_perfil) ON DELETE CASCADE,
    FOREIGN KEY (permissao_id) REFERENCES permissao (id_permissao) ON DELETE CASCADE
);

CREATE TABLE usuario_perfil (
    usuario_id   INT NOT NULL,
    perfil_id    INT NOT NULL,
    atribuido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atribuido_por INT NULL, -- id_usuario do admin que atribuiu
    PRIMARY KEY (usuario_id, perfil_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (perfil_id) REFERENCES perfil (id_perfil) ON DELETE CASCADE,
    FOREIGN KEY (atribuido_por) REFERENCES usuario (id_usuario)
);

-- Extensoes por papel (1:1 com usuario, so quando o papel existir)
CREATE TABLE moderador_regiao (
    usuario_id   INT PRIMARY KEY,
    regiao       VARCHAR(100) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TABLE juridico_detalhes (
    usuario_id               INT PRIMARY KEY,
    oab                      VARCHAR(20) NULL,
    permissao_editar_contrato BOOLEAN NOT NULL DEFAULT FALSE,
    permissao_aprovar_termo   BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TABLE contratante_detalhes (
    usuario_id            INT PRIMARY KEY,
    observacoes_cuidado   TEXT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TABLE profissional_detalhes (
    usuario_id            INT PRIMARY KEY,
    descricao_perfil      TEXT NULL,
    preco_hora            DECIMAL(10,2) NULL CHECK (preco_hora IS NULL OR preco_hora >= 0),
    documento_verificado  BOOLEAN NOT NULL DEFAULT FALSE,
    -- v2.2: fluxo PENDING->UNDER_REVIEW->APPROVED exibido no front
    status_verificacao    VARCHAR(12) NOT NULL DEFAULT 'pendente'
                          CHECK (status_verificacao IN ('pendente','em_analise','aprovado','rejeitado','correcao')),
    nota_media            DECIMAL(3,2) NOT NULL DEFAULT 0.00, -- mantido pelos triggers trg_avaliacao_*
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- =====================================================================
-- 3. ESPECIALIDADE / DOCUMENTO PROFISSIONAL (N:N, REQ-ELO-004/006)
-- =====================================================================

CREATE TABLE especialidade (
    id_especialidade INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL UNIQUE -- ex: Alzheimer, Parkinson, mobilidade reduzida, higiene, alimentacao
);

CREATE TABLE usuario_especialidade (
    usuario_id       INT NOT NULL,
    especialidade_id INT NOT NULL,
    PRIMARY KEY (usuario_id, especialidade_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (especialidade_id) REFERENCES especialidade (id_especialidade) ON DELETE CASCADE
);

CREATE TABLE documento_profissional (
    id_documento  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    INT NOT NULL, -- profissional dono do documento
    tipo          VARCHAR(80) NOT NULL, -- ex: RG, CPF, certificado_curso, referencia
    arquivo_url   VARCHAR(500) NOT NULL,
    status        VARCHAR(10) NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente','aprovado','rejeitado')),
    verificado_por INT NULL,
    verificado_em TIMESTAMP NULL,
    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (verificado_por) REFERENCES usuario (id_usuario)
);
CREATE INDEX idx_docprof_usuario_status ON documento_profissional (usuario_id, status);

-- Disponibilidade do cuidador (UC10 8.2: DISPONIBILIDADES x CUIDADORES)
CREATE TABLE disponibilidade (
    id_disponibilidade INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id         INT NOT NULL,
    data               DATE NOT NULL,
    periodo            VARCHAR(12) NOT NULL CHECK (periodo IN ('matutino','vespertino','noturno')),
    status             VARCHAR(15) NOT NULL DEFAULT 'disponivel' CHECK (status IN ('disponivel','ocupado','indisponivel')),
    criado_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_disp UNIQUE (usuario_id, data, periodo),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- Favoritos (REQ-ELO-018)
CREATE TABLE favorito (
    cliente_id      INT NOT NULL,
    profissional_id INT NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cliente_id, profissional_id),
    FOREIGN KEY (cliente_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT chk_fav_diferente CHECK (cliente_id <> profissional_id)
);

-- =====================================================================
-- 4. CONTRATO / PROPOSTA / NEGOCIACAO / ASSINATURA (REQ-008, 009, 013, 015)
-- =====================================================================

CREATE TABLE contrato (
    id_contrato        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo             VARCHAR(40) NOT NULL UNIQUE, -- ex: ELO-2026-000123, gerado na app
    cliente_id         INT NOT NULL,
    profissional_id    INT NOT NULL,
    titulo             VARCHAR(150) NOT NULL,
    descricao_necessidade TEXT NULL,
    valor_hora         DECIMAL(10,2) NOT NULL CHECK (valor_hora >= 0),
    valor_total        DECIMAL(12,2) NULL CHECK (valor_total IS NULL OR valor_total >= 0),
    endereco_atendimento VARCHAR(255) NULL,
    data_inicio        DATE NULL,
    data_fim           DATE NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'rascunho'
                       CHECK (status IN ('rascunho','proposta','negociacao','aguard_assinatura','ativo','concluido','rescindido','cancelado','em_disputa')),
    criado_por         INT NULL,
    criado_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (criado_por) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_contrato_partes CHECK (cliente_id <> profissional_id),
    CONSTRAINT chk_contrato_datas CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
);
CREATE INDEX idx_contrato_cliente ON contrato (cliente_id, status);
CREATE INDEX idx_contrato_prof ON contrato (profissional_id, status);

CREATE TRIGGER trg_contrato_touch
BEFORE UPDATE ON contrato
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

-- Historico de negociacao (cada contraproposta preservada)
CREATE TABLE proposta (
    id_proposta          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id          INT NOT NULL,
    autor_id             INT NOT NULL,
    mensagem             TEXT NULL,
    valor_hora_proposto  DECIMAL(10,2) NULL CHECK (valor_hora_proposto IS NULL OR valor_hora_proposto >= 0),
    data_inicio_proposta DATE NULL,
    data_fim_proposta    DATE NULL,
    status               VARCHAR(10) NOT NULL DEFAULT 'aberta' CHECK (status IN ('aberta','aceita','recusada','expirada')),
    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (autor_id) REFERENCES usuario (id_usuario)
);
CREATE INDEX idx_proposta_contrato ON proposta (contrato_id, criado_em);

-- Mensagens do contrato (REQ p.23 Mensagem)
CREATE TABLE mensagem_contrato (
    id_mensagem    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id    INT NOT NULL,
    remetente_id   INT NOT NULL,
    destinatario_id INT NOT NULL,
    corpo          TEXT NOT NULL,
    lida           BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em        TIMESTAMP NULL,
    criado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (remetente_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (destinatario_id) REFERENCES usuario (id_usuario)
);
CREATE INDEX idx_msg_contrato ON mensagem_contrato (contrato_id, criado_em);

-- Assinatura digital (REQ-008: registro eletronico da contratacao)
CREATE TABLE assinatura_contrato (
    id_assinatura  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id    INT NOT NULL,
    usuario_id     INT NOT NULL,
    papel          VARCHAR(15) NOT NULL CHECK (papel IN ('cliente','profissional','juridico','testemunha')),
    hash_documento VARCHAR(128) NOT NULL, -- SHA-256 do PDF/termo assinado
    provedor       VARCHAR(50) NOT NULL DEFAULT 'interno', -- interno, govbr, clicksign, docusign
    ip_assinatura  VARCHAR(45) NULL,
    assinado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assinatura UNIQUE (contrato_id, usuario_id, papel),
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
);

-- Escala de trabalho do contrato (REQ-ELO-013 turnos)
CREATE TABLE escala_trabalho (
    id_escala   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id INT NOT NULL,
    data        DATE NOT NULL,
    periodo     VARCHAR(12) NOT NULL CHECK (periodo IN ('matutino','vespertino','noturno')),
    status      VARCHAR(12) NOT NULL DEFAULT 'prevista' CHECK (status IN ('prevista','executada','faltou','cancelada')),
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    CONSTRAINT uq_escala UNIQUE (contrato_id, data, periodo)
);

-- =====================================================================
-- 5. FINANCEIRO: TAXA / PAGAMENTO / REPASSE (REQ-005, 007, 011, 012, 014)
-- RNF-ELO-011: sem duplicidade/perda -> idempotency_key UNIQUE
-- =====================================================================

CREATE TABLE taxa_servico (
    id_taxa      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL,
    percentual   DECIMAL(5,2) NOT NULL DEFAULT 0.00 CHECK (percentual >= 0 AND percentual <= 100),
    valor_fixo   DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (valor_fixo >= 0),
    vigente_de   DATE NOT NULL,
    vigente_ate  DATE NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por   INT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (criado_por) REFERENCES usuario (id_usuario)
);

CREATE TABLE pagamento (
    id_pagamento    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id     INT NOT NULL,
    pagador_id      INT NOT NULL, -- cliente que paga a Elora
    taxa_id         INT NULL,
    valor_bruto     DECIMAL(12,2) NOT NULL CHECK (valor_bruto >= 0),
    valor_taxa      DECIMAL(12,2) NOT NULL DEFAULT 0.00 CHECK (valor_taxa >= 0),
    valor_liquido   DECIMAL(12,2) NOT NULL CHECK (valor_liquido >= 0),
    metodo          VARCHAR(10) NOT NULL CHECK (metodo IN ('pix','cartao','boleto')),
    status          VARCHAR(10) NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente','aprovado','recusado','estornado')),
    gateway_id      VARCHAR(120) NULL, -- id no gateway externo
    idempotency_key VARCHAR(80) NOT NULL UNIQUE, -- RNF-ELO-011: retry seguro
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato),
    FOREIGN KEY (pagador_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (taxa_id) REFERENCES taxa_servico (id_taxa)
);
CREATE INDEX idx_pag_contrato ON pagamento (contrato_id, status);

CREATE TRIGGER trg_pagamento_touch
BEFORE UPDATE ON pagamento
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

CREATE TABLE repasse (
    id_repasse      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pagamento_id    INT NOT NULL,
    profissional_id INT NOT NULL,
    valor           DECIMAL(12,2) NOT NULL CHECK (valor >= 0),
    status          VARCHAR(12) NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente','processado','falha')),
    processado_em   TIMESTAMP NULL,
    FOREIGN KEY (pagamento_id) REFERENCES pagamento (id_pagamento) ON DELETE CASCADE,
    FOREIGN KEY (profissional_id) REFERENCES usuario (id_usuario)
);

-- =====================================================================
-- 6. DENUNCIA / EVIDENCIA / DISPUTA (REQ-016, REQ-015 painel juridico)
-- =====================================================================

CREATE TABLE denuncia (
    id_denuncia    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    denunciante_id INT NOT NULL,
    denunciado_id  INT NOT NULL,
    contrato_id    INT NULL,
    motivo         VARCHAR(150) NOT NULL,
    descricao      TEXT NOT NULL,
    status         VARCHAR(12) NOT NULL DEFAULT 'aberta' CHECK (status IN ('aberta','em_analise','resolvida','arquivada')),
    analisado_por  INT NULL,
    criado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (denunciante_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (denunciado_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato),
    FOREIGN KEY (analisado_por) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_denuncia_partes CHECK (denunciante_id <> denunciado_id)
);
CREATE INDEX idx_denuncia_status ON denuncia (status, criado_em);
CREATE INDEX idx_denuncia_denunciado ON denuncia (denunciado_id, status);

CREATE TRIGGER trg_denuncia_touch
BEFORE UPDATE ON denuncia
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

CREATE TABLE evidencia_denuncia (
    id_evidencia INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    denuncia_id  INT NOT NULL,
    tipo         VARCHAR(12) NOT NULL CHECK (tipo IN ('texto','imagem','audio','video','documento')),
    arquivo_url  VARCHAR(500) NULL, -- NULL quando tipo=texto
    descricao    TEXT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (denuncia_id) REFERENCES denuncia (id_denuncia) ON DELETE CASCADE
);

CREATE TABLE disputa (
    id_disputa   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id  INT NOT NULL,
    aberta_por    INT NOT NULL,
    motivo       TEXT NOT NULL,
    status       VARCHAR(12) NOT NULL DEFAULT 'aberta' CHECK (status IN ('aberta','mediacao','resolvida','encerrada')),
    resolvida_em TIMESTAMP NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (aberta_por) REFERENCES usuario (id_usuario)
);

-- =====================================================================
-- 7. AVALIACAO (REQ-019, RNF-ELO-011) — 1 por contrato, so cliente->profissional
-- =====================================================================

CREATE TABLE avaliacao (
    id_avaliacao INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id  INT NOT NULL UNIQUE, -- uma avaliacao por contrato evita spam/media manipulada
    avaliador_id INT NOT NULL, -- deve ser o cliente do contrato (trigger)
    avaliado_id  INT NOT NULL, -- deve ser o profissional do contrato (trigger)
    nota         SMALLINT NOT NULL,
    comentario   TEXT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contrato_id) REFERENCES contrato (id_contrato) ON DELETE CASCADE,
    FOREIGN KEY (avaliador_id) REFERENCES usuario (id_usuario),
    FOREIGN KEY (avaliado_id) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_avaliacao_nota CHECK (nota BETWEEN 1 AND 5),
    CONSTRAINT chk_avaliacao_partes CHECK (avaliador_id <> avaliado_id)
);
CREATE INDEX idx_avaliacao_avaliado ON avaliacao (avaliado_id, nota);

-- =====================================================================
-- 8. NOTIFICACAO / PREFERENCIA (REQ-020, RNF-ELO-001/016 + LGPD)
-- =====================================================================

CREATE TABLE preferencia_notificacao (
    usuario_id   INT PRIMARY KEY,
    push         BOOLEAN NOT NULL DEFAULT TRUE,
    email        BOOLEAN NOT NULL DEFAULT TRUE,
    sms          BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TRIGGER trg_prefnotif_touch
BEFORE UPDATE ON preferencia_notificacao
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

CREATE TABLE notificacao (
    id_notificacao  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    destinatario_id INT NOT NULL,
    canal           VARCHAR(10) NOT NULL CHECK (canal IN ('push','email','sms','sistema')),
    titulo          VARCHAR(150) NOT NULL,
    corpo           TEXT NOT NULL,
    referencia_tipo VARCHAR(50) NULL, -- ex: contrato, pagamento, denuncia
    referencia_id   INT NULL,
    lida            BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em         TIMESTAMP NULL,
    enviado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (destinatario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);
CREATE INDEX idx_notif_dest ON notificacao (destinatario_id, lida, enviado_em);

-- =====================================================================
-- 9. SESSAO / MFA (REQ-017, RNF-ELO-006) + v2.2: redefinicao de senha
-- =====================================================================

CREATE TABLE sessao (
    id_sessao    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   INT NOT NULL,
    refresh_hash VARCHAR(255) NOT NULL, -- hash do refresh token, nunca o token puro
    ip           VARCHAR(45) NULL,
    user_agent   VARCHAR(255) NULL,
    expira_em    TIMESTAMP NOT NULL,
    revogada     BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);
CREATE INDEX idx_sessao_usuario ON sessao (usuario_id, revogada, expira_em);
CREATE UNIQUE INDEX uq_sessao_refresh ON sessao (refresh_hash); -- v2.2: lookup em todo /auth/refresh

CREATE TABLE mfa_segredo (
    usuario_id  INT PRIMARY KEY, -- obrigatorio p/ juridico/financeiro (RNF-ELO-006)
    segredo     VARCHAR(255) NOT NULL, -- seed TOTP criptografado (AES-256) na app
    ativo       BOOLEAN NOT NULL DEFAULT FALSE,
    ativado_em  TIMESTAMP NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

-- v2.2 item 5: redefinicao de senha (REQ-ELO-002; front POST /auth/password/reset)
CREATE TABLE redefinicao_senha (
    id_redefinicao INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     INT NOT NULL,
    token_hash     VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hex do token opaco
    expira_em      TIMESTAMP NOT NULL,
    usado_em       TIMESTAMP NULL,
    criado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);
CREATE INDEX idx_redef_usuario ON redefinicao_senha (usuario_id, usado_em, expira_em);

-- =====================================================================
-- 10. AUDITORIA / LGPD / CONHECIMENTO (RNF-012, RNF-005, REQ-021)
-- =====================================================================

CREATE TABLE trilha_auditoria (
    id_auditoria  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ator_id       INT NULL, -- NULL = acao do sistema
    acao          VARCHAR(80) NOT NULL, -- ex: usuario.criar, contrato.assinar, pagamento.aprovar
    entidade      VARCHAR(80) NOT NULL,
    entidade_id   VARCHAR(60) NULL,
    dados_antes   JSONB NULL,
    dados_depois  JSONB NULL,
    ip            VARCHAR(45) NULL,
    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ator_id) REFERENCES usuario (id_usuario)
);
CREATE INDEX idx_auditoria_entidade ON trilha_auditoria (entidade, entidade_id, criado_em);
CREATE INDEX idx_auditoria_ator ON trilha_auditoria (ator_id, criado_em);

CREATE TABLE consentimento_lgpd (
    id_consentimento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id       INT NOT NULL,
    tipo             VARCHAR(20) NOT NULL CHECK (tipo IN ('termos','privacidade','marketing','notificacao','geolocalizacao')),
    versao           VARCHAR(20) NOT NULL, -- versao do texto aceito
    aceito           BOOLEAN NOT NULL,
    aceito_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip               VARCHAR(45) NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

CREATE TABLE artigo_conhecimento (
    id_artigo   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    corpo       TEXT NOT NULL,
    categoria   VARCHAR(80) NULL,
    autor_id    INT NULL,
    publicado   BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (autor_id) REFERENCES usuario (id_usuario)
);

CREATE TRIGGER trg_artigo_touch
BEFORE UPDATE ON artigo_conhecimento
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated();

-- =====================================================================
-- 11. TRIGGERS: valida papel da avaliacao + mantem nota_media (plpgsql)
-- =====================================================================

-- Valida cliente->profissional do contrato (INSERT e UPDATE).
CREATE OR REPLACE FUNCTION fn_avaliacao_valida_papel() RETURNS TRIGGER AS $$
DECLARE
    v_cliente INT;
    v_prof    INT;
BEGIN
    SELECT cliente_id, profissional_id INTO v_cliente, v_prof
      FROM contrato WHERE id_contrato = NEW.contrato_id;
    IF NOT FOUND OR v_cliente IS NULL THEN
        RAISE EXCEPTION 'contrato da avaliacao nao existe';
    END IF;
    IF NEW.avaliador_id <> v_cliente OR NEW.avaliado_id <> v_prof THEN
        RAISE EXCEPTION 'avaliacao deve ser cliente->profissional do contrato';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_avaliacao_valida_papel
BEFORE INSERT OR UPDATE ON avaliacao
FOR EACH ROW EXECUTE FUNCTION fn_avaliacao_valida_papel();

-- Recalcula nota_media do profissional (INSERT, UPDATE e DELETE em 1 trigger).
CREATE OR REPLACE FUNCTION fn_avaliacao_media() RETURNS TRIGGER AS $$
DECLARE
    v_avaliado INT;
BEGIN
    v_avaliado := COALESCE(NEW.avaliado_id, OLD.avaliado_id);
    UPDATE profissional_detalhes pd
       SET nota_media = (SELECT COALESCE(ROUND(AVG(a.nota), 2), 0.00)
                           FROM avaliacao a WHERE a.avaliado_id = v_avaliado)
     WHERE pd.usuario_id = v_avaliado;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_avaliacao_media
AFTER INSERT OR UPDATE OR DELETE ON avaliacao
FOR EACH ROW EXECUTE FUNCTION fn_avaliacao_media();

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
ON CONFLICT (nome) DO UPDATE SET descricao = EXCLUDED.descricao;

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
ON CONFLICT (codigo) DO UPDATE SET descricao = EXCLUDED.descricao;

-- admin recebe tudo; demais, escopo minimo
INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p CROSS JOIN permissao pe WHERE p.nome = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('denuncia.analisar','usuario.suspender') WHERE p.nome = 'moderador'
ON CONFLICT DO NOTHING;

INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('contrato.criar','contrato.assinar','contrato.rescindir','denuncia.analisar','documento.validar') WHERE p.nome = 'juridico'
ON CONFLICT DO NOTHING;

INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('taxa.editar','pagamento.processar','relatorio.financeiro') WHERE p.nome = 'financeiro'
ON CONFLICT DO NOTHING;

INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id_perfil, pe.id_permissao FROM perfil p JOIN permissao pe ON pe.codigo IN
 ('contrato.criar','contrato.assinar') WHERE p.nome IN ('cliente','profissional')
ON CONFLICT DO NOTHING;

INSERT INTO taxa_servico (nome, percentual, valor_fixo, vigente_de, ativo) VALUES
 ('Taxa padrao Elora', 10.00, 0.00, CURRENT_DATE, TRUE)
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 13. VIEW DE BUSCA (UC10: nome/foto/distancia/avaliacao + filtros)
-- RN7: so profissional ativo e nao deletado
-- =====================================================================

CREATE OR REPLACE VIEW vw_profissional_busca AS
SELECT
    u.id_usuario,
    u.nome,
    u.foto_url,
    u.genero,
    DATE_PART('year', AGE(CURRENT_DATE, u.data_nascimento))::INT AS idade,
    u.latitude,
    u.longitude,
    u.status,
    pd.preco_hora,
    pd.nota_media,
    pd.status_verificacao, -- v2.2
    pd.descricao_perfil,
    STRING_AGG(DISTINCT e.nome, ', ' ORDER BY e.nome) AS especialidades
FROM usuario u
JOIN usuario_perfil up ON up.usuario_id = u.id_usuario
JOIN perfil pf ON pf.id_perfil = up.perfil_id AND pf.nome = 'profissional'
LEFT JOIN profissional_detalhes pd ON pd.usuario_id = u.id_usuario
LEFT JOIN usuario_especialidade ue ON ue.usuario_id = u.id_usuario
LEFT JOIN especialidade e ON e.id_especialidade = ue.especialidade_id
WHERE u.status = 'ativo' AND u.deleted_at IS NULL
-- PG exige no GROUP BY toda coluna nao agregada (MySQL relaxa isso).
GROUP BY u.id_usuario, u.nome, u.foto_url, u.genero, u.data_nascimento,
         u.latitude, u.longitude, u.status,
         pd.preco_hora, pd.nota_media, pd.status_verificacao, pd.descricao_perfil;

-- =====================================================================
-- FIM V2+V2.2 PG. 34 tabelas + 1 view + 4 funcoes + seeds.
-- Proximo passo no backend p/ apontar ao Neon:
--   pom.xml: trocar mysql-connector-j por org.postgresql:postgresql
--   application.properties: spring.datasource.url=jdbc:postgresql://<host-neon>/<db>?sslmode=require
--   + spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
--   + DB_USERNAME/DB_PASSWORD com usuario/senha do Neon (JDBC, nao o pooler).
-- =====================================================================
