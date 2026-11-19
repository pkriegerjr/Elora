package com.elora.integration.storage;

import java.util.Locale;
import java.util.UUID;

/**
 * Formato da chave: {pasta}/{uuid8}-{nome-sanitizado}
 * Ex.: documentos/3f9a1c02-rg-frente.pdf
 *
 * O uuid8 garante unicidade sem apagar o nome original, e permite
 * reconstruir o nome na hora do download (não há coluna de nome no banco).
 */
final class ChaveStorage {

    private ChaveStorage() {
    }

    static String gerar(String pasta, String nomeOriginal) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return pasta + "/" + uuid + "-" + sanitizar(nomeOriginal);
    }

    static String nomeOriginal(String chave) {
        String base = chave.substring(chave.lastIndexOf('/') + 1);
        int dash = base.indexOf('-');
        return dash == 8 ? base.substring(9) : base; // 8 chars do uuid + '-'
    }

    static String extensao(String nome) {
        if (nome == null) return "";
        int i = nome.lastIndexOf('.');
        return i < 0 || i == nome.length() - 1 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    static String contentType(String chave) {
        return switch (extensao(chave)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String sanitizar(String nomeOriginal) {
        String nome = nomeOriginal == null ? "" : nomeOriginal;
        nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
        nome = nome.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^\\.+", "");
        if (nome.isBlank()) nome = "arquivo";
        if (nome.length() > 80) {
            String ext = extensao(nome);
            nome = nome.substring(0, 76) + (ext.isEmpty() ? "" : "." + ext);
        }
        return nome;
    }
}
