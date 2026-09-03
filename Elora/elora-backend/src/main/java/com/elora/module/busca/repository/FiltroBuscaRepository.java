package com.elora.module.busca.repository;

import com.elora.module.busca.entity.FiltroBusca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FiltroBuscaRepository extends JpaRepository<FiltroBusca, Long> { }
