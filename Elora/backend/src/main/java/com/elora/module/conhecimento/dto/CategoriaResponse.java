package com.elora.module.conhecimento.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoriaResponse {
    private Integer id;
    private String nome;
    private String descricao;
    private LocalDateTime criadoEm;
}