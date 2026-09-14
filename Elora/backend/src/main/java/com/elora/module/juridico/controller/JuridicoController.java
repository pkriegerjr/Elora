package com.elora.module.juridico.controller;

import com.elora.module.juridico.entity.AnaliseJuridica;
import com.elora.module.juridico.entity.ProcessoRescisao;
import com.elora.module.juridico.service.JuridicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Base path SEM prefixo /api: o context-path já é /api
 * (ver application.properties → server.servlet.context-path=/api).
 * Com /api aqui, a rota virava /api/api/juridico (404 no front).
 */
@RestController
@RequestMapping("/juridico")
@RequiredArgsConstructor
public class JuridicoController {

    private final JuridicoService service;

    @PostMapping("/analise")
    public AnaliseJuridica criarAnalise(@RequestBody @Valid AnaliseJuridica a) {
        return service.analisar(a);
    }

    @PostMapping("/rescisoes")
    public ProcessoRescisao criarRescisao(@RequestBody @Valid ProcessoRescisao r) {
        return service.solicitar(r);
    }

    @GetMapping("/painel")
    public Map<String, Object> painel() {
        return service.painel();
    }
}
