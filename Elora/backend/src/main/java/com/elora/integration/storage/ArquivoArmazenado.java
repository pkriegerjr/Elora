package com.elora.integration.storage;

/**
 * Resultado de um upload. {@code chave} é o identificador opaco no provedor —
 * é o que vai para documento_profissional.arquivo_url (VARCHAR(500)).
 */
public record ArquivoArmazenado(String chave, String nomeOriginal, String contentType, long tamanho) {
}
