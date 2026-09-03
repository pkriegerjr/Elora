package com.elora.module.usuario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {
    // @Autowired repository injetado via RequiredArgsConstructor
    @Cacheable(value="usuario")
    public Object buscar(Long id){ return null; }
    public Object criar(Object dto){ return null; }
}
