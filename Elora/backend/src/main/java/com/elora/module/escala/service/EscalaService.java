package com.elora.module.escala.service;

import com.elora.common.audit.AuditoriaService;
import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.CriarEscalaRequest;
import com.elora.module.escala.dto.DefinirDisponibilidadeRequest;
import com.elora.module.escala.dto.DisponibilidadeResponse;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.entity.Disponibilidade;
import com.elora.module.escala.entity.EscalaTrabalho;
import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusDisponibilidade;
import com.elora.module.escala.enums.StatusEscala;
import com.elora.module.escala.repository.EscalaDisponibilidadeRepository;
import com.elora.module.escala.repository.EscalaTrabalhoRepository;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REQ-013: turnos (escala_trabalho, por contrato) + quadro do profissional
 * (disponibilidade). Alteração de turno dispara alerta à outra parte
 * (REQ-013 + REQ-020) via {@code notificarSistema}.
 */
@Service
@RequiredArgsConstructor
public class EscalaService {

    private static final Set<String> FINANCEIRO = Set.of("admin", "financeiro");

    private final EscalaTrabalhoRepository escalas;
    private final EscalaDisponibilidadeRepository disponibilidades;
    private final ContratoRepository contratos;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;

    // ------------------------------------------------------------------
    // Escalas (turnos do contrato)
    // ------------------------------------------------------------------

    @Transactional
    public EscalaResponse criarEscala(Integer viewerId, CriarEscalaRequest req, String ip) {
        Contrato contrato = contratoDo(req.getContratoId());
        exigirParteOuStaff(viewerId, contrato, "Só as partes do contrato definem escalas");
        if (escalas.existsByContratoIdAndDataAndPeriodo(
                contrato.getId(), req.getData(), req.getPeriodo())) {
            throw new BusinessException("Turno já existe neste contrato, data e período");
        }
        EscalaTrabalho e = new EscalaTrabalho();
        e.setContratoId(contrato.getId());
        e.setData(req.getData());
        e.setPeriodo(req.getPeriodo());
        e.setStatus(StatusEscala.prevista);
        EscalaTrabalho salva = escalas.save(e);
        auditoria.registrar(viewerId, "escala.criar", "escala_trabalho", salva.getId(), ip);
        alertarOutraParte(viewerId, contrato,
                "Nova escala em " + req.getData() + " (" + req.getPeriodo() + ")",
                salva.getId());
        return mapear(salva);
    }

    @Transactional
    public EscalaResponse atualizarEscala(Integer viewerId, Integer escalaId,
                                         AtualizarEscalaRequest req, String ip) {
        EscalaTrabalho e = escalas.findById(escalaId)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada"));
        Contrato contrato = contratoDo(e.getContratoId());
        exigirParteOuStaff(viewerId, contrato, "Só as partes do contrato alteram escalas");
        if (e.getStatus() != StatusEscala.prevista) {
            throw new BusinessException("Escala " + e.getStatus() + " não pode ser alterada");
        }
        boolean mudou = false;
        LocalDate novaData = req.getData() != null ? req.getData() : e.getData();
        PeriodoTurno novoPeriodo = req.getPeriodo() != null ? req.getPeriodo() : e.getPeriodo();
        if (!novaData.equals(e.getData()) || novoPeriodo != e.getPeriodo()) {
            if (escalas.existsByContratoIdAndDataAndPeriodo(contrato.getId(), novaData, novoPeriodo)) {
                throw new BusinessException("Turno já existe neste contrato, data e período");
            }
            e.setData(novaData);
            e.setPeriodo(novoPeriodo);
            mudou = true;
        }
        if (req.getStatus() != null && req.getStatus() != e.getStatus()) {
            e.setStatus(req.getStatus());
            mudou = true;
        }
        if (!mudou) {
            return mapear(e);
        }
        EscalaTrabalho salva = escalas.save(e);
        auditoria.registrar(viewerId, "escala.atualizar", "escala_trabalho", salva.getId(), ip);
        alertarOutraParte(viewerId, contrato,
                "Escala alterada: " + salva.getData() + " (" + salva.getPeriodo()
                        + ") → " + salva.getStatus(),
                salva.getId());
        return mapear(salva);
    }

