package com.elora.module.juridico.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.juridico.dto.AtualizarStatusDenunciaRequest;
import com.elora.module.juridico.dto.CriarDenunciaRequest;
import com.elora.module.juridico.dto.DenunciaResponse;
import com.elora.module.juridico.entity.Denuncia;
import com.elora.module.juridico.entity.ProcessoRescisao;
import com.elora.module.juridico.enums.StatusDenuncia;
import com.elora.module.juridico.mapper.DenunciaMapper;
import com.elora.module.juridico.repository.DenunciaRepository;
import com.elora.module.notificacao.service.AvisoSistemaService;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MÓDULO JURÍDICO - Denúncias persistidas na tabela v2 {@code denuncia}.
 *
 * <p>As rescisões seguem como stub em memória (próxima fase: mapear para
 * {@code contrato.status} + {@code disputa}); as denúncias não são mais
 * voláteis e passam a alimentar também o relatório SQL existente.</p>
 */
@Service
@RequiredArgsConstructor
public class JuridicoService {

    private static final Map<StatusDenuncia, Set<StatusDenuncia>> TRANSICOES = Map.of(
            StatusDenuncia.aberta,
            Set.of(StatusDenuncia.em_analise, StatusDenuncia.resolvida, StatusDenuncia.arquivada),
            StatusDenuncia.em_analise,
            Set.of(StatusDenuncia.aberta, StatusDenuncia.resolvida, StatusDenuncia.arquivada),
            StatusDenuncia.resolvida,
            Set.of(StatusDenuncia.arquivada, StatusDenuncia.aberta),
            StatusDenuncia.arquivada,
            Set.of(StatusDenuncia.aberta)
    );

    private final DenunciaRepository denuncias;
    private final ContratoRepository contratos;
    private final UsuarioService usuarioService;
    private final AvisoSistemaService avisos;

    // Rescisões: stub mantido até a próxima fase (Fase 5 do roadmap).
    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, ProcessoRescisao> rescisoes = new ConcurrentHashMap<>();

