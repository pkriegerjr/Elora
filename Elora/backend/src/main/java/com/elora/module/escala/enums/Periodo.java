package com.elora.module.escala.enums;

/**
 * MÓDULO ESCALA - Turno do dia na escala de trabalho.
 *
 * <p>Espelha o CHECK de {@code escala_trabalho.periodo} (e de
 * {@code disponibilidade.periodo}): matutino, vespertino ou noturno.
 * Enum próprio (não reutiliza o do busca): padrão do projeto — cada
 * módulo é dono do seu vocabulário (igual {@code profissional.enums}).</p>
 */
public enum Periodo {
    matutino,
    vespertino,
    noturno
}
