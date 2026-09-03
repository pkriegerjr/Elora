package com.elora.module.profissional.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProfissionalRequestDTO {
    @NotBlank private String nome;
}
