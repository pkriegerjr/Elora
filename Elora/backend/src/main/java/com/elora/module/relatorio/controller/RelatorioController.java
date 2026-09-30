package com.elora.module.relatorio.controller;

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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Relatórios gerenciais (somente leitura). Permissão por relatório no service:
 * financeiro exige admin/financeiro; demais exigem equipe (staff).
 */
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/resumo")
    public ResumoOperacionalResponse resumo(Authentication authentication) {
        return relatorioService.resumo(SecurityUtils.currentUserId(authentication));
    }

    @GetMapping("/financeiro")
    public RelatorioFinanceiroResponse financeiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return relatorioService.financeiro(SecurityUtils.currentUserId(authentication), inicio, fim);
    }

    @GetMapping("/usuarios")
    public RelatorioUsuariosResponse usuarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return relatorioService.usuarios(SecurityUtils.currentUserId(authentication), inicio, fim);
    }

    @GetMapping("/contratos")
    public RelatorioContratosResponse contratos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {
        return relatorioService.contratos(SecurityUtils.currentUserId(authentication), inicio, fim);
    }

    @GetMapping("/avaliacoes")
    public RelatorioAvaliacoesResponse avaliacoes(Authentication authentication) {
        return relatorioService.avaliacoes(SecurityUtils.currentUserId(authentication));
    }

    @GetMapping("/denuncias")
    public RelatorioDenunciasResponse denuncias(Authentication authentication) {
        return relatorioService.denuncias(SecurityUtils.currentUserId(authentication));
    }
}
