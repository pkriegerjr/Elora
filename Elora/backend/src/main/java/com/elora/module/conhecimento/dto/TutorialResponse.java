package com.elora.module.conhecimento.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TutorialResponse {
    private Integer id;
    private String titulo;
    private String descricao;
    private String linkConteudo;
    private String status;
    private Integer categoriaId;
    private LocalDateTime criadoEm;
}