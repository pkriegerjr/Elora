package com.elora.module.escala.dto;

import com.elora.module.escala.enums.Periodo;
import com.elora.module.escala.enums.StatusEscala;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** MÓDULO ESCALA - Um turno da agenda (leitura). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaResponse {

    private Integer id;
    private Integer contratoId;
    private LocalDate data;
    private Periodo periodo;
    private StatusEscala status;
}
