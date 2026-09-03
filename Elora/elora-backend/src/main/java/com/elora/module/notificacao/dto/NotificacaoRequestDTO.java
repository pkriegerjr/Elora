package com.elora.module.notificacao.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificacaoRequestDTO {
    @NotBlank private String nome;
}
