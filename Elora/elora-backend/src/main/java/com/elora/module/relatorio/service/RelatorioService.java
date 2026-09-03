package com.elora.module.relatorio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RelatorioService {
    // @Autowired repository injetado via RequiredArgsConstructor
    @Cacheable(value="relatorio")
    public Object buscar(Long id){ return null; }
    public Object criar(Object dto){ return null; }
}
