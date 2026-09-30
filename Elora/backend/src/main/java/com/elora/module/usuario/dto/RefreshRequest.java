package com.elora.module.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POST /auth/refresh — recebe o refresh token opaco, devolve par novo (rotação). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "refreshToken é obrigatório")
    private String refreshToken;
}
