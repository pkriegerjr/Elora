package com.elora.integration.storage;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;

/**
 * produção: S3 (ou qualquer S3-compatível com endpoint customizado).
 * Credenciais: se S3_ACCESS_KEY/S3_SECRET_KEY vierem vazias, usa a cadeia
 * padrão do SDK (env, profile, IAM role) — o correto em EC2/ECS/EKS.
 */
@Service
@ConditionalOnProperty(name = "app.integracoes.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(
            @Value("${app.integracoes.storage.s3.bucket:}") String bucket,
            @Value("${app.integracoes.storage.s3.region:us-east-1}") String region,
            @Value("${app.integracoes.storage.s3.endpoint:}") String endpoint,
            @Value("${app.integracoes.storage.s3.access-key:}") String accessKey,
            @Value("${app.integracoes.storage.s3.secret-key:}") String secretKey,
            @Value("${app.integracoes.storage.s3.path-style:true}") boolean pathStyle) {

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(
                    "S3_BUCKET nao configurado: use STORAGE_PROVIDER=local ou defina o bucket");
        }

        var builder = S3Client.builder()
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        if (pathStyle) {
            builder.forcePathStyle(true);
        }

        this.s3 = builder.build();
        this.bucket = bucket;
        log.info("Storage S3 bucket={} region={} endpoint={}",
                bucket, region, endpoint == null || endpoint.isBlank() ? "aws" : endpoint);
    }

    @Override
    public ArquivoArmazenado salvar(InputStream conteudo, long tamanho, String nomeOriginal, String contentType) {
        String chave = ChaveStorage.gerar("documentos", nomeOriginal);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(chave)
                            .contentType(contentType)
                            .contentLength(tamanho)
                            .build(),
                    RequestBody.fromInputStream(conteudo, tamanho));
            return new ArquivoArmazenado(chave, ChaveStorage.nomeOriginal(chave), contentType, tamanho);
        } catch (S3Exception e) {
            log.error("Falha ao enviar {} para o S3: {}", chave, e.awsErrorDetails() == null
                    ? e.getMessage() : e.awsErrorDetails().errorMessage());
            throw new BusinessException("Falha ao enviar o arquivo para o storage");
        }
    }

    @Override
    public ArquivoLido ler(String chave) {
        try {
            ResponseInputStream<GetObjectResponse> stream = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(chave).build());
            GetObjectResponse meta = stream.response();
            long tamanho = meta.contentLength() == null ? -1 : meta.contentLength();
            String contentType = (meta.contentType() == null || meta.contentType().isBlank())
                    ? ChaveStorage.contentType(chave)
                    : meta.contentType();
            return new ArquivoLido(stream, ChaveStorage.nomeOriginal(chave), contentType, tamanho);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Arquivo do documento nao encontrado");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Arquivo do documento nao encontrado");
            }
            log.error("Falha ao ler {} do S3: {}", chave, e.getMessage());
            throw new BusinessException("Falha ao ler o arquivo do storage");
        }
    }

    @Override
    public void remover(String chave) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(chave));
        } catch (S3Exception e) {
            log.warn("Nao foi possivel remover {} do S3: {}", chave, e.getMessage());
        }
    }
}
