package com.elora.module.financeiro.repository;

import com.elora.module.financeiro.entity.TaxaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxaServicoRepository extends JpaRepository<TaxaServico, Long> { }
