-- =====================================================================
-- PROJETO ELORA - SEED DE DEMONSTRAÇÃO (PostgreSQL/Neon + PostGIS)
--
-- O QUE ESTE ARQUIVO FAZ
--   * Especialidades (tags usadas nos filtros de busca)
--   * Categorias de conteúdo, artigos publicados, FAQ e tutoriais
--   * Usuários de demonstração: admin, jurídico, cliente e cuidadores
--
-- IMPORTANTE
--   * É IDEMPOTENTE: pode rodar quantas vezes quiser (ON CONFLICT / NOT EXISTS).
--   * NÃO substitui elora_schema_v2_pg.sql / _v2_3_pg / _v2_4_pg.
--     Rode este APÓS o schema já estar aplicado.
--   * Senha de TODOS os usuários demo: Senha@123
--     (hash bcrypt gerado localmente; NÃO use em produção)
--
-- COMO APLICAR
--   psql "<connection-string do Neon>" -f seed_demo_pg.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. ESPECIALIDADES (catálogo de tags da busca)
-- ---------------------------------------------------------------------
INSERT INTO especialidade (nome) VALUES
    ('Idoso'),
    ('Alzheimer'),
    ('Parkinson'),
    ('Mobilidade reduzida'),
    ('Pós-operatório'),
    ('Medicação'),
    ('Cuidados paliativos'),
    ('Demência'),
    ('Autismo (TEA)'),
    ('Reabilitação')
ON CONFLICT (nome) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. CATEGORIAS DE CONTEÚDO
-- ---------------------------------------------------------------------
INSERT INTO categoria_conteudo (nome_categoria, descricao) VALUES
    ('Primeiros passos', 'Como usar a plataforma e contratar um cuidador'),
    ('Saúde e cuidado', 'Orientações práticas para o dia a dia'),
    ('Segurança e contratos', 'Documentos, verificação e assinatura digital'),
    ('Financeiro', 'Pagamentos, taxas e repasses')
ON CONFLICT (nome_categoria) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. USUÁRIOS DEMO (senha: Senha@123)
-- ---------------------------------------------------------------------
INSERT INTO usuario
    (nome, email, cpf, senha_hash, telefone, genero, status, email_verificado,
     latitude, longitude, consentimento_lgpd, consentimento_lgpd_at)
SELECT v.nome, v.email, v.cpf,
       '$2a$10$O/WV1YxP9SB6qI4aFIpUi.4pD0h31/CqnElFlf6rangl4Sm87l92m',
       v.telefone, v.genero, 'ativo', TRUE,
       v.latitude, v.longitude, TRUE, CURRENT_TIMESTAMP
FROM (VALUES
    ('Administração Elora', 'admin@elora.local',    '12345678909', '11999990001', 'prefiro_nao_informar', NULL::DECIMAL, NULL::DECIMAL),
    ('Jurídico Elora',      'juridico@elora.local', '98765432100', '11999990002', 'prefiro_nao_informar', NULL::DECIMAL, NULL::DECIMAL),
    ('Cliente Demonstração','cliente.demo@elora.local','45678912364','11999990003', 'F', -23.56150000::DECIMAL, -46.69180000::DECIMAL),
    ('Ana Souza',           'ana.souza@elora.local', '32165498791', '11999990004', 'F', -23.56530000::DECIMAL, -46.69560000::DECIMAL),
    ('Carlos Lima',         'carlos.lima@elora.local','74185296355','11999990005', 'M', -23.55720000::DECIMAL, -46.68250000::DECIMAL),
    ('Beatriz Alves',       'beatriz.alves@elora.local','15935725819','11999990006','F', -23.57110000::DECIMAL, -46.70140000::DECIMAL),
    ('Roberto Pendente',    'roberto.pendente@elora.local','14725836982','11999990007','M', -23.55000000::DECIMAL, -46.63000000::DECIMAL)
) AS v(nome, email, cpf, telefone, genero, latitude, longitude)
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email = v.email);

