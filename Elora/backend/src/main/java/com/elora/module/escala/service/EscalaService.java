package com.elora.module.escala.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.dto.GerarEscalaRequest;
import com.elora.module.escala.entity.EscalaTrabalho;
import com.elora.module.escala.enums.Periodo;
import com.elora.module.escala.enums.StatusEscala;
import com.elora.module.escala.repository.EscalaTrabalhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MÓDULO ESCALA - Agenda de turnos do contrato (REQ-ELO-013).
 *
 * <p>Decisões:</p>
 * <ul>
 *   <li>Geração <b>idempotente</b>: turno (contrato, data, período) que já
 *       existe é pulado — rodar 2x não duplica (a UNIQUE do banco é o
 *       backstop contra corrida).</li>
 *   <li>Sem dependência do módulo contrato (inexistente): {@code contratoId}
 *       é número puro e a FK do banco valida — violação vira 422, não 500.</li>
 *   <li>Sem DELETE físico: desfazer é mudar para {@code cancelada} (o
 *       histórico da escala é auditoria do serviço prestado).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EscalaService {

    private static final long INTERVALO_MAX_DIAS = 370;

    private final EscalaTrabalhoRepository escalas;

    /**
     * Gera os turnos do intervalo (inclusive) para cada período.
     * Retorna só o que foi criado nesta chamada.
     */
    @Transactional
    public List<EscalaResponse> gerar(GerarEscalaRequest req) {
        if (req.getDataFim().isBefore(req.getDataInicio())) {
            throw new BusinessException("dataFim não pode ser anterior a dataInicio");
        }
        if (req.getDataInicio().plusDays(INTERVALO_MAX_DIAS).isBefore(req.getDataFim())) {
            throw new BusinessException("intervalo máximo de " + INTERVALO_MAX_DIAS + " dias");
        }
        Set<Periodo> periodos = new LinkedHashSet<>(req.getPeriodos());

        List<EscalaResponse> criadas = new ArrayList<>();
        for (LocalDate dia = req.getDataInicio(); !dia.isAfter(req.getDataFim()); dia = dia.plusDays(1)) {
            for (Periodo periodo : periodos) {
                if (escalas.existsByContratoIdAndDataAndPeriodo(req.getContratoId(), dia, periodo)) {
                    continue;
                }
                EscalaTrabalho turno = new EscalaTrabalho();
                turno.setContratoId(req.getContratoId());
                turno.setData(dia);
                turno.setPeriodo(periodo);
                turno.setStatus(StatusEscala.prevista);
                try {
                    criadas.add(toResponse(escalas.save(turno)));
                } catch (DataIntegrityViolationException e) {
                    // Corrida entre exists e save, ou contrato inexistente (FK).
                    throw new BusinessException("contrato inexistente ou turno já gerado");
                }
            }
        }
        return criadas;
    }

    /** Agenda do contrato, opcionalmente recortada por período. */
    @Transactional(readOnly = true)
    public List<EscalaResponse> listar(Integer contratoId, LocalDate inicio, LocalDate fim) {
        List<EscalaTrabalho> base = (inicio == null || fim == null)
                ? escalas.findByContratoIdOrderByDataAscPeriodoAsc(contratoId)
                : escalas.findByContratoIdAndDataBetweenOrderByDataAscPeriodoAsc(contratoId, inicio, fim);
        return base.stream().map(this::toResponse).toList();
    }

    /** Avança o turno na máquina de estados (só transições permitidas). */
    @Transactional
    public EscalaResponse atualizarStatus(Integer id, AtualizarEscalaRequest req) {
        EscalaTrabalho turno = escalas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno não encontrado"));
        if (!turno.getStatus().podeMudarPara(req.getStatus())) {
            throw new BusinessException("transição de " + turno.getStatus().name()
                    + " para " + req.getStatus().name() + " não permitida");
        }
        turno.setStatus(req.getStatus());
        return toResponse(escalas.save(turno));
    }

    private EscalaResponse toResponse(EscalaTrabalho t) {
        return new EscalaResponse(t.getId(), t.getContratoId(), t.getData(), t.getPeriodo(), t.getStatus());
    }
}
