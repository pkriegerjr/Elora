// CobrancaResponse.java
@Data @NoArgsConstructor @AllArgsConstructor
public class CobrancaResponse {
    private Integer pagamentoId;
    private StatusPagamento status;
    private MetodoPagamento metodo;
    private String gatewayId;
    private String qrCode;
    private String qrCodeBase64;
    private String copiaEcola;
    private String linhaDigitavel;
    private String pdfUrl;
    private String checkoutUrl;
    private LocalDateTime expiresAt;
}