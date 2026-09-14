package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);

    Optional<Usuario> findByCpfAndDeletedAtIsNull(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}
