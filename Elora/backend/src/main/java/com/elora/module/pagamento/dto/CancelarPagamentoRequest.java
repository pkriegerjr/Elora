package com.elora.module.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelarPagamentoRequest {

    /** O front envia {@code { reason: "..." }}. */
    @JsonAlias("reason")
    private String motivo;
}