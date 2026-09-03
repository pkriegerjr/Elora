package com.elora.module.avaliacao.repository;

import com.elora.module.avaliacao.entity.HistoricoDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoDenunciaRepository extends JpaRepository<HistoricoDenuncia, Long> { }
