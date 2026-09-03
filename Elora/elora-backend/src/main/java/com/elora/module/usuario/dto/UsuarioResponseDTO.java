package com.elora.module.usuario.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String nome;
}
