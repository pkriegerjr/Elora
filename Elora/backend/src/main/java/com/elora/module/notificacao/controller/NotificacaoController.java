package com.elora.module.notificacao.controller;

import com.elora.module.notificacao.dto.CriarNotificacaoRequest;
import com.elora.module.notificacao.dto.MarcarLidaRequest;
import com.elora.module.notificacao.dto.NotificacaoBulkRequest;
import com.elora.module.notificacao.dto.NotificacaoResponse;
import com.elora.module.notificacao.dto.PreferenciaResponse;
import com.elora.module.notificacao.dto.PreferenciasRequest;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas que o front (notificationService.js) espera quando MOCK_MODE=false.
 * Contagens retornadas como número puro, como o front consome.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public List<NotificacaoResponse> listar(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        return notificacaoService.listar(
                SecurityUtils.currentUserId(authentication), unreadOnly, tipo, Math.min(Math.max(limit, 1), 100));
    }

    @GetMapping("/unread-count")
    public Long naoLidas(Authentication authentication) {
        return notificacaoService.contarNaoLidas(SecurityUtils.currentUserId(authentication));
    }

    @PatchMapping("/read-all")
    public Long lerTodas(Authentication authentication) {
        return notificacaoService.marcarTodasComoLidas(SecurityUtils.currentUserId(authentication));
    }

    @GetMapping("/preferences")
    public PreferenciaResponse preferencias(Authentication authentication) {
        return notificacaoService.obterPreferencias(SecurityUtils.currentUserId(authentication));
    }

    @PatchMapping("/preferences")
    public PreferenciaResponse atualizarPreferencias(
            @RequestBody PreferenciasRequest request, Authentication authentication) {
        return notificacaoService.atualizarPreferencias(SecurityUtils.currentUserId(authentication), request);
    }

    @PatchMapping("/{id}")
    public NotificacaoResponse marcarComoLida(
            @PathVariable Integer id, @Valid @RequestBody MarcarLidaRequest request,
            Authentication authentication) {
        return notificacaoService.marcarComoLida(
                SecurityUtils.currentUserId(authentication), id, request.getLida());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id, Authentication authentication) {
        notificacaoService.excluir(SecurityUtils.currentUserId(authentication), id);
    }

    @PostMapping
    public ResponseEntity<NotificacaoResponse> enviar(
            @Valid @RequestBody CriarNotificacaoRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacaoService.enviar(SecurityUtils.currentUserId(authentication), request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<NotificacaoResponse>> enviarEmLote(
            @Valid @RequestBody NotificacaoBulkRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacaoService.enviarEmLote(
                SecurityUtils.currentUserId(authentication), request.getUserIds(), request.getNotification()));
    }
}
