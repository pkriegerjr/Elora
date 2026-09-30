package com.elora.module.contrato.enums;

/**
 * MÓDULO CONTRATO - Enum (status possíveis de um contrato).
 *
 * <p>Espelha o CHECK da coluna {@code contrato.status} no schema v2:</p>
 * <pre>
 * CHECK (status IN ('rascunho','proposta','negociacao','aguard_assinatura',
 *                   'ativo','concluido','rescindido','cancelado','em_disputa'))
 * </pre>
 * <p>Os nomes são minúsculos de propósito: o banco compara texto exato, e os
 * outros enums do projeto ({@code Canal}, {@code StatusVerificacao}) seguem
 * o mesmo estilo. É salvo como STRING ({@code @Enumerated(EnumType.STRING)}),
 * nunca como número — assim a ordem do enum pode mudar sem quebrar o banco.</p>
 *
 * <p>Fluxo normal: rascunho → proposta → negociacao → aguard_assinatura →
 * ativo → concluido. Os desvios (cancelado, rescindido, em_disputa) são
 * controlados no service (mapa de transições permitidas).</p>
 */
public enum StatusContrato {

    // Contrato recém-criado, ainda em edição pelo cliente.
    rascunho,

    // Enviado ao profissional (primeira oferta, ainda sem contraproposta).
    proposta,

    // Cliente e profissional estão negociando valores/datas.
    negociacao,

    // Termos aceitos, faltando a assinatura digital das partes.
    aguard_assinatura,

    // Assinado e em execução (serviço acontecendo).
    ativo,

    // Serviço finalizado normalmente.
    concluido,

    // Encerrado antes do fim (com mediação/painel jurídico).
    rescindido,

    // Desistência antes de virar ativo.
    cancelado,

    // Contrato em disputa (vai para o módulo jurídico analisar).
    em_disputa
}
