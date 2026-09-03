package com.elora.module.usuario.controller;

import com.elora.module.usuario.dto.*;
import com.elora.module.usuario.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody UsuarioRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
