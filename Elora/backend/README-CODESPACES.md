# Onde colocar cada arquivo no Codespace

Seu Codespace roda em: /workspaces/Elora/Elora/backend/
Sua pasta compartilhada local: C:\Users\gabri\OneDrive\Documentos\Default Project\Elora\backend\

Copie assim (arraste ou git add):

1. pom.xml
   LOCAL:  Elora/backend/pom.xml
   CODESPACE: /workspaces/Elora/Elora/backend/pom.xml  <-- é aqui que o `mvn clean install` procura

2. EloraApplication.java
   LOCAL:  Elora/backend/src/main/java/com/elora/EloraApplication.java
   CODESPACE: /workspaces/Elora/Elora/backend/src/main/java/com/elora/EloraApplication.java

3. application.properties
   LOCAL:  Elora/backend/src/main/resources/application.properties
   CODESPACE: /workspaces/Elora/Elora/backend/src/main/resources/application.properties

4. docker-compose.yml (opcional, para subir MySQL+Redis)
   LOCAL:  Elora/backend/docker-compose.yml
   CODESPACE: /workspaces/Elora/Elora/backend/docker-compose.yml

5. .gitignore
   LOCAL:  Elora/backend/.gitignore
   CODESPACE: /workspaces/Elora/Elora/backend/.gitignore

Após copiar, no terminal do Codespace:
  cd /workspaces/Elora/Elora/backend
  mvn clean install   # deve ficar BUILD SUCCESS
  mvn spring-boot:run # sobe em http://localhost:8080/api

O que estava faltando para codar (análise completa):
- pom.xml inexistente (sem dependências jakarta.persistence não resolve)
- Classe main @SpringBootApplication inexistente
- application.properties inexistente (sem datasource o Spring não sobe)
- docker-compose.yml inexistente (sem banco local elora_schema_v2.sql não carrega)

Nenhum módulo Java estava codado (só .gitkeep), mas com esse scaffold você já consegue criar qualquer módulo em:
  src/main/java/com/elora/module/<nome>/entity|repository|service|controller
