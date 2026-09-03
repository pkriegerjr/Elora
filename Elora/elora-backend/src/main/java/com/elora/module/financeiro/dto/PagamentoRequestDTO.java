package com.elora.module.financeiro.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagamentoRequestDTO {
    @NotBlank private String nome;
}
