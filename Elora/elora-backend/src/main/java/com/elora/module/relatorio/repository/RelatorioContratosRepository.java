package com.elora.module.relatorio.repository;

import com.elora.module.relatorio.entity.RelatorioContratos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RelatorioContratosRepository extends JpaRepository<RelatorioContratos, Long> { }
