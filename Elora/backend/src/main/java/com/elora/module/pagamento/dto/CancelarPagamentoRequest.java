// CancelarPagamentoRequest.java
@Data @NoArgsConstructor @AllArgsConstructor
public class CancelarPagamentoRequest {
    @JsonAlias("reason")
    private String motivo;
}