    @Transactional(readOnly = true)
    public List<EscalaResponse> listarPorContrato(Integer viewerId, Integer contratoId,
                                                 LocalDate inicio, LocalDate fim) {
        Contrato contrato = contratoDo(contratoId);
        exigirParteOuStaff(viewerId, contrato, "Só as partes do contrato veem suas escalas");
        List<EscalaTrabalho> base = (inicio != null && fim != null)
                ? escalas.findByContratoIdAndDataBetween(contratoId, inicio, fim)
                : escalas.findByContratoId(contratoId);
        return base.stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<EscalaResponse> listarPorProfissional(Integer viewerId, Integer profissionalId,
                                                     LocalDate inicio, LocalDate fim) {
        List<String> perfis = usuarioService.perfisDe(viewerId);
        boolean staff = perfis.stream().anyMatch(FINANCEIRO::contains);
        boolean mesmo = viewerId.equals(profissionalId);
        boolean contratante = contratos.existsByCliente_IdAndProfissional_Id(viewerId, profissionalId);
        if (!staff && !mesmo && !contratante) {
            throw new ForbiddenException("Escalas visíveis ao profissional, contratantes e equipe");
        }
        List<EscalaTrabalho> todas = (inicio != null && fim != null)
                ? escalas.findAll().stream()
                        .filter(e -> e.getData() != null
                                && !e.getData().isBefore(inicio) && !e.getData().isAfter(fim))
                        .toList()
                : escalas.findAll();
        return todas.stream()
                .filter(e -> {
                    Contrato c = contratos.findById(e.getContratoId()).orElse(null);
                    return c != null && c.getProfissional().getId().equals(profissionalId);
                })
                .map(this::mapear).toList();
    }

    // ------------------------------------------------------------------
    // Disponibilidade (quadro do profissional)
    // ------------------------------------------------------------------

    @Transactional
    public DisponibilidadeResponse definirDisponibilidade(Integer viewerId,
                                                         DefinirDisponibilidadeRequest req, String ip) {
        Integer alvo = req.getProfissionalId() == null ? viewerId : req.getProfissionalId();
        List<String> perfis = usuarioService.perfisDe(viewerId);
        boolean staff = perfis.stream().anyMatch(FINANCEIRO::contains);
        if (!alvo.equals(viewerId) && !staff) {
            throw new ForbiddenException("Só o profissional ou a equipe altera disponibilidade");
        }
        if (!usuarioService.perfisDe(alvo).contains("profissional") && !staff) {
            throw new BusinessException("Disponibilidade é do profissional");
        }
        Optional<Disponibilidade> existente = disponibilidades
                .findByUsuarioIdAndDataAndPeriodo(alvo, req.getData(), req.getPeriodo());
        Disponibilidade d = existente.orElseGet(Disponibilidade::new);
        boolean novo = d.getId() == null;
        d.setUsuarioId(alvo);
        d.setData(req.getData());
        d.setPeriodo(req.getPeriodo());
        d.setStatus(req.getStatus() == null ? StatusDisponibilidade.disponivel : req.getStatus());
        Disponibilidade salva = disponibilidades.save(d);
        auditoria.registrar(viewerId,
                novo ? "disponibilidade.criar" : "disponibilidade.atualizar",
                "disponibilidade", salva.getId(), ip);
        return mapear(salva);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeResponse> listarDisponibilidade(Integer viewerId, Integer profissionalId,
                                                              LocalDate inicio, LocalDate fim) {
        Integer alvo = profissionalId == null ? viewerId : profissionalId;
        List<Disponibilidade> base = (inicio != null && fim != null)
                ? disponibilidades.findByUsuarioIdAndDataBetween(alvo, inicio, fim)
                : disponibilidades.findByUsuarioId(alvo);
        return base.stream().map(this::mapear).toList();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Contrato contratoDo(Integer contratoId) {
        return contratos.findById(contratoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
    }

    private void exigirParteOuStaff(Integer viewerId, Contrato contrato, String mensagem) {
        boolean parte = viewerId.equals(contrato.getCliente().getId())
                || viewerId.equals(contrato.getProfissional().getId());
        boolean staff = usuarioService.perfisDe(viewerId).stream().anyMatch(FINANCEIRO::contains);
        if (!parte && !staff) {
            throw new ForbiddenException(mensagem);
        }
    }

    private void alertarOutraParte(Integer autorId, Contrato contrato, String texto, Integer escalaId) {
        Integer outro = autorId.equals(contrato.getCliente().getId())
                ? contrato.getProfissional().getId() : contrato.getCliente().getId();
        notificacoes.notificarSistema(outro, "Alteração de escala", texto, "escala", escalaId);
    }

    private EscalaResponse mapear(EscalaTrabalho e) {
        return new EscalaResponse(e.getId(), e.getContratoId(), e.getData(), e.getPeriodo(), e.getStatus());
    }

    private DisponibilidadeResponse mapear(Disponibilidade d) {
        return new DisponibilidadeResponse(
                d.getId(), d.getUsuarioId(), d.getData(), d.getPeriodo(), d.getStatus(), d.getCriadoEm());
    }
}
