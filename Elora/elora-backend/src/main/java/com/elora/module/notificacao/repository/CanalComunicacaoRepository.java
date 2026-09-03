package com.elora.module.notificacao.repository;

import com.elora.module.notificacao.entity.CanalComunicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CanalComunicacaoRepository extends JpaRepository<CanalComunicacao, Long> { }
