package com.elora.module.busca.repository;

import com.elora.module.busca.entity.ResultadoBusca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoBuscaRepository extends JpaRepository<ResultadoBusca, Long> { }
