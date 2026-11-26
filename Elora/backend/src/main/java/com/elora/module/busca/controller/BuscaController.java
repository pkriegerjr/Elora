package com.elora.module.busca.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.busca.dto.EspecialidadeResponse;
import com.elora.module.busca.dto.FavoritoRequest;
import com.elora.module.busca.dto.FavoritoResponse;
import com.elora.module.busca.dto.ProfissionalResponse;
import com.elora.module.busca.enums.Periodo;
import com.elora.module.busca.service.BuscaService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * MÓDULO BUSCA - Controller (padrão dos demais: fino, delega ao service).
 * Um endpoint flexível de busca (não um por filtro) + detalhe + catálogo
 * + favoritos. Todas exigem login (SecurityConfig).
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/busca")
@RequiredArgsConstructor
public class BuscaController {

    private final BuscaService buscaService;

    // GET /busca/profissionais?nome=ana&precoMax=30&notaMinima=4&especialidadeId=1&especialidadeId=2
    //   &data=2026-03-10&periodo=matutino&ordenarPor=preco&direcao=ASC&limit=20
    @GetMapping("/profissionais")
    public ResponseEntity<ApiResponse<List<ProfissionalResponse>>> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) BigDecimal notaMinima,
            @RequestParam(required = false) List<Integer> especialidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Periodo periodo,
            @RequestParam(defaultValue = "nota") String ordenarPor, // preco|nota|nome
            @RequestParam(defaultValue = "DESC") String direcao, // ASC|DESC
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(buscaService.buscar(nome, precoMin, precoMax, notaMinima, especialidadeId,
                data, periodo, ordenarPor, direcao, Math.min(Math.max(limit, 1), 100))));
    }

    @GetMapping("/profissionais/{id}")
    public ResponseEntity<ApiResponse<ProfissionalResponse>> detalhar(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(buscaService.detalhar(id)));
    }

    @GetMapping("/especialidades")
    public ResponseEntity<ApiResponse<List<EspecialidadeResponse>>> especialidades() {
        return ResponseEntity.ok(ApiResponse.ok(buscaService.listarEspecialidades()));
    }

    @PostMapping("/favoritos")
    public ResponseEntity<ApiResponse<FavoritoResponse>> favoritar(
            @Valid @RequestBody FavoritoRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Favorito adicionado",
                buscaService.favoritar(SecurityUtils.currentUserId(authentication), request)));
    }

    @GetMapping("/favoritos")
    public ResponseEntity<ApiResponse<List<ProfissionalResponse>>> meusFavoritos(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(buscaService.meusFavoritos(SecurityUtils.currentUserId(authentication))));
    }

    @DeleteMapping("/favoritos/{profissionalId}")
    public ResponseEntity<ApiResponse<Void>> desfavoritar(@PathVariable Integer profissionalId, Authentication authentication) {
        buscaService.desfavoritar(SecurityUtils.currentUserId(authentication), profissionalId);
        return ResponseEntity.ok(ApiResponse.ok("Favorito removido com sucesso", null));
    }
}
