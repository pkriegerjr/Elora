package com.elora.module.notificacao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PATCH /notifications/{id} — marca como lida ou recoloca como não lida. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarcarLidaRequest {

    @NotNull(message = "lida é obrigatório")
    private Boolean lida;
}
