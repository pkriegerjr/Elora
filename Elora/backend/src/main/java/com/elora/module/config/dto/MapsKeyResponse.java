package com.elora.module.config.dto;

/**
 * GET /config/maps-key.
 * {@code configured=false} permite ao front degradar para o mapa simulado
 * em vez de carregar um script do Google com chave vazia (que gera erro 400
 * no console e mapa cinza).
 */
public record MapsKeyResponse(String apiKey, boolean configured) {
}
