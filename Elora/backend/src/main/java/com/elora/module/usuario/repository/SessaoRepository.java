package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, Integer> {

    Optional<Sessao> findByRefreshHashAndRevogadaFalse(String refreshHash);
}
