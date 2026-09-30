package com.elora.module.usuario.controller;

import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.service.UsuarioService;
import com.elora.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClienteController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody ClienteRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registerCliente(request));
    }

    @GetMapping("/{id}")
    public UsuarioResponse get(@PathVariable Integer id, Authentication authentication) {
        return usuarioService.findForViewer(SecurityUtils.currentUserId(authentication), id);
    }
}
