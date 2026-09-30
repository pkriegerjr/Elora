package com.elora.module.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /auth/password/confirm — troca a senha com o token opaco
 * gerado em /auth/password/reset (uso único, expira em 1h).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirm {

    @NotBlank(message = "Token é obrigatório")
    private String token;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,}$",
            message = "Senha deve ter maiúscula, minúscula, número e caractere especial")
    private String novaSenha;
}
