package com.elora.module.avaliacao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO AVALIACAO - DTO do POST /avaliacoes.
 *
 * <p>O front manda SÓ contrato + nota + comentário. O avaliador é quem está
 * logado (JWT) e o avaliado é o profissional do contrato — isso impede
 * fraude e é exigido pelo trigger do banco. Validação Bean Validation
 * (padrão do projeto): @Valid no controller devolve 400 sozinho.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarAvaliacaoRequest {

    @NotNull(message = "contratoId é obrigatório")
    private Integer contratoId;

    @NotNull(message = "Nota é obrigatória")
    @Min(value = 1, message = "Nota mínima é 1")
    @Max(value = 5, message = "Nota máxima é 5")
    private Integer nota;

    @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
    private String comentario;
}
