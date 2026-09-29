package com.elora.module.escala.controller;

import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.CriarEscalaRequest;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.service.EscalaService;
import com.elora.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REQ-013: turnos por contrato. Rotas exatas antes de {@code /{id}}.
 * Alteração de turno notifica a outra parte (serviço).
 */
@RestController
@RequestMapping("/escalas")
@RequiredArgsConstructor
public class EscalaController {

    private final EscalaService escalas;

    @PostMapping
    public ResponseEntity<EscalaResponse> criar(
            Authentication authentication,
            @Valid @RequestBody CriarEscalaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalas.criarEscala(
                SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr()));
    }

    @GetMapping
    public List<EscalaResponse> listar(
            Authentication authentication,
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) Integer profissionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        Integer viewerId = SecurityUtils.currentUserId(authentication);
        if (profissionalId != null) {
            return escalas.listarPorProfissional(viewerId, profissionalId, inicio, fim);
        }
        if (contratoId != null) {
            return escalas.listarPorContrato(viewerId, contratoId, inicio, fim);
        }
        throw new com.elora.common.exception.BusinessException(
                "Informe contratoId ou profissionalId");
    }

    @PutMapping("/{id}")
    public EscalaResponse atualizar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody AtualizarEscalaRequest request,
            HttpServletRequest http) {
        return escalas.atualizarEscala(
                SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr());
    }
}
