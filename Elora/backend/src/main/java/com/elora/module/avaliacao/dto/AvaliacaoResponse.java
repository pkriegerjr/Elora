package com.elora.module.avaliacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MÓDULO AVALIACAO - DTO de uma avaliação (item da lista).
 *
 * <p>Só IDs + nota + comentário + data (sem CPF/senha/e-mail — nada sensível).
 * O campo chama {@code cuidadorId} (nome do escopo) em vez de avaliadoId.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoResponse {

    private Integer id;
    private Integer contratoId;
    private Integer avaliadorId;
    private Integer cuidadorId;
    private Integer nota;
    private String comentario;
    private LocalDateTime criadoEm;
}
