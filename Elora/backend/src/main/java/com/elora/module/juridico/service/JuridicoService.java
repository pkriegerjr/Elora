package com.elora.module.juridico.service;

import com.elora.module.juridico.entity.AnaliseJuridica;
import com.elora.module.juridico.entity.ProcessoRescisao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub in-memory até o módulo jurídico ser mapeado para as tabelas v2
 * (denuncia/disputa). Não toca no banco — seguro com ddl-auto=validate.
 */
@Service
@RequiredArgsConstructor
public class JuridicoService {

    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, AnaliseJuridica> analises = new ConcurrentHashMap<>();
    private final Map<Integer, ProcessoRescisao> rescisoes = new ConcurrentHashMap<>();

    public AnaliseJuridica analisar(AnaliseJuridica a) {
        if (a.getIdAnalise() == null) {
            a.setIdAnalise(seq.getAndIncrement());
        }
        analises.put(a.getIdAnalise(), a);
        return a;
    }

    public ProcessoRescisao solicitar(ProcessoRescisao r) {
        if (r.getIdAnalise() == null) {
            r.setIdAnalise(seq.getAndIncrement());
        }
        rescisoes.put(r.getIdAnalise(), r);
        return r;
    }

    /** Fila de denúncias/análises (mais recentes primeiro). */
    public List<AnaliseJuridica> listarAnalises() {
        return analises.values().stream()
                .sorted(Comparator.comparing(AnaliseJuridica::getCriadoEm).reversed())
                .toList();
    }

    /** Fila de rescisões (mais recentes primeiro). */
    public List<ProcessoRescisao> listarRescisoes() {
        return rescisoes.values().stream()
                .sorted(Comparator.comparing(ProcessoRescisao::getCriadoEm).reversed())
                .toList();
    }

    public Map<String, Object> painel() {
        long pendentes = analises.values().stream()
                .filter(a -> "pendente".equalsIgnoreCase(a.getStatusAnalise()))
                .count();
        return Map.of(
                "analisesPendentes", pendentes,
                "analisesTotal", analises.size(),
                "rescisoesTotal", rescisoes.size()
        );
    }
}
