package com.elora.module.conhecimento.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ArtigoRequest {
    @NotBlank private String titulo;
    @NotBlank private String corpo;
    private String categoria;
    private Integer categoriaId;
    private Boolean publicado;
}