-- ---------------------------------------------------------------------
-- 4. PERFIS (RBAC já vem semeado pelo schema)
-- ---------------------------------------------------------------------
INSERT INTO usuario_perfil (usuario_id, perfil_id)
SELECT u.id_usuario, p.id_perfil
FROM (VALUES
    ('admin@elora.local', 'admin'),
    ('juridico@elora.local', 'juridico'),
    ('cliente.demo@elora.local', 'cliente'),
    ('ana.souza@elora.local', 'profissional'),
    ('carlos.lima@elora.local', 'profissional'),
    ('beatriz.alves@elora.local', 'profissional'),
    ('roberto.pendente@elora.local', 'profissional')
) AS v(email, perfil)
JOIN usuario u ON u.email = v.email
JOIN perfil  p ON p.nome  = v.perfil
ON CONFLICT (usuario_id, perfil_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 5. DETALHES DE CONTRATANTE
-- ---------------------------------------------------------------------
INSERT INTO contratante_detalhes (usuario_id, observacoes_cuidado)
SELECT u.id_usuario, 'Prefere atendimento no período da manhã. Contato por telefone.'
FROM usuario u
WHERE u.email = 'cliente.demo@elora.local'
ON CONFLICT (usuario_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 6. DETALHES DE PROFISSIONAL
--    (a busca pública lista apenas status_verificacao = 'aprovado')
-- ---------------------------------------------------------------------
INSERT INTO profissional_detalhes
    (usuario_id, descricao_perfil, preco_hora, documento_verificado, status_verificacao, nota_media)
SELECT u.id_usuario, v.descricao, v.preco, v.verificado, v.status, 0.00
FROM (VALUES
    ('ana.souza@elora.local',
     'Cuidadora de idosos com 8 anos de experiência, especializada em Alzheimer e demência. Formação em técnico de enfermagem.',
     45.00::DECIMAL, TRUE, 'aprovado'),
    ('carlos.lima@elora.local',
     'Fisioterapeuta com foco em reabilitação e mobilidade reduzida. Atendimento domiciliar e acompanhamento de Parkinson.',
     60.00::DECIMAL, TRUE, 'aprovado'),
    ('beatriz.alves@elora.local',
     'Acompanhante de pós-operatório e administração de medicação, com curso de primeiros socorros atualizado.',
     38.00::DECIMAL, TRUE, 'aprovado'),
    ('roberto.pendente@elora.local',
     'Cadastro em análise: aparece no painel, mas não na vitrine pública.',
     30.00::DECIMAL, FALSE, 'pendente')
) AS v(email, descricao, preco, verificado, status)
JOIN usuario u ON u.email = v.email
ON CONFLICT (usuario_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 7. ESPECIALIDADES POR PROFISSIONAL
-- ---------------------------------------------------------------------
INSERT INTO usuario_especialidade (usuario_id, especialidade_id)
SELECT u.id_usuario, e.id_especialidade
FROM (VALUES
    ('ana.souza@elora.local', 'Alzheimer'),
    ('ana.souza@elora.local', 'Idoso'),
    ('ana.souza@elora.local', 'Demência'),
    ('carlos.lima@elora.local', 'Parkinson'),
    ('carlos.lima@elora.local', 'Mobilidade reduzida'),
    ('carlos.lima@elora.local', 'Reabilitação'),
    ('beatriz.alves@elora.local', 'Pós-operatório'),
    ('beatriz.alves@elora.local', 'Medicação')
) AS v(email, especialidade)
JOIN usuario u ON u.email = v.email
JOIN especialidade e ON e.nome = v.especialidade
ON CONFLICT (usuario_id, especialidade_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 8. CONSENTIMENTOS LGPD
-- ---------------------------------------------------------------------
INSERT INTO consentimento_lgpd (usuario_id, tipo, versao, aceito, ip)
SELECT u.id_usuario, v.tipo, 'v1.0', TRUE, '127.0.0.1'
FROM usuario u
CROSS JOIN (VALUES ('termos'), ('privacidade')) AS v(tipo)
WHERE u.email IN (
    'admin@elora.local',
    'juridico@elora.local',
    'cliente.demo@elora.local',
    'ana.souza@elora.local',
    'carlos.lima@elora.local',
    'beatriz.alves@elora.local',
    'roberto.pendente@elora.local'
)
AND NOT EXISTS (
    SELECT 1 FROM consentimento_lgpd c
    WHERE c.usuario_id = u.id_usuario AND c.tipo = v.tipo AND c.versao = 'v1.0'
);

-- ---------------------------------------------------------------------
-- 9. ARTIGOS PUBLICADOS
-- ---------------------------------------------------------------------
INSERT INTO artigo_conhecimento
    (titulo, corpo, categoria, autor_id, publicado, categoria_id)
SELECT v.titulo, v.corpo, v.categoria, a.id_usuario, TRUE, c.id_categoria
FROM (VALUES
    ('Como escolher um cuidador de idosos',
     'Comece definindo as necessidades da pessoa cuidada: mobilidade, medicação, acompanhamento noturno e tempo de dedicação. Na Elora, todos os profissionais exibem documentos verificados e avaliações de outros contratantes. Converse por mensagem, tire dúvidas e combine uma visita antes de fechar o contrato.',
     'Primeiros passos'),
    ('Alzheimer: rotina e segurança em casa',
     'Uma rotina previsível reduz a ansiedade. Mantenha horários fixos para refeições, banho e sono, retire tapetes soltos e sinalize portas. Evite discutir ou corrigir a pessoa sobre fatos esquecidos; redirecione a conversa com gentileza. Cuidadoras especializadas em Alzheimer podem orientar a família.',
     'Saúde e cuidado'),
    ('Contrato digital: o que assinar',
     'O contrato registra valor por hora, jornada, período e responsabilidades de cada parte. Leia com atenção antes de assinar, confira os dados do profissional e guarde o código do contrato (ELO-AAAA-XXXXXX). Alterações de jornada devem ser registradas na plataforma.',
     'Segurança e contratos'),
    ('Pagamento protegido: como funciona',
     'O valor é reservado no momento da contratação e liberado ao profissional após a execução do serviço. A taxa da plataforma é informada antes da confirmação. Em caso de cancelamento, as regras aparecem na tela de pagamento e no contrato.',
     'Financeiro')
) AS v(titulo, corpo, categoria)
JOIN usuario a ON a.email = 'admin@elora.local'
JOIN categoria_conteudo c ON c.nome_categoria = v.categoria
WHERE NOT EXISTS (SELECT 1 FROM artigo_conhecimento x WHERE x.titulo = v.titulo);

-- ---------------------------------------------------------------------
-- 10. FAQ PUBLICADO
-- ---------------------------------------------------------------------
INSERT INTO faq (pergunta, resposta, categoria, status, categoria_id)
SELECT v.pergunta, v.resposta, v.categoria, 'publicado', c.id_categoria
FROM (VALUES
    ('Os cuidadores são verificados?',
     'Sim. Documentos e antecedentes são analisados pela equipe. O selo "Verificado" aparece no perfil quando a análise é concluída.',
     'Segurança e contratos'),
    ('Preciso pagar algo para buscar cuidadores?',
     'Não. A busca e o contato são gratuitos. A taxa da plataforma é aplicada apenas quando você contrata.',
     'Financeiro'),
    ('Posso trocar o cuidador no meio do contrato?',
     'Sim. Você pode encerrar o contrato atual e criar um novo com outro profissional. O histórico de avaliações permanece.',
     'Primeiros passos'),
    ('Como funciona o suporte?',
     'Dúvidas sobre contrato e pagamento podem ser abertas na área de ajuda. Denúncias são encaminhadas ao time jurídico.',
     'Segurança e contratos')
) AS v(pergunta, resposta, categoria)
JOIN categoria_conteudo c ON c.nome_categoria = v.categoria
WHERE NOT EXISTS (SELECT 1 FROM faq f WHERE f.pergunta = v.pergunta);

-- ---------------------------------------------------------------------
-- 11. TUTORIAIS
-- ---------------------------------------------------------------------
INSERT INTO tutorial (titulo, descricao, link_conteudo, status, categoria_id)
SELECT v.titulo, v.descricao, v.link, 'publicado', c.id_categoria
FROM (VALUES
    ('Criando sua conta', 'Cadastro de cliente em menos de 2 minutos.', '/src/pages/auth/register.html', 'Primeiros passos'),
    ('Buscando e favoritando cuidadores', 'Use os filtros de especialidade, avaliação e preço.', '/src/pages/busca/buscar-cuidadores.html', 'Primeiros passos'),
    ('Assinando o contrato', 'Passo a passo da proposta até a assinatura.', '/src/pages/contratos/meus-contratos.html', 'Segurança e contratos')
) AS v(titulo, descricao, link, categoria)
JOIN categoria_conteudo c ON c.nome_categoria = v.categoria
WHERE NOT EXISTS (SELECT 1 FROM tutorial t WHERE t.titulo = v.titulo);

COMMIT;

-- ---------------------------------------------------------------------
-- Conferência rápida (rode manualmente se quiser):
--   SELECT email, status FROM usuario ORDER BY id_usuario;
--   SELECT nome, status_verificacao, preco_hora FROM profissional_detalhes d
--     JOIN usuario u ON u.id_usuario = d.usuario_id ORDER BY u.nome;
--   SELECT count(*) FROM artigo_conhecimento WHERE publicado;
--   SELECT count(*) FROM faq WHERE status = 'publicado';
-- ---------------------------------------------------------------------
