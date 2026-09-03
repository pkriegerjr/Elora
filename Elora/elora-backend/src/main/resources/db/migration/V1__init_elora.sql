-- V1__init_elora.sql
-- ELORA - Sistema de cuidadores de idosos v3.0
-- PostgreSQL + AES-256 considerations, LGPD compliant

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    telefone VARCHAR(50),
    status VARCHAR(50),
    data_cadastro DATE,
    created_at TIMESTAMP, updated_at TIMESTAMP, ativo BOOLEAN DEFAULT TRUE
);

CREATE TABLE cliente (
    id_cliente SERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuario(id_usuario),
    cpf VARCHAR(14) UNIQUE,
    endereco VARCHAR(500),
    data_nascimento DATE
);

CREATE TABLE operador (
    id_operador SERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuario(id_usuario),
    setor VARCHAR(100),
    cargo VARCHAR(100)
);

CREATE TABLE permissao (
    id_permissao SERIAL PRIMARY KEY,
    nome_permissao VARCHAR(100) UNIQUE,
    descricao VARCHAR(255)
);

CREATE TABLE perfil_acesso (
    id_perfil SERIAL PRIMARY KEY,
    nome_perfil VARCHAR(100),
    descricao VARCHAR(255)
);

CREATE TABLE perfil_permissao (
    perfil_id BIGINT REFERENCES perfil_acesso(id_perfil),
    permissao_id BIGINT REFERENCES permissao(id_permissao),
    PRIMARY KEY (perfil_id, permissao_id)
);

CREATE TABLE sessao (
    id_sessao SERIAL PRIMARY KEY,
    token VARCHAR(500),
    data_inicio TIMESTAMP,
    data_expiracao TIMESTAMP,
    usuario_id BIGINT REFERENCES usuario(id_usuario)
);

-- Profissional
CREATE TABLE profissional (
    id_profissional SERIAL PRIMARY KEY,
    nome VARCHAR(255), cpf VARCHAR(14) UNIQUE, telefone VARCHAR(50),
    email VARCHAR(255), endereco VARCHAR(500),
    status_validacao VARCHAR(50), status_atividade VARCHAR(50),
    data_cadastro DATE
);
CREATE TABLE documento_profissional (
    id_documento SERIAL PRIMARY KEY, tipo_documento VARCHAR(100),
    numero_documento VARCHAR(100), arquivo VARCHAR(500),
    status_documento VARCHAR(50), data_envio DATE,
    profissional_id BIGINT REFERENCES profissional(id_profissional)
);
CREATE TABLE certificacao (
    id_certificacao SERIAL PRIMARY KEY, nome_certificacao VARCHAR(255),
    instituicao VARCHAR(255), data_conclusao DATE,
    arquivo_comprovante VARCHAR(500), profissional_id BIGINT REFERENCES profissional(id_profissional)
);
CREATE TABLE especialidade (
    id_especialidade SERIAL PRIMARY KEY, nome_especialidade VARCHAR(255), descricao VARCHAR(500)
);
CREATE TABLE validacao_profissional (
    id_validacao SERIAL PRIMARY KEY, data_validacao DATE,
    resultado VARCHAR(50), observacao TEXT, profissional_id BIGINT REFERENCES profissional(id_profissional)
);

-- Busca
CREATE TABLE filtro_busca (id_filtro SERIAL PRIMARY KEY, genero VARCHAR(50), especialidade VARCHAR(100), localizacao VARCHAR(255), avaliacao_minima DOUBLE PRECISION, disponibilidade VARCHAR(50));
CREATE TABLE localizacao (id_localizacao SERIAL PRIMARY KEY, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, cidade VARCHAR(255), estado VARCHAR(50), raio_busca DOUBLE PRECISION);
CREATE TABLE favorito (id_favorito SERIAL PRIMARY KEY, data_inclusao DATE, cliente_id BIGINT REFERENCES cliente(id_cliente), profissional_id BIGINT REFERENCES profissional(id_profissional));

-- Contrato
CREATE TABLE proposta_contrato (id_proposta SERIAL PRIMARY KEY, descricao_servico TEXT, valor_proposta DOUBLE PRECISION, data_proposta DATE, status_proposta VARCHAR(50), profissional_id BIGINT, cliente_id BIGINT);
CREATE TABLE status_contrato (id_status SERIAL PRIMARY KEY, nome_status VARCHAR(50), descricao VARCHAR(255));
CREATE TABLE contrato (id_contrato SERIAL PRIMARY KEY, data_inicio DATE, data_fim DATE, valor_total DOUBLE PRECISION, status_contrato VARCHAR(50), proposta_id BIGINT REFERENCES proposta_contrato(id_proposta), status_id BIGINT REFERENCES status_contrato(id_status));
CREATE TABLE negociacao (id_negociacao SERIAL PRIMARY KEY, mensagem TEXT, data_mensagem DATE, valor_sugerido DOUBLE PRECISION, contrato_id BIGINT REFERENCES contrato(id_contrato));
CREATE TABLE assinatura_digital (id_assinatura SERIAL PRIMARY KEY, data_assinatura DATE, hash_assinatura VARCHAR(500), status_assinatura VARCHAR(50), contrato_id BIGINT REFERENCES contrato(id_contrato));

