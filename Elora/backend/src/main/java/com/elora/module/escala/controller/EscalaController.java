package com.elora.module.escala.controller;

import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.dto.GerarEscalaRequest;
import com.elora.module.escala.service.EscalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * MÓDULO ESCALA - Controller (fino, delega ao service — padrão dos demais).
 * Rotas exigem login (SecurityConfig: qualquer autenticado; sem papel
 * específico nesta versão).
 */
@RestController
@RequestMapping("/escalas")
@RequiredArgsConstructor
public class EscalaController {

    private final EscalaService escalaService;

    // POST /escalas/gerar {contratoId, dataInicio, dataFim, periodos:[...]}
    @PostMapping("/gerar")
    public ResponseEntity<List<EscalaResponse>> gerar(@Valid @RequestBody GerarEscalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalaService.gerar(request));
    }

    // GET /escalas?contratoId=1&inicio=2026-01-01&fim=2026-01-31
    @GetMapping
    public List<EscalaResponse> listar(
            @RequestParam Integer contratoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return escalaService.listar(contratoId, inicio, fim);
    }

    // PATCH /escalas/{id} {status}
    @PatchMapping("/{id}")
    public EscalaResponse atualizarStatus(
            @PathVariable Integer id, @Valid @RequestBody AtualizarEscalaRequest request) {
        return escalaService.atualizarStatus(id, request);
    }
}
