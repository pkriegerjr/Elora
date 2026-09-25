package com.elora.module.busca.repository;

import com.elora.module.busca.entity.Favorito;
import com.elora.module.busca.entity.FavoritoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** MÓDULO BUSCA - Favoritos (PK composta impede duplicar no banco também). */
public interface FavoritoRepository extends JpaRepository<Favorito, FavoritoId> {

    List<Favorito> findByClienteIdOrderByCriadoEmDesc(Integer clienteId);

    boolean existsByClienteIdAndProfissionalId(Integer clienteId, Integer profissionalId);

    void deleteByClienteIdAndProfissionalId(Integer clienteId, Integer profissionalId);
}
