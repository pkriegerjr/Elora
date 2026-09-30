package com.elora.module.conhecimento.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ArtigoResponse {
    private Integer id;
    private String titulo;
    private String corpo;
    private String categoria;
    private Integer categoriaId;
    private Integer autorId;
    private Boolean publicado;
    private LocalDateTime criadoEm;
}