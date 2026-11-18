// PagamentoStatusResponse.java
@Data @NoArgsConstructor @AllArgsConstructor
public class PagamentoStatusResponse {
    private Integer pagamentoId;
    private StatusPagamento status;
    private String statusLabel;
    private boolean canPay;
    private boolean canCancel;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}