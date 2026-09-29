package com.elora.module.contrato.repository;

import com.elora.module.contrato.entity.AssinaturaContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssinaturaContratoRepository extends JpaRepository<AssinaturaContrato, Integer> {

    List<AssinaturaContrato> findByContratoId(Integer contratoId);

    boolean existsByContratoIdAndUsuario_IdAndPapel(Integer contratoId, Integer usuarioId, String papel);
}
