-- =====================================================================
-- PROJETO ELORA - SCHEMA v2.4 (PostgreSQL/Neon)
-- Corrige o schema do modulo CONHECIMENTO, que ficou fora do
-- elora_schema_v2_pg.sql enquanto as entities Java ja existiam.
-- Sem breaking change: apenas ADICIONA o que faltava. Rode uma vez.
--
-- Por que: com spring.jpa.hibernate.ddl-auto=validate a aplicacao
-- nao sobe se alguma tabela/coluna do codigo nao existir no banco.
-- Faltavam: tabela categoria_conteudo, tabelas tutorial e faq,
-- e a coluna categoria_id em artigo_conhecimento.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Categoria de conteudo (tabela mae das tres abaixo)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categoria_conteudo (
    id_categoria  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome_categoria VARCHAR(80) NOT NULL UNIQUE,
    descricao     TEXT NULL,
    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 2) Tutorial
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tutorial (
    id_tutorial     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo          VARCHAR(255) NOT NULL,
    descricao       TEXT NULL,
    link_conteudo   VARCHAR(500) NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'rascunho',
    categoria_id    INT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tutorial_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria_conteudo (id_categoria)
);

-- ---------------------------------------------------------------------
-- 3) FAQ
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faq (
    id_faq       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pergunta     TEXT NOT NULL,
    resposta     TEXT NOT NULL,
    categoria    VARCHAR(100) NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'rascunho',
    categoria_id INT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_faq_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria_conteudo (id_categoria)
);

-- ---------------------------------------------------------------------
-- 4) Artigo: categoria desnormalizada (texto) ja existe;
--    falta a FK para a categoria.
-- ---------------------------------------------------------------------
ALTER TABLE artigo_conhecimento
    ADD COLUMN IF NOT EXISTS categoria_id INT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_artigo_categoria'
          AND table_name = 'artigo_conhecimento'
    ) THEN
        ALTER TABLE artigo_conhecimento
            ADD CONSTRAINT fk_artigo_categoria FOREIGN KEY (categoria_id)
                REFERENCES categoria_conteudo (id_categoria);
    END IF;
END $$;

-- =====================================================================
-- FIM SCHEMA v2.4 PG
-- =====================================================================
