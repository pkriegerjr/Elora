package com.elora.module.juridico.controller;

import com.elora.module.juridico.dto.*;
import com.elora.module.juridico.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/juridico")
@RequiredArgsConstructor
public class DisputaController {
    private final DisputaService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody DisputaRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
