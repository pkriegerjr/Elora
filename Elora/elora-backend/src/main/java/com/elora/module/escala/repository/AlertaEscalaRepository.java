package com.elora.module.escala.repository;

import com.elora.module.escala.entity.AlertaEscala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaEscalaRepository extends JpaRepository<AlertaEscala, Long> { }
