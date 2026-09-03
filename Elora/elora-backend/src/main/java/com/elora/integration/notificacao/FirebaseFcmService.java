package com.elora.integration.notificacao;

import org.springframework.stereotype.Service;

@Service
public class FirebaseFcmService {
    public void enviarPush(String token, String titulo, String mensagem){
        // Firebase Cloud Messaging (RNF-ELO-016)
    }
    public void enviarEmail(String email, String assunto, String corpo){}
    public void enviarSms(String telefone, String mensagem){}
}
