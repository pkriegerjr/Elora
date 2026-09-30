# Elora — Como Rodar (Local e MOCK_MODE, Neon)

## 1. O que é MOCK_MODE

`frontend-web/src/config/config.js:21` `CONFIG.API.MOCK_MODE`

- `true` (padrão): front roda **sem backend**. Todos os services (`authService.js`, `caregiverService.js`, `contractService.js`, `paymentService.js`, `notificationService.js`, `relatorioService.js`, `avaliacao/conhecimento/escala/juridicoService.js`) usam `localStorage` (`mockData.js` com `cpfDigits`, `codigo ELO-...`, `periodos`) + delay simulado. Ideal para UI/WCAG.
- `false`: front consome **API real** `http://localhost:8080/api` (`application.properties:3` `server.servlet.context-path=/api`). Troque para `false` quando o backend com Neon estiver online. `apiService.js` desembala `ApiResponse` e tenta `POST /auth/refresh` em 401.

Troca: edite `src/config/config.js:21` → `MOCK_MODE: false` (frontend) e recarregue. Não precisa rebuild.

## 2. O que precisa para cada modo

| Modo | Precisa |
|---|---|
| **MOCK_MODE=true** | Só navegador + `npx serve` ou `python -m http.server`. Sem Java, sem Docker, sem banco. |
| **MOCK_MODE=false** | Java 17, Maven 3.9+, conta/projeto no Neon (PostgreSQL), `backend/.env` com `DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET` |

> Docker foi descontinuado. Não há `docker-compose.yml`. Não use MySQL/XAMPP/Redis — o backend só fala PostgreSQL (`pom.xml` sem `mysql-connector`, `application.properties` com `org.postgresql.Driver` + `ddl-auto=validate`).

## 3. Estrutura relevante

```
Elora/
├── frontend-web/index.html  # entry (fora de src, serve raiz)
├── frontend-web/src/config/config.js  # MOCK_MODE (:21), baseURL /api, ENDPOINTS, mappers V2
├── frontend-web/src/services/ # apiService, authService, client/caregiver, contract, payment, maps, notification, avaliacao, conhecimento, escala, juridico, relatorio
├── frontend-web/src/pages/auth|busca|contratos|financeiro|perfil|notificacoes|relatorios|juridico|escalas|avaliacoes|favoritos|conhecimento
├── backend/pom.xml (Spring Boot 3.2.5, Java 17), src/main/java/com/elora/EloraApplication.java
├── backend/src/main/resources/application.properties  # lê ${DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET}
├── backend/database/postgres/elora_schema_v2_pg.sql  # CANÔNICO Neon (+ _v2_3_pg + db/migration/*)
├── backend/database/elora_schema_v2*.sql  # legado MySQL, não usar para subir
├── backend/.env / .env.example  # DB_* Neon + JWT_* (gitignore)
└── infra/{docker,k8s,scripts}/  # reservados, vazios (sem compose)
```

## 4. Rodar com MOCK_MODE=true (só front)

```bash
cd Elora/frontend-web
npx serve . --listen 8000
# ou, da raiz:
python -m http.server --directory Elora/frontend-web 8000
# abre http://localhost:8000/index.html
```

Logins mock (`src/services/mockData.js`): `maria.santos@email.com / Senha@123` (cliente), `ana.ferreira@email.com / Senha@123` (profissional APPROVED), `admin / Admin@123` (admin), `juridico / Juridico@123`.

Teste: `src/pages/auth/cadastro-cliente.html` → `login.html` → `src/pages/perfil/perfil.html` (mostra `perfis[]` mock) → `notificacoes/lista.html` (polling 30s) → `relatorios/relatorios.html` (6 abas mock, sem 403).

## 5. Rodar com MOCK_MODE=false (stack completo, Neon)

### 5.1 Banco (Neon)

```bash
# Crie o projeto no Neon, anote host/db/user/pass (use a connection string DIRETA, não a do pooler, para JDBC).
psql "host=ep-xxx.us-east-2.aws.neon.tech dbname=neondb user=... password=... sslmode=require" \
  -f backend/database/postgres/elora_schema_v2_pg.sql
psql "$NEON_URL" -f backend/database/postgres/elora_schema_v2_3_pg.sql
psql "$NEON_URL" -f backend/src/main/resources/db/migration/conhecimento.sql
psql "$NEON_URL" -f backend/src/main/resources/db/migration/juridico.sql
psql "$NEON_URL" -f backend/src/main/resources/db/migration/profissional.sql
```

### 5.2 Backend

```bash
cd Elora/backend
cp .env.example .env  # preencha DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET
# Gere JWT: openssl rand -base64 48  (mínimo 32 chars)
cat .env  # confira DB_HOST do Neon (direto) e JWT_SECRET preenchido

mvn clean install  # BUILD SUCCESS (sem wrapper mvnw, use Maven instalado)
mvn spring-boot:run  # -> http://localhost:8080/api/swagger-ui.html
```

`SecurityConfig.java` libera `POST /api/auth/**`, `POST /api/clients`, `POST /api/caregivers` sem token; resto exige `Authorization: Bearer <accessToken>`.

### 5.3 Frontend (MOCK_MODE=false)

Edite `frontend-web/src/config/config.js:21` `MOCK_MODE: false` (baseURL já `/api`; com `npx serve` use proxy ou `http://localhost:8080/api`), recarregue `http://localhost:8000/index.html`.

Teste real:
```bash
POST /api/clients {nome,email,cpf:"11122233344",senha:"Senha@123",consentimentoLgpd:true}
POST /api/auth/login {identifier:"email ou 11122233344",password:"Senha@123"} -> {accessToken,refreshToken,user:{perfis:["cliente"]}}
GET  /api/auth/me  # Header Bearer
GET  /api/notifications?unreadOnly=true  # 200
GET  /api/relatorios/resumo  # 403 se perfil cliente, 200 se admin/financeiro (RelatorioService.java)
```

## 6. Troubleshooting

- `PSQL connection failed`: confira `DB_HOST` direto do Neon (sem `-pooler`), `sslmode=require`, senha com caracteres especiais entre aspas.
- `ddl-auto=validate` falhou: schema Neon desatualizado → reaplique `elora_schema_v2_pg.sql` + `_v2_3_pg` + `db/migration/*`.
- `Perfil não configurado`: seed de `perfil` não aplicado → reaplique o SQL canônico do Postgres (não o legado MySQL).
- `401 Não autenticado`: `JWT_SECRET` vazio/curto ou expirado → gere 48 bytes e tente `POST /api/auth/refresh {refreshToken}`.
- `CORS`: sirva o front via `npx serve` (não `file://`); backend permite `http://localhost:*`.

## 7. Voltar ao MOCK

Basta `CONFIG.API.MOCK_MODE = true` e recarregar — front volta ao `localStorage`, sem depender do Neon.
