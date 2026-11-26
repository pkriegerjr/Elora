package com.elora.module.juridico.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.juridico.dto.AtualizarStatusDenunciaRequest;
import com.elora.module.juridico.dto.CriarDenunciaRequest;
import com.elora.module.juridico.dto.DenunciaResponse;
import com.elora.module.juridico.entity.ProcessoRescisao;
import com.elora.module.juridico.service.JuridicoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Base path SEM prefixo /api: o context-path já é /api
 * (ver application.properties → server.servlet.context-path=/api).
 * Todas as respostas envolvidas em {@link ApiResponse} para o front
 * (apiService.js) fazer desembrulho automático.
 *
 * <p>Denúncias: endpoints canônicos em /denuncias; /analise e /analises
 * permanecem como aliases para não quebrar o front existente.</p>
 */
@RestController
@RequestMapping("/juridico")
@RequiredArgsConstructor
public class JuridicoController {

    private final JuridicoService service;

    @PostMapping({"/analise", "/denuncias"})
    public ResponseEntity<ApiResponse<DenunciaResponse>> criar(
            @Valid @RequestBody CriarDenunciaRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Denúncia registrada",
                service.criar(SecurityUtils.currentUserId(authentication), request)));
    }

    @GetMapping({"/analises", "/denuncias"})
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<List<DenunciaResponse>>> listarDenuncias() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarTodas()));
    }

    @GetMapping("/denuncias/minhas")
    public ResponseEntity<ApiResponse<List<DenunciaResponse>>> minhasDenuncias(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.minhas(SecurityUtils.currentUserId(authentication))));
    }

    /** Detalhe: equipe vê qualquer uma; denunciante vê apenas a própria (checagem no service). */
    @GetMapping({"/analises/{id}", "/denuncias/{id}"})
    public ResponseEntity<ApiResponse<DenunciaResponse>> detalhar(
            @PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.detalhar(id, SecurityUtils.currentUserId(authentication))));
    }

    @PatchMapping("/denuncias/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<DenunciaResponse>> atualizarStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusDenunciaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Status atualizado",
                service.atualizarStatus(id, request, SecurityUtils.currentUserId(authentication))));
    }

    @PostMapping("/rescisoes")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<ProcessoRescisao>> criarRescisao(
            @RequestBody @Valid ProcessoRescisao r) {
        return ResponseEntity.ok(ApiResponse.ok("Rescisão solicitada", service.solicitar(r)));
    }

    @GetMapping("/rescisoes")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<List<ProcessoRescisao>>> listarRescisoes() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarRescisoes()));
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> painel() {
        return ResponseEntity.ok(ApiResponse.ok(service.painel()));
    }
}
