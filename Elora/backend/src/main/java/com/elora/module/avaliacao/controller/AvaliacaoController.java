package com.elora.module.avaliacao.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.avaliacao.dto.AvaliacaoResponse;
import com.elora.module.avaliacao.dto.AvaliacoesCuidadorResponse;
import com.elora.module.avaliacao.dto.CriarAvaliacaoRequest;
import com.elora.module.avaliacao.dto.MediaAvaliacaoResponse;
import com.elora.module.avaliacao.service.AvaliacaoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MÓDULO AVALIACAO - Controller (padrão NotificacaoController).
 * Rotas: POST /avaliacoes (201), GET /avaliacoes/cuidador/{id},
 * GET /avaliacoes/cuidador/{id}/media. Avaliador sempre = JWT.
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<ApiResponse<AvaliacaoResponse>> criar(
            @Valid @RequestBody CriarAvaliacaoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Avaliação criada com sucesso",
                avaliacaoService.criar(SecurityUtils.currentUserId(authentication), request)));
    }

    @GetMapping("/cuidador/{cuidadorId}")
    public ResponseEntity<ApiResponse<AvaliacoesCuidadorResponse>> listarPorCuidador(@PathVariable Integer cuidadorId) {
        return ResponseEntity.ok(ApiResponse.ok(avaliacaoService.listarPorCuidador(cuidadorId)));
    }

    @GetMapping("/cuidador/{cuidadorId}/media")
    public ResponseEntity<ApiResponse<MediaAvaliacaoResponse>> mediaPorCuidador(@PathVariable Integer cuidadorId) {
        return ResponseEntity.ok(ApiResponse.ok(avaliacaoService.mediaPorCuidador(cuidadorId)));
    }
}
