package com.elora.module.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /auth/login — identifier é e-mail ou CPF (com ou sem máscara).
 * userType (CLIENT/CAREGIVER/ADMIN) identifica a aba escolhida no front e é
 * validado contra os perfis reais da conta antes de emitir o token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "CPF/E-mail é obrigatório")
    private String identifier;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    private String userType;
}
