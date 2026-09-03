package com.elora.module.conhecimento.repository;

import com.elora.module.conhecimento.entity.CategoriaConteudo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaConteudoRepository extends JpaRepository<CategoriaConteudo, Long> { }
