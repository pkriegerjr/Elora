package com.elora.module.pagamento.service;

import com.elora.common.audit.AuditoriaService;
import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.pagamento.dto.AtualizarTaxaRequest;
import com.elora.module.pagamento.dto.CriarTaxaRequest;
import com.elora.module.pagamento.dto.TaxaResponse;
import com.elora.module.pagamento.entity.TaxaServico;
import com.elora.module.pagamento.repository.TaxaServicoRepository;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REQ-005/012: taxas da Elora (percentual + fixo, com vigência).
 * Sem delete físico — desativar via {@code ativo=false}.
 */
@Service
@RequiredArgsConstructor
public class TaxaServicoService {

    private static final Set<String> FINANCEIRO = Set.of("admin", "financeiro");

    private final TaxaServicoRepository taxas;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    /** Taxa vigente hoje (maior {@code vigente_de}); vazia quando nenhuma cobre a data. */
    @Transactional(readOnly = true)
    public Optional<TaxaServico> vigente() {
        LocalDate hoje = LocalDate.now();
        return taxas.findByAtivoTrueOrderByVigenteDeDesc().stream()
                .filter(t -> !t.getVigenteDe().isAfter(hoje)
                        && (t.getVigenteAte() == null || !t.getVigenteAte().isBefore(hoje)))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<TaxaResponse> listar(Integer viewerId) {
        exigirFinanceiro(viewerId);
        return taxas.findAll().stream().map(this::mapear).toList();
    }

    @Transactional
    public TaxaResponse criar(Integer viewerId, CriarTaxaRequest req, String ip) {
        exigirFinanceiro(viewerId);
        TaxaServico t = new TaxaServico();
        t.setNome(req.getNome().trim());
        t.setPercentual(req.getPercentual());
        t.setValorFixo(req.getValorFixo() == null ? BigDecimal.ZERO : req.getValorFixo());
        t.setVigenteDe(req.getVigenteDe());
        t.setVigenteAte(req.getVigenteAte());
        t.setAtivo(true);
        t.setCriadoPor(viewerId);
        TaxaServico salva = taxas.save(t);
        auditoria.registrar(viewerId, "taxa.criar", "taxa_servico", salva.getId(), ip);
        return mapear(salva);
    }

    @Transactional
    public TaxaResponse atualizar(Integer viewerId, Integer taxaId, AtualizarTaxaRequest req, String ip) {
        exigirFinanceiro(viewerId);
        TaxaServico t = taxas.findById(taxaId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxa não encontrada"));
        if (req.getNome() != null && !req.getNome().isBlank()) {
            t.setNome(req.getNome().trim());
        }
        if (req.getPercentual() != null) {
            t.setPercentual(req.getPercentual());
        }
        if (req.getValorFixo() != null) {
            t.setValorFixo(req.getValorFixo());
        }
        if (req.getVigenteAte() != null) {
            t.setVigenteAte(req.getVigenteAte());
        }
        if (req.getAtivo() != null) {
            t.setAtivo(req.getAtivo());
        }
        TaxaServico salva = taxas.save(t);
        auditoria.registrar(viewerId, "taxa.atualizar", "taxa_servico", salva.getId(), ip);
        return mapear(salva);
    }

    private void exigirFinanceiro(Integer viewerId) {
        boolean ok = usuarioService.perfisDe(viewerId).stream().anyMatch(FINANCEIRO::contains);
        if (!ok) {
            throw new ForbiddenException("Taxas restritas à equipe financeira");
        }
    }

    private TaxaResponse mapear(TaxaServico t) {
        return new TaxaResponse(
                t.getId(), t.getNome(), t.getPercentual(), t.getValorFixo(),
                t.getVigenteDe(), t.getVigenteAte(), t.getAtivo(),
                t.getCriadoPor(), t.getCriadoEm());
    }
}
