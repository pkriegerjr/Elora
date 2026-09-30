# Elora - Sistema de Cuidadores de Idosos

> Plataforma para conectar pessoas com deficiência (PcD) e seus familiares a cuidadores especializados de forma rápida, prática e eficiente.

Documento de Arquitetura v3.0 — Baseado no modelo 4+1 [KRU95]. Foco em acessibilidade (WCAG 2.1 AA), LGPD, disponibilidade 99,5% e resposta de busca ≤ 2s.

## Stack (Atual — Vanilla + Spring Boot + PostgreSQL Neon)

| Camada | Tecnologia (atual) | Observação |
| :--- | :--- | :--- |
| **Apresentação Web** | HTML5 + CSS3 + Vanilla JS + Bootstrap 5.3 | Sem build; `MOCK_MODE=true` roda 100% no `localStorage` |
| **Apresentação Mobile** | Roadmap — `mobile-app/` é esqueleto vazio (só `.gitkeep`) | Sem `package.json`, sem telas |
| **Backend API** | Spring Boot 3.2.5 (Java 17) + JWT + RBAC parcial | `backend/pom.xml`; MFA ausente (só tabela `mfa_segredo`); RBAC: `@PreAuthorize` em cuidadores + checagem em relatórios |
| **Banco Principal** | PostgreSQL (Neon) — canônico `backend/database/postgres/elora_schema_v2_pg.sql` | `application.properties` só Postgres; `ddl-auto=validate`; arquivos `elora_schema_v2*.sql` MySQL mantidos como legado |
| **Cache / Sessão** | Roadmap — sem Redis no `pom.xml` | `infra/docker-compose.yml` com Redis foi removido |
| **Storage** | Roadmap — sem S3 integrado | Upload real pendente |
| **Tempo Real** | Mock — `notificationService.js` com polling 30s | FCM não integrado |
| **Integrações** | Google Maps (mock, chave via `GET /api/config/maps-key` em prod), Gateway pagamento mock (`MockGatewayPagamentoService`), ViaCEP no front | PIX/cartão/boleto com `idempotency_key`, sem gateway real |

> Docker foi descontinuado no projeto. Não há `docker-compose.yml`. Banco local/prod é PostgreSQL (Neon).

## Arquitetura em Camadas

```
View (HTML/Bootstrap + services JS) -> API (/api, context-path /api) -> Serviços (Regras de Negócio) -> Persistência (PostgreSQL Neon) -> Integrações Externas (mock/roadmap)
```

11 módulos backend (`src/main/java/com/elora/module/`): `usuario`, `profissional`, `busca`, `contrato`, `escala`, `pagamento`, `avaliacao`, `notificacao`, `relatorio`, `conhecimento`, `juridico`.
Endpoints (`/api` + `RequestMapping`): `/auth, /clients, /caregivers (Profissional + Cuidador — colisão conhecida), /busca, /contratos, /escalas, /payments, /notifications, /relatorios, /avaliacoes, /conhecimento, /juridico`.
Docs: `GET /api/swagger-ui.html`.

10 Subsistemas previstos (Documento p.26-34):
`Cadastro e Acesso`, `Cadastro de Profissionais`, `Busca de Profissionais`, `Contratos e Assinatura Digital`, `Financeiro`, `Escalas de Trabalho`, `Jurídico e Denúncias`, `Notificações e Comunicação`, `Relatórios`, `FAQ e Base de Conhecimento`.

## Estrutura de Pastas (Atual)

