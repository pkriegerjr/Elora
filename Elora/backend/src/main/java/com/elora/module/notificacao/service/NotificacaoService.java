package com.elora.module.notificacao.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.notificacao.dto.CriarNotificacaoRequest;
import com.elora.module.notificacao.dto.NotificacaoResponse;
import com.elora.module.notificacao.dto.PreferenciaResponse;
import com.elora.module.notificacao.dto.PreferenciasRequest;
import com.elora.module.notificacao.entity.Notificacao;
import com.elora.module.notificacao.entity.PreferenciaNotificacao;
import com.elora.module.notificacao.enums.Canal;
import com.elora.module.notificacao.repository.NotificacaoRepository;
import com.elora.module.notificacao.repository.PreferenciaNotificacaoRepository;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notificações sobre o schema v2. Leitura/escrita sempre no escopo do dono,
 * exceto envio (staff). Disparo push/e-mail real = integração futura.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;
    private final PreferenciaNotificacaoRepository preferencias;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(Integer viewerId, boolean unreadOnly, String tipo, int limit) {
        List<Notificacao> base;
        if (unreadOnly && tipo != null) {
            base = notificacoes.findByDestinatario_IdAndLidaFalseAndReferenciaTipoOrderByEnviadoEmDescIdDesc(viewerId, tipo);
        } else if (unreadOnly) {
            base = notificacoes.findByDestinatario_IdAndLidaFalseOrderByEnviadoEmDescIdDesc(viewerId);
        } else if (tipo != null) {
            base = notificacoes.findByDestinatario_IdAndReferenciaTipoOrderByEnviadoEmDescIdDesc(viewerId, tipo);
        } else {
            base = notificacoes.findByDestinatario_IdOrderByEnviadoEmDescIdDesc(viewerId);
        }
        return base.stream().limit(limit).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Integer viewerId) {
        return notificacoes.countByDestinatario_IdAndLidaFalse(viewerId);
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Integer viewerId, Integer id, boolean lida) {
        Notificacao notificacao = notificacoes.findByIdAndDestinatario_Id(id, viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        notificacao.setLida(lida);
        notificacao.setLidaEm(lida ? LocalDateTime.now() : null);
        return toResponse(notificacoes.save(notificacao));
    }

    @Transactional
    public long marcarTodasComoLidas(Integer viewerId) {
        List<Notificacao> pendentes =
                notificacoes.findByDestinatario_IdAndLidaFalseOrderByEnviadoEmDescIdDesc(viewerId);
        LocalDateTime agora = LocalDateTime.now();
        pendentes.forEach(n -> {
            n.setLida(true);
            n.setLidaEm(agora);
        });
        notificacoes.saveAll(pendentes);
        return pendentes.size();
    }

    @Transactional
    public void excluir(Integer viewerId, Integer id) {
        Notificacao notificacao = notificacoes.findByIdAndDestinatario_Id(id, viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        notificacoes.delete(notificacao);
    }

    @Transactional
    public NotificacaoResponse enviar(Integer staffId, CriarNotificacaoRequest req) {
        exigirStaff(staffId);
        if (req.getDestinatarioId() == null) {
            throw new BusinessException("destinatarioId é obrigatório");
        }
        Usuario destinatario = usuarioService.getVisivel(req.getDestinatarioId());

        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatario(destinatario);
        notificacao.setCanal(req.getCanal() != null ? req.getCanal() : Canal.sistema);
        notificacao.setTitulo(req.getTitulo().trim());
        notificacao.setCorpo(req.getCorpo());
        notificacao.setReferenciaTipo(req.getReferenciaTipo());
        notificacao.setReferenciaId(req.getReferenciaId());
        notificacao.setLida(false);
        return toResponse(notificacoes.save(notificacao));
    }

    @Transactional
    public List<NotificacaoResponse> enviarEmLote(Integer staffId, List<Integer> userIds, CriarNotificacaoRequest base) {
        exigirStaff(staffId);
        return userIds.stream().map(id -> {
            CriarNotificacaoRequest item = new CriarNotificacaoRequest();
            item.setDestinatarioId(id);
            item.setCanal(base.getCanal());
            item.setTitulo(base.getTitulo());
            item.setCorpo(base.getCorpo());
            item.setReferenciaTipo(base.getReferenciaTipo());
            item.setReferenciaId(base.getReferenciaId());
            return enviar(staffId, item);
        }).toList();
    }

    @Transactional
    public PreferenciaResponse obterPreferencias(Integer viewerId) {
        return toResponse(obterOuCriar(viewerId));
    }

    @Transactional
    public PreferenciaResponse atualizarPreferencias(Integer viewerId, PreferenciasRequest req) {
        PreferenciaNotificacao pref = obterOuCriar(viewerId);
        if (req.getPush() != null) {
            pref.setPush(req.getPush());
        }
        if (req.getEmail() != null) {
            pref.setEmail(req.getEmail());
        }
        if (req.getSms() != null) {
            pref.setSms(req.getSms());
        }
        return toResponse(preferencias.save(pref));
    }

    private void exigirStaff(Integer usuarioId) {
        if (!usuarioService.ehStaff(usuarioId)) {
            throw new ForbiddenException("Envio de notificações restrito à equipe Elora");
        }
    }

    private PreferenciaNotificacao obterOuCriar(Integer usuarioId) {
        return preferencias.findById(usuarioId).orElseGet(() -> {
            PreferenciaNotificacao pref = new PreferenciaNotificacao();
            pref.setUsuario(usuarioService.getVisivel(usuarioId));
            pref.setPush(true);
            pref.setEmail(true);
            pref.setSms(false);
            return preferencias.save(pref);
        });
    }

    private NotificacaoResponse toResponse(Notificacao n) {
        return new NotificacaoResponse(
                n.getId(),
                n.getDestinatario().getId(),
                n.getCanal(),
                n.getTitulo(),
                n.getCorpo(),
                n.getReferenciaTipo(),
                n.getReferenciaId(),
                n.getLida(),
                n.getLidaEm(),
                n.getEnviadoEm()
        );
    }

    private PreferenciaResponse toResponse(PreferenciaNotificacao p) {
        return new PreferenciaResponse(p.getPush(), p.getEmail(), p.getSms(), p.getAtualizadoEm());
    }
}
