package com.elora.module.notificacao.dto;

import com.elora.module.notificacao.enums.Canal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /notifications. {@code destinatarioId} é validado no service
 * (no /bulk ele é ignorado — vale o userId de cada item).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarNotificacaoRequest {

    private Integer destinatarioId;

    private Canal canal;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150)
    private String titulo;

    @NotBlank(message = "Corpo é obrigatório")
    private String corpo;

    @Size(max = 50)
    private String referenciaTipo;

    private Integer referenciaId;
}
