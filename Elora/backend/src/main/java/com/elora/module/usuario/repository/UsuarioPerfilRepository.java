package com.elora.module.usuario.repository;

import com.elora.module.usuario.entity.UsuarioPerfil;
import com.elora.module.usuario.entity.UsuarioPerfilId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UsuarioPerfilRepository extends JpaRepository<UsuarioPerfil, UsuarioPerfilId> {

    List<UsuarioPerfil> findByUsuario_Id(Integer usuarioId);

    List<UsuarioPerfil> findByPerfil_NomeIn(Collection<String> nomesPerfis);
}
