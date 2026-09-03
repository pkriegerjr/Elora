package com.elora.module.relatorio.repository;

import com.elora.module.relatorio.entity.RelatorioJuridico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RelatorioJuridicoRepository extends JpaRepository<RelatorioJuridico, Long> { }
