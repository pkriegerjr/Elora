package com.elora.module.usuario.controller;

import com.elora.common.dto.ApiResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respostas padronizadas em envelope {@link ApiResponse}: o front (apiService.js)
 * faz desembrulho automático de {@code { success, message, data, timestamp }}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, http.getRemoteAddr(), http.getHeader("User-Agent"))));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.getRefreshToken(), http.getRemoteAddr(), http.getHeader("User-Agent"))));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(SecurityUtils.currentUserId(authentication))));
    }

    /**
     * REQ-ELO-002. Resposta sempre genérica (não revela se o identifier
     * existe). Rota pública via {@code POST /auth/**} no SecurityConfig.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getIdentifier());
        return ResponseEntity.ok(ApiResponse.ok("Se existir um cadastro com este e-mail/CPF, instruções de redefinição serão enviadas.", null));
    }

    @PostMapping("/password/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        authService.confirmPasswordReset(request.getToken(), request.getNovaSenha());
        return ResponseEntity.ok(ApiResponse.ok("Senha redefinida com sucesso.", null));
    }
}
