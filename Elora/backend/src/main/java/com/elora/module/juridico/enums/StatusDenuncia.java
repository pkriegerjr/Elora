package com.elora.module.juridico.enums;

/**
 * MÓDULO JURÍDICO - Status da denúncia.
 *
 * <p>Espelha o CHECK da coluna {@code denuncia.status} no schema v2
 * ({@code aberta, em_analise, resolvida, arquivada}).</p>
 */
public enum StatusDenuncia {
    aberta,
    em_analise,
    resolvida,
    arquivada
}