```
Elora/
├── frontend-web/
│   ├── index.html                 # Entry-point (hero + busca + destaques)
│   └── src/
│       ├── assets/images/default-avatar.svg
│       ├── components/navbar.js   # stub (placeholder, não reutilizável ainda)
│       ├── config/config.js       # CONFIG.API.MOCK_MODE (:21), ENDPOINTS, mappers V1<->V2
│       ├── css/style.css, dashboard.css, responsive.css (WCAG 2.1 AA)
│       ├── pages/
│       │   ├── auth/login.html, cadastro-cliente.html, cadastro-cuidador.html
│       │   ├── busca/buscar-cuidadores.html, perfil-cuidador.html
│       │   ├── contratos/contratacao.html, contrato.html
│       │   ├── financeiro/pagamento.html, ganhos.html
│       │   ├── dashboards/dashboard-cliente/cuidador/admin.html
│       │   ├── avaliacoes/avaliar.html
│       │   ├── conhecimento/artigos.html
│       │   ├── escalas/agenda.html
│       │   ├── favoritos/lista.html
│       │   ├── juridico/denunciar.html, painel.html
│       │   ├── notificacoes/lista.html
│       │   ├── perfil/perfil.html
│       │   ├── relatorios/relatorios.html
│       │   └── termos/privacidade.html
│       ├── services/              # camada de serviços + mock
│       │   ├── apiService.js (JWT Bearer + refresh, /api)
│       │   ├── authService.js, clientService.js, caregiverService.js
│       │   ├── contractService.js, paymentService.js, mapsService.js
│       │   ├── notificationService.js, mockData.js, dto.js, app.js
│       │   ├── avaliacaoService.js, conhecimentoService.js, escalaService.js
│       │   └── juridicoService.js, relatorioService.js
│       └── utils/format.js, validation.js  # stubs; lógica canônica em app.js
├── mobile-app/src/...             # roadmap: só .gitkeep (assets, components, hooks, screens/*, services, utils)
├── backend/
│   ├── database/elora_schema_v2.sql            # legado MySQL (30 tabelas, InnoDB)
│   ├── database/elora_schema_v2_2.sql          # ALTERs (ultimo_login, status_verificacao, redefinicao_senha)
│   ├── database/elora_schema_v2_3.sql          # UNIQUE avaliacao
│   ├── database/postgres/elora_schema_v2_pg.sql      # CANÔNICO Neon (usar este)
│   ├── database/postgres/elora_schema_v2_3_pg.sql
│   ├── src/main/resources/db/migration/{conhecimento, juridico, profissional}.sql
│   ├── src/main/resources/application.properties  # lê ${DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET}
│   ├── .env.example            # modelo Neon (sem .env commitado, gitignore)
│   └── src/main/java/com/elora/ (Spring Boot)
├── infra/{docker,k8s,scripts}/   # reservados, só .gitkeep (sem compose, sem manifests)
├── docs/                          # roadmap, vazio
└── .github/                       # roadmap, sem workflows
```

> `MOCK_MODE=true` em `src/config/config.js:21` permite rodar 100% no `localStorage` sem backend.

## Banco de Dados — Schema v2 (Atual, Neon)

> Canônico: `backend/database/postgres/elora_schema_v2_pg.sql` (+ `_v2_3_pg` + `resources/db/migration/*`). Arquivos `database/elora_schema_v2*.sql` são legado MySQL.

* **Usuario único** `usuario` (herança p.15) — `cpf CHAR(11)` só dígitos (`CONFIG.normalizeCPF`), `genero ENUM(M,F,outro,prefiro_nao_informar)`, `geo_ponto` + índice espacial para busca por proximidade, `consentimento_lgpd`, `deleted_at` soft-delete.
* **RBAC dinâmico** `perfil`, `permissao`, `usuario_perfil`, `perfil_permissao` + extensões `moderador_regiao`, `juridico_detalhes`, `contratante_detalhes`, `profissional_detalhes(preco_hora, nota_media, status_verificacao)`.
* **Domínio** `especialidade` N:N, `documento_profissional`, `disponibilidade(periodo matutino/vespertino/noturno)`, `favorito`, `contrato(codigo ELO-2026-..., status rascunho/proposta/negociacao/aguard_assinatura/ativo/concluido/rescindido/cancelado/em_disputa)`, `proposta`, `mensagem_contrato`, `assinatura_contrato`, `escala_trabalho`, `taxa_servico`, `pagamento(valor_bruto/valor_taxa/valor_liquido, metodo pix/cartao/boleto, idempotency_key UNIQUE)`, `repasse`, `denuncia/evidencia_denuncia/disputa`, `avaliacao(1 por contrato, trigger valida cliente->profissional, nota_media via trigger)`, `notificacao/preferencia_notificacao`, `sessao/mfa_segredo (tabela sem código MFA)`, `trilha_auditoria(LGPD)`, `consentimento_lgpd`, `artigo_conhecimento + categoria_conteudo/faq/tutorial (migration)`, `analise_juridica/processo_rescisao (migration)`, `redefinicao_senha`, `vw_profissional_busca`.

Compat front: `config.js` `CONTRACT_STATUS`/`PAYMENT_STATUS` com aliases inglês (mock) + PT V2 + mappers `toV2ContractStatus()`, `toV2PaymentStatus()`, `scheduleToPeriodos()`, `normalizeCPF()`.

## Como Rodar (Atualizado — Neon, sem Docker)

> Front é estático — não precisa `npm install`. Backend Spring Boot opcional (MOCK_MODE).

