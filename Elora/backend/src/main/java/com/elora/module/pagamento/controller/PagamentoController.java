package com.elora.module.pagamento.controller;

import com.elora.module.pagamento.dto.AtualizarTaxaRequest;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.dto.CriarTaxaRequest;
import com.elora.module.pagamento.dto.EstornarRequest;
import com.elora.module.pagamento.dto.PagarRequest;
import com.elora.module.pagamento.dto.PagamentoResponse;
import com.elora.module.pagamento.dto.RepasseResponse;
import com.elora.module.pagamento.dto.TaxaResponse;
import com.elora.module.pagamento.service.PagamentoService;
import com.elora.module.pagamento.service.TaxaServicoService;
import com.elora.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Base {@code /payments} (compatível com {@code CONFIG.ENDPOINTS.payments} do front).
 * Rotas exatas antes de {@code /{id}} (mesmo padrão do /search de cuidadores).
 * Papéis (docs): cliente paga/cancela os próprios; profissional vê repasses e
 * histórico próprio; financeiro/admin gerenciam taxas, repasses e estornos.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentos;
    private final TaxaServicoService taxas;

    @PostMapping
    public ResponseEntity<PagamentoResponse> criar(
            Authentication authentication,
            @Valid @RequestBody CriarPagamentoRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentos.criar(
                SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr()));
    }

    @GetMapping
    public List<PagamentoResponse> listar(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return pagamentos.listar(SecurityUtils.currentUserId(authentication), status);
    }

    @GetMapping("/repasses")
    public List<RepasseResponse> repasses(Authentication authentication) {
        return pagamentos.listarRepasses(SecurityUtils.currentUserId(authentication));
    }

    @GetMapping("/taxas")
    public List<TaxaResponse> taxas(Authentication authentication) {
        return taxas.listar(SecurityUtils.currentUserId(authentication));
    }

    @PostMapping("/taxas")
    public ResponseEntity<TaxaResponse> criarTaxa(
            Authentication authentication,
            @Valid @RequestBody CriarTaxaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxas.criar(
                SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr()));
    }

    @PutMapping("/taxas/{id}")
    public TaxaResponse atualizarTaxa(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody AtualizarTaxaRequest request,
            HttpServletRequest http) {
        return taxas.atualizar(
                SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public PagamentoResponse buscarPorId(
            Authentication authentication, @PathVariable Integer id) {
        return pagamentos.buscarPorId(SecurityUtils.currentUserId(authentication), id);
    }

    @PostMapping("/{id}/pagar")
    public PagamentoResponse pagar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody(required = false) PagarRequest request,
            HttpServletRequest http) {
        return pagamentos.pagar(
                SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr());
    }

    @PostMapping("/{id}/estornar")
    public PagamentoResponse estornar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody(required = false) EstornarRequest request,
            HttpServletRequest http) {
        return pagamentos.estornar(
                SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr());
    }
}
