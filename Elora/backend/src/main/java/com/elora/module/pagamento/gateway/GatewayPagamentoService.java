package com.elora.module.pagamento.gateway;

/**
 * Fronteira da integração externa de cobrança (RNF-014).
 * Trocar de provedor (Stripe/Mercado Pago/PagSeguro) = nova implementação
 * desta interface (marcada {@code @Primary}) — o serviço não muda.
 */
public interface GatewayPagamentoService {

    GatewayCobrancaResult cobrar(GatewayCobrancaRequest request);

    GatewayCobrancaResult consultar(String gatewayId);

    GatewayCobrancaResult estornar(String gatewayId);

    boolean cancelar(String gatewayId);
}

@Service
@ConditionalOnProperty(name = "app.integracoes.pagamento.provider", havingValue = "asaas")
public class AsaasGatewayPagamentoService implements GatewayPagamentoService {

    private static final Logger log = LoggerFactory.getLogger(AsaasGatewayPagamentoService.class);

    private final RestClient rest;
    private final ObjectMapper mapper;

    public AsaasGatewayPagamentoService(
            @Value("${app.integracoes.pagamento.asaas.base-url:https://sandbox.asaas.com/api/v3}") String baseUrl,
            @Value("${app.integracoes.pagamento.asaas.api-key:}") String apiKey,
            @Value("${app.integracoes.pagamento.asaas.timeout-ms:10000}") int timeoutMs,
            ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "ASAAS_API_KEY nao configurada: use PAGAMENTO_PROVIDER=mock ou defina a chave");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("access_token", apiKey)
                .requestFactory(factory)
                .build();
        this.mapper = mapper;
    }

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "cobrarIndisponivel")
    public GatewayCobrancaResult cobrar(GatewayCobrancaRequest req) {
        try {
            String customerId = garantirCliente(req);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("customer", customerId);
            body.put("billingType", switch (req.getMetodo()) {
                case pix -> "PIX";
                case boleto -> "BOLETO";
                case cartao -> "UNDEFINED";
            });
            body.put("value", req.getValor());
            body.put("dueDate", (req.getVencimento() == null
                    ? LocalDate.now() : req.getVencimento()).toString());
            body.put("description", req.getDescricao());
            body.put("externalReference", "pagamento-" + req.getPagamentoId());

            JsonNode criado = rest.post().uri("/payments").body(body)
                    .retrieve().body(JsonNode.class);
            if (criado == null || criado.path("id").asText("").isBlank()) {
                return GatewayCobrancaResult.indisponivel();
            }
            String gatewayId = criado.path("id").asText();
            GatewayCobrancaResult r = mapearStatus(criado, gatewayId);

            if (req.getMetodo() == MetodoPagamento.pix) {
                aplicarPix(r, gatewayId);
            } else if (req.getMetodo() == MetodoPagamento.boleto) {
                r.setLinhaDigitavel(texto(criado, "identificationField"));
                r.setBoletoUrl(texto(criado, "bankSlipUrl"));
                r.setExpiraEm(parseData(texto(criado, "dueDate")));
            } else {
                r.setCheckoutUrl(texto(criado, "invoiceUrl"));
            }
            return r;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return GatewayCobrancaResult.recusada(mensagemErro(e));
            }
            throw e;
        } catch (IllegalArgumentException e) {
            return GatewayCobrancaResult.recusada(e.getMessage());
        }
    }

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "consultarIndisponivel")
    public GatewayCobrancaResult consultar(String gatewayId) {
        JsonNode p = rest.get().uri("/payments/{id}", gatewayId)
                .retrieve().body(JsonNode.class);
        GatewayCobrancaResult r = mapearStatus(p, gatewayId);
        if ("PIX".equalsIgnoreCase(texto(p, "billingType"))) {
            aplicarPix(r, gatewayId);
        }
        return r;
    }

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "estornarIndisponivel")
    public GatewayCobrancaResult estornar(String gatewayId) {
        try {
            rest.post().uri("/payments/{id}/refund", gatewayId)
                    .body(Map.of()).retrieve().body(JsonNode.class);
            return GatewayCobrancaResult.aprovada(gatewayId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return GatewayCobrancaResult.recusada(mensagemErro(e));
            }
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "gatewayPagamento", fallbackMethod = "cancelarIndisponivel")
    public boolean cancelar(String gatewayId) {
        try {
            rest.delete().uri("/payments/{id}", gatewayId).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            return false;
        }
    }

    private String garantirCliente(GatewayCobrancaRequest req) {
        String cpfCnpj = DocumentUtils.onlyDigits(req.getClienteCpfCnpj());
        if (cpfCnpj.length() != 11 && cpfCnpj.length() != 14) {
            throw new IllegalArgumentException("CPF/CNPJ do pagador invalido");
        }
        JsonNode lista = rest.get()
                .uri(b -> b.path("/customers").queryParam("cpfCnpj", cpfCnpj).build())
                .retrieve().body(JsonNode.class);
        if (lista != null && lista.path("data").isArray() && !lista.path("data").isEmpty()) {
            return lista.path("data").get(0).path("id").asText();
        }
        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("name", req.getClienteNome());
        cliente.put("email", req.getClienteEmail());
        cliente.put("cpfCnpj", cpfCnpj);
        cliente.put("externalReference", "elora-pagamento-" + req.getPagamentoId());
        JsonNode criado = rest.post().uri("/customers").body(cliente)
                .retrieve().body(JsonNode.class);
        String id = criado == null ? "" : criado.path("id").asText("");
        if (id.isBlank()) throw new IllegalStateException("Asaas nao retornou customer id");
        return id;
    }

    private void aplicarPix(GatewayCobrancaResult r, String gatewayId) {
        JsonNode qr = rest.get().uri("/payments/{id}/pixQrCode", gatewayId)
                .retrieve().body(JsonNode.class);
        if (qr == null) return;
        String encoded = texto(qr, "encodedImage");
        r.setQrCodeBase64(encoded == null || encoded.isBlank()
                ? null : "data:image/png;base64," + encoded);
        r.setPixCopiaECola(texto(qr, "payload"));
        r.setExpiraEm(parseData(texto(qr, "expirationDate")));
    }

    private GatewayCobrancaResult mapearStatus(JsonNode p, String gatewayId) {
        String status = texto(p, "status");
        if (status == null) return GatewayCobrancaResult.pendente(gatewayId);
        return switch (status.toUpperCase()) {
            case "CONFIRMED", "RECEIVED", "RECEIVED_IN_CASH" ->
                    GatewayCobrancaResult.aprovada(gatewayId);
            default -> GatewayCobrancaResult.pendente(gatewayId);
        };
    }

    private String mensagemErro(RestClientResponseException e) {
        try {
            JsonNode erros = mapper.readTree(e.getResponseBodyAsString()).path("errors");
            if (erros.isArray() && !erros.isEmpty()) {
                String d = erros.get(0).path("description").asText("");
                if (!d.isBlank()) return d;
            }
        } catch (Exception ignored) { }
        return "Gateway recusou a cobranca (HTTP " + e.getStatusCode().value() + ")";
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static LocalDateTime parseData(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try { return LocalDateTime.parse(valor); } catch (Exception ignored) { }
        try { return OffsetDateTime.parse(valor).toLocalDateTime(); } catch (Exception ignored) { }
        try { return LocalDate.parse(valor).atStartOfDay(); } catch (Exception ignored) { }
        try { return LocalDateTime.parse(valor.replace(' ', 'T')); } catch (Exception ignored) { return null; }
    }

    public GatewayCobrancaResult cobrarIndisponivel(GatewayCobrancaRequest r, Throwable t) {
        log.warn("Asaas indisponivel ao cobrar {}: {}", r.getPagamentoId(), t.toString());
        return GatewayCobrancaResult.indisponivel();
    }
    public GatewayCobrancaResult consultarIndisponivel(String id, Throwable t) {
        return GatewayCobrancaResult.indisponivel();
    }
    public GatewayCobrancaResult estornarIndisponivel(String id, Throwable t) {
        return GatewayCobrancaResult.indisponivel();
    }
    public boolean cancelarIndisponivel(String id, Throwable t) { return false; }
}
