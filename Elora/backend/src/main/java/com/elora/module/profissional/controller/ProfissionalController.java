package com.elora.module.profissional.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.integration.storage.ArquivoLido;
import com.elora.module.profissional.dto.*;
import com.elora.module.profissional.entity.*;
import com.elora.module.profissional.service.ProfissionalService;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.service.UsuarioService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * MÓDULO PROFISSIONAL - Controller único para todos os endpoints de {@code /caregivers}.
 * Consolida registro, listagem, perfil, documentos, disponibilidade e validação.
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/caregivers")
@RequiredArgsConstructor
public class ProfissionalController {

    private final ProfissionalService service;
    private final UsuarioService usuarioService;

    /* ------------------------------------------------------------
     * REGISTRO (era CuidadorController)
     * ---------------------------------------------------------- */

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> register(@Valid @RequestBody CuidadorRegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cuidador cadastrado com sucesso",
                usuarioService.registerCuidador(request)));
    }

    /* ------------------------------------------------------------
     * LISTAGEM / DETALHE
     * ---------------------------------------------------------- */

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfissionalResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(status, page, size)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfissionalResponse>> me(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(SecurityUtils.currentUserId(auth))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> buscarPorId(@PathVariable Integer id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.findForViewer(SecurityUtils.currentUserId(auth), id)));
    }

    /* ------------------------------------------------------------
     * ATUALIZAÇÃO DE PERFIL
     * ---------------------------------------------------------- */

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ProfissionalResponse>> updateMe(@Valid @RequestBody ProfissionalUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Perfil atualizado com sucesso",
                service.updateMe(SecurityUtils.currentUserId(auth), req)));
    }

    /* ------------------------------------------------------------
     * DOCUMENTOS
     * ---------------------------------------------------------- */

    /**
     * Legado: JSON com arquivoUrl pronta. Preferir POST /{id}/documentos/arquivo (multipart).
     */
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DocumentoProfissional>> upload(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Documento anexado",
                service.uploadDocumento(id, SecurityUtils.currentUserId(auth), body.get("tipo"), body.get("arquivoUrl"))));
    }

    /** Upload real do arquivo (multipart/form-data: campos "tipo" e "arquivo"). */
    @PostMapping(value = "/{id}/documentos/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfissionalResponse.DocumentoDTO>> anexar(
            @PathVariable Integer id,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Arquivo anexado com sucesso",
                service.anexarDocumento(id, SecurityUtils.currentUserId(auth), tipo, arquivo)));
    }

    /** Download do arquivo: dono do documento ou staff. */
    @GetMapping("/{id}/documentos/{docId}/arquivo")
    public ResponseEntity<InputStreamResource> baixar(
            @PathVariable Integer id,
            @PathVariable Integer docId,
            Authentication auth) {
        ArquivoLido arquivo = service.baixarDocumento(id, docId, SecurityUtils.currentUserId(auth));
        String contentType = arquivo.contentType() == null || arquivo.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : arquivo.contentType();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (arquivo.tamanho() >= 0) headers.setContentLength(arquivo.tamanho());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(arquivo.conteudo()));
    }

    /* ------------------------------------------------------------
     * VALIDAÇÃO (admin/moderador/jurídico)
     * ---------------------------------------------------------- */

    @PatchMapping("/{id}/validacao")
    @PreAuthorize("hasAnyRole('ADMIN','MODERADOR','JURIDICO')")
    public ResponseEntity<ApiResponse<ProfissionalResponse>> validar(
            @PathVariable Integer id,
            @Valid @RequestBody ValidacaoRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Profissional validado",
                service.validar(id, SecurityUtils.currentUserId(auth), req)));
    }

    /* ------------------------------------------------------------
     * DISPONIBILIDADE
     * ---------------------------------------------------------- */

    @PutMapping("/me/disponibilidade")
    public ResponseEntity<ApiResponse<String>> setDisp(
            @RequestBody List<Map<String, String>> body,
            Authentication auth) {
        service.salvarDisponibilidade(SecurityUtils.currentUserId(auth), body);
        return ResponseEntity.ok(ApiResponse.ok("Disponibilidade salva", "OK"));
    }

    @GetMapping("/me/disponibilidade")
    public ResponseEntity<ApiResponse<List<Disponibilidade>>> getDisp(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDisponibilidade(SecurityUtils.currentUserId(auth))));
    }

    @GetMapping("/{id}/disponibilidade")
    public ResponseEntity<ApiResponse<List<Disponibilidade>>> getDispById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDisponibilidade(id)));
    }
}