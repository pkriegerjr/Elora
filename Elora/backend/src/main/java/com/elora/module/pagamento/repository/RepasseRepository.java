package com.elora.module.pagamento.repository;

import com.elora.module.pagamento.entity.Repasse;
import com.elora.module.pagamento.enums.StatusRepasse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepasseRepository extends JpaRepository<Repasse, Integer> {

    List<Repasse> findByProfissionalId(Integer profissionalId);

    Optional<Repasse> findByPagamentoId(Integer pagamentoId);

    List<Repasse> findByStatus(StatusRepasse status);
}
