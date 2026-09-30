package com.elora.module.pagamento.repository;

import com.elora.module.pagamento.entity.Pagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Integer> {

    /** RNF-011: replay com a mesma chave retorna o pagamento original. */
    Optional<Pagamento> findByIdempotencyKey(String idempotencyKey);

    List<Pagamento> findByPagadorId(Integer pagadorId);

    List<Pagamento> findByPagadorIdAndStatus(Integer pagadorId, StatusPagamento status);

    /** Pagamentos dos contratos de um profissional (vitrine do repasse). */
    @Query("SELECT p FROM Pagamento p WHERE p.contratoId IN " +
            "(SELECT c.id FROM Contrato c WHERE c.profissional.id = :profissionalId)")
    List<Pagamento> findByContratoProfissional(Integer profissionalId);
}
