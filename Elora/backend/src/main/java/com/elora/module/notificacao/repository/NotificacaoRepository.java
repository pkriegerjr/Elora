package com.elora.module.notificacao.repository;

import com.elora.module.notificacao.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {

    List<Notificacao> findByDestinatario_IdOrderByEnviadoEmDescIdDesc(Integer destinatarioId);

    List<Notificacao> findByDestinatario_IdAndLidaFalseOrderByEnviadoEmDescIdDesc(Integer destinatarioId);

    List<Notificacao> findByDestinatario_IdAndReferenciaTipoOrderByEnviadoEmDescIdDesc(
            Integer destinatarioId, String referenciaTipo);

    List<Notificacao> findByDestinatario_IdAndLidaFalseAndReferenciaTipoOrderByEnviadoEmDescIdDesc(
            Integer destinatarioId, String referenciaTipo);

    long countByDestinatario_IdAndLidaFalse(Integer destinatarioId);

    Optional<Notificacao> findByIdAndDestinatario_Id(Integer id, Integer destinatarioId);
}
