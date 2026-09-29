package com.elora.module.contrato.dto;

import com.elora.module.contrato.enums.StatusContrato;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PATCH /contracts/{id}/status — transição validada no service. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusRequest {

    @NotNull(message = "Status é obrigatório")
    private StatusContrato status;
}
