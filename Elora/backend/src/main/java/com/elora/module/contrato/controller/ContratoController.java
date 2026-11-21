package com.elora.module.contrato.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.contrato.dto.AtualizarContratoRequest;
import com.elora.module.contrato.dto.AtualizarStatusRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.service.ContratoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MÓDULO CONTRATO - Controller fino (padrão do projeto, @Valid + JWT).
 * Todas as respostas são envolvidas em {@link ApiResponse} para o front
 * (apiService.js) fazer desembrulho automático.
 */
@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContratoResponse>> criar(
            @Valid @RequestBody CriarContratoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Contrato criado com sucesso",
                contratoService.criar(SecurityUtils.currentUserId(authentication), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContratoResponse>>> meusContratos(
            @RequestParam(required = false) String papel,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(contratoService.meusContratos(SecurityUtils.currentUserId(authentication), papel)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContratoResponse>> buscarPorId(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(contratoService.buscarPorId(SecurityUtils.currentUserId(authentication), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContratoResponse>> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarContratoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Contrato atualizado com sucesso",
                contratoService.atualizar(SecurityUtils.currentUserId(authentication), id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContratoResponse>> atualizarStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Status do contrato atualizado",
                contratoService.atualizarStatus(SecurityUtils.currentUserId(authentication), id, request.getStatus())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id, Authentication authentication) {
        contratoService.excluir(SecurityUtils.currentUserId(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("Contrato excluído com sucesso", null));
    }
}