    @Transactional
    public DenunciaResponse criar(Integer denuncianteId, CriarDenunciaRequest req) {
        Usuario denunciante = usuarioService.getVisivel(denuncianteId);
        Usuario denunciado = usuarioService.getVisivel(req.getDenunciadoId());
        if (denunciante.getId().equals(denunciado.getId())) {
            throw new BusinessException("Você não pode denunciar a si mesmo");
        }

        Contrato contrato = null;
        if (req.getContratoId() != null) {
            contrato = contratos.findById(req.getContratoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
            Set<Integer> partes = Set.of(
                    contrato.getCliente().getId(), contrato.getProfissional().getId());
            if (!partes.contains(denunciante.getId()) || !partes.contains(denunciado.getId())) {
                throw new BusinessException("O contrato informado não envolve as partes da denúncia");
            }
        }

        String motivo = req.getMotivo() == null ? "" : req.getMotivo().trim();
        String descricao = req.getDescricao() == null ? "" : req.getDescricao().trim();
        if (motivo.isEmpty() || motivo.length() > 150) {
            throw new BusinessException("Informe um motivo de até 150 caracteres");
        }
        if (descricao.length() < 10) {
            throw new BusinessException("Descreva o ocorrido com pelo menos 10 caracteres");
        }

        Denuncia d = new Denuncia();
        d.setDenunciante(denunciante);
        d.setDenunciado(denunciado);
        d.setContrato(contrato);
        d.setMotivo(motivo);
        d.setDescricao(descricao);
        d.setStatus(StatusDenuncia.aberta);
        Denuncia salva = denuncias.save(d);
        avisos.notificarSeguro(AvisoSistemaService.JURIDICO,
                "Nova denúncia #" + salva.getId(),
                "Denúncia registrada por " + denunciante.getNome() + " — motivo: " + motivo
                        + ". Acesse o Painel de Denúncias para analisar.",
                "denuncia", salva.getId());
        return DenunciaMapper.toResponse(salva);
    }

    /** Fila completa (somente equipe; checagem de papel no controller). */
    @Transactional(readOnly = true)
    public List<DenunciaResponse> listarTodas() {
        return denuncias.findAllByOrderByCriadoEmDesc().stream()
                .map(DenunciaMapper::toResponse).toList();
    }

    /** Denúncias feitas pelo usuário logado. */
    @Transactional(readOnly = true)
    public List<DenunciaResponse> minhas(Integer denuncianteId) {
        return denuncias.findByDenunciante_IdOrderByCriadoEmDesc(denuncianteId).stream()
                .map(DenunciaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DenunciaResponse detalhar(Integer id, Integer viewerId) {
        Denuncia d = denuncias.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Denúncia não encontrada"));
        if (!usuarioService.ehStaff(viewerId) && !d.getDenunciante().getId().equals(viewerId)) {
            throw new ForbiddenException("Sem permissão para ver esta denúncia");
        }
        return DenunciaMapper.toResponse(d);
    }

    @Transactional
    public DenunciaResponse atualizarStatus(Integer id, AtualizarStatusDenunciaRequest req,
                                            Integer viewerId) {
        if (!usuarioService.ehStaff(viewerId)) {
            throw new ForbiddenException("Somente a equipe jurídica pode analisar denúncias");
        }
        Denuncia d = denuncias.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Denúncia não encontrada"));
        StatusDenuncia novo = req.getStatus();
        Set<StatusDenuncia> permitidos = TRANSICOES.getOrDefault(d.getStatus(), Set.of());
        if (!permitidos.contains(novo)) {
            throw new BusinessException("Transição de status inválida: "
                    + d.getStatus() + " → " + novo);
        }
        if (req.getParecer() != null && !req.getParecer().isBlank()) {
            d.setParecer(req.getParecer().trim());
        }
        if (novo == StatusDenuncia.aberta) {
            d.setAnalisadoPor(null);
        } else {
            d.setAnalisadoPor(usuarioService.getVisivel(viewerId));
        }
        d.setStatus(novo);
        Denuncia salva = denuncias.save(d);
        String rotulo = switch (novo) {
            case aberta -> "Aberta";
            case em_analise -> "Em análise";
            case resolvida -> "Resolvida";
            case arquivada -> "Arquivada";
        };
        String parecer = d.getParecer() == null || d.getParecer().isBlank()
                ? "" : " Parecer: " + d.getParecer();
        avisos.notificarSeguroUsuario(salva.getDenunciante().getId(),
                "Denúncia #" + salva.getId() + " — " + rotulo,
                "Sua denúncia foi atualizada para " + rotulo + "." + parecer,
                "denuncia", salva.getId());
        return DenunciaMapper.toResponse(salva);
    }

    /** Contadores do painel jurídico (compatível com o front atual). */
    @Transactional(readOnly = true)
    public Map<String, Object> painel() {
        long abertas = denuncias.countByStatus(StatusDenuncia.aberta);
        long emAnalise = denuncias.countByStatus(StatusDenuncia.em_analise);
        return Map.of(
                "analisesPendentes", abertas + emAnalise,
                "analisesTotal", denuncias.count(),
                "denunciasAbertas", abertas,
                "denunciasEmAnalise", emAnalise,
                "rescisoesTotal", rescisoes.size(),
                "validacaoPendentes", 0L,
                "validacaoEmAnalise", 0L,
                "rescisoesPendentes", 0L,
                "emDisputa", 0L
        );
    }

    public ProcessoRescisao solicitar(ProcessoRescisao r) {
        if (r.getIdAnalise() == null) {
            r.setIdAnalise(seq.getAndIncrement());
        }
        rescisoes.put(r.getIdAnalise(), r);
        return r;
    }

    /** Fila de rescisões (mais recentes primeiro). */
    public List<ProcessoRescisao> listarRescisoes() {
        return rescisoes.values().stream()
                .sorted(Comparator.comparing(ProcessoRescisao::getCriadoEm).reversed())
                .toList();
    }
}
