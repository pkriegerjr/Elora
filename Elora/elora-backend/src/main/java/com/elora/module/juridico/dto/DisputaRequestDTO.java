package com.elora.module.juridico.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DisputaRequestDTO {
    @NotBlank private String nome;
}
