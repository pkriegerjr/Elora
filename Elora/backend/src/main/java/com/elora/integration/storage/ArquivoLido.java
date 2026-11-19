package com.elora.integration.storage;

import java.io.InputStream;

/**
 * Arquivo aberto para download. O {@code conteudo} é fechado por quem consome
 * (no controller, pelo InputStreamResource do Spring).
 */
public record ArquivoLido(InputStream conteudo, String nomeOriginal, String contentType, long tamanho) {
}
