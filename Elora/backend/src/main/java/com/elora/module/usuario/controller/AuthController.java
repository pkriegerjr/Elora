package com.elora.module.usuario.controller;

import com.elora.module.usuario.dto.AuthResponse;
import com.elora.module.usuario.dto.LoginRequest;
import com.elora.module.usuario.dto.PasswordResetConfirm;
import com.elora.module.usuario.dto.PasswordResetRequest;
import com.elora.module.usuario.dto.RefreshRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.service.AuthService;
import com.elora.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respostas diretas (sem envelope): o front lê {@code res.accessToken},
 * {@code res.refreshToken} e {@code res.user}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return authService.refresh(request.getRefreshToken(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.me(SecurityUtils.currentUserId(authentication));
    }

    /**
     * REQ-ELO-002. Resposta sempre genérica (não revela se o identifier
     * existe). Rota pública via {@code POST /auth/**} no SecurityConfig.
     */
    @PostMapping("/password/reset")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getIdentifier());
    }

    @PostMapping("/password/confirm")
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        authService.confirmPasswordReset(request.getToken(), request.getNovaSenha());
    }
}
