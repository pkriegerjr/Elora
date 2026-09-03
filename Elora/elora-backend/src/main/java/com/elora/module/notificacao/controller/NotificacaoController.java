package com.elora.module.notificacao.controller;

import com.elora.module.notificacao.dto.*;
import com.elora.module.notificacao.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody NotificacaoRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
