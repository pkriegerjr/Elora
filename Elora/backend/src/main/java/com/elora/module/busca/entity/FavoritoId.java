package com.elora.module.busca.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** PK composta de {@code favorito} (cliente_id, profissional_id). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritoId implements Serializable {

    private Integer clienteId;
    private Integer profissionalId;
}
