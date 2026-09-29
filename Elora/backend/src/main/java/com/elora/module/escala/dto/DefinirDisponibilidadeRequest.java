package com.elora.module.escala.dto;

import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusDisponibilidade;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * POST /disponibilidade (upsert por usuario+data+periodo).
 * {@code profissionalId} ausente = próprio usuário autenticado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefinirDisponibilidadeRequest {

    private Integer profissionalId;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    @NotNull(message = "Período é obrigatório")
    private PeriodoTurno periodo;

    private StatusDisponibilidade status;
}
