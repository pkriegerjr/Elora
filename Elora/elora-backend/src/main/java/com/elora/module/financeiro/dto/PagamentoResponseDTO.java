package com.elora.module.financeiro.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagamentoResponseDTO {
    private Long id;
    private String nome;
}
