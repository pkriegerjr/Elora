package com.elora.module.juridico.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DisputaService {
    // @Autowired repository injetado via RequiredArgsConstructor
    @Cacheable(value="juridico")
    public Object buscar(Long id){ return null; }
    public Object criar(Object dto){ return null; }
}
