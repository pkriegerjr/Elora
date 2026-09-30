package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Integer> {

    Optional<RedefinicaoSenha> findByTokenHash(String tokenHash);
}
