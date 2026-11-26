package com.elora.module.juridico.repository;

import com.elora.module.juridico.entity.Denuncia;
import com.elora.module.juridico.enums.StatusDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** MÓDULO JURÍDICO - Consultas da fila de denúncias (schema v2). */
public interface DenunciaRepository extends JpaRepository<Denuncia, Integer> {

    List<Denuncia> findAllByOrderByCriadoEmDesc();

    List<Denuncia> findByDenunciante_IdOrderByCriadoEmDesc(Integer denuncianteId);

    List<Denuncia> findByDenunciado_IdOrderByCriadoEmDesc(Integer denunciadoId);

    List<Denuncia> findByStatusOrderByCriadoEmDesc(StatusDenuncia status);

    long countByStatus(StatusDenuncia status);
}
