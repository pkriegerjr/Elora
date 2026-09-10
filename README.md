# Elora - Sistema de Cuidadores de Idosos

> Plataforma para conectar pessoas com deficiência (PcD) e seus familiares a cuidadores especializados de forma rápida, prática e eficiente.

Documento de Arquitetura v3.0 — Baseado no modelo 4+1 [KRU95]. Foco em acessibilidade (WCAG 2.1 AA), LGPD, disponibilidade 99,5% e resposta de busca ≤ 2s.

## Stack (Atualizado — Frente focada em Vanilla + Spring Boot)

| Camada | Tecnologia (atual) | Planejado / Alternativa |
| :--- | :--- | :--- |
| **Apresentação Web** | HTML5 + CSS3 + Vanilla JS + Bootstrap 5.3 | React.js (previsto inicialmente, mantido como evolução futura) |
| **Apresentação Mobile** | React Native (Android / iOS) — estrutura pronta | — |
| **Backend API** | Spring Boot (Java) + JWT + RBAC + MFA | NestJS era previsto inicialmente; código atual em `backend/src/main/java/com/elora` |
| **Banco Principal** | MySQL 8.0 / PostgreSQL (utf8mb4) — `elora_schema_v2.sql` | PostgreSQL |
| **Cache / Sessão** | Redis | — |
| **Storage** | AWS S3 (fotos, documentos, evidências) | — |
| **Tempo Real** | Firebase FCM (push) | — |
| **Integrações** | Google Maps SDK, Gateway de Pagamento (PIX/cartão/boleto), Validador de Documentos, ViaCEP | — |

## Arquitetura em Camadas

```
View (HTML/Bootstrap + services JS) -> API Gateway (Spring Boot /api) -> Serviços (Regras de Negócio) -> Persistência (MySQL/PostgreSQL + Redis) -> Integrações Externas
```

10 Subsistemas previstos (Documento p.26-34):
`Cadastro e Acesso`, `Cadastro de Profissionais`, `Busca de Profissionais`, `Contratos e Assinatura Digital`, `Financeiro`, `Escalas de Trabalho`, `Jurídico e Denúncias`, `Notificações e Comunicação`, `Relatórios`, `FAQ e Base de Conhecimento`.

## Estrutura de Pastas (Atual — após reorganização)

```
/elora
├── /frontend-web
│   ├── index.html                 # Entry-point (hero + busca + destaques)
│   └── src/
│       ├── /assets/images/default-avatar.svg
│       ├── /components/navbar.js  # componentes reutilizáveis (navbar, cards)
│       ├── /config/config.js      # CONFIG.API.MOCK_MODE, ENDPOINTS, mappers V1<->V2
│       ├── /css/style.css, dashboard.css, responsive.css (WCAG 2.1 AA)
│       ├── /pages/
│       │   ├── /auth/login.html, cadastro-cliente.html, cadastro-cuidador.html
│       │   ├── /busca/buscar-cuidadores.html, perfil-cuidador.html
│       │   ├── /contratos/contratacao.html, contrato.html
│       │   ├── /financeiro/pagamento.html
│       │   └── /dashboards/dashboard-cliente/cuidador/admin.html
│       ├── /services/             # ex-src/js — camada de serviços + mock
│       │   ├── apiService.js (JWT Bearer + refresh, /api)
│       │   ├── authService.js, clientService.js, caregiverService.js
│       │   ├── contractService.js, paymentService.js, mapsService.js
│       │   ├── notificationService.js, mockData.js, dto.js, app.js
│       │   └── 
│       └── /utils/format.js, validation.js
├── /mobile-app/src/...
├── /backend
│   ├── /database/elora_schema_v2.sql
│   └── /src/main/java/com/elora/ (Spring Boot)
├── /docs
├── /infra
└── /.github/workflows
```

> Removidos: `frontend-web/src/hooks/` (conceito React, sem uso em Vanilla), `src/screens/` (vazio, trocado por `src/pages/`), `src/js/` (renomeado para `src/services/`), `.gitkeep` redundantes.
> `MOCK_MODE=true` em `src/config/config.js:21` permite rodar 100% no `localStorage` sem backend.

## Banco de Dados — Schema v2 (Atual)

> `backend/database/elora_schema_v2.sql` corrige v1 e espelha REQ-ELO-001..021 + RNF-ELO.

* **Usuario único** `usuario` (herança p.15) — `cpf CHAR(11)` só dígitos (`CONFIG.normalizeCPF`), `genero ENUM(M,F,outro,prefiro_nao_informar)`, `geo_ponto POINT STORED` + `SPATIAL INDEX` para busca por proximidade, `consentimento_lgpd`, `deleted_at` soft-delete.
* **RBAC dinâmico** `perfil`, `permissao`, `usuario_perfil`, `perfil_permissao` + extensões `moderador_regiao`, `juridico_detalhes`, `contratante_detalhes`, `profissional_detalhes(preco_hora, nota_media)`.
* **Domínio** `especialidade` N:N, `documento_profissional`, `disponibilidade(periodo matutino/vespertino/noturno)`, `favorito`, `contrato(codigo ELO-2026-..., status rascunho/proposta/negociacao/aguard_assinatura/ativo/concluido/rescindido/cancelado/em_disputa)`, `proposta`, `mensagem_contrato`, `assinatura_contrato`, `escala_trabalho`, `taxa_servico`, `pagamento(valor_bruto/valor_taxa/valor_liquido, metodo pix/cartao/boleto, idempotency_key UNIQUE)`, `repasse`, `denuncia/evidencia_denuncia/disputa`, `avaliacao(1 por contrato, trigger valida cliente->profissional, nota_media via trigger)`, `notificacao/preferencia_notificacao`, `sessao/mfa_segredo`, `trilha_auditoria(LGPD)`, `consentimento_lgpd`, `artigo_conhecimento`, `vw_profissional_busca`.

