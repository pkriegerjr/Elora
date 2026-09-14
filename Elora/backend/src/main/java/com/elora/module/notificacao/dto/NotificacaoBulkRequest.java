package com.elora.module.notificacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** POST /notifications/bulk — mesmo payload para vários destinatários. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoBulkRequest {

    @NotEmpty(message = "userIds é obrigatório")
    private List<Integer> userIds;

    @NotNull(message = "notification é obrigatória")
    @Valid
    private CriarNotificacaoRequest notification;
}
