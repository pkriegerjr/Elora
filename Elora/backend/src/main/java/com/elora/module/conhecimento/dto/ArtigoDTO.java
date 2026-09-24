import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class ArtigoRequestDTO {
    @NotBlank String titulo; @NotBlank String conteudo; String categoria; Long categoriaId;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class ArtigoResposeDTO {Long idArtigo; String conteudo; String categoria; LocalDate dataPublicacao; Long categoriaId;}
