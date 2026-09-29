package com.elora.module.escala.controller;

import com.elora.module.escala.dto.DefinirDisponibilidadeRequest;
import com.elora.module.escala.dto.DisponibilidadeResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REQ-013 + UC10: quadro de disponibilidade do profissional
 * (alimenta a busca por período). Upsert por usuario+data+periodo.
 */
@RestController
@RequestMapping("/disponibilidade")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final EscalaService escalas;

    @PostMapping
    public ResponseEntity<DisponibilidadeResponse> definir(
            Authentication authentication,
            @Valid @RequestBody DefinirDisponibilidadeRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalas.definirDisponibilidade(
                SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr()));
    }

    @GetMapping
    public List<DisponibilidadeResponse> listar(
            Authentication authentication,
            @RequestParam(required = false) Integer profissionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return escalas.listarDisponibilidade(
                SecurityUtils.currentUserId(authentication), profissionalId, inicio, fim);
    }
}
