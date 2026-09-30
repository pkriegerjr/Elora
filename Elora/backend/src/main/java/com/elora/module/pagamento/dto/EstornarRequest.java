package com.elora.module.pagamento.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /payments/{id}/estornar. {@code motivo} reservado ao fluxo de
 * solicitação de estorno (UC estorno) — ainda sem coluna própria.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstornarRequest {

    @Size(max = 500)
    private String motivo;
}
