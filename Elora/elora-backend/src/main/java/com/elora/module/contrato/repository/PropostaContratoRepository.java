package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.PropostaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropostaContratoRepository extends JpaRepository<PropostaContrato, Long> { }
