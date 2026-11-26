package com.elora.module.relatorio.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.relatorio.dto.RelatorioAvaliacoesResponse;
import com.elora.module.relatorio.dto.RelatorioContratosResponse;
import com.elora.module.relatorio.dto.RelatorioDenunciasResponse;
import com.elora.module.relatorio.dto.RelatorioFinanceiroResponse;
import com.elora.module.relatorio.dto.RelatorioUsuariosResponse;
import com.elora.module.relatorio.dto.ResumoOperacionalResponse;
import com.elora.module.relatorio.service.RelatorioService;
import com.elora.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Relatórios gerenciais (somente leitura). Permissão por relatório no service:
 * financeiro exige admin/financeiro; demais exigem equipe (staff).
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/resumo")
    public ResponseEntity<ApiResponse<ResumoOperacionalResponse>> resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.resumo(
                SecurityUtils.currentUserId(authentication), inicio, fim)));
    }

    @GetMapping("/financeiro")
    public ResponseEntity<ApiResponse<RelatorioFinanceiroResponse>> financeiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.financeiro(SecurityUtils.currentUserId(authentication), inicio, fim)));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<RelatorioUsuariosResponse>> usuarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.usuarios(SecurityUtils.currentUserId(authentication), inicio, fim)));
    }

    @GetMapping("/contratos")
    public ResponseEntity<ApiResponse<RelatorioContratosResponse>> contratos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.contratos(SecurityUtils.currentUserId(authentication), inicio, fim)));
    }

    @GetMapping("/avaliacoes")
    public ResponseEntity<ApiResponse<RelatorioAvaliacoesResponse>> avaliacoes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.avaliacoes(
                SecurityUtils.currentUserId(authentication), inicio, fim)));
    }

    @GetMapping("/denuncias")
    public ResponseEntity<ApiResponse<RelatorioDenunciasResponse>> denuncias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(relatorioService.denuncias(
                SecurityUtils.currentUserId(authentication), inicio, fim)));
    }
}
