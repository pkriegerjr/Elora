package com.elora.module.escala.repository;

import com.elora.module.escala.entity.Disponibilidade;
import com.elora.module.escala.enums.PeriodoTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EscalaDisponibilidadeRepository
        extends JpaRepository<Disponibilidade, Integer> {

    Optional<Disponibilidade> findByUsuarioIdAndDataAndPeriodo(
            Integer usuarioId,
            LocalDate data,
            PeriodoTurno periodo);

    List<Disponibilidade> findByUsuarioId(Integer usuarioId);

    List<Disponibilidade> findByUsuarioIdAndDataBetween(
            Integer usuarioId,
            LocalDate inicio,
            LocalDate fim);
}
