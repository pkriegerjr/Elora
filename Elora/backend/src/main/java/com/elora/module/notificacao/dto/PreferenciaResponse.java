package com.elora.module.notificacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaResponse {

    private Boolean push;
    private Boolean email;
    private Boolean sms;
    private LocalDateTime atualizadoEm;
}
