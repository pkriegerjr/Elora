package com.elora.module.escala.repository;

import com.elora.module.escala.entity.EscalaTrabalho;
import com.elora.module.escala.enums.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** MÓDULO ESCALA - Turnos por contrato (ordenados p/ a agenda do front). */
public interface EscalaTrabalhoRepository extends JpaRepository<EscalaTrabalho, Integer> {

    List<EscalaTrabalho> findByContratoIdOrderByDataAscPeriodoAsc(Integer contratoId);

    List<EscalaTrabalho> findByContratoIdAndDataBetweenOrderByDataAscPeriodoAsc(
            Integer contratoId, LocalDate inicio, LocalDate fim);

    boolean existsByContratoIdAndDataAndPeriodo(Integer contratoId, LocalDate data, Periodo periodo);
}
