package com.elora.module.conhecimento.repository;

import com.elora.module.conhecimento.entity.ArtigoConhecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtigoConhecimentoRepository extends JpaRepository<ArtigoConhecimento, Long> { }
