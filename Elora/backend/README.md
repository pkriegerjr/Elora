# Elora — Como Rodar (Codespace, Local e MOCK_MODE)

## 1. O que é MOCK_MODE

`frontend-web/src/config/config.js:13` `CONFIG.API.MOCK_MODE`

- `true` (padrão): front roda **sem backend**. Todos os services (`authService.js`, `caregiverService.js`, `contractService.js`, `paymentService.js`, `notificationService.js`, `relatorioService.js`) usam `localStorage` (`mockData.js:551` com `cpfDigits`, `codigo ELO-...`, `periodos`) + delay simulado. Ideal para UI/WCAG.
- `false`: front consome **API real** `http://localhost:8080/api` (`application.properties:3` `server.servlet.context-path=/api`). Troque para `false` quando `infra/docker-compose.yml` + `mvn spring-boot:run` estiverem OK. `apiService.js:80` desembala `ApiResponse` e tenta `POST /auth/refresh` em 401.

Troca: edite `src/config/config.js:13` → `MOCK_MODE: false` (frontend) e recarregue. Não precisa rebuild.

## 2. O que precisa para cada modo

| Modo | Precisa |
|---|---|
| **MOCK_MODE=true** | Só navegador + `npx serve` ou `python -m http.server`. Sem Java, Docker ou MySQL. |
| **MOCK_MODE=false** | Java 17, Maven 3.9+, Docker Desktop (ou XAMPP MySQL), `backend/.env` com `JWT_SECRET` (64 chars, já gerado), `infra/docker-compose.yml` (MySQL 8.0 + Redis) |

## 3. Estrutura relevante

```
Elora/
├── frontend-web/index.html  # entry (fora de src, serve raiz)
├── frontend-web/src/config/config.js  # baseURL http://localhost:8080/api
├── frontend-web/src/services/ # apiService, authService, notificationService, relatorioService
├── frontend-web/src/pages/auth|busca|contratos|financeiro|perfil|notificacoes|relatorios
├── backend/pom.xml, src/main/java/com/elora/EloraApplication.java
├── backend/src/main/resources/application.properties  # lê ${DB_*} e ${JWT_SECRET}
├── backend/database/elora_schema_v2.sql  # DDL + seeds perfil (cliente/profissional/admin)
├── backend/.env / .env.example  # DB_USERNAME/DB_PASSWORD/JWT_SECRET (gitignore)
└── infra/docker-compose.yml  # MySQL:3306 + Redis:6379, volume ../backend/database/elora_schema_v2.sql
```

## 4. Rodar com MOCK_MODE=true (só front)

```powershell
Set-Location -LiteralPath "C:\Users\SONY VAIO\OneDrive\Documentos\vscode\Elora-testes\Elora\frontend-web"
npx serve . --listen 8000
# ou
python -m http.server 8000
# abre http://localhost:8000/index.html
```

Logins mock (`mockData.js:347`): `maria.santos@email.com / Senha@123` (cliente), `ana.ferreira@email.com / Senha@123` (profissional APPROVED), `admin / Admin@123`.

Teste: `src/pages/auth/cadastro-cliente.html` → `login.html` → `src/pages/perfil/perfil.html` (mostra `perfis[]` mock) → `notificacoes/lista.html` (polling 30s) → `relatorios/relatorios.html` (6 abas mock, sem 403).

## 5. Rodar com MOCK_MODE=false (stack completo)

### 5.1 Banco (Docker — recomendado)

```powershell
Set-Location -LiteralPath "C:\Users\SONY VAIO\OneDrive\Documentos\vscode\Elora-testes\Elora"
docker compose -f infra/docker-compose.yml up -d
docker ps  # elora_mysql :3306, elora_redis :6379
docker logs elora_mysql --tail 20  # deve criar elora_db
# sem Docker: use XAMPP Control → Start MySQL e:
# mysql -u root -e "CREATE DATABASE elora_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# mysql -u root elora_db < backend\database\elora_schema_v2.sql
```

Se Docker falhar `virtualisation support wasn’t detected`: habilite `VT-x/AMD SVM` na BIOS + `Hipervisor Windows` em Recursos do Windows, ou use XAMPP.

### 5.2 Backend

```powershell
Set-Location -LiteralPath "C:\Users\SONY VAIO\OneDrive\Documentos\vscode\Elora-testes\Elora\backend"
# .env já existe com JWT_SECRET 64 chars; se não:
# Copy-Item .env.example .env; gere JWT: [Convert]::ToBase64String((1..48 | % {Get-Random -Max 256}))
Get-Content .env  # confere DB_USERNAME=elora / DB_PASSWORD=elora123

# sem wrapper mvnw, use Maven instalado:
winget install Apache.Maven  # ou https://maven.apache.org
mvn clean install  # BUILD SUCCESS
mvn spring-boot:run  # -> http://localhost:8080/api/swagger-ui.html e /actuator/health
```

`SecurityConfig.java:45` libera `POST /api/auth/**`, `POST /api/clients`, `POST /api/caregivers` sem token; resto exige `Authorization: Bearer <accessToken>`.

### 5.3 Frontend (MOCK_MODE=false)

Edite `frontend-web/src/config/config.js:13` `MOCK_MODE: false` (baseURL já `http://localhost:8080/api`), recarregue `http://localhost:8000/index.html`.

Teste real:
```bash
POST /api/clients {nome,email,cpf:"11122233344",senha:"Senha@123",consentimentoLgpd:true}
POST /api/auth/login {identifier:"email ou 11122233344",password:"Senha@123"} -> {accessToken,refreshToken,user:{perfis:["cliente"]}}
GET  /api/auth/me  # Header Bearer
GET  /api/notifications?unreadOnly=true  # 200
GET  /api/relatorios/resumo  # 403 se perfil cliente, 200 se admin/financeiro (RelatorioService.java:255)
```

## 6. Troubleshooting

- `failed to connect to docker_engine`: abre Docker Desktop e espera “Engine running” ou usa XAMPP.
- `Perfil não configurado`: `elora_schema_v2.sql` não seedou `perfil` → `mysql -u root elora_db < database\elora_schema_v2.sql`.
- `401 Não autenticado`: `JWT_SECRET` vazio ou expirado → `POST /api/auth/refresh {refreshToken}`.
- `CORS`: `SecurityConfig.java:66` permite `http://localhost:*`; sirva front via `npx serve` (não `file://`).

## 7. Voltar ao MOCK

Basta `CONFIG.API.MOCK_MODE = true` e `docker compose -f infra/docker-compose.yml down` — front volta ao `localStorage`.
