package com.elora.module.avaliacao.controller;

import com.elora.module.avaliacao.dto.AvaliacaoResponse;
import com.elora.module.avaliacao.dto.AvaliacoesCuidadorResponse;
import com.elora.module.avaliacao.dto.CriarAvaliacaoRequest;
import com.elora.module.avaliacao.dto.MediaAvaliacaoResponse;
import com.elora.module.avaliacao.service.AvaliacaoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 */
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(
            @Valid @RequestBody CriarAvaliacaoRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                avaliacaoService.criar(SecurityUtils.currentUserId(authentication), request));
    }

    @GetMapping("/cuidador/{cuidadorId}")
    public AvaliacoesCuidadorResponse listarPorCuidador(@PathVariable Integer cuidadorId) {
        return avaliacaoService.listarPorCuidador(cuidadorId);
    }

    @GetMapping("/cuidador/{cuidadorId}/media")
    public MediaAvaliacaoResponse mediaPorCuidador(@PathVariable Integer cuidadorId) {
        return avaliacaoService.mediaPorCuidador(cuidadorId);
    }
}
