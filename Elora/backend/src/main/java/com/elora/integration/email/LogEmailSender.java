package com.elora.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default (dev/Codespaces/testes): não abre conexão nenhuma, só imprime.
 * Serve para ver o link de reset e o conteúdo das notificações sem SMTP.
 *
 * ATENÇÃO: o corpo do e-mail de reset contém o token — este bean só deve
 * ficar ativo com EMAIL_ENABLED=false (nunca em produção).
 */
@Service
@ConditionalOnProperty(name = "app.integracoes.email.enabled",
        havingValue = "false", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        log.info("[EMAIL:log] para={} assunto={}\n{}", destinatario, assunto, corpo);
    }
}
