package com.elora.module.conhecimento.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TutorialRequest {
    @NotBlank private String titulo;
    private String descricao;
    private String linkConteudo;
    private String status;
    private Integer categoriaId;
}