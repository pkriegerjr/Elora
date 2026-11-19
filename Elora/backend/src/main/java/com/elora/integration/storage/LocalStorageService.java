package com.elora.integration.storage;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Default (dev/Codespaces): grava em disco sob app.integracoes.storage.local.dir.
 * Não serve para produção com múltiplas instâncias — use provider=s3.
 */
@Service
@ConditionalOnProperty(name = "app.integracoes.storage.provider",
        havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path raiz;

    public LocalStorageService(@Value("${app.integracoes.storage.local.dir:./uploads}") String dir) {
        this.raiz = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel criar o diretorio de uploads: " + raiz, e);
        }
        log.info("Storage local de documentos em {}", raiz);
    }

    @Override
    public ArquivoArmazenado salvar(InputStream conteudo, long tamanho, String nomeOriginal, String contentType) {
        String chave = ChaveStorage.gerar("documentos", nomeOriginal);
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
            return new ArquivoArmazenado(chave, ChaveStorage.nomeOriginal(chave),
                    contentType, Files.size(destino));
        } catch (IOException e) {
            throw new BusinessException("Falha ao gravar o arquivo enviado");
        }
    }

    @Override
    public ArquivoLido ler(String chave) {
        Path arquivo = resolver(chave);
        if (!Files.isRegularFile(arquivo)) {
            throw new ResourceNotFoundException("Arquivo do documento nao encontrado");
        }
        try {
            return new ArquivoLido(Files.newInputStream(arquivo), ChaveStorage.nomeOriginal(chave),
                    ChaveStorage.contentType(chave), Files.size(arquivo));
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler o arquivo do documento");
        }
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (Exception e) {
            log.warn("Nao foi possivel remover {} do storage local: {}", chave, e.toString());
        }
    }

    /** Impede path traversal: a chave resolvida tem de continuar dentro de raiz. */
    private Path resolver(String chave) {
        Path caminho = raiz.resolve(chave).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new BusinessException("Chave de arquivo invalida");
        }
        return caminho;
    }
}
