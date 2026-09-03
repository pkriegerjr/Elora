package com.elora.module.escala.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EscalaRequestDTO {
    @NotBlank private String nome;
}
