package com.elora.module.relatorio.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RelatorioRequestDTO {
    @NotBlank private String nome;
}
