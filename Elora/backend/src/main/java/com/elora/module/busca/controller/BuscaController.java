package com.elora.module.busca.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * MÓDULO BUSCA - Controller (padrão dos demais: fino, delega ao service).
 * Um endpoint flexível de busca (não um por filtro) + detalhe + catálogo
 * + favoritos. Todas exigem login (SecurityConfig).
 */
@RestController
@RequestMapping("/busca")
@RequiredArgsConstructor
public class BuscaController {

    private final BuscaService buscaService;

    // GET /busca/profissionais?nome=ana&precoMax=30&notaMinima=4&especialidadeId=1
    //   &data=2026-03-10&periodo=matutino&ordenarPor=preco&direcao=ASC&limit=20
    @GetMapping("/profissionais")
    public List<ProfissionalResponse> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) BigDecimal notaMinima,
            @RequestParam(required = false) Integer especialidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Periodo periodo,
            @RequestParam(defaultValue = "nota") String ordenarPor, // preco|nota|nome
            @RequestParam(defaultValue = "DESC") String direcao, // ASC|DESC
            @RequestParam(defaultValue = "20") int limit) {
        return buscaService.buscar(nome, precoMin, precoMax, notaMinima, especialidadeId,
                data, periodo, ordenarPor, direcao, Math.min(Math.max(limit, 1), 100));
    }

    @GetMapping("/profissionais/{id}")
    public ProfissionalResponse detalhar(@PathVariable Integer id) {
        return buscaService.detalhar(id);
    }

    @GetMapping("/especialidades")
    public List<EspecialidadeResponse> especialidades() {
        return buscaService.listarEspecialidades();
    }

    @PostMapping("/favoritos")
    public ResponseEntity<FavoritoResponse> favoritar(
            @Valid @RequestBody FavoritoRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                buscaService.favoritar(SecurityUtils.currentUserId(authentication), request));
    }

    @GetMapping("/favoritos")
    public List<ProfissionalResponse> meusFavoritos(Authentication authentication) {
        return buscaService.meusFavoritos(SecurityUtils.currentUserId(authentication));
    }

    @DeleteMapping("/favoritos/{profissionalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desfavoritar(@PathVariable Integer profissionalId, Authentication authentication) {
        buscaService.desfavoritar(SecurityUtils.currentUserId(authentication), profissionalId);
    }
}
