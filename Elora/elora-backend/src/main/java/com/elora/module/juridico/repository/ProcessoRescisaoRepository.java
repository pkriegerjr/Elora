package com.elora.module.juridico.repository;

import com.elora.module.juridico.entity.ProcessoRescisao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ProcessoRescisaoRepository extends JpaRepository<ProcessoRescisao, Long> { }
