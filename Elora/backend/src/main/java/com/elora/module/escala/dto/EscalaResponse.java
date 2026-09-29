package com.elora.module.escala.dto;

import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusEscala;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Espelha a linha de {@code escala_trabalho} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaResponse {

    private Integer id;
    private Integer contratoId;
    private LocalDate data;
    private PeriodoTurno periodo;
    private StatusEscala status;
}
