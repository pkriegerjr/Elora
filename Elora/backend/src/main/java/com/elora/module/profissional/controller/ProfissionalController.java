package com.elora.module.profissional.controller;
import com.elora.common.dto.ApiResponse; import com.elora.integration.storage.ArquivoLido;
import com.elora.module.profissional.dto.*; import com.elora.module.profissional.entity.*; import com.elora.module.profissional.service.ProfissionalService;
import com.elora.security.SecurityUtils; import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; import org.springframework.core.io.InputStreamResource; import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets; import java.util.*;

@RestController @RequestMapping("/caregivers") @RequiredArgsConstructor
public class ProfissionalController {
  private final ProfissionalService service;

  @GetMapping
  public ApiResponse<List<ProfissionalResponse>> list(@RequestParam(required=false) String status, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size){
    return ApiResponse.ok(service.list(status,page,size));
  }
  @GetMapping("/me")
  public ApiResponse<ProfissionalResponse> me(Authentication auth){ return ApiResponse.ok(service.getById(SecurityUtils.currentUserId(auth))); }

  @PatchMapping("/me")
  public ApiResponse<ProfissionalResponse> updateMe(@Valid @RequestBody ProfissionalUpdateRequest req, Authentication auth){
    return ApiResponse.ok(service.updateMe(SecurityUtils.currentUserId(auth), req));
  }
  // legado: JSON com arquivoUrl pronta. Preferir POST /{id}/documentos/arquivo (multipart).
  @PostMapping(value="/{id}/documentos", consumes=MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<DocumentoProfissional> upload(@PathVariable Integer id, @RequestBody Map<String,String> body, Authentication auth){
    return ApiResponse.ok(service.uploadDocumento(id, SecurityUtils.currentUserId(auth), body.get("tipo"), body.get("arquivoUrl")));
  }
  /** Upload real do arquivo (multipart/form-data: campos "tipo" e "arquivo"). */
  @PostMapping(value="/{id}/documentos/arquivo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<ProfissionalResponse.DocumentoDTO> anexar(@PathVariable Integer id,
      @RequestParam(value="tipo", required=false) String tipo,
      @RequestParam(value="arquivo", required=false) MultipartFile arquivo,
      Authentication auth){
    return ApiResponse.ok(service.anexarDocumento(id, SecurityUtils.currentUserId(auth), tipo, arquivo));
  }
  /** Download do arquivo: dono do documento ou staff. */
  @GetMapping("/{id}/documentos/{docId}/arquivo")
  public ResponseEntity<InputStreamResource> baixar(@PathVariable Integer id, @PathVariable Integer docId, Authentication auth){
    ArquivoLido arquivo = service.baixarDocumento(id, docId, SecurityUtils.currentUserId(auth));
    String contentType = arquivo.contentType()==null||arquivo.contentType().isBlank()
        ? MediaType.APPLICATION_OCTET_STREAM_VALUE : arquivo.contentType();
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(contentType));
    if(arquivo.tamanho()>=0) headers.setContentLength(arquivo.tamanho());
    headers.setContentDisposition(ContentDisposition.attachment()
        .filename(arquivo.nomeOriginal(), StandardCharsets.UTF_8).build());
    return ResponseEntity.ok().headers(headers).body(new InputStreamResource(arquivo.conteudo()));
  }
  @PatchMapping("/{id}/validacao")
@PreAuthorize("hasAnyRole('ADMIN','MODERADOR','JURIDICO')")
public ApiResponse<ProfissionalResponse> validar(@PathVariable Integer id, @Valid @RequestBody ValidacaoRequest req, Authentication auth){
  return ApiResponse.ok(service.validar(id, SecurityUtils.currentUserId(auth), req));
  }
  @PutMapping("/me/disponibilidade")
  public ApiResponse<String> setDisp(@RequestBody List<Map<String,String>> body, Authentication auth){
    service.salvarDisponibilidade(SecurityUtils.currentUserId(auth), body); return ApiResponse.ok("Disponibilidade salva");
  }
  @GetMapping("/me/disponibilidade")
  public ApiResponse<List<Disponibilidade>> getDisp(Authentication auth){ return ApiResponse.ok(service.getDisponibilidade(SecurityUtils.currentUserId(auth))); }
  @GetMapping("/{id}/disponibilidade")
  public ApiResponse<List<Disponibilidade>> getDispById(@PathVariable Integer id){ return ApiResponse.ok(service.getDisponibilidade(id)); }
}
