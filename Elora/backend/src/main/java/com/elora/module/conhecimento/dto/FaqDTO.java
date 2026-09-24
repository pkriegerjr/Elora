import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FaqRequestDTO {
    @NotBlank String pergunta; @NotBlank String resposta;
    String categoria; String status; Long categoriaid;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FaqResponseDTO{
    Long idFaq; String pergunta; String resposta; String categoria; String status; Long categoriaid; String categoriaNome; LocalDateTime createdAt;
}