package com.elora.module.escala.dto;

import com.elora.module.escala.enums.Periodo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * MÓDULO ESCALA - POST /escalas/gerar. Gera um turno por dia do intervalo
 * para cada período pedido (idempotente: o que já existe é pulado).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GerarEscalaRequest {

    @NotNull(message = "contratoId é obrigatório")
    private Integer contratoId;

    @NotNull(message = "dataInicio é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @NotNull(message = "dataFim é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @NotNull(message = "periodos é obrigatório")
    @Size(min = 1, max = 3, message = "informe de 1 a 3 períodos")
    private List<Periodo> periodos;
}
