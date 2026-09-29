package com.elora.module.escala.dto;

import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusEscala;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** PUT /escalas/{id} — reagendar e/ou mudar status. Tudo opcional. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarEscalaRequest {

    private LocalDate data;

    private PeriodoTurno periodo;

    private StatusEscala status;
}
