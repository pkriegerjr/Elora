package com.elora.module.busca.repository;

import com.elora.module.busca.entity.UsuarioEspecialidade;
import com.elora.module.busca.entity.UsuarioEspecialidadeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MÓDULO BUSCA - Vínculos N:N. Responde: "que tags ele tem?" e
 * "quem tem esta tag?" (filtro qualitativo).
 */
public interface UsuarioEspecialidadeRepository extends JpaRepository<UsuarioEspecialidade, UsuarioEspecialidadeId> {

    List<UsuarioEspecialidade> findByUsuarioId(Integer usuarioId);

    List<UsuarioEspecialidade> findByEspecialidadeId(Integer especialidadeId);
}
