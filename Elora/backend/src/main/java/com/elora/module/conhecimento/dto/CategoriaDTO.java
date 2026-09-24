import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class CategoriaRequestDTO {
    @notblank @Size(max=255) String nomeCategoria; String descricao;

@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class CategoriaResponseDTO {
    Long idCategoria; String nomeCategoria; String descricao; LocalDateTime createdAt;

}
}