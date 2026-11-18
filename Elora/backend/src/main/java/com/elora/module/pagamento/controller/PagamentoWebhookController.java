@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PagamentoWebhookController {

    private final PagamentoService pagamentos;

    @PostMapping("/webhook")
    public Map<String, Object> webhook(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody JsonNode body) {
        boolean processado = pagamentos.processarWebhook(token, body);
        return Map.of("received", true, "processed", processado);
    }
}