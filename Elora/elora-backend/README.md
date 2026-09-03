# ELORA - Plataforma de Cuidadores v3.0
## Arquitetura N-Tier + Spring Boot

### Como rodar
\\\ash
docker-compose up --build
# ou
mvn spring-boot:run -Dspring-boot.run.profiles=dev
\\\

### Swagger
http://localhost:8080/api/swagger-ui.html

### Estrutura
- \com.elora.module.usuario\ - Cadastro e Acesso (Anexo 5)
- \com.elora.module.profissional\ - Cadastro de Profissionais (Anexo 6)
- \com.elora.module.busca\ - Busca com Geolocalização + Redis Cache (Anexo 8)
- \com.elora.module.contrato\ - Contratos e Assinatura Digital (Anexo 9)
- \com.elora.module.financeiro\ - Pagamentos, Repasses, Taxas (Anexo 11)
- \com.elora.module.escala\ - Escalas e Disponibilidade (Anexo 10)
- \com.elora.module.juridico\ + \valiacao\ - Jurídico e Denúncias (Anexo 20-21)
- \com.elora.module.notificacao\ - Notificações FCM/Email/SMS (Anexo 22)
- \com.elora.module.conhecimento\ - FAQ/Tutoriais/Artigos (Anexo 23)
- \com.elora.module.relatorio\ - Relatórios (Anexo 24)
- \com.elora.integration\ - Integrações Externas (Anexo 29)

### Requisitos atendidos
RNF-ELO-004 AES-256/TLS, RNF-ELO-005 LGPD, RNF-ELO-006 MFA, RNF-ELO-009 ≤2s cache Redis, RNF-ELO-016 FCM, etc.

### Grupo
Lucas Molina Carrijo, Tiago Felipe, Wallyson Barbosa, Mateus Xavier, Otávio Augusto
