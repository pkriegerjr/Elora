package com.elora.module.escala.repository;

import com.elora.module.escala.entity.EscalaTrabalho;
import com.elora.module.escala.enums.PeriodoTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EscalaTrabalhoRepository extends JpaRepository<EscalaTrabalho, Integer> {

    boolean existsByContratoIdAndDataAndPeriodo(Integer contratoId, LocalDate data,
                                               PeriodoTurno periodo);

    List<EscalaTrabalho> findByContratoId(Integer contratoId);

    List<EscalaTrabalho> findByContratoIdAndDataBetween(Integer contratoId, LocalDate inicio, LocalDate fim);
}
