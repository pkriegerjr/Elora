package com.elora.module.busca.repository;

import com.elora.module.busca.entity.Disponibilidade;
import com.elora.module.busca.enums.Periodo;
import com.elora.module.busca.enums.StatusDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** MÓDULO BUSCA - Agenda: quem está livre num dia/turno. */
public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Integer> {

    List<Disponibilidade> findByDataAndPeriodoAndStatus(
            LocalDate data, Periodo periodo, StatusDisponibilidade status);
}
