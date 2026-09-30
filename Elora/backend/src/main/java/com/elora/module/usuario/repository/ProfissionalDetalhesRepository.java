package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.ProfissionalDetalhes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfissionalDetalhesRepository extends JpaRepository<ProfissionalDetalhes, Integer> {

    Optional<ProfissionalDetalhes> findByUsuarioId(Integer usuarioId);
}
