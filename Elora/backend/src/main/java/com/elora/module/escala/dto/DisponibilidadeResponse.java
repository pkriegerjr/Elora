package com.elora.module.escala.dto;

import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusDisponibilidade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Espelha a linha de {@code disponibilidade} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeResponse {

    private Integer id;
    private Integer usuarioId;
    private LocalDate data;
    private PeriodoTurno periodo;
    private StatusDisponibilidade status;
    private LocalDateTime criadoEm;
}
