package com.elora.module.profissional.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfissionalService {
    // @Autowired repository injetado via RequiredArgsConstructor
    @Cacheable(value="profissional")
    public Object buscar(Long id){ return null; }
    public Object criar(Object dto){ return null; }
}
