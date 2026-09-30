package com.elora.module.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /auth/password/reset — identifier é e-mail ou CPF.
 * Resposta sempre genérica (não revela se o usuário existe).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "CPF/E-mail é obrigatório")
    private String identifier;
}
