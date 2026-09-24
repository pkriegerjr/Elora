package com.elora.module.conhecimento.dto;
import jakarta.validation.constraints.NotBlank; import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FaqRequestDTO {
    @NotBlank String pergunta; 
    @NotBlank String resposta; 
    String categoria; 
    String status; // rascunho|publicado
    Integer categoriaId;
}