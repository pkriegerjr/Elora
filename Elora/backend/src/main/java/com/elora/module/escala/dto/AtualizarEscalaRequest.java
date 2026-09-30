package com.elora.module.escala.dto;

import com.elora.module.escala.enums.StatusEscala;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO ESCALA - PATCH /escalas/{id}. Só transições da máquina de estados
 * ({@code prevista} → {@code executada|faltou|cancelada}); o service barra
 * o resto com 422.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarEscalaRequest {

    @NotNull(message = "status é obrigatório")
    private StatusEscala status;
}
