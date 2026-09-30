package com.elora.module.escala.enums;

import java.util.Map;
import java.util.Set;

/**
 * MÓDULO ESCALA - Situação de um turno na escala (REQ-ELO-013).
 *
 * <p>Espelha o CHECK de {@code escala_trabalho.status}. Transições válidas
 * (máquina de estados, ver {@link #podeMudarPara(StatusEscala)}): tudo nasce
 * {@code prevista}; estados finais ({@code executada}, {@code faltou},
 * {@code cancelada}) são imutáveis — correção se faz gerando nova escala,
 * nunca reescrevendo histórico.</p>
 */
public enum StatusEscala {
    prevista,
    executada,
    faltou,
    cancelada;

    private static final Map<StatusEscala, Set<StatusEscala>> TRANSICOES = Map.of(
            prevista, Set.of(executada, faltou, cancelada),
            executada, Set.of(),
            faltou, Set.of(),
            cancelada, Set.of());

    public boolean podeMudarPara(StatusEscala destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
