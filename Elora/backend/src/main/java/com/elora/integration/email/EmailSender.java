package com.elora.integration.email;

/**
 * Fronteira de e-mail transacional (RNF-014).
 *
 * Implementações:
 *  - LogEmailSender   → app.integracoes.email.enabled=false (default, dev/teste)
 *  - SmtpEmailSender  → app.integracoes.email.enabled=true  (SMTP real)
 *
 * Contrato: quem chama NUNCA deve quebrar a requisição por causa de e-mail.
 * As implementações podem falhar; o try/catch é responsabilidade do chamador
 * (ou o Spring Boot trata como erro de infraestrutura).
 */
public interface EmailSender {

    void enviar(String destinatario, String assunto, String corpo);
}
