package com.elora.module.juridico.dto;

import com.elora.module.juridico.enums.StatusDenuncia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** MÓDULO JURÍDICO - PATCH /juridico/denuncias/{id}/status (somente equipe). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusDenunciaRequest {

    @NotNull(message = "Status é obrigatório")
    private StatusDenuncia status;

    @Size(max = 2000, message = "Parecer deve ter no máximo 2000 caracteres")
    private String parecer;
}
