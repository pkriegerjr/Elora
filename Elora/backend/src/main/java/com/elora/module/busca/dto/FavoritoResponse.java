package com.elora.module.busca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** MÓDULO BUSCA - Resposta do favorito (o front marca o "coração"). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritoResponse {

    private Integer clienteId;
    private Integer profissionalId;
    private LocalDateTime criadoEm;
}
