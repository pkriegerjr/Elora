package com.elora.integration.storage;

import java.io.InputStream;

/**
 * Fronteira de armazenamento de arquivos (RNF-014) — mesma ideia do
 * GatewayPagamentoService: trocar de provedor é criar outra implementação.
 *
 * Implementações:
 *  - LocalStorageService  → app.integracoes.storage.provider=local (default)
 *  - S3StorageService     → app.integracoes.storage.provider=s3
 *
 * A {@code chave} é opaca: nunca monte caminho de disco/URL a partir dela.
 */
public interface StorageService {

    ArquivoArmazenado salvar(InputStream conteudo, long tamanho, String nomeOriginal, String contentType);

    ArquivoLido ler(String chave);

    void remover(String chave);
}
