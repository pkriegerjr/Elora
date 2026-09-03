package com.elora.module.juridico.repository;

import com.elora.module.juridico.entity.PainelJuridico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface PainelJuridicoRepository extends JpaRepository<PainelJuridico, Long> { }
