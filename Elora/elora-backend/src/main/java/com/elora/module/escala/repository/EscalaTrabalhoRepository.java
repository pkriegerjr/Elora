package com.elora.module.escala.repository;

import com.elora.module.escala.entity.EscalaTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscalaTrabalhoRepository extends JpaRepository<EscalaTrabalho, Long> { }
