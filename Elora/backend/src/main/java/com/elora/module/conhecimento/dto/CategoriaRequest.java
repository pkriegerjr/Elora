package com.elora.module.conhecimento.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoriaRequest {
    @NotBlank @Size(max=80) private String nome;
    private String descricao;
}