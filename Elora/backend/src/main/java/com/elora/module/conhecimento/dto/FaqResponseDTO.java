package com.elora.module.conhecimento.dto;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FaqResponseDTO {
    Integer id; String pergunta; String resposta; 
    String categoria; String status; 
    Integer categoriaId; String categoriaNome; 
    LocalDateTime criadoEm;
}