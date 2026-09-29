package com.elora.module.contrato.controller;

import com.elora.module.contrato.dto.AtualizarContratoRequest;
import com.elora.module.contrato.dto.AtualizarStatusRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.service.ContratoService;
import com.elora.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Base {@code /contracts} (compatível com {@code CONFIG.ENDPOINTS.contracts} do front).
 * POST /contracts (201) | GET /contracts?papel= | GET /contracts/{id} |
 * PUT /contracts/{id} | PATCH /contracts/{id}/status |
 * POST /contracts/{id}/assinaturas (ativa com as duas partes) |
 * DELETE (204, só rascunho).
 */
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping
    public ResponseEntity<ContratoResponse> criar(
            @Valid @RequestBody CriarContratoRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.criar(
                SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr()));
    }

    @GetMapping
    public List<ContratoResponse> meusContratos(
            @RequestParam(required = false) String papel,
            Authentication authentication) {
        return contratoService.meusContratos(SecurityUtils.currentUserId(authentication), papel);
    }

    @GetMapping("/{id}")
    public ContratoResponse buscarPorId(@PathVariable Integer id, Authentication authentication) {
        return contratoService.buscarPorId(SecurityUtils.currentUserId(authentication), id);
    }

    @PutMapping("/{id}")
    public ContratoResponse atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarContratoRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        return contratoService.atualizar(
                SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/status")
    public ContratoResponse atualizarStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        return contratoService.atualizarStatus(
                SecurityUtils.currentUserId(authentication), id, request.getStatus(), http.getRemoteAddr());
    }

    @PostMapping("/{id}/assinaturas")
    public ContratoResponse assinar(
            @PathVariable Integer id,
            Authentication authentication,
            HttpServletRequest http) {
        return contratoService.assinar(
                SecurityUtils.currentUserId(authentication), id, http.getRemoteAddr());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id,
                        Authentication authentication,
                        HttpServletRequest http) {
        contratoService.excluir(
                SecurityUtils.currentUserId(authentication), id, http.getRemoteAddr());
    }
}
