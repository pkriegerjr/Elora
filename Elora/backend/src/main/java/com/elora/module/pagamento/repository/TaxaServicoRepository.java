package com.elora.module.pagamento.repository;

import com.elora.module.pagamento.entity.TaxaServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxaServicoRepository extends JpaRepository<TaxaServico, Integer> {

    List<TaxaServico> findByAtivoTrueOrderByVigenteDeDesc();
}
