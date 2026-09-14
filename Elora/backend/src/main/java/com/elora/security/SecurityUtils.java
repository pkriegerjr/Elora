package com.elora.security;

import com.elora.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;

/** Extrai o userId (subject do JWT) do contexto de segurança. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Integer currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Não autenticado");
        }
        try {
            return Integer.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Não autenticado");
        }
    }
}
