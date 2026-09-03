package com.elora.module.contrato.controller;

import com.elora.module.contrato.dto.*;
import com.elora.module.contrato.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {
    private final ContratoService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody ContratoRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
