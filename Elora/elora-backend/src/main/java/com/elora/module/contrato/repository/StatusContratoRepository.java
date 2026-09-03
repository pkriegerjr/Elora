package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.StatusContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusContratoRepository extends JpaRepository<StatusContrato, Long> { }
