package com.elora.integration.validacaodocumento;

import org.springframework.stereotype.Service;

@Service
public class ValidadorDocumentoService {
    public boolean validarCPF(String cpf){ return true; }
    public boolean validarCertidao(String doc){ return true; }
}
