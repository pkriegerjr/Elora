package com.elora.module.config.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.config.dto.MapsKeyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega a chave do Google Maps JavaScript API para o front
 * (CONFIG.GOOGLE_MAPS.configEndpoint em frontend-web/src/config/config.js).
 *
 * A chave NÃO é segredo — ela vai no HTML de qualquer forma. O que importa:
 *  1. não deixá-la hardcoded no repositório;
 *  2. restringi-la por HTTP referrer no Google Cloud Console;
 *  3. exigir autenticação (feito pelo anyRequest().authenticated() do SecurityConfig).
 */
@RestController
@RequestMapping("/config")
public class MapsKeyController {

    @Value("${app.integracoes.maps.api-key:}")
    private String apiKey = "";

    @GetMapping("/maps-key")
    public ApiResponse<MapsKeyResponse> mapsKey() {
        boolean configurada = apiKey != null && !apiKey.isBlank();
        return configurada
                ? ApiResponse.ok(new MapsKeyResponse(apiKey, true))
                : ApiResponse.ok("Google Maps nao configurado",
                        new MapsKeyResponse(null, false));
    }
}
