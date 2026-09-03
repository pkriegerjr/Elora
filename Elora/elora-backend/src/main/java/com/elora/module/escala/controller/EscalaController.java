package com.elora.module.escala.controller;

import com.elora.module.escala.dto.*;
import com.elora.module.escala.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/escalas")
@RequiredArgsConstructor
public class EscalaController {
    private final EscalaService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody EscalaRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
