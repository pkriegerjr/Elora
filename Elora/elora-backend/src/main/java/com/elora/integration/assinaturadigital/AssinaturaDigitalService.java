package com.elora.integration.assinaturadigital;

import org.springframework.stereotype.Service;

@Service
public class AssinaturaDigitalService {
    public String solicitarAssinatura(Long contratoId){ return "hash-assinatura"; }
    public boolean validarAssinatura(String hash){ return true; }
}
