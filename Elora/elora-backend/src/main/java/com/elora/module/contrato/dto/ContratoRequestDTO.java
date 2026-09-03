package com.elora.module.contrato.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ContratoRequestDTO {
    @NotBlank private String nome;
}
