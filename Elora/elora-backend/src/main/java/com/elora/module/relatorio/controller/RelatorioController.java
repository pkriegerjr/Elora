package com.elora.module.relatorio.controller;

import com.elora.module.relatorio.dto.*;
import com.elora.module.relatorio.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {
    private final RelatorioService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody RelatorioRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
