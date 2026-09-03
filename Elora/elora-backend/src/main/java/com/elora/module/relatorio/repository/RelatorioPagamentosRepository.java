package com.elora.module.relatorio.repository;

import com.elora.module.relatorio.entity.RelatorioPagamentos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RelatorioPagamentosRepository extends JpaRepository<RelatorioPagamentos, Long> { }
