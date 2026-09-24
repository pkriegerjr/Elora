package com.elora.module.profissional.controller;
import com.elora.common.dto.ApiResponse; import com.elora.module.profissional.dto.*; import com.elora.module.profissional.entity.*; import com.elora.module.profissional.service.ProfissionalService;
import com.elora.security.SecurityUtils; import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

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
  // compat com UsuarioController: GET /caregivers/{id} já existe, este complementa
  @PostMapping("/{id}/documentos")
  public ApiResponse<DocumentoProfissional> upload(@PathVariable Integer id, @RequestBody Map<String,String> body){
    return ApiResponse.ok(service.uploadDocumento(id, body.get("tipo"), body.get("arquivoUrl")));
  }
  @PatchMapping("/{id}/validacao") // só moderador/juridico/admin - validar via @PreAuthorize
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