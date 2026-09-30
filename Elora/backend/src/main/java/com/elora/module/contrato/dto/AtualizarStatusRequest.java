package com.elora.module.contrato.dto;

import com.elora.module.contrato.enums.StatusContrato;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** MÓDULO CONTRATO - DTO do PATCH /contratos/{id}/status. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusRequest {

    @NotNull(message = "status é obrigatório")
    private StatusContrato status;
}