Compat front: `config.js:64` `CONTRACT_STATUS`/`PAYMENT_STATUS` com aliases inglês (mock) + PT V2 + mappers `toV2ContractStatus()`, `toV2PaymentStatus()`, `scheduleToPeriodos()`, `normalizeCPF()`.

## Como Rodar (Atualizado)

> Front é estático — não precisa `npm install`. Backend Spring Boot opcional (MOCK_MODE).

```bash
# 1. Clonar
git clone https://github.com/seu-usuario/elora.git
cd elora

# 2. Banco (Docker) — schema v2
docker-compose up -d postgres redis   # ou mysql:8.0
mysql -u root -p < Elora/backend/database/elora_schema_v2.sql
# ou psql -U postgres -f Elora/backend/database/elora_schema_v2.sql

# 3. Backend (opcional — se MOCK_MODE=false)
cd Elora/backend
# ./mvnw spring-boot:run  -> http://localhost:8080/api

# 4. Frontend Web — 100% estático, MOCK_MODE=true
# Opção A: abrir direto
start Elora/frontend-web/index.html
# Opção B: servir (recomendado para resolver CORS/file://)
npx serve Elora/frontend-web
# ou
python -m http.server --directory Elora/frontend-web 8000
# -> http://localhost:8000  (index) e http://localhost:8000/src/pages/...

# 5. Mobile
cd ../mobile-app
npm install
npm run start # Expo
```

Variáveis em `.env` (não commitar):
```
DATABASE_URL=postgresql://user:pass@localhost:5432/elora_db  # ou mysql://
REDIS_URL=redis://localhost:6379
JWT_SECRET=
GOOGLE_CLIENT_ID=
MAPS_API_KEY=            # em prod vem de GET /api/config/maps-key (config.js:37)
```

**Logins mock** (`src/services/mockData.js:347`):
`maria.santos@email.com / Senha@123` (cliente), `ana.ferreira@email.com / Senha@123` (cuidador APPROVED), `admin / Admin@123`.

## Funcionalidades

| Módulo | Funcionamento | Status |
| :--- | :--- | :--- |
| **Cadastro e Acesso** | Cadastro/login senha + Google (mock), JWT Bearer + refresh (`apiService.js:104`), RBAC, MFA para jurídico/financeiro, recuperação senha, `cpfDigits CHAR(11)` | ✅ Telas `src/pages/auth/*` + `authService.js:76` |
| **Cadastro de Profissionais** | Cadastro cuidador, envio documentos `documento_profissional`, validação moderador/jurídico `PENDING→UNDER_REVIEW→APPROVED` | ✅ `cadastro-cuidador.html` + `caregiverService.js:20` |
| **Busca de Profissionais** | Filtros especialidade/gênero/avaliação/periodo + geolocalização `geo_ponto` + favoritos, `#map` mock ou Google Maps | ✅ `src/pages/busca/*` + `mapsService.js:30` |
| **Contratos e Assinatura** | Proposta/negociação (`proposta`), `contrato.codigo`, assinatura digital `assinatura_contrato` hash, `getStatusLabel()` V2 | ✅ `src/pages/contratos/*` + `contractService.js:19` |
| **Financeiro** | PIX/cartão/boleto, `valor_bruto/taxa/liquido`, `idempotency_key`, `taxa_servico`, `repasse` | ✅ `src/pages/financeiro/pagamento.html` + `paymentService.js:23` |
| **Escalas de Trabalho** | `disponibilidade` e `escala_trabalho` por `periodo matutino/vespertino/noturno`, `scheduleToPeriodos()` | 🚧 Services ok, telas em `dashboards` (agenda) |
| **Jurídico e Denúncias** | `denuncia/evidencia/disputa`, `trilha_auditoria` | 🚧 Schema pronto, painel em `dashboard-admin.html` |
| **Notificações e Comunicação** | Push FCM, `notificacao/mensagem_contrato`, `preferencia_notificacao`, polling 30s | ✅ `notificationService.js:1` + badges `[data-notification-badge]` |
| **Relatórios** | Admin/financeiro/jurídico | 🚧 Estrutura `dashboard-admin` |
| **FAQ e Base** | `artigo_conhecimento` | 🚧 Previsto |

## Requisitos Não Funcionais

* RNF-009: Busca ≤ 2s | RNF-007: 99,5% uptime | RNF-003: WCAG 2.1 AA ( `responsive.css:17` alvos 44px, `style.css:19` `focus-visible`, `prefers-reduced-motion`) | RNF-005: LGPD (`consentimento_lgpd`, `deleted_at`, `trilha_auditoria`) | RNF-015: Backup diário | RNF-014: `geo_ponto` ST_Distance_Sphere

## Equipe

Wallyson Barbosa, Lucas M. Carrijo, Tiago F. Dias, Mateus Xavier, Gabriel Krieger.

## Licença

Definir — ver `LICENSE`
