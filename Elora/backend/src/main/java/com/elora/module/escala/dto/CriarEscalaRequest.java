package com.elora.module.escala.dto;

import com.elora.module.escala.enums.PeriodoTurno;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** POST /escalas (REQ-013). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarEscalaRequest {

    @NotNull(message = "Contrato é obrigatório")
    private Integer contratoId;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    @NotNull(message = "Período é obrigatório")
    private PeriodoTurno periodo;
}
