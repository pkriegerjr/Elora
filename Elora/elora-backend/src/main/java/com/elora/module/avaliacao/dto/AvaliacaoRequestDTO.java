package com.elora.module.avaliacao.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AvaliacaoRequestDTO {
    @NotBlank private String nome;
}
