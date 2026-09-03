package com.elora.module.conhecimento.controller;

import com.elora.module.conhecimento.dto.*;
import com.elora.module.conhecimento.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conhecimento")
@RequiredArgsConstructor
public class FAQController {
    private final FAQService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id){ return ResponseEntity.ok(service.buscar(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','PROFISSIONAL','ADMIN')")
    public ResponseEntity<?> criar(@RequestBody FAQRequestDTO dto){ return ResponseEntity.ok(service.criar(dto)); }
}
