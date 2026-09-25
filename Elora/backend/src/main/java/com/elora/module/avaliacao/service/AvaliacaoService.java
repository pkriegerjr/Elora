package com.elora.module.avaliacao.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.avaliacao.dto.AvaliacaoResponse;
import com.elora.module.avaliacao.dto.AvaliacoesCuidadorResponse;
import com.elora.module.avaliacao.dto.CriarAvaliacaoRequest;
import com.elora.module.avaliacao.dto.MediaAvaliacaoResponse;
import com.elora.module.avaliacao.entity.Avaliacao;
import com.elora.module.avaliacao.mapper.AvaliacaoMapper;
import com.elora.module.avaliacao.repository.AvaliacaoRepository;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * MÓDULO AVALIACAO - Regras de negócio.
 *
 * <p>Regras (banco + escopo): avaliação nasce de um contrato; só o cliente
 * avalia (trigger); um usuário avalia um cuidador UMA vez (par único: app
 * aqui + UNIQUE no banco via migration v2.3); nota 1–5; comentário opcional.
 * Sem PUT (nada na documentação/schema sugere edição) e sem DELETE
 * (não pedido no escopo).</p>
 */
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private static final String PERFIL_PROFISSIONAL = "profissional";

    private final AvaliacaoRepository avaliacoes;
    private final ContratoRepository contratos;
    private final UsuarioService usuarioService;

    /** POST: cria (201). Avaliador = logado; avaliado = profissional do contrato. */
    @Transactional
    public AvaliacaoResponse criar(Integer viewerId, CriarAvaliacaoRequest req) {
        // Defesa em profundidade: o @Valid barra no controller (400), mas o
        // service também garante (testável sem camada web) → 422.
        if (req.getNota() == null || req.getNota() < 1 || req.getNota() > 5) {
            throw new BusinessException("Nota deve estar entre 1 e 5");
        }
        // Contrato precisa existir (404). O trigger exige o resto, mas aqui
        // devolvemos erros amigáveis antes do banco reclamar.
        Contrato contrato = contratos.findById(req.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        if (!contrato.getCliente().getId().equals(viewerId)) {
            throw new ForbiddenException("Somente o cliente do contrato pode avaliá-lo");
        }
        // Par único: este usuário já avaliou este cuidador? (422; UNIQUE no banco de backup).
        if (avaliacoes.existsByAvaliador_IdAndAvaliado_Id(viewerId, contrato.getProfissional().getId())) {
            throw new BusinessException("Você já avaliou este cuidador");
        }

        Avaliacao nova = new Avaliacao();
        nova.setContrato(contrato);
        nova.setAvaliador(contrato.getCliente());
        nova.setAvaliado(contrato.getProfissional());
        nova.setNota(req.getNota().shortValue()); // API Integer → SMALLINT
        nova.setComentario(req.getComentario() == null ? null : req.getComentario().trim());
        // nota_media do profissional: trigger do banco recalcula sozinho.
        return AvaliacaoMapper.toResponse(avaliacoes.save(nova));
    }

    /** GET /cuidador/{id}: agregado + itens (404 se não é cuidador). */
    @Transactional(readOnly = true)
    public AvaliacoesCuidadorResponse listarPorCuidador(Integer cuidadorId) {
        List<Avaliacao> base = avaliacoesDoCuidador(cuidadorId);
        MediaAvaliacaoResponse m = agregar(cuidadorId, base);
        List<AvaliacaoResponse> itens = base.stream().map(AvaliacaoMapper::toResponse).toList();
        return AvaliacaoMapper.toCuidadorResponse(
                cuidadorId, m.getMedia(), m.getTotalAvaliacoes(), itens);
    }

    /** GET /cuidador/{id}/media: soma + quantidade + média dos dados persistidos. */
    @Transactional(readOnly = true)
    public MediaAvaliacaoResponse mediaPorCuidador(Integer cuidadorId) {
        return agregar(cuidadorId, avaliacoesDoCuidador(cuidadorId));
    }

    // Valida o cuidador (existe + tem perfil) e carrega as avaliações dele.
    private List<Avaliacao> avaliacoesDoCuidador(Integer cuidadorId) {
        usuarioService.getVisivel(cuidadorId); // 404 se não existe/apagado
        if (!usuarioService.perfisDe(cuidadorId).contains(PERFIL_PROFISSIONAL)) {
            throw new ResourceNotFoundException("Cuidador não encontrado");
        }
        return avaliacoes.findByAvaliado_IdOrderByCriadoEmDesc(cuidadorId);
    }

    // Agregação pura em Java (consistente por construção: lê e calcula junto).
    // Ex.: 5+4+5+3=17, total 4 → 17/4 = 4.25. Vazio → soma 0, total 0, média 0.00.
    private MediaAvaliacaoResponse agregar(Integer cuidadorId, List<Avaliacao> base) {
        long soma = base.stream().mapToLong(a -> a.getNota()).sum();
        long total = base.size();
        BigDecimal media = total == 0
                ? new BigDecimal("0.00")
                : BigDecimal.valueOf(soma).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return AvaliacaoMapper.toMediaResponse(cuidadorId, soma, total, media);
    }
}
