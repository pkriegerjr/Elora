package com.elora.module.usuario.controller;

import com.elora.module.usuario.dto.CuidadorCardResponse;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.service.UsuarioService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/caregivers")
@RequiredArgsConstructor
public class CuidadorController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody CuidadorRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registerCuidador(request));
    }

    /**
     * Vitrine de cuidadores com filtros opcionais. Rota exata — o Spring a
     * prefere sobre {@code /{id}}, corrigindo o erro
     * {@code For input string: "search"}.
     */
    @GetMapping("/search")
    public Page<CuidadorCardResponse> search(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CuidadorCardResponse> all =
                usuarioService.buscarProfissionais(specialty, minRating, lat, lng, radius);
        int from = Math.min(Math.max(page, 0) * Math.max(size, 1), all.size());
        int to = Math.min(from + Math.max(size, 1), all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(Math.max(page, 0), Math.max(size, 1)), all.size());
    }

    /**
     * Próprio usuário ou staff recebe {@link UsuarioResponse} completo;
     * outro autenticado vendo um profissional recebe o card público
     * (sem e-mail/CPF/telefone).
     */
    @GetMapping("/{id}")
    public Object get(@PathVariable Integer id, Authentication authentication) {
        Integer viewerId = SecurityUtils.currentUserId(authentication);
        if (viewerId.equals(id) || usuarioService.ehStaff(viewerId)) {
            return usuarioService.findForViewer(viewerId, id);
        }
        return usuarioService.verPerfilProfissional(id);
    }
}
