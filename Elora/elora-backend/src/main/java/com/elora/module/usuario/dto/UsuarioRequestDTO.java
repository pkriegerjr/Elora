package com.elora.module.usuario.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UsuarioRequestDTO {
    @NotBlank private String nome;
}
