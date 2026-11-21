package com.elora.module.notificacao.controller;

import com.elora.common.dto.ApiResponse;
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
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático. Contagens retornadas como número puro dentro do envelope.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificacaoResponse>>> listar(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(notificacaoService.listar(
                SecurityUtils.currentUserId(authentication), unreadOnly, tipo, Math.min(Math.max(limit, 1), 100))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> naoLidas(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(notificacaoService.contarNaoLidas(SecurityUtils.currentUserId(authentication))));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Long>> lerTodas(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(notificacaoService.marcarTodasComoLidas(SecurityUtils.currentUserId(authentication))));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<PreferenciaResponse>> preferencias(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(notificacaoService.obterPreferencias(SecurityUtils.currentUserId(authentication))));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<ApiResponse<PreferenciaResponse>> atualizarPreferencias(
            @RequestBody PreferenciasRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Preferências atualizadas",
                notificacaoService.atualizarPreferencias(SecurityUtils.currentUserId(authentication), request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificacaoResponse>> marcarComoLida(
            @PathVariable Integer id, @Valid @RequestBody MarcarLidaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(notificacaoService.marcarComoLida(
                SecurityUtils.currentUserId(authentication), id, request.getLida())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id, Authentication authentication) {
        notificacaoService.excluir(SecurityUtils.currentUserId(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("Notificação excluída com sucesso", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificacaoResponse>> enviar(
            @Valid @RequestBody CriarNotificacaoRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Notificação enviada",
                notificacaoService.enviar(SecurityUtils.currentUserId(authentication), request)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<NotificacaoResponse>>> enviarEmLote(
            @Valid @RequestBody NotificacaoBulkRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Notificações enviadas em lote",
                notificacaoService.enviarEmLote(SecurityUtils.currentUserId(authentication), request.getUserIds(), request.getNotification())));
    }
}
