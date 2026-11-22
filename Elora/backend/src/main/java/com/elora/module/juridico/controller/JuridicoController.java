package com.elora.module.juridico.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.juridico.entity.AnaliseJuridica;
import com.elora.module.juridico.entity.ProcessoRescisao;
import com.elora.module.juridico.service.JuridicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Base path SEM prefixo /api: o context-path já é /api
 * (ver application.properties → server.servlet.context-path=/api).
 * Com /api aqui, a rota virava /api/api/juridico (404 no front).
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/juridico")
@RequiredArgsConstructor
public class JuridicoController {

    private final JuridicoService service;

    @PostMapping("/analise")
    public ResponseEntity<ApiResponse<AnaliseJuridica>> criarAnalise(@RequestBody @Valid AnaliseJuridica a) {
        return ResponseEntity.ok(ApiResponse.ok("Análise jurídica criada", service.analisar(a)));
    }

    @PostMapping("/rescisoes")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<ProcessoRescisao>> criarRescisao(@RequestBody @Valid ProcessoRescisao r) {
        return ResponseEntity.ok(ApiResponse.ok("Rescisão solicitada", service.solicitar(r)));
    }

    @GetMapping("/analises")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<java.util.List<AnaliseJuridica>>> listarAnalises() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAnalises()));
    }

    @GetMapping("/rescisoes")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<java.util.List<ProcessoRescisao>>> listarRescisoes() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarRescisoes()));
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('ADMIN','JURIDICO')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> painel() {
        return ResponseEntity.ok(ApiResponse.ok(service.painel()));
    }
}
