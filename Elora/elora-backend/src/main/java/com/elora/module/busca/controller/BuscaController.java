package com.elora.module.busca.controller;

import com.elora.module.busca.dto.*;
import com.elora.module.busca.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/busca")
@RequiredArgsConstructor
public class BuscaController {
    private final BuscaService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody BuscaRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
