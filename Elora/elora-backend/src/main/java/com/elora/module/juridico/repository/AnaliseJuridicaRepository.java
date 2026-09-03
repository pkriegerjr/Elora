package com.elora.module.juridico.repository;

import com.elora.module.juridico.entity.AnaliseJuridica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AnaliseJuridicaRepository extends JpaRepository<AnaliseJuridica, Long> { }
