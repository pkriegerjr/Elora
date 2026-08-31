# Elora - Sistema de Cuidadores de Idosos

> Plataforma para conectar pessoas com deficiência (PcD) e seus familiares a cuidadores especializados de forma rápida, prática e eficiente.

Documento de Arquitetura v3.0 — Baseado no modelo 4+1 [KRU95]. Foco em acessibilidade (WCAG), LGPD, disponibilidade 99,5% e resposta de busca ≤ 2s.

## Stack

| Camada | Tecnologia |
| :--- | :--- |
| **Apresentação Web** | React.js |
| **Apresentação Mobile** | React Native (Android / iOS) |
| **Backend API** | NestJS (REST + JWT + RBAC + MFA) |
| **Banco Principal** | PostgreSQL (utf8mb4) |
| **Cache / Sessão** | Redis |
| **Storage** | AWS S3 (fotos, documentos, evidências) |
| **Tempo Real** | Firebase FCM (push) |
| **Integrações** | Google Maps SDK, Gateway de Pagamento, Validador de Documentos |

## Arquitetura em Camadas

```
View (React) -> API Gateway (NestJS) -> Serviços (Regras de Negócio) -> Persistência (PostgreSQL/Redis) -> Integrações Externas
```

10 Subsistemas previstos (Documento p.26-34):
`Cadastro e Acesso`, `Cadastro de Profissionais`, `Busca de Profissionais`, `Contratos e Assinatura Digital`, `Financeiro`, `Escalas de Trabalho`, `Jurídico e Denúncias`, `Notificações e Comunicação`, `Relatórios`, `FAQ e Base de Conhecimento`.

## Estrutura de Pastas

```
/elora
├── /frontend-web      # React.js
│   └── src/
│       ├── /components
│       ├── /pages
│       ├── /services
│       ├── /hooks
│       └── /utils
├── /mobile-app        # React Native
│   └── src/
│       ├── /components
│       ├── /screens
│       ├── /services
│       ├── /hooks
│       └── /utils
├── /backend           # NestJS
│   └── src/
│       ├── /common        # JWT, RBAC, Guards
│       ├── /modules       # 10 subsistemas (controller/service/repository)
│       ├── /integrations  # maps, payment, fcm, validation
│       ├── /config
│       └── /database      # migrations / seeds
├── /docs              # Diagramas UML, DER, DAS
├── /infra             # Docker + Kubernetes (auto-scaling)
└── /.github/workflows
```

> Pastas vazias contêm `.gitkeep` — Git não rastreia diretório vazio.

## Banco de Dados - Planejado (v1)

> 🚧 Projeto em desenvolvimento — nenhum módulo codado ainda. Schema `elora_schema_v1.sql` define a base.

* `usuario_interno` (admin, moderador, jurídico) + `moderador_regiao`, `juridico_detalhes`
* `usuario_externo` (contratante, contratado) + `contratante_detalhes`, `contratado_detalhes`
* `avaliacao` (nota 1-5)

Login híbrido `senha` / `google` com `senha_hash` (bcrypt/Argon2) e `google_id` único, com geolocalização (`latitude`, `longitude`) para busca por proximidade.

**Previsto para v2:** `contrato`, `pagamento/repasse`, `denuncia/disputa`, `notificacao/mensagem`, `documento/certificacao`, `auditoria_log` (RNF-ELO-012), `perfil/permissao` (RBAC dinâmico).

## Como Rodar (Previsto - projeto ainda em progresso)

> 🚧 Nenhum código implementado ainda. Passos abaixo são o setup planejado para Codespace / Local.

```bash
# 1. Clonar
git clone https://github.com/seu-usuario/elora.git
cd elora

# 2. Banco (Docker)
docker-compose up -d postgres redis

# 3. Importar schema v1
mysql -u root -p < elora_schema_v1.sql
# ou psql se migrar para PostgreSQL: psql -U postgres -f elora_schema_v1.sql

# 4. Backend
cd backend
npm install
npm run start:dev # http://localhost:3000/api

# 5. Frontend Web
cd ../frontend-web
npm install
npm run dev # http://localhost:5173

# 6. Mobile
cd ../mobile-app
npm install
npm run start # Expo
```

Variáveis em `.env` (não commitar):
```
DATABASE_URL=postgresql://user:pass@localhost:5432/elora_db
REDIS_URL=redis://localhost:6379
JWT_SECRET=
GOOGLE_CLIENT_ID=
MAPS_API_KEY=
```

## Funcionalidades Planejadas

> 🚧 Em progresso — nenhuma funcionalidade finalizada. Baseado no Documento de Arquitetura v3.0.

| Módulo | Funcionamento Previsto | Status |
| :--- | :--- | :--- |
| **Cadastro e Acesso** | Cadastro/login com senha + Google, JWT, RBAC por perfil, MFA para jurídico/financeiro, recuperação de senha | 🚧 Em progresso |
| **Cadastro de Profissionais** | Cadastro de cuidador, envio de documentos/certificações, validação por moderador/jurídico | 🚧 Em progresso |
| **Busca de Profissionais** | Filtros por especialidade/gênero/avaliação/disponibilidade + geolocalização (Google Maps) + favoritos | 🚧 Em progresso |
| **Contratos e Assinatura** | Proposta, negociação, geração de contrato e assinatura digital | 🚧 Em progresso |
| **Financeiro** | Pagamento via gateway, cálculo de repasse/taxa, painel financeiro | 🚧 Em progresso |
| **Escalas de Trabalho** | Gestão de turnos, disponibilidade e alertas de escala | 🚧 Em progresso |
| **Jurídico e Denúncias** | Denúncias com evidências, disputas, rescisões e análise jurídica | 🚧 Em progresso |
| **Notificações e Comunicação** | Push (FCM), e-mail, SMS, chat e preferências de notificação | 🚧 Em progresso |
| **Relatórios** | Relatórios administrativos, financeiros e jurídicos com exportação | 🚧 Em progresso |
| **FAQ e Base de Conhecimento** | FAQ, tutoriais e artigos de ajuda | 🚧 Em progresso |

## Requisitos Não Funcionais Principais

* RNF-009: Busca ≤ 2s | RNF-007: 99,5% uptime | RNF-003: WCAG 2.1 | RNF-005: LGPD | RNF-015: Backup diário

## Equipe

Wallyson Barbosa, Lucas M. Carrijo, Tiago F. Dias, Mateus Xavier, Otávio Augusto

## Licença

Definir — ver `LICENSE`
