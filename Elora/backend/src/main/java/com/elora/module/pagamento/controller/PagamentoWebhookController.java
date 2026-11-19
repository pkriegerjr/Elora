package com.elora.module.pagamento.controller;

import com.elora.module.pagamento.service.PagamentoService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Receptor de webhooks do gateway (rota liberada no SecurityConfig). */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PagamentoWebhookController {

    private final PagamentoService pagamentos;

    @PostMapping("/webhook")
    public Map<String, Object> webhook(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody JsonNode body) {
        boolean processado = pagamentos.processarWebhook(token, body);
        return Map.of("received", true, "processed", processado);
    }
}