package com.elora.module.busca.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BuscaRequestDTO {
    @NotBlank private String nome;
}
