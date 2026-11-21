package com.elora.module.pagamento.controller;

import com.elora.common.dto.ApiResponse;
import com.elora.module.pagamento.dto.AtualizarTaxaRequest;
import com.elora.module.pagamento.dto.CancelarPagamentoRequest;
import com.elora.module.pagamento.dto.CobrancaResponse;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.dto.CriarTaxaRequest;
import com.elora.module.pagamento.dto.EstornarRequest;
import com.elora.module.pagamento.dto.PagarRequest;
import com.elora.module.pagamento.dto.PagamentoResponse;
import com.elora.module.pagamento.dto.PagamentoStatusResponse;
import com.elora.module.pagamento.dto.RepasseResponse;
import com.elora.module.pagamento.dto.TaxaResponse;
import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.service.PagamentoService;
import com.elora.module.pagamento.service.TaxaServicoService;
import com.elora.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Todas as respostas envolvidas em {@link ApiResponse} para o front (apiService.js)
 * fazer desembrulho automático.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentos;
    private final TaxaServicoService taxas;

    @PostMapping
    public ResponseEntity<ApiResponse<PagamentoResponse>> criar(
            Authentication authentication,
            @Valid @RequestBody CriarPagamentoRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Pagamento criado com sucesso",
                pagamentos.criar(SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PagamentoResponse>>> listar(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(pagamentos.listar(SecurityUtils.currentUserId(authentication), status)));
    }

    @GetMapping("/repasses")
    public ResponseEntity<ApiResponse<List<RepasseResponse>>> repasses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(pagamentos.listarRepasses(SecurityUtils.currentUserId(authentication))));
    }

    @GetMapping("/taxas")
    public ResponseEntity<ApiResponse<List<TaxaResponse>>> taxas(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(taxas.listar(SecurityUtils.currentUserId(authentication))));
    }

    @PostMapping("/taxas")
    public ResponseEntity<ApiResponse<TaxaResponse>> criarTaxa(
            Authentication authentication,
            @Valid @RequestBody CriarTaxaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Taxa criada com sucesso",
                taxas.criar(SecurityUtils.currentUserId(authentication), request, http.getRemoteAddr())));
    }

    @PutMapping("/taxas/{id}")
    public ResponseEntity<ApiResponse<TaxaResponse>> atualizarTaxa(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody AtualizarTaxaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Taxa atualizada com sucesso",
                taxas.atualizar(SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PagamentoResponse>> buscarPorId(
            Authentication authentication, @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(pagamentos.buscarPorId(SecurityUtils.currentUserId(authentication), id)));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<ApiResponse<PagamentoResponse>> pagar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody(required = false) PagarRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Pagamento processado",
                pagamentos.pagar(SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr())));
    }

    @PostMapping("/{id}/estornar")
    public ResponseEntity<ApiResponse<PagamentoResponse>> estornar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody(required = false) EstornarRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Pagamento estornado",
                pagamentos.estornar(SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr())));
    }

    // Cobrança: status, cancelamento, PIX e boleto

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PagamentoStatusResponse>> status(
            Authentication authentication, @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(pagamentos.status(SecurityUtils.currentUserId(authentication), id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PagamentoResponse>> cancelar(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody(required = false) CancelarPagamentoRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Pagamento cancelado",
                pagamentos.cancelar(SecurityUtils.currentUserId(authentication), id, request, http.getRemoteAddr())));
    }

    @PostMapping("/{id}/pix/qrcode")
    public ResponseEntity<ApiResponse<CobrancaResponse>> pix(
            Authentication authentication,
            @PathVariable Integer id,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("QR Code PIX gerado",
                pagamentos.gerarCobranca(SecurityUtils.currentUserId(authentication), id,
                        MetodoPagamento.pix, http.getRemoteAddr())));
    }

    @PostMapping("/{id}/boleto")
    public ResponseEntity<ApiResponse<CobrancaResponse>> boleto(
            Authentication authentication,
            @PathVariable Integer id,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Boleto gerado",
                pagamentos.gerarCobranca(SecurityUtils.currentUserId(authentication), id,
                        MetodoPagamento.boleto, http.getRemoteAddr())));
    }
}