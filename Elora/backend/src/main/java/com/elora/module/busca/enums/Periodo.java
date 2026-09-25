package com.elora.module.busca.enums;

/**
 * MÓDULO BUSCA - Enum do turno do dia.
 *
 * <p>Espelha o CHECK da coluna {@code disponibilidade.periodo} (e também de
 * {@code escala_trabalho.periodo}): matutino, vespertino ou noturno.
 * Nomes minúsculos para bater exatamente com o texto guardado no banco.</p>
 */
public enum Periodo {

    // Manhã.
    matutino,

    // Tarde.
    vespertino,

    // Noite.
    noturno
}
