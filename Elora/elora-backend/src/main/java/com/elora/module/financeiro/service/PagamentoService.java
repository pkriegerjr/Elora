package com.elora.module.financeiro.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PagamentoService {
    // @Autowired repository injetado via RequiredArgsConstructor
    @Cacheable(value="financeiro")
    public Object buscar(Long id){ return null; }
    public Object criar(Object dto){ return null; }
}
