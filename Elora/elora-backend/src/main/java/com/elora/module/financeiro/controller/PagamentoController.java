package com.elora.module.financeiro.controller;

import com.elora.module.financeiro.dto.*;
import com.elora.module.financeiro.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {
    private final PagamentoService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody PagamentoRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
