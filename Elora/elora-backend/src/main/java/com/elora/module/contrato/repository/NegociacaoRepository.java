package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> { }
