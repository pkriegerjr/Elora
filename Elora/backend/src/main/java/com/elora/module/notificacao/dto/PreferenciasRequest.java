package com.elora.module.notificacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PATCH /notifications/preferences — parcial: só campos não-nulos são aplicados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasRequest {

    private Boolean push;
    private Boolean email;
    private Boolean sms;
}
