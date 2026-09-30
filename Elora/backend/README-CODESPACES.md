# Elora Backend no Codespace — Guia rápido (Neon)

> Docker foi descontinuado. O banco é PostgreSQL (Neon). Detalhes completos em `backend/README.md`.

## 1. Pré-requisitos

- Java 17 + Maven 3.9+ (`mvn -v`)
- Projeto/DB no Neon (connection string **direta**, sem `-pooler`, para JDBC)
- Schema canônico aplicado:
  `backend/database/postgres/elora_schema_v2_pg.sql` + `_v2_3_pg` + `src/main/resources/db/migration/{conhecimento,juridico,profissional}.sql`

## 2. Configurar

```bash
cd /workspaces/Elora/Elora/backend
cp .env.example .env
# edite .env: DB_HOST (Neon direto), DB_NAME, DB_USERNAME, DB_PASSWORD
# JWT_SECRET (min 32 chars): openssl rand -base64 48
```

Ou exporte no ambiente do Codespace (Secrets):
`DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET`.

## 3. Rodar

```bash
cd /workspaces/Elora/Elora/backend
mvn clean install   # BUILD SUCCESS
mvn spring-boot:run # sobe em http://localhost:8080/api
# Docs: http://localhost:8080/api/swagger-ui.html
```

## 4. Front

`frontend-web/src/config/config.js:21` → `MOCK_MODE: false` só com o backend acima online. Caso contrário mantenha `true` (só `npx serve Elora/frontend-web`).
