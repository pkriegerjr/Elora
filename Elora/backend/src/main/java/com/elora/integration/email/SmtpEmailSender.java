package com.elora.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * E-mail real via SMTP (spring.mail.*).
 *
 * O bean JavaMailSender só existe porque spring.mail.host está definido no
 * application.properties (mesmo vazio). Com EMAIL_ENABLED=true e MAIL_HOST
 * vazio, a falha acontece no envio — e o chamador só loga o erro.
 */
@Service
@ConditionalOnProperty(name = "app.integracoes.email.enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.integracoes.email.remetente:nao-responda@elora.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        log.info("E-mail SMTP ativo, remetente={}", remetente);
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject(assunto);
        msg.setText(corpo);
        mailSender.send(msg);
        log.info("E-mail enviado para {}", destinatario);
    }
}
