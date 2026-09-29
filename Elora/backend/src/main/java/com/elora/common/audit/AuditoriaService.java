package com.elora.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto único de escrita da trilha de auditoria (RNF-012).
 * Ações seguem o padrão {@code entidade.operacao} (ex.: pagamento.aprovar).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final TrilhaAuditoriaRepository auditoria;

    @Transactional
    public void registrar(Integer atorId, String acao, String entidade, Integer entidadeId, String ip) {
        TrilhaAuditoria t = new TrilhaAuditoria();
        t.setAtorId(atorId);
        t.setAcao(acao);
        t.setEntidade(entidade);
        t.setEntidadeId(entidadeId == null ? null : String.valueOf(entidadeId));
        t.setIp(ip);
        auditoria.save(t);
    }
}
