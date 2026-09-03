package com.elora.module.relatorio.repository;

import com.elora.module.relatorio.entity.RelatorioUsuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RelatorioUsuariosRepository extends JpaRepository<RelatorioUsuarios, Long> { }
