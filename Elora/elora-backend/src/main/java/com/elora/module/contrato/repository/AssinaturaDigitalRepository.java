package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.AssinaturaDigital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssinaturaDigitalRepository extends JpaRepository<AssinaturaDigital, Long> { }
