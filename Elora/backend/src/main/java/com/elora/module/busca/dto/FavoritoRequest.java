package com.elora.module.busca.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO BUSCA - POST /busca/favoritos. Quem favorita é o logado (JWT);
 * o front informa só QUEM favoritar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritoRequest {

    @NotNull(message = "profissionalId é obrigatório")
    private Integer profissionalId;
}
