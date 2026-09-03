package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilAcessoRepository extends JpaRepository<PerfilAcesso, Long> { }
