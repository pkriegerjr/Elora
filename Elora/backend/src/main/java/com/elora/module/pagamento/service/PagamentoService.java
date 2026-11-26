package com.elora.module.pagamento.service;

import com.elora.common.audit.AuditoriaService;
import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.common.exception.UnauthorizedException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.notificacao.service.AvisoSistemaService;
import com.elora.module.pagamento.dto.CancelarPagamentoRequest;
import com.elora.module.pagamento.dto.CobrancaResponse;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.dto.EstornarRequest;
import com.elora.module.pagamento.dto.PagarRequest;
import com.elora.module.pagamento.dto.PagamentoResponse;
import com.elora.module.pagamento.dto.PagamentoStatusResponse;
import com.elora.module.pagamento.dto.RepasseResponse;
import com.elora.module.pagamento.entity.Pagamento;
import com.elora.module.pagamento.entity.Repasse;
import com.elora.module.pagamento.entity.TaxaServico;
import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
import com.elora.module.pagamento.enums.StatusRepasse;
import com.elora.module.pagamento.gateway.GatewayCobrancaRequest;
import com.elora.module.pagamento.gateway.GatewayCobrancaResult;
import com.elora.module.pagamento.gateway.GatewayPagamentoService;
import com.elora.module.pagamento.repository.PagamentoRepository;
import com.elora.module.pagamento.repository.RepasseRepository;
import com.elora.module.usuario.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * REQ-007 (cliente paga à Elora) + REQ-011 (repasse = bruto − taxa) +
 * RNF-011 (idempotência por {@code idempotency_key}).
 *
 * <p>Gateway selecionável por {@code app.integracoes.pagamento.provider}:
 * {@code mock} (default, determinístico) ou {@code asaas} (PIX/boleto/cartão).
 * PIX e boleto voltam {@code PENDENTE} e só viram {@code aprovado} no webhook;
 * cartão usa checkout hospedado (nenhum dado de cartão passa pela Elora).</p>
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
    private final AvisoSistemaService avisos;

    @Value("${app.integracoes.pagamento.provider:mock}")
    private String provider;

    @Value("${app.integracoes.pagamento.asaas.webhook-token:}")
    private String webhookToken;

    @Value("${app.webapp.base-url:http://localhost:8000}")
    private String webappBaseUrl;

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
        BigDecimal perc = taxa == null || taxa.getPercentual() == null
                ? BigDecimal.ZERO : taxa.getPercentual();
        BigDecimal fixo = taxa == null || taxa.getValorFixo() == null
                ? BigDecimal.ZERO : taxa.getValorFixo();
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
        avisarFinanceiro(salvo, "Pagamento pendente", "está aguardando confirmação.");
        avisarUsuario(p.getPagadorId(), salvo, "Pagamento pendente", "está aguardando confirmação.");
        return mapear(salvo);
    }

    // ------------------------------------------------------------------
    // Cobrança / aprovação / recusa / estorno / cancelamento
    // ------------------------------------------------------------------

    @Transactional
    public PagamentoResponse pagar(Integer viewerId, Integer pagamentoId, PagarRequest req, String ip) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        exigirDonoOuFinanceiro(p, viewerId, "Pagamento de outro usuário");
        if (p.getStatus() != StatusPagamento.pendente) {
            throw new BusinessException("Pagamento já processado");
        }
        Contrato contrato = contratos.findById(p.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        String token = req == null ? null : req.getGatewayToken();
        GatewayCobrancaResult cobranca = gateway.cobrar(montarCobranca(p, contrato, token));
        return aplicarCobranca(p, cobranca, viewerId, ip);
    }

    @Transactional
    public CobrancaResponse gerarCobranca(Integer viewerId, Integer pagamentoId,
                                          MetodoPagamento metodo, String ip) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        exigirDonoOuFinanceiro(p, viewerId, "Pagamento de outro usuário");
        if (p.getStatus() != StatusPagamento.pendente) {
            throw new BusinessException("Pagamento não está pendente");
        }
        if (p.getMetodo() != metodo) {
            throw new BusinessException("Método do pagamento é " + p.getMetodo());
        }
        Contrato contrato = contratos.findById(p.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));

        GatewayCobrancaResult cobranca = (p.getGatewayId() == null || p.getGatewayId().isBlank())
                ? gateway.cobrar(montarCobranca(p, contrato, null))
                : gateway.consultar(p.getGatewayId());

        if (cobranca.getStatus() == GatewayCobrancaResult.StatusCobranca.INDISPONIVEL
                || cobranca.getStatus() == GatewayCobrancaResult.StatusCobranca.RECUSADA) {
            throw new BusinessException(cobranca.getMotivo());
        }
        aplicarCobranca(p, cobranca, viewerId, ip);
        return mapearCobranca(p, cobranca);
    }

    @Transactional
    public PagamentoResponse cancelar(Integer viewerId, Integer pagamentoId,
                                      CancelarPagamentoRequest req, String ip) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        exigirDonoOuFinanceiro(p, viewerId, "Pagamento de outro usuário");
        if (p.getStatus() == StatusPagamento.aprovado) {
            throw new BusinessException("Pagamento aprovado deve ser estornado pelo financeiro");
        }
        if (p.getStatus() != StatusPagamento.pendente) {
            throw new BusinessException("Pagamento já encerrado");
        }
        if (p.getGatewayId() != null && !p.getGatewayId().isBlank()
                && !gateway.cancelar(p.getGatewayId())) {
            throw new BusinessException("Gateway não permitiu cancelar a cobrança");
        }
        // schema v2 não tem 'cancelado': cancelamento fica como recusado
        p.setStatus(StatusPagamento.recusado);
        pagamentos.save(p);
        String motivo = req == null || req.getMotivo() == null ? "" : " (" + req.getMotivo() + ")";
        auditoria.registrar(viewerId, "pagamento.cancelar" + motivo, "pagamento", p.getId(), ip);
        avisarFinanceiro(p, "Pagamento recusado", "foi cancelado pelo usuário.");
        avisarUsuario(p.getPagadorId(), p, "Pagamento recusado", "foi cancelado.");
        return mapear(p);
    }

    @Transactional
    public PagamentoResponse estornar(Integer viewerId, Integer pagamentoId,
                                      EstornarRequest req, String ip) {
        exigirPerfis(viewerId, FINANCEIRO, "Estorno restrito à equipe financeira");
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        if (p.getStatus() != StatusPagamento.aprovado) {
            throw new BusinessException("Só pagamento aprovado pode ser estornado");
        }
        if (p.getGatewayId() != null && !p.getGatewayId().isBlank()) {
            GatewayCobrancaResult estorno = gateway.estornar(p.getGatewayId());
            if (estorno.getStatus() == GatewayCobrancaResult.StatusCobranca.INDISPONIVEL) {
                throw new BusinessException(estorno.getMotivo());
            }
            if (estorno.getStatus() == GatewayCobrancaResult.StatusCobranca.RECUSADA) {
                throw new BusinessException("Estorno recusado: " + estorno.getMotivo());
            }
        }
        marcarEstornado(p, viewerId, ip);
        return mapear(p);
    }

    // ------------------------------------------------------------------
    // Webhook do gateway
    // ------------------------------------------------------------------

    @Transactional
    public boolean processarWebhook(String tokenRecebido, JsonNode body) {
        validarWebhook(tokenRecebido);
        String evento = body.path("event").asText("");
        String gatewayId = body.path("payment").path("id").asText("");
        if (gatewayId.isBlank()) {
            return false;
        }
        Pagamento p = pagamentos.findByGatewayId(gatewayId).orElse(null);
        if (p == null) {
            return false;
        }
        switch (evento) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> {
                aprovar(p, p.getPagadorId(), "webhook");
                return true;
            }
            case "PAYMENT_REFUNDED", "PAYMENT_PARTIALLY_REFUNDED" -> {
                marcarEstornado(p, p.getPagadorId(), "webhook");
                return true;
            }
            default -> {
                return false;
            }
        }
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
    public PagamentoStatusResponse status(Integer viewerId, Integer pagamentoId) {
        Pagamento p = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        Contrato contrato = contratos.findById(p.getContratoId()).orElse(null);
        boolean dono = p.getPagadorId().equals(viewerId)
                || (contrato != null && contrato.getProfissional().getId().equals(viewerId));
        if (!dono && !temPerfil(viewerId, FINANCEIRO)) {
            throw new ForbiddenException("Acesso negado ao pagamento");
        }
        boolean pendente = p.getStatus() == StatusPagamento.pendente;
        return new PagamentoStatusResponse(p.getId(), p.getStatus(), statusLabel(p.getStatus()),
                pendente, pendente, p.getCriadoEm(), p.getAtualizadoEm());
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
    // Internos: cobrança
    // ------------------------------------------------------------------

    private GatewayCobrancaRequest montarCobranca(Pagamento p, Contrato c, String token) {
        // valorBruto: o cliente paga o valor cheio; a taxa fica com a Elora
        GatewayCobrancaRequest req = new GatewayCobrancaRequest(
                p.getId(), p.getValorBruto(), p.getMetodo(), token);
        req.setDescricao(c.getTitulo() + " - " + c.getCodigo());
        req.setClienteNome(c.getCliente().getNome());
        req.setClienteEmail(c.getCliente().getEmail());
        req.setClienteCpfCnpj(c.getCliente().getCpf());
        req.setVencimento(LocalDate.now()
                .plusDays(p.getMetodo() == MetodoPagamento.boleto ? 3 : 1));
        req.setParcelas(1);
        req.setUrlRetorno(webappBaseUrl
                + "/src/pages/financeiro/pagamento.html?pagamentoId=" + p.getId());
        return req;
    }

    private PagamentoResponse aplicarCobranca(Pagamento p, GatewayCobrancaResult c,
                                              Integer atorId, String ip) {
        switch (c.getStatus()) {
            case INDISPONIVEL -> throw new BusinessException(c.getMotivo());
            case RECUSADA -> {
                p.setStatus(StatusPagamento.recusado);
                pagamentos.save(p);
                auditoria.registrar(atorId, "pagamento.recusar", "pagamento", p.getId(), ip);
                avisarFinanceiro(p, "Pagamento recusado", "foi recusado pelo gateway.");
                avisarUsuario(p.getPagadorId(), p, "Pagamento recusado", "foi recusado pelo gateway.");
                return mapear(p);
            }
            case PENDENTE -> {
                atualizarGatewayId(p, c.getGatewayId());
                pagamentos.save(p);
                auditoria.registrar(atorId, "pagamento.aguardar", "pagamento", p.getId(), ip);
                return mapear(p);
            }
            case APROVADA -> {
                atualizarGatewayId(p, c.getGatewayId());
                return aprovar(p, atorId, ip);
            }
            default -> throw new BusinessException("Status de cobrança desconhecido");
        }
    }

    private void atualizarGatewayId(Pagamento p, String gatewayId) {
        if (gatewayId != null && !gatewayId.isBlank()) {
            p.setGatewayId(gatewayId);
        }
    }

    private PagamentoResponse aprovar(Pagamento p, Integer atorId, String ip) {
        boolean novo = p.getStatus() != StatusPagamento.aprovado;
        p.setStatus(StatusPagamento.aprovado);
        pagamentos.save(p);
        if (novo) {
            garantirRepasse(p, ip);
            auditoria.registrar(atorId, "pagamento.aprovar", "pagamento", p.getId(), ip);
            avisarUsuario(p.getPagadorId(), p, "Pagamento confirmado", "foi confirmado.");
        }
        return mapear(p);
    }

    private void garantirRepasse(Pagamento p, String ip) {
        if (repasses.findByPagamentoId(p.getId()).isPresent()) {
            return;
        }
        Contrato contrato = contratos.findById(p.getContratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado"));
        Repasse r = new Repasse();
        r.setPagamentoId(p.getId());
        r.setProfissionalId(contrato.getProfissional().getId());
        r.setValor(p.getValorLiquido());
        r.setStatus(StatusRepasse.pendente);
        Repasse salvo = repasses.save(r);
        auditoria.registrar(p.getPagadorId(), "repasse.criar", "repasse", salvo.getId(), ip);
        avisarUsuario(contrato.getProfissional().getId(), p, "Repasse pendente",
                "um repasse foi criado e está pendente.");
    }

    private void marcarEstornado(Pagamento p, Integer atorId, String ip) {
        if (p.getStatus() == StatusPagamento.estornado) {
            return;
        }
        p.setStatus(StatusPagamento.estornado);
        pagamentos.save(p);
        Repasse repasse = repasses.findByPagamentoId(p.getId()).orElse(null);
        boolean bloqueouRepasse = repasse != null && repasse.getStatus() == StatusRepasse.pendente;
        if (bloqueouRepasse) {
            repasse.setStatus(StatusRepasse.falha);
            repasses.save(repasse);
        }
        auditoria.registrar(atorId, "pagamento.estornar", "pagamento", p.getId(), ip);
        avisarFinanceiro(p, "Pagamento estornado", "foi estornado e o repasse foi bloqueado.");
        avisarUsuario(p.getPagadorId(), p, "Pagamento estornado", "foi estornado.");
        if (bloqueouRepasse) {
            Contrato contrato = contratos.findById(p.getContratoId()).orElse(null);
            if (contrato != null) {
                avisarUsuario(contrato.getProfissional().getId(), p, "Repasse bloqueado",
                        "o repasse foi bloqueado após o estorno.");
            }
        }
    }

    private void avisarFinanceiro(Pagamento p, String titulo, String descricao) {
        avisos.notificarSeguro(AvisoSistemaService.FINANCEIRO, titulo,
                "Pagamento #" + p.getId() + " do contrato " + p.getContratoId() + " " + descricao,
                "pagamento", p.getId());
    }

    private void avisarUsuario(Integer usuarioId, Pagamento p, String titulo, String descricao) {
        avisos.notificarSeguroUsuario(usuarioId, titulo,
                "Pagamento #" + p.getId() + " do contrato " + p.getContratoId() + " " + descricao,
                "pagamento", p.getId());
    }

    private void validarWebhook(String tokenRecebido) {
        if (webhookToken == null || webhookToken.isBlank()) {
            if ("mock".equalsIgnoreCase(provider)) {
                return;
            }
            throw new UnauthorizedException("Webhook sem token configurado");
        }
        byte[] esperado = webhookToken.getBytes(StandardCharsets.UTF_8);
        byte[] recebido = (tokenRecebido == null ? "" : tokenRecebido)
                .getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, recebido)) {
            throw new UnauthorizedException("Webhook não autorizado");
        }
    }

    // ------------------------------------------------------------------
    // Internos: autorização e mapeamento
    // ------------------------------------------------------------------

    private void exigirDonoOuFinanceiro(Pagamento p, Integer viewerId, String mensagem) {
        if (!p.getPagadorId().equals(viewerId) && !temPerfil(viewerId, FINANCEIRO)) {
            throw new ForbiddenException(mensagem);
        }
    }

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

    private String statusLabel(StatusPagamento status) {
        return switch (status) {
            case pendente -> "Aguardando Pagamento";
            case aprovado -> "Aprovado";
            case recusado -> "Recusado";
            case estornado -> "Estornado";
        };
    }

    private CobrancaResponse mapearCobranca(Pagamento p, GatewayCobrancaResult c) {
        CobrancaResponse r = new CobrancaResponse();
        r.setPagamentoId(p.getId());
        r.setStatus(p.getStatus());
        r.setMetodo(p.getMetodo());
        r.setGatewayId(p.getGatewayId());
        r.setQrCode(c.getPixCopiaECola());
        r.setCopiaEcola(c.getPixCopiaECola());
        r.setQrCodeBase64(c.getQrCodeBase64());
        r.setLinhaDigitavel(c.getLinhaDigitavel());
        r.setPdfUrl(c.getBoletoUrl());
        r.setCheckoutUrl(c.getCheckoutUrl());
        r.setExpiresAt(c.getExpiraEm());
        return r;
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