```bash
# 1. Clonar
git clone https://github.com/seu-usuario/elora.git
cd elora

# 2. Banco (Neon — único suportado)
# 2a. Crie o projeto/DB no Neon e anote host/db/user/pass (use a connection string DIRETA, não a do pooler, para JDBC)
# 2b. Aplique o schema canônico + incrementos:
psql "host=ep-xxx.us-east-2.aws.neon.tech dbname=neondb user=... password=... sslmode=require" \
  -f Elora/backend/database/postgres/elora_schema_v2_pg.sql
psql "$NEON_URL" -f Elora/backend/database/postgres/elora_schema_v2_3_pg.sql
psql "$NEON_URL" -f Elora/backend/src/main/resources/db/migration/conhecimento.sql
psql "$NEON_URL" -f Elora/backend/src/main/resources/db/migration/juridico.sql
psql "$NEON_URL" -f Elora/backend/src/main/resources/db/migration/profissional.sql

# 3. Backend (opcional — se MOCK_MODE=false)
cd Elora/backend
cp .env.example .env   # preencha DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET (min 32 chars: openssl rand -base64 48)
mvn clean install
mvn spring-boot:run  # -> http://localhost:8080/api/swagger-ui.html (context-path /api)

# 4. Frontend Web — 100% estático, MOCK_MODE=true (default src/config/config.js:21)
# Para MOCK_MODE=false, edite MOCK_MODE para false e recarregue com backend online.
npx serve Elora/frontend-web
# ou
python -m http.server --directory Elora/frontend-web 8000
# -> http://localhost:8000  (index) e http://localhost:8000/src/pages/...

# 5. Mobile / Docs / Infra
# Roadmap: mobile-app, docs, infra/{docker,k8s,scripts} estão vazios (só .gitkeep). Sem npm install / workflows.
```

Variáveis em `Elora/backend/.env` (não commitar, ver `.env.example`):
```
DB_HOST=ep-xxx.us-east-2.aws.neon.tech
DB_NAME=neondb
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=  # min 32 chars — openssl rand -base64 48
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=2592000000
```

**Logins mock** (`src/services/mockData.js`):
`maria.santos@email.com / Senha@123` (cliente), `ana.ferreira@email.com / Senha@123` (cuidador APPROVED), `admin / Admin@123` (admin), `juridico / Juridico@123`.

## Funcionalidades

| Módulo | Funcionamento | Status |
| :--- | :--- | :--- |
| **Cadastro e Acesso** | Cadastro/login senha + Google (mock), JWT Bearer + refresh (`apiService.js`), RBAC parcial, recuperação senha, `cpfDigits CHAR(11)` | ✅ Telas `src/pages/auth/*` + `authService.js` |
| **Cadastro de Profissionais** | Cadastro cuidador, envio documentos `documento_profissional`, validação moderador/jurídico `PENDING→UNDER_REVIEW→APPROVED` | ✅ `cadastro-cuidador.html` + `caregiverService.js` |
| **Busca de Profissionais** | Filtros especialidade/gênero/avaliação/periodo + geolocalização `geo_ponto` + favoritos, `#map` mock ou Google Maps | ✅ `src/pages/busca/*` + `mapsService.js` |
| **Contratos e Assinatura** | Proposta/negociação (`proposta`), `contrato.codigo`, assinatura digital `assinatura_contrato` hash, `getStatusLabel()` V2 | ✅ `src/pages/contratos/*` + `contractService.js` |
| **Financeiro** | PIX/cartão/boleto, `valor_bruto/taxa/liquido`, `idempotency_key`, `taxa_servico`, `repasse` (gateway mock) | ✅ `src/pages/financeiro/*` + `paymentService.js` |
| **Escalas de Trabalho** | `disponibilidade` e `escala_trabalho` por `periodo matutino/vespertino/noturno`, `scheduleToPeriodos()` | 🚧 Services ok, telas em `dashboards`/`escalas/agenda.html` |
| **Jurídico e Denúncias** | `denuncia/evidencia/disputa`, `trilha_auditoria` | 🚧 Schema + API prontos, painel em `juridico/painel.html` + `dashboard-admin.html` |
| **Notificações e Comunicação** | `notificacao/mensagem_contrato`, `preferencia_notificacao`, polling 30s (FCM roadmap) | ✅ `notificationService.js` + badges `[data-notification-badge]` + `notificacoes/lista.html` |
| **Relatórios** | Admin/financeiro/jurídico (403 se perfil cliente) | 🚧 `relatorios/relatorios.html` + `dashboard-admin` |
| **FAQ e Base** | `artigo_conhecimento + faq/tutorial/categoria` | 🚧 `conhecimento/artigos.html` |
| **Avaliações / Favoritos / Perfil** | 1 avaliação por contrato, favoritos, perfil | ✅ `avaliacoes/avaliar.html`, `favoritos/lista.html`, `perfil/perfil.html` |

## Requisitos Não Funcionais

* RNF-009: Busca ≤ 2s | RNF-007: 99,5% uptime | RNF-003: WCAG 2.1 AA (`responsive.css` alvos 44px, `style.css` `focus-visible`, `prefers-reduced-motion`) | RNF-005: LGPD (`consentimento_lgpd`, `deleted_at`, `trilha_auditoria`) | RNF-015: Backup diário (Neon PITR) | RNF-014: `geo_ponto` busca por proximidade

## Equipe

Wallyson Barbosa, Lucas M. Carrijo, Tiago F. Dias, Mateus Xavier, Otávio Augusto

## Licença

Apache-2.0 — ver `LICENSE`
