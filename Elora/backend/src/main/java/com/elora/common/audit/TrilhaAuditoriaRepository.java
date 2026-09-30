package com.elora.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Trilha de auditoria (RNF-012): só escrita pelo {@link AuditoriaService},
 * leitura via SQL/relatórios.
 */
public interface TrilhaAuditoriaRepository extends JpaRepository<TrilhaAuditoria, Long> {
}
