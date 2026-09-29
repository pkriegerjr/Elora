package com.elora.module.pagamento.service;

import com.elora.common.audit.AuditoriaService;
import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.dto.EstornarRequest;
import com.elora.module.pagamento.dto.PagarRequest;
import com.elora.module.pagamento.dto.PagamentoResponse;
import com.elora.module.pagamento.dto.RepasseResponse;
import com.elora.module.pagamento.entity.Pagamento;
import com.elora.module.pagamento.entity.Repasse;
import com.elora.module.pagamento.entity.TaxaServico;
import com.elora.module.pagamento.enums.StatusPagamento;
import com.elora.module.pagamento.enums.StatusRepasse;
import com.elora.module.pagamento.gateway.GatewayCobrancaRequest;
import com.elora.module.pagamento.gateway.GatewayCobrancaResult;
import com.elora.module.pagamento.gateway.GatewayPagamentoService;
import com.elora.module.pagamento.repository.PagamentoRepository;
import com.elora.module.pagamento.repository.RepasseRepository;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * REQ-007 (cliente paga à Elora) + REQ-011 (repasse = bruto − taxa) +
 * RNF-011 (idempotência por {@code idempotency_key}).
 * Gateway é mock determinístico: só {@code "tok_recusado"} recusa.
 * Produção: trocar o trecho de aprovação por client do gateway real
 * (o restante — valores, repasse, auditoria — continua igual).
 */
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private static final Set<String> FINANCEIRO = Set.of("admin", "financeiro");

    private final PagamentoRepository pagamentos;
    private final RepasseRepository repasses;
    private final ContratoRepository contratos;
    private final TaxaServicoService taxas;
    private final GatewayPagamentoService gateway;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    // ------------------------------------------------------------------
    // Criação (idempotente)
    // ------------------------------------------------------------------

    @Transactional
    public PagamentoResponse criar(Integer viewerId, CriarPagamentoRequest req, String ip) {
        Contrato contrato = contratos.findById(req.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        if (contrato.getStatus() != StatusContrato.ativo) {
            throw new BusinessException("Pagamento exige contrato ativo");
        }
        String chave = (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank())
                ? "pay-" + req.getContratoId() + "-" + UUID.randomUUID()
                : req.getIdempotencyKey().trim();
        Optional<Pagamento> existente = pagamentos.findByIdempotencyKey(chave);
        if (existente.isPresent()) {
            return mapear(existente.get());
        }
        BigDecimal bruto = req.getValorBruto();
        TaxaServico taxa = taxas.vigente().orElse(null);
        BigDecimal perc = taxa == null || taxa.getPercentual() == null ? BigDecimal.ZERO : taxa.getPercentual();
        BigDecimal fixo = taxa == null || taxa.getValorFixo() == null ? BigDecimal.ZERO : taxa.getValorFixo();
        BigDecimal valorTaxa = bruto.multiply(perc)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .add(fixo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal liquido = bruto.subtract(valorTaxa);
        if (liquido.signum() < 0) {
            throw new BusinessException("Taxa supera o valor bruto");
        }
        Pagamento p = new Pagamento();
        p.setContratoId(contrato.getId());
        p.setPagadorId(viewerId);
        p.setTaxaId(taxa == null ? null : taxa.getId());
        p.setValorBruto(bruto);
        p.setValorTaxa(valorTaxa);
        p.setValorLiquido(liquido);
        p.setMetodo(req.getMetodo());
        p.setStatus(StatusPagamento.pendente);
        p.setIdempotencyKey(chave);
        Pagamento salvo = pagamentos.save(p);
        auditoria.registrar(viewerId, "pagamento.criar", "pagamento", salvo.getId(), ip);
        return mapear(salvo);
    }

    // ------------------------------------------------------------------
    // Aprovação / recusa (gateway mock) / estorno
    // ------------------------------------------------------------------

    @Transactional
    public PagamentoResponse pagar(Integer viewerId, Integer pagamentoId, PagarRequest req, String ip) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        if (!p.getPagadorId().equals(viewerId) && !temPerfil(viewerId, FINANCEIRO)) {
            throw new ForbiddenException("Pagamento de outro usuário");
        }
        if (p.getStatus() != StatusPagamento.pendente) {
            throw new BusinessException("Pagamento já processado");
        }
        String token = req == null ? null : req.getGatewayToken();
        GatewayCobrancaResult cobranca = gateway.cobrar(
                new GatewayCobrancaRequest(p.getId(), p.getValorLiquido(), p.getMetodo(), token));
        if (cobranca.getStatus() == GatewayCobrancaResult.StatusCobranca.INDISPONIVEL) {
            throw new BusinessException(cobranca.getMotivo());
        }
        if (cobranca.getStatus() == GatewayCobrancaResult.StatusCobranca.RECUSADA) {
            p.setStatus(StatusPagamento.recusado);
            pagamentos.save(p);
            auditoria.registrar(viewerId, "pagamento.recusar", "pagamento", p.getId(), ip);
            return mapear(p);
        }
        p.setStatus(StatusPagamento.aprovado);
        p.setGatewayId(cobranca.getGatewayId());
        pagamentos.save(p);
        Contrato contrato = contratos.findById(p.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        Repasse r = new Repasse();
        r.setPagamentoId(p.getId());
        r.setProfissionalId(contrato.getProfissional().getId());
        r.setValor(p.getValorLiquido());
        r.setStatus(StatusRepasse.pendente);
        Repasse salvo = repasses.save(r);
        auditoria.registrar(viewerId, "pagamento.aprovar", "pagamento", p.getId(), ip);
        auditoria.registrar(viewerId, "repasse.criar", "repasse", salvo.getId(), ip);
        return mapear(p);
    }

    @Transactional
    public PagamentoResponse estornar(Integer viewerId, Integer pagamentoId, EstornarRequest req, String ip) {
        exigirPerfis(viewerId, FINANCEIRO, "Estorno restrito à equipe financeira");
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        if (p.getStatus() != StatusPagamento.aprovado) {
            throw new BusinessException("Só pagamento aprovado pode ser estornado");
        }
        p.setStatus(StatusPagamento.estornado);
        pagamentos.save(p);
        repasses.findByPagamentoId(p.getId()).ifPresent(r -> {
            if (r.getStatus() == StatusRepasse.pendente) {
                r.setStatus(StatusRepasse.falha);
                repasses.save(r);
            }
        });
        auditoria.registrar(viewerId, "pagamento.estornar", "pagamento", p.getId(), ip);
        return mapear(p);
    }

    // ------------------------------------------------------------------
    // Leitura com escopo por papel
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Integer viewerId, String status) {
        StatusPagamento st = parseStatus(status);
        List<String> perfis = usuarioService.perfisDe(viewerId);
        List<Pagamento> base;
        if (perfis.stream().anyMatch(FINANCEIRO::contains)) {
            base = pagamentos.findAll();
        } else if (perfis.contains("profissional")) {
            base = pagamentos.findByContratoProfissional(viewerId);
        } else {
            base = pagamentos.findByPagadorId(viewerId);
        }
        return base.stream()
                .filter(p -> st == null || p.getStatus() == st)
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorId(Integer viewerId, Integer pagamentoId) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        Contrato contrato = contratos.findById(p.getContratoId()).orElse(null);
        boolean dono = p.getPagadorId().equals(viewerId)
                || (contrato != null && contrato.getProfissional().getId().equals(viewerId));
        if (!dono && !temPerfil(viewerId, FINANCEIRO)) {
            throw new ForbiddenException("Acesso negado ao pagamento");
        }
        return mapear(p);
    }

    @Transactional(readOnly = true)
    public List<RepasseResponse> listarRepasses(Integer viewerId) {
        List<String> perfis = usuarioService.perfisDe(viewerId);
        List<Repasse> base;
        if (perfis.stream().anyMatch(FINANCEIRO::contains)) {
            base = repasses.findAll();
        } else if (perfis.contains("profissional")) {
            base = repasses.findByProfissionalId(viewerId);
        } else {
            List<Integer> ids = pagamentos.findByPagadorId(viewerId).stream()
                    .map(Pagamento::getId).toList();
            base = ids.isEmpty() ? List.of() : repasses.findAllById(ids);
        }
        return base.stream().map(this::mapear).toList();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private boolean temPerfil(Integer viewerId, Set<String> permitidos) {
        return usuarioService.perfisDe(viewerId).stream().anyMatch(permitidos::contains);
    }

    private void exigirPerfis(Integer viewerId, Set<String> permitidos, String mensagem) {
        if (!temPerfil(viewerId, permitidos)) {
            throw new ForbiddenException(mensagem);
        }
    }

    private StatusPagamento parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusPagamento.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + status);
        }
    }

    private PagamentoResponse mapear(Pagamento p) {
        return new PagamentoResponse(
                p.getId(), p.getContratoId(), p.getPagadorId(), p.getTaxaId(),
                p.getValorBruto(), p.getValorTaxa(), p.getValorLiquido(),
                p.getMetodo(), p.getStatus(), p.getGatewayId(), p.getIdempotencyKey(),
                p.getCriadoEm(), p.getAtualizadoEm());
    }

    private RepasseResponse mapear(Repasse r) {
        return new RepasseResponse(
                r.getId(), r.getPagamentoId(), r.getProfissionalId(),
                r.getValor(), r.getStatus(), r.getProcessadoEm());
    }
}
