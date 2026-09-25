package com.elora.module.avaliacao.repository;

import com.elora.module.avaliacao.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MÓDULO AVALIACAO - Repository Spring Data JPA.
 *
 * <p>Query methods (SQL gerado pelo nome, padrão do projeto): avaliações de
 * um cuidador (mais novas primeiro) + checagem do par único
 * (avaliador+avaliado). A unicidade também existe no banco (migration v2.3)
 * — app + banco, como pede o escopo (§4).</p>
 */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Integer> {

    // Avaliações RECEBIDAS pelo cuidador (mais novas primeiro).
    List<Avaliacao> findByAvaliado_IdOrderByCriadoEmDesc(Integer avaliadoId);

    // "Este usuário já avaliou este cuidador?" (regra do par único).
    boolean existsByAvaliador_IdAndAvaliado_Id(Integer avaliadorId, Integer avaliadoId);
}
