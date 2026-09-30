package com.elora.module.notificacao.dto;

import com.elora.module.notificacao.enums.Canal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Espelha a linha de {@code notificacao} (v2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoResponse {

    private Integer id;
    private Integer destinatarioId;
    private Canal canal;
    private String titulo;
    private String corpo;
    private String referenciaTipo;
    private Integer referenciaId;
    private Boolean lida;
    private LocalDateTime lidaEm;
    private LocalDateTime enviadoEm;
}