-- Financeiro
CREATE TABLE pagamento (id_pagamento SERIAL PRIMARY KEY, valor DOUBLE PRECISION, data_pagamento DATE, forma_pagamento VARCHAR(50), status_pagamento VARCHAR(50), contrato_id BIGINT REFERENCES contrato(id_contrato));
CREATE TABLE repasse (id_repasse SERIAL PRIMARY KEY, valor_repasse DOUBLE PRECISION, data_repasse DATE, status_repasse VARCHAR(50), pagamento_id BIGINT REFERENCES pagamento(id_pagamento), profissional_id BIGINT REFERENCES profissional(id_profissional));
CREATE TABLE taxa_servico (id_taxa SERIAL PRIMARY KEY, nome_taxa VARCHAR(100), percentual DOUBLE PRECISION, data_vigencia DATE, status_taxa VARCHAR(50));
CREATE TABLE configuracao_financeira (id_configuracao SERIAL PRIMARY KEY, percentual_repasse DOUBLE PRECISION, percentual_plataforma DOUBLE PRECISION, data_configuracao DATE);

-- Escalas
CREATE TABLE escala_trabalho (id_escala SERIAL PRIMARY KEY, data_inicio DATE, data_fim DATE, tipo_escala VARCHAR(50), status_escala VARCHAR(50), contrato_id BIGINT REFERENCES contrato(id_contrato));
CREATE TABLE turno (id_turno SERIAL PRIMARY KEY, horario_inicio TIME, horario_fim TIME, tipo_turno VARCHAR(50), escala_id BIGINT REFERENCES escala_trabalho(id_escala));
CREATE TABLE disponibilidade (id_disponibilidade SERIAL PRIMARY KEY, dia_semana VARCHAR(20), horario_inicio VARCHAR(20), horario_fim VARCHAR(20), status_disponibilidade VARCHAR(50), profissional_id BIGINT REFERENCES profissional(id_profissional));
CREATE TABLE alerta_escala (id_alerta SERIAL PRIMARY KEY, mensagem TEXT, data_envio DATE, status_alerta VARCHAR(50), escala_id BIGINT REFERENCES escala_trabalho(id_escala));

-- Avaliacao / Denuncia
CREATE TABLE avaliacao (id_avaliacao SERIAL PRIMARY KEY, nota INT, comentario TEXT, data_avaliacao DATE, profissional_id BIGINT, cliente_id BIGINT);
CREATE TABLE denuncia (id_denuncia SERIAL PRIMARY KEY, motivo VARCHAR(255), descricao TEXT, data_denuncia DATE, status_denuncia VARCHAR(50));
CREATE TABLE evidencia (id_evidencia SERIAL PRIMARY KEY, tipo_evidencia VARCHAR(100), arquivo VARCHAR(500), descricao TEXT, denuncia_id BIGINT REFERENCES denuncia(id_denuncia));
CREATE TABLE historico_denuncia (id_historico SERIAL PRIMARY KEY, data_registro DATE, descricao_acao TEXT, responsavel VARCHAR(255), denuncia_id BIGINT REFERENCES denuncia(id_denuncia));

-- Juridico
CREATE TABLE painel_juridico (id_painel SERIAL PRIMARY KEY, data_atualizacao DATE);
CREATE TABLE analise_juridica (id_analise SERIAL PRIMARY KEY, parecer TEXT, data_analise DATE, status_analise VARCHAR(50), contrato_id BIGINT REFERENCES contrato(id_contrato));
CREATE TABLE processo_rescisao (id_rescisao SERIAL PRIMARY KEY, motivo TEXT, data_solicitacao DATE, status_rescisao VARCHAR(50), contrato_id BIGINT REFERENCES contrato(id_contrato));
CREATE TABLE disputa (id_disputa SERIAL PRIMARY KEY, descricao TEXT, status_disputa VARCHAR(50), data_abertura DATE, contrato_id BIGINT REFERENCES contrato(id_contrato));

-- Notificacao
CREATE TABLE canal_comunicacao (id_canal SERIAL PRIMARY KEY, tipo_canal VARCHAR(50), ativo BOOLEAN);
CREATE TABLE notificacao (id_notificacao SERIAL PRIMARY KEY, titulo VARCHAR(255), mensagem TEXT, tipo_notificacao VARCHAR(50), data_envio DATE, status_leitura VARCHAR(50), usuario_id BIGINT REFERENCES usuario(id_usuario), canal_id BIGINT REFERENCES canal_comunicacao(id_canal));
CREATE TABLE preferencia_notificacao (id_preferencia SERIAL PRIMARY KEY, receber_email BOOLEAN, receber_sms BOOLEAN, receber_push BOOLEAN, usuario_id BIGINT REFERENCES usuario(id_usuario));
CREATE TABLE mensagem (id_mensagem SERIAL PRIMARY KEY, conteudo TEXT, data_envio DATE, status_mensagem VARCHAR(50), remetente_id BIGINT, destinatario_id BIGINT);

-- Conhecimento
CREATE TABLE categoria_conteudo (id_categoria SERIAL PRIMARY KEY, nome_categoria VARCHAR(255), descricao TEXT);
CREATE TABLE faq (id_faq SERIAL PRIMARY KEY, pergunta TEXT, resposta TEXT, categoria VARCHAR(100), status VARCHAR(50), categoria_id BIGINT REFERENCES categoria_conteudo(id_categoria));
CREATE TABLE tutorial (id_tutorial SERIAL PRIMARY KEY, titulo VARCHAR(255), descricao TEXT, link_conteudo VARCHAR(500), status VARCHAR(50), categoria_id BIGINT REFERENCES categoria_conteudo(id_categoria));
CREATE TABLE artigo_conhecimento (id_artigo SERIAL PRIMARY KEY, titulo VARCHAR(255), conteudo TEXT, categoria VARCHAR(100), data_publicacao DATE, categoria_id BIGINT REFERENCES categoria_conteudo(id_categoria));

-- Auditoria LGPD (RNF-ELO-012)
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    usuario VARCHAR(255),
    acao VARCHAR(100),
    entidade VARCHAR(100),
    entidade_id BIGINT,
    data_hora TIMESTAMP DEFAULT NOW(),
    detalhes TEXT
);
