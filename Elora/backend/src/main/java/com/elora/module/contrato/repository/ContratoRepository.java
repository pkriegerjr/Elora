package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.enums.StatusContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MÓDULO CONTRATO - Repository Spring Data JPA (query methods, sem SQL).
 * Escopo do dono (por ID + parte) igual ao das notificações.
 */
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    List<Contrato> findByCliente_IdOrderByCriadoEmDesc(Integer clienteId);

    List<Contrato> findByProfissional_IdOrderByCriadoEmDesc(Integer profissionalId);

    Optional<Contrato> findByIdAndCliente_Id(Integer id, Integer clienteId);

    Optional<Contrato> findByIdAndProfissional_Id(Integer id, Integer profissionalId);

    List<Contrato> findByStatusOrderByCriadoEmDesc(StatusContrato status);

    boolean existsByCodigo(String codigo);
}
