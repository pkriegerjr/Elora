package com.elora.module.conhecimento.controller;
import com.elora.common.dto.ApiResponse;
import com.elora.module.conhecimento.dto.*;
import com.elora.module.conhecimento.service.ConhecimentoService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/conhecimento") @RequiredArgsConstructor
public class ConhecimentoController {
    private final ConhecimentoService service;
    @GetMapping("/artigos") public ApiResponse<List<ArtigoResponse>> listar() { return ApiResponse.ok(service.listarPublicados()); }
    @GetMapping("/artigos/{id}") public ApiResponse<ArtigoResponse> get(@PathVariable Integer id) { return ApiResponse.ok(service.buscarArtigo(id)); }
    @PostMapping("/artigos") public ApiResponse<ArtigoResponse> criar(@Valid @RequestBody ArtigoRequest req, Authentication auth) {
        return ApiResponse.ok(service.criarArtigo(req, SecurityUtils.currentUserId(auth)));
    }
    @GetMapping("/faq") public ApiResponse<List<FaqResponseDTO>> faq() { return ApiResponse.ok(service.listarFaqPublicado()); }
}